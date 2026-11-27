package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.NagashiPacket;
import com.sekwah.narutomod.network.s2c.NagashiPacket.Phase;
import com.sekwah.narutomod.util.NagashiWave;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;

public final class NagashiRenderer {
    private static final int LIMIT = 64;
    private static final int WAVE_TICKS = NagashiWave.EXPANSION_TICKS + NagashiWave.RESIDUAL_TICKS;
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final ArrayDeque<Effect> EFFECTS = new ArrayDeque<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private NagashiRenderer() {
    }

    public static void accept(NagashiPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        onLevel(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (EFFECTS.size() >= LIMIT) {
            EFFECTS.removeFirst();
        }
        EFFECTS.addLast(new Effect(packet, level.getGameTime()));
    }

    public static float pose(Entity entity, float partialTick) {
        if (entity.level() != world.get()) {
            return 0;
        }
        for (var iterator = EFFECTS.descendingIterator(); iterator.hasNext();) {
            Effect effect = iterator.next();
            NagashiPacket packet = effect.packet();
            if (packet.actor() == entity.getId() && packet.phase() != Phase.HIT) {
                float age = entity.level().getGameTime() - effect.started() + partialTick;
                return packet.phase() == Phase.BRACE
                        ? Mth.clamp(0.4f + age * 0.20f, 0, 1) * Mth.clamp(5.0f - age, 0, 1)
                        : Mth.clamp(1.0f - age / 5.0f, 0, 1);
            }
        }
        return 0;
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers,
                     Vec3 camera, float partialTick) {
        onLevel(level);
        long now = level.getGameTime();
        EFFECTS.removeIf(effect -> now - effect.started() >= (effect.packet().phase() == Phase.FRONT ? WAVE_TICKS : 5));
        if (EFFECTS.isEmpty()) {
            return;
        }
        VertexConsumer buffer = buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW);
        boolean low = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        for (Effect effect : EFFECTS) {
            NagashiPacket packet = effect.packet();
            Entity entity = packet.phase() != Phase.FRONT ? level.getEntity(packet.actor()) : null;
            Vec3 location = entity == null || packet.phase() == Phase.FRONT ? packet.point()
                    : entity.getPosition(partialTick).add(0, entity.getBbHeight() * 0.57, 0);
            if (location.distanceToSqr(camera) > 48 * 48) {
                continue;
            }
            Vec3 origin = location.subtract(camera);
            float age = now - effect.started() + partialTick;
            if (packet.phase() == Phase.FRONT) {
                front(matrix, buffer, packet, origin, age, low);
            } else {
                nearBody(matrix, buffer, packet, origin, age);
            }
        }
        buffers.endBatch(NarutoRenderTypes.CHAKRA_GLOW);
    }

    private static void front(Matrix4f matrix, VertexConsumer buffer, NagashiPacket packet,
                              Vec3 origin, float age, boolean low) {
        float fade = 1.0f - Mth.clamp((age - NagashiWave.EXPANSION_TICKS) / NagashiWave.RESIDUAL_TICKS, 0, 1);
        double radius = NagashiWave.radius(age, packet.radius());
        int stride = low ? 2 : 1;
        for (int i = 0; i < NagashiWave.SPOKES; i += stride) {
            Vec3 forward = NagashiWave.direction(i);
            Vec3 side = forward.cross(UP);
            double reach = Math.min(radius, packet.reaches().get(i));
            Vec3 previous = origin.add(0, 0.22, 0);
            for (int step = 1; step <= 5; step++) {
                double t = step / 5.0;
                double bend = Mth.sin(i * 2.11f + step * 4.35f + age * 1.4f + (packet.seed() & 127))
                        * 0.15 * Math.sin(t * Math.PI);
                Vec3 next = origin.add(forward.scale(reach * t)).add(side.scale(bend))
                        .add(0, 0.20 + Math.abs(bend) * 0.66, 0);
                bolt(matrix, buffer, previous, next, UP, fade * (0.45f + (float) t * 0.44f));
                if (!low && step == 3) {
                    Vec3 fork = next.add(forward.scale(reach * 0.14)).add(side.scale(i % 2 == 0 ? 0.23 : -0.23));
                    bolt(matrix, buffer, next, fork, UP, fade * 0.42f);
                }
                previous = next;
            }
            int adjacent = (i + stride) % NagashiWave.SPOKES;
            if (reach >= radius - 0.05 && packet.reaches().get(adjacent) >= radius - 0.05 && radius > 0.2) {
                Vec3 rim = origin.add(NagashiWave.direction(adjacent).scale(radius)).add(0, 0.20, 0);
                bolt(matrix, buffer, previous, previous.lerp(rim, 0.56), forward, fade * 0.61f);
            }
        }
    }

    private static void nearBody(Matrix4f matrix, VertexConsumer buffer, NagashiPacket packet, Vec3 origin, float age) {
        float fade = Mth.clamp(1.0f - age / 5, 0, 1);
        boolean hitsTarget = packet.phase() == Phase.HIT;
        for (int i = 0; i < 6; i++) {
            Vec3 axis = NagashiWave.direction((i * 3 + Math.floorMod(packet.seed(), NagashiWave.SPOKES)) % NagashiWave.SPOKES);
            Vec3 a = origin.add(axis.scale(0.19)).add(0, i % 2 == 0 ? -0.38 : 0.31, 0);
            Vec3 b = origin.add(axis.scale(hitsTarget ? 0.53 : 0.34)).add(0, 0.18 * Mth.sin(age * 3f + i), 0);
            Vec3 c = origin.add(axis.scale(hitsTarget ? 0.27 : 0.52)).add(0, i % 2 == 0 ? 0.33 : -0.36, 0);
            Vec3 width = axis.cross(UP);
            bolt(matrix, buffer, a, b, width, fade);
            bolt(matrix, buffer, b, c, width, fade * 0.74f);
        }
    }

    private static void bolt(Matrix4f matrix, VertexConsumer buffer, Vec3 a, Vec3 b, Vec3 normal, float alpha) {
        Vec3 wide = normal.scale(0.043);
        quad(matrix, buffer, a, b, wide, 0.14f, 0.57f, alpha * 0.53f);
        quad(matrix, buffer, a, b, normal.scale(0.014), 0.72f, 0.94f, alpha);
        if (normal == UP) {
            quad(matrix, buffer, a, b, b.subtract(a).cross(UP).normalize().scale(0.034),
                    0.42f, 0.82f, alpha * 0.78f);
        }
    }

    private static void quad(Matrix4f matrix, VertexConsumer buffer, Vec3 a, Vec3 b, Vec3 width,
                             float red, float green, float alpha) {
        WaterDragonImpactRenderer.vertex(buffer, matrix, a.add(width), red, green, 1, alpha);
        WaterDragonImpactRenderer.vertex(buffer, matrix, a.subtract(width), red, green, 1, alpha);
        WaterDragonImpactRenderer.vertex(buffer, matrix, b.subtract(width), red, green, 1, alpha * 0.70f);
        WaterDragonImpactRenderer.vertex(buffer, matrix, b.add(width), red, green, 1, alpha * 0.70f);
    }

    private static void onLevel(ClientLevel level) {
        if (world.get() != level) {
            EFFECTS.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Effect(NagashiPacket packet, long started) {
    }
}
