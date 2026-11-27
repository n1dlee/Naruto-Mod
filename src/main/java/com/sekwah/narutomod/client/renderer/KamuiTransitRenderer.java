package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.KamuiTransitPacket;
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

public final class KamuiTransitRenderer {
    private static final Map<Integer, Portal> PORTALS = new LinkedHashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private KamuiTransitRenderer() {
    }

    public static void receive(KamuiTransitPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        useLevel(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (PORTALS.size() >= 24 && !PORTALS.containsKey(packet.id())) {
            PORTALS.remove(PORTALS.keySet().iterator().next());
        }
        PORTALS.put(packet.id(), new Portal(packet, level.getGameTime()));
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        useLevel(level);
        long now = level.getGameTime();
        PORTALS.values().removeIf(portal -> now - portal.start() >= duration(portal.packet()));
        if (PORTALS.isEmpty()) {
            return;
        }
        boolean fast = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        drawPass(buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE), matrix, new View(camera, now, partial, fast), false);
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
        drawPass(buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW), matrix, new View(camera, now, partial, fast), true);
        buffers.endBatch(NarutoRenderTypes.CHAKRA_GLOW);
    }

    private static int duration(KamuiTransitPacket packet) {
        return switch (packet.cue()) {
            case GATHER -> Math.min(15, Math.max(5, packet.waitTicks() + 3));
            case FOLD -> 8;
            case OPEN -> 13;
            case CANCEL -> 5;
        };
    }

    private static void drawPass(VertexConsumer consumer, Matrix4f matrix, View view, boolean rim) {
        for (Portal portal : PORTALS.values()) {
            var packet = portal.packet();
            Vec3 base = packet.anchor().subtract(view.camera());
            if (base.lengthSqr() > 42 * 42) {
                continue;
            }
            float elapsed = view.now() - portal.start() + view.partial();
            float t = Mth.clamp(elapsed / duration(packet), 0, 1);
            var motion = profile(packet.cue(), t);
            Vec3 center = base.add(0, Math.min(2.3, packet.height()) * 0.54, 0);
            double eyeFade = Mth.clamp((center.length() - 0.38) / 0.90, 0.09, 1);
            int steps = view.fast() || base.lengthSqr() > 22 * 22 ? 15 : 32;
            double salt = (packet.id() & 127) / 127.0;
            var arc = new Arc(center, motion.radius(), motion.spin() + salt, motion.alpha() * eyeFade, steps);
            funnel(matrix, consumer, arc, rim);
        }
    }

    private static Motion profile(KamuiTransitPacket.Cue cue, float time) {
        return switch (cue) {
            case GATHER -> new Motion(1.35 - 0.62 * time, time * 5.4, Math.sin(time * Math.PI * 0.66) * 0.79);
            case FOLD -> new Motion(0.78 * (1 - time) + 0.05, 4.8 + time * 8, Math.pow(1 - time, 1.9));
            case OPEN -> new Motion(0.21 + time * 1.43, -time * 5.9, (1 - time) * 0.90);
            case CANCEL -> new Motion(0.91 * (1 - time), 2.0 - time * 1.9, Math.pow(1 - time, 2) * 0.41);
        };
    }

    private static void funnel(Matrix4f matrix, VertexConsumer consumer, Arc arc, boolean rim) {
        if (arc.alpha() < 0.01) {
            return;
        }
        for (int strand = 0; strand < (arc.steps() == 15 ? 2 : 3); strand++) {
            double offset = strand * (Math.PI * 2 / 3);
            for (int i = 0; i < arc.steps(); i++) {
                double t = i / (double) arc.steps();
                double v = (i + 1.0) / arc.steps();
                double thick = rim ? 0.024 : 0.35;
                float a = (float) (Math.sin(t * Math.PI) * arc.alpha());
                float b = (float) (Math.sin(v * Math.PI) * arc.alpha());
                vertex(matrix, consumer, point(arc, t, offset), a, rim);
                vertex(matrix, consumer, point(arc, v, offset), b, rim);
                vertex(matrix, consumer, point(arc, v, offset + thick), b * 0.27f, rim);
                vertex(matrix, consumer, point(arc, t, offset + thick), a * 0.27f, rim);
            }
        }
    }

    private static Vec3 point(Arc arc, double t, double offset) {
        double angle = offset + arc.spin() + t * 12.4;
        double radius = arc.radius() * (0.11 + t * 0.95) * (0.92 + 0.08 * Math.cos(angle * 3));
        return arc.center().add(Math.cos(angle) * radius, Math.sin(angle * 2) * radius * 0.24 + (t - 0.51) * radius * 1.7,
                Math.sin(angle) * radius);
    }

    private static void vertex(Matrix4f matrix, VertexConsumer consumer, Vec3 p, float alpha, boolean rim) {
        consumer.vertex(matrix, (float) p.x, (float) p.y, (float) p.z)
                .color(rim ? 0.69f : 0.059f, rim ? 0.55f : 0.032f, rim ? 0.84f : 0.094f,
                        alpha * (rim ? 0.64f : 0.82f)).endVertex();
    }

    private static void useLevel(ClientLevel level) {
        if (world.get() != level) {
            PORTALS.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Portal(KamuiTransitPacket packet, long start) {
    }

    private record View(Vec3 camera, long now, float partial, boolean fast) {
    }

    private record Motion(double radius, double spin, double alpha) {
    }

    private record Arc(Vec3 center, double radius, double spin, double alpha, int steps) {
    }
}
