package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.KaitenPacket;
import com.sekwah.narutomod.util.KaitenDome;
import com.sekwah.narutomod.util.KaitenDome.Step;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

public final class KaitenRenderer {
    private static final Map<Integer, Dome> DOMES = new LinkedHashMap<>();
    private static final Tint SILK = new Tint(94, 164, 227);
    private static final Tint BRAID = new Tint(142, 226, 251);
    private static final Tint FLASH = new Tint(239, 254, 255);
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private KaitenRenderer() {
    }

    public static void receive(KaitenPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        checkWorld(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        long now = level.getGameTime();
        Dome prev = DOMES.get(packet.entity());
        switch (packet.step()) {
            case GATHER -> {
                if (prev == null && DOMES.size() >= 20) {
                    DOMES.remove(DOMES.keySet().iterator().next());
                }
                DOMES.put(packet.entity(), new Dome(now, packet.center(), Step.GATHER, now));
            }
            case ACTIVE -> {
                if (prev != null && prev.phase != Step.COLLAPSE) {
                    if (prev.phase == Step.GATHER) {
                        prev.since = now - KaitenDome.WINDUP;
                    }
                    prev.phase = Step.ACTIVE;
                    prev.updated = now;
                } else if (DOMES.size() < 20) {
                    DOMES.put(packet.entity(), new Dome(now - KaitenDome.WINDUP, packet.center(), Step.ACTIVE, now));
                }
            }
            case CONTACT -> {
                if (prev != null) {
                    prev.hit = packet.contact().subtract(packet.center());
                    prev.hitTick = now;
                }
            }
            case COLLAPSE -> {
                if (prev != null) {
                    prev.phase = Step.COLLAPSE;
                    prev.updated = now;
                }
            }
        }
    }

    public static float spin(LivingEntity entity, float partial) {
        ClientLevel level = world.get();
        Dome dome = level == entity.level() ? DOMES.get(entity.getId()) : null;
        if (dome == null) {
            return 0;
        }
        float age = level.getGameTime() - dome.since + partial;
        if (dome.phase == Step.GATHER) {
            return -17 * Mth.sin(Mth.clamp(age / KaitenDome.WINDUP, 0, 1) * Mth.PI);
        }
        if (dome.phase == Step.COLLAPSE) {
            float last = Mth.wrapDegrees((dome.updated - dome.since - KaitenDome.WINDUP) * 86f);
            return last * (1 - Mth.clamp((level.getGameTime() - dome.updated + partial) / KaitenDome.COLLAPSE, 0, 1));
        }
        return Math.max(0, age - KaitenDome.WINDUP) * 86;
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        checkWorld(level);
        double now = level.getGameTime() + partial;
        DOMES.values().removeIf(dome -> now - dome.updated > (dome.phase == Step.COLLAPSE ? KaitenDome.COLLAPSE : 11));
        if (DOMES.isEmpty()) {
            return;
        }
        var buffer = buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE);
        boolean fast = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        for (Dome dome : DOMES.values()) {
            Vec3 offset = dome.center.subtract(camera);
            if (offset.lengthSqr() < 39 * 39) {
                drawDome(new Mesh(matrix, buffer, offset, 1), dome, now, fast || offset.lengthSqr() > 24 * 24);
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static void drawDome(Mesh mesh, Dome dome, double now, boolean low) {
        double age = now - dome.since;
        if (dome.phase == Step.GATHER) {
            drawGather(mesh, Math.min(1, age / KaitenDome.WINDUP), low);
            return;
        }
        float fade = dome.phase == Step.COLLAPSE ? (float) Mth.clamp(1 - (now - dome.updated) / KaitenDome.COLLAPSE, 0, 1) : 1;
        float height = fade * fade;
        mesh = new Mesh(mesh.matrix(), mesh.buffer(), mesh.center(), height);
        double yaw = age * 0.56;
        int segments = low ? 20 : 32;
        int rows = low ? 4 : 8;
        for (int y = 0; y < rows; y++) {
            double height0 = y * Math.PI * 0.5 / rows;
            double height1 = (y + 1) * Math.PI * 0.5 / rows;
            for (int i = 0; i < segments; i++) {
                double angle0 = yaw + i * Math.PI * 2 / segments;
                double angle1 = yaw + (i + 1) * Math.PI * 2 / segments;
                double phase = i * Math.PI * 6 / segments + y * 1.07 + age * 0.47;
                mesh.patch(angle0, angle1, height0, height1, SILK, (int) ((19 + 11 * Math.cos(phase)) * fade));
                if (i % (low ? 3 : 2) == 0) {
                    mesh.patch(angle0, angle1, height0, height0 + 0.013, BRAID, (int) ((41 + 75 * Math.pow(Math.max(0, Math.sin(phase)), 2)) * fade));
                }
            }
        }
        mesh.braid(yaw, segments, height, fade);
        float hit = (float) (1 - (now - dome.hitTick) / 3.3);
        if (dome.hit != null && hit > 0 && fade > 0.97f) {
            mesh.flare(dome.hit, hit);
        }
    }

    private static void drawGather(Mesh mesh, double progress, boolean low) {
        int count = low ? 12 : 22;
        for (int i = 0; i < count; i++) {
            double a = i * Math.PI * 2 / count + progress * 0.76;
            double b = a + Math.PI / count * (i % 2 == 0 ? 1.5 : 0.74);
            double scale = 0.25 + progress * 0.55;
            Vec3 p = KaitenDome.surface(a, 0.015).scale(scale);
            Vec3 q = KaitenDome.surface(b, 0.015).scale(scale);
            mesh.quad(p, q, q.scale(0.93), p.scale(0.93), BRAID, (int) (128 * progress));
        }
    }

    private static void checkWorld(ClientLevel level) {
        if (world.get() != level) {
            DOMES.clear();
            world = new WeakReference<>(level);
        }
    }

    private static Vec3 surface(double angle, double height, float scale) {
        Vec3 point = KaitenDome.surface(angle, height);
        return new Vec3(point.x, point.y * scale, point.z);
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer, Vec3 center, float yScale) {
        private void patch(double a, double b, double lo, double hi, Tint tint, int alpha) {
            quad(surface(a, lo, this.yScale), surface(b, lo, this.yScale), surface(b, hi, this.yScale), surface(a, hi, this.yScale), tint, alpha);
        }

        private void braid(double yaw, int segments, float scale, float fade) {
            for (int i = 0; i < segments; i++) {
                double a = yaw + i * Math.PI * 2 / segments;
                double b = yaw + (i + 1) * Math.PI * 2 / segments;
                double f = 0.065 + 0.033 * Math.sin(a * 2 + yaw);
                double g = 0.065 + 0.033 * Math.sin(b * 2 + yaw);
                quad(surface(a, f, scale), surface(b, g, scale), surface(b, g + 0.023, scale),
                        surface(a, f + 0.023, scale), FLASH, (int) (187 * fade));
            }
        }

        private void flare(Vec3 contact, float fade) {
            Vec3 position = KaitenDome.scaled(contact);
            double angle = Math.atan2(position.z, position.x);
            double latitude = Math.asin(Mth.clamp(position.y, 0, 1));
            double reach = 0.17 + 0.23 * (1 - fade);
            quad(surface(angle - reach, latitude, 1), surface(angle, Math.min(Math.PI / 2, latitude + reach * 0.53), 1),
                    surface(angle + reach, latitude, 1), surface(angle, Math.max(0, latitude - reach * 0.3), 1), FLASH, (int) (247 * fade));
        }

        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Tint tint, int alpha) {
            vertex(a, tint, alpha);
            vertex(b, tint, alpha);
            vertex(c, tint, alpha);
            vertex(d, tint, alpha);
        }

        private void vertex(Vec3 pos, Tint tint, int alpha) {
            this.buffer.vertex(this.matrix, (float) (this.center.x + pos.x), (float) (this.center.y + pos.y),
                    (float) (this.center.z + pos.z)).color(tint.red(), tint.green(), tint.blue(), alpha).endVertex();
        }
    }

    private record Tint(int red, int green, int blue) { }

    private static final class Dome {
        private final Vec3 center;
        private long since;
        private Step phase;
        private long updated;
        private Vec3 hit;
        private long hitTick;

        private Dome(long since, Vec3 center, Step phase, long updated) {
            this.since = since;
            this.center = center;
            this.phase = phase;
            this.updated = updated;
        }
    }
}
