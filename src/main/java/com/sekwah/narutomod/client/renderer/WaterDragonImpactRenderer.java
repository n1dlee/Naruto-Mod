package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.WaterDragonImpactPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Random;

public final class WaterDragonImpactRenderer {
    private static final int FAN_COUNT = 12;
    private static final long LIFETIME = 11;
    private static final int MAX_SPLASHES = 24;
    private static final ArrayDeque<Splash> SPLASHES = new ArrayDeque<>();
    private static WeakReference<ClientLevel> activeLevel = new WeakReference<>(null);

    private WaterDragonImpactRenderer() {
    }

    public static void add(WaterDragonImpactPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        useLevel(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (SPLASHES.size() == MAX_SPLASHES) {
            SPLASHES.removeFirst();
        }
        Vec3 axis = packet.axis().normalize();
        Vec3 reference = Math.abs(axis.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 tangent = axis.cross(reference).normalize();
        Vec3 bitangent = axis.cross(tangent);
        Spray[] sprays = new Spray[FAN_COUNT];
        Random random = new Random(packet.seed());
        for (int i = 0; i < sprays.length; i++) {
            double theta = (i + random.nextDouble() * 0.55) * Mth.TWO_PI / sprays.length;
            Vec3 radial = tangent.scale(Math.cos(theta)).add(bitangent.scale(Math.sin(theta)));
            boolean surface = packet.kind() == WaterDragonImpactPacket.Kind.BLOCK;
            Vec3 velocity = radial.scale(0.23 + random.nextDouble() * 0.32)
                    .add(axis.scale((surface ? 0.16 : 0.28) + random.nextDouble() * 0.2));
            sprays[i] = new Spray(velocity, axis.cross(radial).scale(0.18 + random.nextDouble() * 0.18));
            Vec3 origin = packet.point().add(axis.scale(0.06));
            level.addParticle(ParticleTypes.SPLASH, origin.x, origin.y, origin.z, velocity.x, velocity.y, velocity.z);
        }
        SPLASHES.addLast(new Splash(level.getGameTime(), packet.point(), sprays,
                packet.kind() == WaterDragonImpactPacket.Kind.DISSIPATE ? 0.59f : 1.0f));
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers,
                     Vec3 camera, float partialTick) {
        useLevel(level);
        long now = level.getGameTime();
        SPLASHES.removeIf(splash -> now - splash.started() >= LIFETIME);
        if (SPLASHES.isEmpty()) {
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE);
        for (Splash splash : SPLASHES) {
            if (splash.point().distanceToSqr(camera) > 64.0 * 64.0) {
                continue;
            }
            float time = Math.max(0, now - splash.started() + partialTick);
            Vec3 origin = splash.point().subtract(camera);
            drawFan(consumer, matrix, origin, splash, time);
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static void useLevel(ClientLevel level) {
        if (level != activeLevel.get()) {
            activeLevel = new WeakReference<>(level);
            SPLASHES.clear();
        }
    }

    private static void drawFan(VertexConsumer consumer, Matrix4f matrix, Vec3 origin, Splash splash, float time) {
        float fade = 1.0f - Mth.clamp(time / LIFETIME, 0.0f, 1.0f);
        for (Spray spray : splash.sprays()) {
            Vec3 nose = arc(origin, spray.velocity(), (time + 1.1) * splash.scale());
            Vec3 base = arc(origin, spray.velocity(), Math.max(0, time - 3.0) * splash.scale());
            Vec3 wing = spray.width().scale((0.4 + Math.sin(time / LIFETIME * Math.PI)) * splash.scale());
            vertex(consumer, matrix, base.subtract(wing), 0.08f, 0.46f, 0.75f, fade * 0.24f);
            vertex(consumer, matrix, base.add(wing), 0.12f, 0.68f, 0.92f, fade * 0.34f);
            vertex(consumer, matrix, nose.add(wing.scale(0.15)), 0.89f, 0.98f, 1.0f, fade * 0.87f);
            vertex(consumer, matrix, nose.subtract(wing.scale(0.15)), 0.53f, 0.87f, 1.0f, fade * 0.72f);
        }
    }

    private static Vec3 arc(Vec3 origin, Vec3 velocity, double age) {
        return origin.add(velocity.scale(age)).add(0, 0.16 * age - 0.033 * age * age, 0);
    }

    static void vertex(VertexConsumer buffer, Matrix4f matrix, Vec3 point,
                       float red, float green, float blue, float alpha) {
        buffer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha).endVertex();
    }

    private record Splash(long started, Vec3 point, Spray[] sprays, float scale) {
    }

    private record Spray(Vec3 velocity, Vec3 width) {
    }
}
