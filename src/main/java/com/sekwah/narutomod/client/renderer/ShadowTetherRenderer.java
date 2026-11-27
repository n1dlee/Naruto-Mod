package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.ShadowTetherPacket;
import com.sekwah.narutomod.network.s2c.ShadowTetherPacket.Cue;
import com.sekwah.narutomod.util.ShadowSurface;
import com.sekwah.narutomod.util.ShadowNeedle;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ShadowTetherRenderer {
    private static final Map<Key, Track> SHADOWS = new LinkedHashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private ShadowTetherRenderer() {
    }

    public static void receive(ShadowTetherPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        useLevel(level);
        if (level == null || !level.dimension().location().equals(packet.dimension()) || packet.path().size() < 2) {
            return;
        }
        var key = new Key(packet.actor(), packet.id());
        Track current = SHADOWS.get(key);
        if (SHADOWS.size() >= 32 && current == null) {
            SHADOWS.remove(SHADOWS.keySet().iterator().next());
        }
        SHADOWS.put(key, new Track(packet, level.getGameTime()));
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        useLevel(level);
        long now = level.getGameTime();
        SHADOWS.values().removeIf(track -> now - track.received() >= expiry(track.packet().cue()));
        if (SHADOWS.isEmpty()) {
            return;
        }
        boolean low = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        VertexConsumer shadow = buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE);
        for (Track track : SHADOWS.values()) {
            float age = now - track.received() + partial;
            if (track.packet().path().get(0).distanceToSqr(camera) <= 45 * 45) {
                drawTrack(new SurfaceMesh(matrix, shadow, camera), track.packet(), age, now + partial, low);
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static int expiry(Cue cue) {
        return switch (cue) {
            case CAST -> ShadowSurface.WINDUP + 4;
            case BOUND -> 9;
            case SNAP -> 7;
            case SEW_TRACE -> ShadowNeedle.RISE_AT + 3;
            case SEW_HIT, SEW_MISS -> 7;
        };
    }

    private static void drawTrack(SurfaceMesh mesh, ShadowTetherPacket packet, float age, float clock, boolean low) {
        List<Vec3> path = packet.path();
        int segments = path.size() - 1;
        float progress = switch (packet.cue()) {
            case CAST -> Mth.clamp((age + 0.38f) / ShadowSurface.WINDUP, 0, 1);
            case SEW_TRACE -> Mth.clamp((age + 0.38f) / ShadowNeedle.RISE_AT, 0, 1);
            default -> 1;
        };
        boolean sewing = packet.cue() == Cue.SEW_HIT || packet.cue() == Cue.SEW_MISS;
        float collapse = packet.cue() == Cue.SNAP ? Mth.clamp(age / 7, 0, 1) : 0;
        float end = segments * (progress - collapse * 0.75f);
        for (int i = 0; i < segments && i < end; i++) {
            Vec3 from = path.get(i);
            Vec3 to = from.lerp(path.get(i + 1), Math.min(1, end - i));
            float alpha = (1 - collapse) * (0.47f + 0.27f * Math.min(1, (end - i) / 1.8f));
            double motion = 0.86 + Math.sin(i * 2.31 + clock * 0.26 + packet.id()) * 0.16;
            boolean tip = i + 1 >= end || i + 1 == segments;
            double width = (packet.cue() == Cue.BOUND && tip ? 0.24 : 0.135) * motion;
            mesh.segment(from, to, width, sewing ? alpha * Mth.clamp(1 - age / 7, 0, 1) : alpha, !low);
        }
        if (packet.cue() == Cue.BOUND) {
            mesh.knot(path.get(segments), clock, low ? 8 : 16);
        } else if (sewing) {
            Vec3 tip = path.get(segments);
            Vec3 facing = tip.subtract(path.get(segments - 1)).multiply(1, 0, 1).normalize();
            mesh.needle(tip, facing, ShadowNeedle.height(age, packet.cue() == Cue.SEW_HIT), low ? 4 : 7);
        }
    }

    private static void useLevel(ClientLevel level) {
        if (world.get() != level) {
            SHADOWS.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Key(int actor, int cast) {
    }

    private record Track(ShadowTetherPacket packet, long received) {
    }

    private record SurfaceMesh(Matrix4f matrix, VertexConsumer mesh, Vec3 camera) {
        private void segment(Vec3 from, Vec3 to, double width, float alpha, boolean fray) {
            Vec3 direction = to.subtract(from);
            Vec3 edge = new Vec3(-direction.z, 0, direction.x).normalize().scale(width);
            Vec3 a = from.add(0, 0.037 + Math.max(0, direction.y) * 0.15, 0);
            Vec3 b = to.add(0, 0.038 + Math.max(0, -direction.y) * 0.12, 0);
            quad(a.add(edge), b.add(edge.scale(0.84)), b.subtract(edge.scale(0.89)), a.subtract(edge), alpha, false);
            if (fray) {
                quad(a.add(edge.scale(1.43)), b.add(edge.scale(1.08)), b.add(edge.scale(0.82)), a.add(edge.scale(0.76)), alpha * 0.61f, true);
                quad(a.subtract(edge.scale(0.76)), b.subtract(edge.scale(0.85)), b.subtract(edge.scale(1.22)),
                        a.subtract(edge.scale(1.31)), alpha * 0.56f, true);
            }
        }

        private void knot(Vec3 feet, float clock, int sides) {
            Vec3 center = feet.add(0, 0.045, 0);
            double radius = 0.32 + 0.036 * Math.sin(clock * 0.40);
            for (int i = 0; i < sides; i++) {
                double a = i * Math.PI * 2 / sides;
                double b = (i + 1) * Math.PI * 2 / sides;
                Vec3 left = new Vec3(Math.cos(a), 0, Math.sin(a)).scale(radius);
                Vec3 right = new Vec3(Math.cos(b), 0, Math.sin(b)).scale(radius);
                quad(center.add(left), center.add(right), center.add(right.scale(0.53)), center.add(left.scale(0.53)), 0.66f, true);
            }
        }

        private void needle(Vec3 base, Vec3 facing, double height, int parts) {
            if (height < 0.015) {
                return;
            }
            Vec3 right = new Vec3(-facing.z, 0, facing.x);
            for (int section = 0; section < parts; section++) {
                double s = section / (double) parts;
                double t = (section + 1.0) / parts;
                Vec3 a = needlePoint(base, facing, height, s);
                Vec3 b = needlePoint(base, facing, height, t);
                double root = 0.115 * Math.pow(1 - s, 0.76) + 0.003;
                double tip = 0.115 * Math.pow(1 - t, 0.76) + 0.003;
                Vec3 r0 = right.scale(root);
                Vec3 r1 = right.scale(tip);
                Vec3 f0 = facing.scale(root * 0.9);
                Vec3 f1 = facing.scale(tip * 0.9);
                quad(a.add(r0), b.add(r1), b.add(f1), a.add(f0), 0.83f, false);
                quad(a.add(f0), b.add(f1), b.subtract(r1), a.subtract(r0), 0.73f, false);
                quad(a.subtract(r0), b.subtract(r1), b.subtract(f1), a.subtract(f0), 0.77f, false);
                quad(a.subtract(f0), b.subtract(f1), b.add(r1), a.add(r0), 0.80f, false);
                quad(a.add(r0.scale(1.10)), b.add(r1.scale(1.50)), b.add(r1), a.add(r0), 0.62f, true);
            }
        }

        private static Vec3 needlePoint(Vec3 base, Vec3 facing, double height, double t) {
            return base.add(facing.scale((t * t * 0.29 - Math.sin(Math.PI * t) * 0.19) * height))
                    .add(0, 0.055 + t * height, 0);
        }

        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, float alpha, boolean faded) {
            vertex(a, faded ? alpha * 0.40f : alpha, faded);
            vertex(b, faded ? alpha * 0.44f : alpha, faded);
            vertex(c, alpha, faded);
            vertex(d, alpha, faded);
        }

        private void vertex(Vec3 world, float alpha, boolean fray) {
            Vec3 point = world.subtract(camera);
            mesh.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color(fray ? 0.13f : 0.022f, fray ? 0.10f : 0.016f, fray ? 0.22f : 0.036f, alpha).endVertex();
        }
    }
}
