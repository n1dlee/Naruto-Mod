package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.network.s2c.RasenganImpactPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Deque;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RasenganImpactRenderer {
    private static final int MAX_IMPACTS = 32;
    private static final int SEGMENTS = 16;
    private static final float LIFETIME = 10.0F;
    private static final double RANGE_SQUARED = 48.0 * 48.0;
    private static final double TURN = 2.0 * Math.PI;
    private static final Deque<Impact> IMPACTS = new ArrayDeque<>();
    private static ClientLevel currentLevel;

    private RasenganImpactRenderer() {
    }

    public static void addImpact(RasenganImpactPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        useLevel(level);
        Vec3 axis = packet.direction().normalize();
        if (axis.lengthSqr() == 0.0) {
            return;
        }
        Vec3 reference = Math.abs(axis.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = axis.cross(reference).normalize();
        var random = RandomSource.create(packet.seed());
        Impact impact = new Impact(packet.contact(), axis, right, axis.cross(right), packet.strength(),
                random.nextDouble() * TURN, packet.hitsBlock(), level.getGameTime());
        if (IMPACTS.size() == MAX_IMPACTS) {
            IMPACTS.removeFirst();
        }
        IMPACTS.addLast(impact);

        if (minecraft.options.particles().get() != ParticleStatus.MINIMAL
                && minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(impact.origin) < 24 * 24) {
            spawnFlecks(level, impact, packet.blockStateId(), random);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        useLevel(null);
    }

    static boolean draw(ClientLevel level, Matrix4f matrix, VertexConsumer consumer, Vec3 camera, float partialTick) {
        useLevel(level);
        long now = level.getGameTime();
        while (!IMPACTS.isEmpty() && now - IMPACTS.getFirst().startTick >= LIFETIME) {
            IMPACTS.removeFirst();
        }
        boolean drew = false;
        for (Impact impact : IMPACTS) {
            if (impact.origin.distanceToSqr(camera) > RANGE_SQUARED) {
                continue;
            }
            float time = (now - impact.startTick + partialTick) / LIFETIME;
            if (time >= 0.0F && time < 1.0F) {
                drawImpact(matrix, consumer, impact, time);
                drew = true;
            }
        }
        return drew;
    }

    private static void useLevel(ClientLevel level) {
        if (currentLevel != level) {
            IMPACTS.clear();
            currentLevel = level;
        }
    }

    private static void drawImpact(Matrix4f matrix, VertexConsumer consumer, Impact impact, float time) {
        double fade = 1.0 - time;
        double expansion = 1.0 - fade * fade * fade;
        double size = 0.72 + impact.strength * 0.60;
        double depth = impact.surface ? 0.04 : -0.08;
        if (time < 0.45F) {
            double rim = size * (0.10 + 0.61 * expansion);
            drawRing(matrix, consumer, impact, rim, depth, (float) (0.95 - time * 2.0));
        }
        for (int arm = 0; arm < 3; arm++) {
            double spin = impact.spin + TURN * arm / 3.0 - time * 2.2;
            drawRibbon(matrix, consumer, impact, depth, spin, size * (0.26 + expansion), time);
        }
    }

    private static void drawRing(Matrix4f matrix, VertexConsumer consumer, Impact impact,
                                 double radius, double depth, float alpha) {
        double innerRadius = radius * 0.79;
        Vec3 previousInner = impact.point(innerRadius, 0, depth);
        Vec3 previousOuter = impact.point(radius, 0, depth);
        for (int i = 1; i <= SEGMENTS * 2; i++) {
            double angle = TURN * i / (SEGMENTS * 2);
            Vec3 inner = impact.point(innerRadius, angle, depth);
            Vec3 outer = impact.point(radius, angle, depth);
            vertex(matrix, consumer, previousInner, 0.68F, 0.93F, alpha);
            vertex(matrix, consumer, previousOuter, 0.28F, 0.62F, 0.13F * alpha);
            vertex(matrix, consumer, outer, 0.28F, 0.62F, 0.13F * alpha);
            vertex(matrix, consumer, inner, 0.68F, 0.93F, alpha);
            previousInner = inner;
            previousOuter = outer;
        }
    }

    private static void drawRibbon(Matrix4f matrix, VertexConsumer consumer, Impact impact,
                                   double depth, double spin, double size, float time) {
        double twist = impact.surface ? 1.85 : 2.45;
        double depthTravel = impact.surface ? 0.13 : -0.66;
        double fade = (1.0 - time) * (1.0 - time);
        Vec3 previousInner = impact.point(size * 0.24, spin, depth);
        Vec3 previousOuter = previousInner;
        float previousAlpha = 0.0F;
        for (int i = 1; i <= SEGMENTS; i++) {
            double progress = i / (double) SEGMENTS;
            double angle = spin + twist * progress;
            double radius = size * (0.24 + progress * 0.76);
            double taper = Math.sin(Math.PI * progress);
            double width = size * 0.13 * taper;
            double axial = depth + depthTravel * progress * time;
            Vec3 inner = impact.point(radius - width, angle, axial);
            Vec3 outer = impact.point(radius + width, angle, axial);
            float alpha = (float) (fade * taper * 0.91);
            vertex(matrix, consumer, previousInner, 0.52F, 0.88F, previousAlpha);
            vertex(matrix, consumer, previousOuter, 0.12F, 0.48F, previousAlpha * 0.26F);
            vertex(matrix, consumer, outer, 0.12F, 0.48F, alpha * 0.26F);
            vertex(matrix, consumer, inner, 0.52F, 0.88F, alpha);
            previousInner = inner;
            previousOuter = outer;
            previousAlpha = alpha;
        }
    }

    private static void vertex(Matrix4f matrix, VertexConsumer consumer, Vec3 pos,
                               float red, float green, float alpha) {
        consumer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(red, green, 1.0F, alpha).endVertex();
    }

    private static void spawnFlecks(ClientLevel level, Impact impact, int blockStateId, RandomSource random) {
        ParticleOptions material = impact.surface
                ? new BlockParticleOption(ParticleTypes.BLOCK, Block.stateById(blockStateId))
                : ParticleTypes.END_ROD;
        int count = impact.surface ? 9 : 4;
        double depth = impact.surface ? 0.06 : -0.10;
        for (int i = 0; i < count; i++) {
            double angle = impact.spin + TURN * (i + random.nextDouble()) / count;
            Vec3 point = impact.point(0.08 + random.nextDouble() * 0.18, angle, depth);
            Vec3 velocity = point.subtract(impact.origin).scale(0.8 + impact.strength)
                    .add(impact.axis.scale(depth * 1.7));
            level.addParticle(material, point.x, point.y, point.z, velocity.x, velocity.y, velocity.z);
        }
    }

    private record Impact(Vec3 origin, Vec3 axis, Vec3 right, Vec3 up, float strength,
                          double spin, boolean surface, long startTick) {
        Vec3 point(double radius, double angle, double depth) {
            double x = Math.cos(angle) * radius;
            double y = Math.sin(angle) * radius;
            return new Vec3(
                    origin.x + axis.x * depth + right.x * x + up.x * y,
                    origin.y + axis.y * depth + right.y * x + up.y * y,
                    origin.z + axis.z * depth + right.z * x + up.z * y);
        }
    }
}
