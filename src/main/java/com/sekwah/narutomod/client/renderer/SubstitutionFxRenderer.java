package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.SubstitutionFxPacket;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Random;

public final class SubstitutionFxRenderer {
    private static final int TTL = 10;
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final ArrayDeque<Swap> SWAPS = new ArrayDeque<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private SubstitutionFxRenderer() {
    }

    public static void add(SubstitutionFxPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        useLevel(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (SWAPS.size() >= 24) {
            SWAPS.removeFirst();
        }
        Random random = new Random(packet.seed());
        Vec3[] scatter = new Vec3[9];
        for (int i = 0; i < scatter.length; i++) {
            double angle = (i + random.nextDouble() * 0.6) * Mth.TWO_PI / scatter.length;
            scatter[i] = new Vec3(Math.cos(angle) * 0.65, random.nextDouble() * 0.46, Math.sin(angle) * 0.65);
        }
        SWAPS.addLast(new Swap(packet.from(), packet.to(), scatter, level.getGameTime()));
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers,
                     Vec3 camera, float partialTick) {
        useLevel(level);
        long now = level.getGameTime();
        SWAPS.removeIf(swap -> now - swap.started() >= TTL);
        if (SWAPS.isEmpty()) {
            return;
        }
        VertexConsumer buffer = buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE);
        boolean low = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        for (Swap swap : SWAPS) {
            float time = now - swap.started() + partialTick;
            cloud(matrix, buffer, swap.from(), camera, swap.scatter(), time, low ? 5 : 9);
            cloud(matrix, buffer, swap.to(), camera, swap.scatter(), time + 1.2f, low ? 3 : 4);
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static void cloud(Matrix4f matrix, VertexConsumer buffer, Vec3 world, Vec3 camera,
                              Vec3[] scatter, float time, int count) {
        boolean main = count >= 5;
        if (world.distanceToSqr(camera) > 36 * 36) {
            return;
        }
        Vec3 origin = world.subtract(camera).add(0, main ? 0.76 : 0.43, 0);
        float fade = Mth.clamp(1f - time / TTL, 0, 1);
        double spread = (0.22 + 0.77 * (1.0 - Math.exp(-time * 0.61))) * (main ? 1.0 : 0.43);
        for (int i = 0; i < count; i++) {
            Vec3 local = origin.add(scatter[i].scale(spread)).add(0, time * (main ? 0.045 : 0.031), 0);
            float alpha = fade * (main ? 0.40f : 0.23f) * (float) Math.min(1, local.lengthSqr() / 1.7);
            puff(matrix, buffer, local, spread * (0.42 + i % 3 * 0.062), alpha);
        }
    }

    private static void puff(Matrix4f matrix, VertexConsumer buffer, Vec3 center, double radius, float alpha) {
        Vec3 look = center.normalize();
        Vec3 side = look.cross(Math.abs(look.y) > 0.92 ? new Vec3(1, 0, 0) : UP).normalize().scale(radius);
        Vec3 top = side.cross(look).scale(0.88);
        for (int i = 0; i < 9; i++) {
            double a = i * Mth.TWO_PI / 9;
            double b = a + Mth.TWO_PI / 9;
            Vec3 first = center.add(side.scale(Math.cos(a))).add(top.scale(Math.sin(a)));
            Vec3 second = center.add(side.scale(Math.cos(b))).add(top.scale(Math.sin(b)));
            vertex(matrix, buffer, center, alpha);
            vertex(matrix, buffer, center, alpha);
            vertex(matrix, buffer, first, 0);
            vertex(matrix, buffer, second, 0);
        }
    }

    private static void vertex(Matrix4f matrix, VertexConsumer buffer, Vec3 point, float alpha) {
        buffer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(0.78f, 0.82f, 0.86f, alpha).endVertex();
    }

    private static void useLevel(ClientLevel level) {
        if (world.get() != level) {
            SWAPS.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Swap(Vec3 from, Vec3 to, Vec3[] scatter, long started) {
    }
}
