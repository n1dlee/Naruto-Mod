package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.KamuiStepPacket;
import com.sekwah.narutomod.util.KamuiStep;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;

public final class KamuiStepRenderer {
    private static final int MAX_EVENTS = 28;
    private static final ArrayDeque<Incident> FOLDS = new ArrayDeque<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private KamuiStepRenderer() {
    }

    public static void add(KamuiStepPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        useLevel(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        FOLDS.removeIf(active -> active.packet().actor() == packet.actor() && active.packet().seed() == packet.seed());
        if (FOLDS.size() >= MAX_EVENTS) {
            FOLDS.removeFirst();
        }
        FOLDS.addLast(new Incident(packet, level.getGameTime()));
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers,
                     Vec3 camera, float partialTick) {
        useLevel(level);
        long now = level.getGameTime();
        FOLDS.removeIf(fold -> now - fold.start() >= duration(fold.packet().phase()));
        if (FOLDS.isEmpty()) {
            return;
        }
        boolean low = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        drawPass(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE), camera, now, partialTick, low, false);
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
        drawPass(matrix, buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW), camera, now, partialTick, low, true);
        buffers.endBatch(NarutoRenderTypes.CHAKRA_GLOW);
    }

    private static int duration(KamuiStepPacket.Phase phase) {
        return switch (phase) {
            case PREPARE -> 5;
            case ABORTED -> 4;
            case ARRIVED -> 11;
        };
    }

    private static void drawPass(Matrix4f matrix, VertexConsumer mesh, Vec3 camera, long now, float partialTick,
                                 boolean low, boolean rim) {
        for (Incident fold : FOLDS) {
            KamuiStepPacket packet = fold.packet();
            float time = Mth.clamp((now - fold.start() + partialTick) / duration(packet.phase()), 0, 1);
            Vec3 from = packet.from().subtract(camera);
            Vec3 facing = KamuiStep.facing(packet.yaw());
            switch (packet.phase()) {
                case PREPARE -> render(matrix, mesh, fold(packet, from, facing, 1 - time * 0.55, time * 4,
                        Math.sin(time * Math.PI) * 0.78, low), rim);
                case ABORTED -> render(matrix, mesh, fold(packet, from, facing, 0.45 + time * 0.16, 3 - time,
                        (1 - time) * 0.54, low), rim);
                case ARRIVED -> {
                    render(matrix, mesh, fold(packet, from, facing, 0.67 * (1 - time) + 0.04,
                            4 + 7 * time, Math.pow(1 - time, 2), low), rim);
                    render(matrix, mesh, fold(packet, packet.to().subtract(camera), facing, 0.22 + 1.06 * time,
                            -4 - 5 * time, (1 - time) * 0.95, low), rim);
                }
            }
        }
    }

    private static Fold fold(KamuiStepPacket packet, Vec3 feet, Vec3 facing, double radius, double spin,
                             double alpha, boolean low) {
        Vec3 right = new Vec3(facing.z, 0, -facing.x);
        int side = packet.leftEye() ? -1 : 1;
        Vec3 eye = feet.add(0, packet.height() * 0.89, 0).add(facing.scale(0.16)).add(right.scale(side * 0.13));
        double ratio = Mth.clamp((eye.length() - 0.47) / 0.77, 0.0, 1.0);
        int steps = low || eye.lengthSqr() > 20 * 20 ? 15 : 30;
        double startAngle = ((packet.seed() & 511) / 511.0 - 0.5) * 1.1;
        return new Fold(eye, facing, right, side, radius, startAngle + spin * side, alpha * ratio, steps);
    }

    private static void render(Matrix4f matrix, VertexConsumer mesh, Fold fold, boolean rim) {
        if (fold.alpha() < 0.01 || fold.pivot().lengthSqr() > 36 * 36) {
            return;
        }
        int blades = fold.steps() == 15 ? 2 : 3;
        for (int blade = 0; blade < blades; blade++) {
            double offset = blade * (Math.PI * 2 / blades);
            for (int i = 0; i < fold.steps(); i++) {
                segment(matrix, mesh, fold, offset, i / (double) fold.steps(), (i + 1.0) / fold.steps(), rim);
            }
        }
    }

    private static void segment(Matrix4f matrix, VertexConsumer mesh, Fold fold, double offset,
                                double from, double to, boolean rim) {
        double leading = rim ? 0.282 : 0.04;
        double trailing = rim ? 0.326 : 0.31;
        float a = opacity(fold, from, rim);
        float b = opacity(fold, to, rim);
        vertex(matrix, mesh, point(fold, offset, from, leading), a * 0.5f, rim);
        vertex(matrix, mesh, point(fold, offset, from, trailing), a, rim);
        vertex(matrix, mesh, point(fold, offset, to, trailing), b, rim);
        vertex(matrix, mesh, point(fold, offset, to, leading), b * 0.5f, rim);
    }

    private static Vec3 point(Fold fold, double offset, double radius, double edge) {
        double t = fold.radius() * (0.07 + radius * 0.98);
        double angle = offset + fold.spin() + radius * 9.2 * fold.side() + edge * (1.4 - radius);
        double x = Math.cos(angle) * t * 0.78;
        double y = (Math.sin(angle) * 0.88 - radius * 0.63) * t;
        double z = (Math.sin(angle * 2.0) * 0.17 - radius * 0.38) * t;
        return fold.pivot().add(fold.right().scale(x)).add(0, y, 0).add(fold.facing().scale(z));
    }

    private static float opacity(Fold fold, double t, boolean rim) {
        return (float) (fold.alpha() * Math.pow(Math.sin(Math.PI * t), 0.65) * (rim ? 0.59 : 0.87));
    }

    private static void vertex(Matrix4f matrix, VertexConsumer mesh, Vec3 point, float alpha, boolean rim) {
        mesh.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(rim ? 0.57f : 0.035f, rim ? 0.44f : 0.024f, rim ? 0.69f : 0.068f, alpha).endVertex();
    }

    private static void useLevel(ClientLevel level) {
        if (world.get() != level) {
            world = new WeakReference<>(level);
            FOLDS.clear();
        }
    }

    private record Incident(KamuiStepPacket packet, long start) {
    }

    private record Fold(Vec3 pivot, Vec3 facing, Vec3 right, int side, double radius, double spin,
                        double alpha, int steps) {
    }
}
