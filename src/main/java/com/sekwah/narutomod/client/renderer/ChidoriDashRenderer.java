package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.ChidoriDashPacket;
import com.sekwah.narutomod.network.s2c.ChidoriDashPacket.Phase;
import com.sekwah.narutomod.util.JutsuVfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ChidoriDashRenderer {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final ArrayDeque<Effect> TRAIL = new ArrayDeque<>();
    private static final Map<Integer, Effect> ACTORS = new LinkedHashMap<>();
    private static WeakReference<ClientLevel> activeLevel = new WeakReference<>(null);

    private ChidoriDashRenderer() {
    }

    public static void accept(ChidoriDashPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        useLevel(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        var effect = new Effect(packet, level.getGameTime());
        ACTORS.put(packet.actor(), effect);
        if (ACTORS.size() > 64) {
            ACTORS.remove(ACTORS.keySet().iterator().next());
        }
        if (packet.phase() != Phase.BRACE && packet.phase() != Phase.RECOVER) {
            if (TRAIL.size() >= 72) {
                TRAIL.removeFirst();
            }
            TRAIL.addLast(effect);
        }
    }

    @Nullable
    public static DashPose pose(Entity actor, float partialTick) {
        if (actor.level() != activeLevel.get()) {
            return null;
        }
        Effect effect = ACTORS.get(actor.getId());
        if (effect == null) {
            return null;
        }
        float age = actor.level().getGameTime() - effect.started() + partialTick;
        Phase phase = effect.packet().phase();
        if (age >= duration(phase)) {
            return null;
        }
        float weight = switch (phase) {
            case BRACE -> Mth.clamp(0.35f + age * 0.22f, 0.0f, 1.0f);
            case TRAVEL -> 1.0f;
            default -> 1.0f - age / duration(phase);
        };
        return new DashPose(phase, weight);
    }

    static void render(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partialTick) {
        useLevel(level);
        long now = level.getGameTime();
        TRAIL.removeIf(effect -> now - effect.started() >= 7);
        ACTORS.values().removeIf(effect -> now - effect.started() >= duration(effect.packet().phase()));
        if (TRAIL.isEmpty() && ACTORS.isEmpty()) {
            return;
        }
        VertexConsumer buffer = buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW);
        for (Effect effect : TRAIL) {
            ChidoriDashPacket packet = effect.packet();
            if (packet.end().distanceToSqr(camera) > 64.0 * 64.0) {
                continue;
            }
            float age = now - effect.started() + partialTick;
            float fade = Mth.clamp(1.0f - age / 7, 0, 1);
            if (packet.phase() == Phase.TRAVEL) {
                drawTrail(matrix, buffer, packet, camera, fade);
            } else {
                drawFlash(matrix, buffer, packet, camera, age, fade);
            }
        }
        for (Effect effect : ACTORS.values()) {
            Entity actor = level.getEntity(effect.packet().actor());
            if (actor != null && actor.position().distanceToSqr(camera) < 64.0 * 64.0) {
                drawHand(matrix, buffer, effect, actor, camera, partialTick);
            }
        }
        buffers.endBatch(NarutoRenderTypes.CHAKRA_GLOW);
    }

    private static void useLevel(ClientLevel level) {
        if (activeLevel.get() != level) {
            activeLevel = new WeakReference<>(level);
            TRAIL.clear();
            ACTORS.clear();
        }
    }

    private static void drawTrail(Matrix4f matrix, VertexConsumer buffer, ChidoriDashPacket packet, Vec3 camera, float fade) {
        Vec3 from = packet.start().subtract(camera);
        Vec3 to = packet.end().subtract(camera);
        Vec3 side = packet.heading().cross(UP);
        beam(matrix, buffer, from, to, side.scale(0.18), fade * 0.38f, false);
        beam(matrix, buffer, from, to, UP.scale(0.10), fade * 0.46f, false);
        beam(matrix, buffer, from, to, side.scale(0.045), fade * 0.84f, true);
        for (int sign : new int[] {-1, 1}) {
            Vec3 heel = from.add(side.scale(sign * 0.24)).subtract(0, 0.87, 0);
            Vec3 root = heel.lerp(to.subtract(0, 0.80, 0), 0.62);
            Vec3 spark = root.add(side.scale(sign * 0.22)).add(0, 0.26, 0);
            beam(matrix, buffer, heel, root, UP.scale(0.027), fade * 0.70f, true);
            beam(matrix, buffer, root, spark, side.scale(0.027), fade * 0.72f, false);
        }
    }

    private static void drawFlash(Matrix4f matrix, VertexConsumer buffer, ChidoriDashPacket packet,
                                  Vec3 camera, float age, float fade) {
        Vec3 center = packet.end().subtract(camera);
        Vec3 lateral = packet.heading().cross(UP);
        boolean wall = packet.phase() == Phase.BLOCK;
        for (int i = 0; i < 8; i++) {
            double angle = Mth.TWO_PI * i / 8;
            Vec3 ray = lateral.scale(Math.cos(angle)).add(UP.scale(Math.sin(angle)));
            Vec3 tip = center.add(ray.scale((wall ? 0.72 : 1.11) + age * 0.10))
                    .subtract(packet.heading().scale(wall ? 0.22 : 0.03));
            beam(matrix, buffer, center, tip, packet.heading().scale(0.09), fade * fade, i % 2 == 0);
        }
    }

    private static void drawHand(Matrix4f matrix, VertexConsumer buffer, Effect effect, Entity actor,
                                 Vec3 camera, float partialTick) {
        Phase phase = effect.packet().phase();
        if (phase != Phase.BRACE && phase != Phase.TRAVEL) {
            return;
        }
        Vec3 forward = effect.packet().heading();
        Vec3 side = forward.cross(UP);
        Vec3 hand = actor instanceof Player player
                ? JutsuVfx.handPosition(player, player.getMainArm() == HumanoidArm.RIGHT ? 1.0 : -1.0, partialTick)
                : actor.getPosition(partialTick).add(side.scale(0.40)).add(0, actor.getBbHeight() * 0.61, 0);
        Vec3 origin = hand.subtract(camera);
        Vec3 point = origin.add(forward.scale(phase == Phase.TRAVEL ? 0.85 : 0.39));
        float pulse = 0.83f + 0.17f * Mth.sin((actor.tickCount + partialTick) * 2.4f);
        beam(matrix, buffer, origin, point, UP.scale(0.13), pulse, false);
        beam(matrix, buffer, origin, point, side.scale(0.045), pulse, true);
        beam(matrix, buffer, origin.add(side.scale(-0.22)), point, UP.scale(0.046), pulse * 0.79f, true);
        beam(matrix, buffer, origin.add(side.scale(0.22)), point, UP.scale(0.046), pulse * 0.79f, true);
    }

    private static void beam(Matrix4f matrix, VertexConsumer buffer, Vec3 start, Vec3 end, Vec3 width,
                             float alpha, boolean core) {
        float red = core ? 0.72f : 0.09f;
        float green = core ? 0.96f : 0.46f;
        vertex(matrix, buffer, start.subtract(width), red, green, alpha);
        vertex(matrix, buffer, start.add(width), red, green, alpha);
        vertex(matrix, buffer, end.add(width.scale(0.08)), red, green, alpha * 0.12f);
        vertex(matrix, buffer, end.subtract(width.scale(0.08)), red, green, alpha * 0.12f);
    }

    private static void vertex(Matrix4f matrix, VertexConsumer buffer, Vec3 point, float r, float g, float alpha) {
        buffer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z).color(r, g, 1.0f, alpha).endVertex();
    }

    private static int duration(Phase phase) {
        return phase == Phase.BRACE ? 8 : phase == Phase.TRAVEL ? 4 : 10;
    }

    public record DashPose(Phase phase, float weight) {
    }

    private record Effect(ChidoriDashPacket packet, long started) {
    }
}
