package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.LeapFxPacket;
import com.sekwah.narutomod.network.s2c.LeapFxPacket.Phase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LeapFxRenderer {
    private static final Map<Integer, Effect> POSES = new LinkedHashMap<>();
    private static final ArrayDeque<Effect> CONTACTS = new ArrayDeque<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private LeapFxRenderer() {
    }

    public static void accept(LeapFxPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        useLevel(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (packet.phase() == Phase.CANCEL) {
            POSES.remove(packet.actor());
            return;
        }
        var effect = new Effect(packet, level.getGameTime());
        POSES.put(packet.actor(), effect);
        if (POSES.size() > 64) {
            POSES.remove(POSES.keySet().iterator().next());
        }
        if (packet.phase() != Phase.BRACE) {
            if (CONTACTS.size() >= 32) {
                CONTACTS.removeFirst();
            }
            CONTACTS.addLast(effect);
            dust(level, packet);
        }
    }

    private static void dust(ClientLevel level, LeapFxPacket packet) {
        var state = Block.stateById(packet.material());
        boolean wet = packet.phase() == Phase.WATER || state.is(net.minecraft.world.level.block.Blocks.WATER);
        if (!wet && state.isAir()) {
            return;
        }
        var particle = wet ? ParticleTypes.SPLASH : new BlockParticleOption(ParticleTypes.BLOCK, state);
        int count = 3 + (int) (packet.strength() * 7);
        Vec3 side = packet.forward().cross(new Vec3(0, 1, 0));
        for (int i = 0; i < count; i++) {
            double offset = (i % 2 == 0 ? -1 : 1) * (0.14 + level.random.nextDouble() * 0.38);
            Vec3 point = packet.point().add(side.scale(offset));
            Vec3 velocity = side.scale(offset * 0.48).add(0, 0.08 + 0.19 * packet.strength(), 0);
            level.addParticle(particle, point.x, point.y, point.z, velocity.x, velocity.y, velocity.z);
        }
    }

    @Nullable
    public static LeapPose pose(Entity entity, float partialTick) {
        Effect effect = POSES.get(entity.getId());
        if (entity.level() != world.get() || effect == null) {
            return null;
        }
        float age = entity.level().getGameTime() - effect.started() + partialTick;
        if (age >= duration(effect.packet().phase())) {
            return null;
        }
        return new LeapPose(effect.packet().phase(), age, effect.packet().strength());
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers,
                     Vec3 camera, float partialTick) {
        useLevel(level);
        long now = level.getGameTime();
        CONTACTS.removeIf(effect -> now - effect.started() >= 8);
        POSES.values().removeIf(effect -> now - effect.started() >= duration(effect.packet().phase()));
        if (CONTACTS.isEmpty()) {
            return;
        }
        VertexConsumer buffer = buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE);
        for (Effect effect : CONTACTS) {
            LeapFxPacket packet = effect.packet();
            if (packet.point().distanceToSqr(camera) > 48 * 48) {
                continue;
            }
            float age = now - effect.started() + partialTick;
            Vec3 origin = packet.point().subtract(camera);
            int color = color(level, packet);
            if (packet.phase() == Phase.BOOST) {
                boost(matrix, buffer, origin, age, packet.strength());
            } else {
                waves(matrix, buffer, origin, packet, age, color);
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static int color(ClientLevel level, LeapFxPacket packet) {
        if (packet.phase() == Phase.WATER) {
            return 0x9adef6;
        }
        int tint = Block.stateById(packet.material()).getMapColor(level, BlockPos.containing(packet.point())).col;
        return tint == 0 ? 0x9da1a6 : tint;
    }

    private static void waves(Matrix4f matrix, VertexConsumer buffer, Vec3 origin,
                              LeapFxPacket packet, float age, int color) {
        float alpha = Mth.clamp(1.0f - age / 8.0f, 0, 1) * (0.25f + packet.strength() * 0.51f);
        double width = 0.14 + packet.strength() * 0.17;
        Vec3 side = packet.forward().cross(new Vec3(0, 1, 0));
        for (int sign = -1; sign <= 1; sign += 2) {
            Vec3 a = origin.add(side.scale(sign * (0.25 + age * width)));
            Vec3 b = a.add(packet.forward().scale(-0.54 - width));
            Vec3 c = a.add(packet.forward().scale(0.65 + width));
            Vec3 d = a.add(side.scale(sign * (0.19 + age * 0.035))).add(0, 0.07 + age * 0.015, 0);
            vertex(matrix, buffer, b, color, alpha * 0.09f);
            vertex(matrix, buffer, a, color, alpha);
            vertex(matrix, buffer, c, color, alpha * 0.14f);
            vertex(matrix, buffer, d, color, 0);
        }
    }

    private static void boost(Matrix4f matrix, VertexConsumer buffer, Vec3 origin, float age, float strength) {
        float fade = Mth.clamp(1f - age / 8f, 0, 1);
        double radius = 0.13 + age * (0.12 + strength * 0.04);
        for (int curl = 0; curl < 2; curl++) {
            double spin = age * -0.26 + curl * Math.PI;
            for (int i = 0; i < 11; i++) {
                double a = spin + Mth.TWO_PI * (i / 24.0);
                double b = a + Mth.TWO_PI / 24.0;
                Vec3 from = origin.add(Math.cos(a) * radius, 0.05 - age * 0.030, Math.sin(a) * radius);
                Vec3 to = origin.add(Math.cos(b) * radius, 0.05 - age * 0.030, Math.sin(b) * radius);
                Vec3 edge = new Vec3(Math.cos(a) * 0.06, 0.035, Math.sin(a) * 0.06);
                float alpha = fade * Mth.sin((i + 0.5f) * Mth.PI / 11) * 0.79f;
                vertex(matrix, buffer, from, 0xc9f7ff, alpha);
                vertex(matrix, buffer, to, 0xb0e4ff, alpha * 0.94f);
                vertex(matrix, buffer, to.add(edge), 0x68bafa, 0);
                vertex(matrix, buffer, from.add(edge), 0x68bafa, 0);
            }
        }
    }

    private static void vertex(Matrix4f matrix, VertexConsumer buffer, Vec3 point, int rgb, float alpha) {
        buffer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f, alpha).endVertex();
    }

    private static int duration(Phase phase) {
        return phase == Phase.TAKEOFF ? 72 : phase == Phase.BRACE ? 5 : phase == Phase.BOOST ? 8 : 6;
    }

    private static void useLevel(ClientLevel level) {
        if (world.get() != level) {
            world = new WeakReference<>(level);
            CONTACTS.clear();
            POSES.clear();
        }
    }

    public record LeapPose(Phase phase, float age, float strength) {
    }

    private record Effect(LeapFxPacket packet, long started) {
    }
}
