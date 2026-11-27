package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.CrowFlockPacket;
import com.sekwah.narutomod.util.CrowFlock;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CrowFlockRenderer {
    private static final int MAX_WAVES = 24;
    private static final int PRIVATE_DURATION = 13;
    private static final int PUBLIC_DURATION = CrowFlock.GATHER + CrowFlock.FLIGHT + CrowFlock.FADE;
    private static final Map<Integer, Sight> FLOCKS = new LinkedHashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);
    private static Sight phantom;

    private CrowFlockRenderer() {
    }

    public static void add(CrowFlockPacket packet) {
        Minecraft game = Minecraft.getInstance();
        ClientLevel level = game.level;
        useLevel(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (packet.privateVision()) {
            if (game.player != null && game.player.getId() == packet.viewer()) {
                phantom = new Sight(packet, level.getGameTime());
            }
        } else {
            Sight known = FLOCKS.get(packet.id());
            if (known == null && FLOCKS.size() >= MAX_WAVES) {
                FLOCKS.remove(FLOCKS.keySet().iterator().next());
            }
            long start = level.getGameTime() - packet.age();
            FLOCKS.put(packet.id(), new Sight(packet, known == null ? start : Math.min(start, known.start())));
        }
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers,
                     Vec3 camera, float partial) {
        useLevel(level);
        long now = level.getGameTime();
        FLOCKS.values().removeIf(flock -> now - flock.start() >= PUBLIC_DURATION);
        if (phantom != null && now - phantom.start() >= PRIVATE_DURATION) {
            phantom = null;
        }
        if (FLOCKS.isEmpty() && phantom == null) {
            return;
        }
        Minecraft game = Minecraft.getInstance();
        VertexConsumer translucent = buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE);
        boolean low = game.options.graphicsMode().get() == GraphicsStatus.FAST;
        for (Sight flock : FLOCKS.values()) {
            Vec3 origin = flock.packet().origin().subtract(camera);
            if (origin.lengthSqr() < 44 * 44) {
                flock(new Mesh(matrix, translucent), flock.packet(), origin, now - flock.start() + partial, low);
            }
        }
        if (phantom != null && game.getCameraEntity() == game.player && !game.player.isSpectator()
                && game.player.getId() == phantom.packet().viewer()) {
            echo(new Mesh(matrix, translucent), phantom.packet(), camera, now - phantom.start() + partial,
                    game.options.screenEffectScale().get().floatValue());
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static void flock(Mesh mesh, CrowFlockPacket packet, Vec3 origin, float age, boolean low) {
        int count = CrowFlock.birdBudget(low, origin.lengthSqr());
        for (int bird = 0; bird < count; bird++) {
            int lane = (bird + 3) % CrowFlock.LANES;
            float seed = ((packet.id() * 13 + bird * 17) & 255) / 255f;
            float local = age - bird / (float) CrowFlock.LANES * 0.72f;
            if (local <= 0) {
                continue;
            }
            Vec3 heading = CrowFlock.heading(packet.yaw(), lane);
            double limit = packet.limit(lane);
            double flight = CrowFlock.travel(local);
            float visibility = (float) Mth.clamp(1 - Math.max(0, (flight - limit + 0.1)) * 2.8, 0, 1);
            float fade = Mth.clamp((PUBLIC_DURATION - local) / CrowFlock.FADE, 0, 1);
            float unfold = Mth.clamp(local / CrowFlock.GATHER, 0, 1);
            Vec3 point = place(origin, heading, local, seed, Math.min(flight, Math.max(0, limit - 0.10)));
            float alpha = fade * visibility * unfold * (float) Mth.clamp((point.length() - 0.30) / 0.46, 0, 1);
            if (alpha > 0.015f) {
                var shape = new Bird(point, heading, local, seed, alpha, 0.13 + 0.14 * seed);
                drawBird(mesh, shape);
                if (!low && local > CrowFlock.GATHER && bird % 2 == 0) {
                    feather(mesh, shape);
                }
            }
        }
    }

    private static void feather(Mesh mesh, Bird bird) {
        double phase = (bird.age() * 0.14 + bird.seed()) % 1.0;
        Vec3 side = new Vec3(bird.heading().z, 0, -bird.heading().x);
        Vec3 point = bird.at().subtract(bird.heading().scale(0.25 + phase * 0.66))
                .add(side.scale(Math.sin(phase * 6.2) * 0.21)).add(0, -phase * 0.22, 0);
        Vec3 across = side.scale(0.020);
        Vec3 tail = point.subtract(bird.heading().scale(0.12)).add(0, -0.027, 0);
        mesh.quad(point, point.add(across), tail, point.subtract(across), 0x382C42,
                bird.alpha() * (float) Math.sin(Math.PI * phase) * 0.64f);
    }

    private static Vec3 place(Vec3 origin, Vec3 forward, float age, float seed, double distance) {
        Vec3 side = new Vec3(forward.z, 0, -forward.x);
        double width = Math.sin(age * 1.2 + seed * Math.PI * 2) * (age < CrowFlock.GATHER ? 0.39 : 0.15);
        double height = (seed - 0.52) * 0.36 + Math.sin(age * 0.8 + seed * 8) * 0.13;
        return origin.add(forward.scale(distance)).add(side.scale(width)).add(0, height, 0);
    }

    private static void drawBird(Mesh mesh, Bird bird) {
        Vec3 side = new Vec3(bird.heading().z, 0, -bird.heading().x);
        Vec3 beak = bird.at().add(bird.heading().scale(bird.scale() * 0.65));
        Vec3 tail = bird.at().subtract(bird.heading().scale(bird.scale() * 0.88));
        mesh.quad(beak, bird.at().add(side.scale(bird.scale() * 0.23)), tail,
                bird.at().subtract(side.scale(bird.scale() * 0.23)), 0x171422, bird.alpha());
        double flap = Math.sin(bird.age() * 1.84 + bird.seed() * 17);
        wing(mesh, bird, side, flap);
        wing(mesh, bird, side.reverse(), flap);
    }

    private static void wing(Mesh mesh, Bird bird, Vec3 side, double flap) {
        double size = bird.scale();
        Vec3 pivot = bird.at().add(side.scale(size * 0.14));
        Vec3 bend = pivot.add(side.scale(size * 0.62)).add(0, flap * size * 0.54, 0);
        Vec3 tip = bend.add(side.scale(size * 0.96)).add(bird.heading().scale(-size * 0.46))
                .add(0, size * (flap * 0.87 - 0.27), 0);
        Vec3 trail = bend.add(bird.heading().scale(-size * 0.83)).add(0, -size * 0.13, 0);
        Vec3 root = pivot.subtract(bird.heading().scale(size * 0.42));
        mesh.quad(pivot, bend, trail, root, 0x1A1523, bird.alpha() * 0.92f);
        mesh.quad(bend, tip, tip.subtract(bird.heading().scale(size * 0.28)), trail, 0x342B3D, bird.alpha() * 0.78f);
    }

    private static void echo(Mesh mesh, CrowFlockPacket packet, Vec3 camera, float age, float scale) {
        Vec3 chest = packet.origin().subtract(camera);
        float alpha = scale * Mth.clamp((PRIVATE_DURATION - age) / PRIVATE_DURATION, 0, 1) * 0.53f;
        if (alpha <= 0.01f || chest.lengthSqr() < 1.3) {
            return;
        }
        Vec3 facing = CrowFlock.heading(packet.yaw(), 3);
        Vec3 side = new Vec3(facing.z, 0, -facing.x);
        Vec3 head = chest.add(0, 0.28, 0);
        Vec3 hem = chest.add(0, -0.78, 0);
        for (int strip = -2; strip <= 2; strip++) {
            Vec3 shift = side.scale(strip * (0.073 + age * 0.015));
            Vec3 top = head.add(shift.scale(0.42));
            Vec3 bottom = hem.add(shift).add(0, Math.abs(strip) * 0.085 + age * 0.021, 0);
            Vec3 edge = side.scale(0.035 + Math.abs(strip) * 0.009);
            mesh.quad(top.subtract(edge), top.add(edge), bottom.add(edge), bottom.subtract(edge),
                    strip == 0 ? 0x1D1329 : 0x3D263E, alpha);
        }
    }

    private static void useLevel(ClientLevel level) {
        if (world.get() != level) {
            world = new WeakReference<>(level);
            FLOCKS.clear();
            phantom = null;
        }
    }

    private record Sight(CrowFlockPacket packet, long start) {
    }

    private record Bird(Vec3 at, Vec3 heading, float age, float seed, float alpha, double scale) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer vertices) {
        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int rgb, float alpha) {
            vertex(a, rgb, alpha);
            vertex(b, rgb, alpha);
            vertex(c, rgb, alpha * 0.82f);
            vertex(d, rgb, alpha * 0.88f);
        }

        private void vertex(Vec3 point, int rgb, float alpha) {
            vertices.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, Math.round(alpha * 255)).endVertex();
        }
    }
}
