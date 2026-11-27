package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.AdamantineChainPacket;
import com.sekwah.narutomod.util.AdamantineChain;
import com.sekwah.narutomod.util.AdamantineChain.Phase;
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

public final class AdamantineChainRenderer {
    private static final Map<Integer, Track> CHAINS = new LinkedHashMap<>();
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 X = new Vec3(1, 0, 0);
    private static final Tint GOLD = new Tint(239, 151, 48);
    private static final Tint BRONZE = new Tint(106, 55, 27);
    private static final Tint EDGE = new Tint(255, 237, 176);
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private AdamantineChainRenderer() {
    }

    public static void receive(AdamantineChainPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        checkWorld(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        Track old = CHAINS.get(packet.id());
        if (old == null && CHAINS.size() >= 42) {
            CHAINS.remove(CHAINS.keySet().iterator().next());
        }
        long now = level.getGameTime();
        CHAINS.put(packet.id(), new Track(packet, now, old == null ? packet.root() : old.root(now),
                old == null ? packet.tip() : old.tip(now)));
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        checkWorld(level);
        double now = level.getGameTime() + partial;
        CHAINS.values().removeIf(track -> now - track.stamp() >= (track.packet().phase() == Phase.RETRACT ? AdamantineChain.RETRACT : 9));
        if (CHAINS.isEmpty()) {
            return;
        }
        boolean fast = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        var mesh = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        for (Track track : CHAINS.values()) {
            Vec3 root = track.root(now).subtract(camera);
            if (root.lengthSqr() < 39 * 39) {
                drawChain(mesh, track, root, track.tip(now).subtract(camera), (float) (now - track.stamp()), fast || root.lengthSqr() > 22 * 22);
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static void drawChain(Mesh mesh, Track track, Vec3 root, Vec3 tip, float elapsed, boolean low) {
        var packet = track.packet();
        float age = packet.age() + elapsed;
        double extension = switch (packet.phase()) {
            case SEEK -> Math.min(packet.fraction(), AdamantineChain.extension(age));
            case SEALED -> packet.fraction();
            case RETRACT -> packet.fraction() * Mth.clamp(1 - elapsed / AdamantineChain.RETRACT, 0, 1);
        };
        double distance = root.distanceTo(tip);
        int count = Math.min(42, (int) Math.floor(extension * distance / 0.26));
        int alpha = packet.phase() == Phase.RETRACT ? (int) (215 * (1 - elapsed / AdamantineChain.RETRACT)) : 242;
        boolean taut = packet.girth() > 0;
        for (int i = 0; i < count; i++) {
            double p = (i + 0.5) * 0.26 / distance;
            Vec3 pos = AdamantineChain.point(root, tip, p, taut);
            Vec3 tangent = AdamantineChain.point(root, tip, Math.min(extension, p + 0.02), taut)
                    .subtract(AdamantineChain.point(root, tip, Math.max(0, p - 0.02), taut)).normalize();
            double roll = i * Math.PI * 0.5 + packet.id() * 0.14;
            if (!taut) {
                roll += age * 0.07 + p * 1.1;
            }
            mesh.link(pos, tangent, roll, alpha, low);
        }
        if (packet.phase() == Phase.SEALED && taut && packet.fraction() > 0.98f) {
            seal(mesh, tip, packet.girth(), low);
        }
    }

    private static void seal(Mesh mesh, Vec3 tip, double radius, boolean low) {
        int count = Mth.clamp((int) Math.ceil(2 * Math.PI * radius / 0.28), 5, 36);
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            Vec3 around = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            Vec3 tangent = new Vec3(-around.z, 0, around.x);
            Vec3 position = tip.add(around.scale(radius));
            mesh.link(position.add(0, -0.14 - around.x * 0.22, 0), tangent, 0.35 + i * Math.PI * 0.5, 248, low);
            if (!low) {
                mesh.link(position.add(0, 0.14 + around.x * 0.22, 0), tangent, -0.35 + i * Math.PI * 0.5, 206, true);
            }
        }
    }

    private static void checkWorld(ClientLevel level) {
        if (world.get() != level) {
            CHAINS.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Track(AdamantineChainPacket packet, long stamp, Vec3 oldRoot, Vec3 oldTip) {
        private Vec3 root(double now) {
            return this.oldRoot.lerp(this.packet.root(), Mth.clamp((now - this.stamp) / 2, 0, 1));
        }

        private Vec3 tip(double now) {
            return this.oldTip.lerp(this.packet.tip(), Mth.clamp((now - this.stamp) / 2, 0, 1));
        }
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void link(Vec3 center, Vec3 tangent, double roll, int alpha, boolean low) {
            Vec3 base = tangent.cross(Math.abs(tangent.y) > 0.91 ? X : UP).normalize();
            Vec3 major = base.scale(Math.cos(roll)).add(tangent.cross(base).scale(Math.sin(roll)));
            Vec3 normal = tangent.cross(major);
            int sides = low ? 6 : 10;
            for (int i = 0; i < sides; i++) {
                Vec3 a = rim(tangent, major, i, sides, 0.164, 0.086);
                Vec3 b = rim(tangent, major, i + 1, sides, 0.164, 0.086);
                Vec3 c = rim(tangent, major, i + 1, sides, 0.111, 0.041);
                Vec3 d = rim(tangent, major, i, sides, 0.111, 0.041);
                var top = center.add(normal.scale(0.021));
                var bottom = center.add(normal.scale(-0.021));
                quad(top.add(a), top.add(b), top.add(c), top.add(d), i % 3 == 1 ? EDGE : GOLD, alpha);
                quad(top.add(a), bottom.add(a), bottom.add(b), top.add(b), BRONZE, alpha);
                quad(bottom.add(d), bottom.add(c), bottom.add(b), bottom.add(a), GOLD, alpha);
            }
        }

        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Tint tint, int alpha) {
            vertex(a, tint, alpha);
            vertex(b, tint, alpha);
            vertex(c, tint, alpha);
            vertex(d, tint, alpha);
        }

        private void vertex(Vec3 point, Tint tint, int alpha) {
            this.buffer.vertex(this.matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color(tint.red(), tint.green(), tint.blue(), alpha).endVertex();
        }
    }

    private static Vec3 rim(Vec3 tangent, Vec3 major, int i, int sides, double length, double width) {
        double angle = i * 2 * Math.PI / sides;
        return tangent.scale(Math.cos(angle) * length).add(major.scale(Math.sin(angle) * width));
    }

    private record Tint(int red, int green, int blue) {
    }
}
