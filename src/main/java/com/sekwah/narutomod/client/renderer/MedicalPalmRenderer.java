package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.MedicalPalmPacket;
import com.sekwah.narutomod.util.JutsuVfx;
import com.sekwah.narutomod.util.MedicalPalm;
import com.sekwah.narutomod.util.MedicalPalm.Phase;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MedicalPalmRenderer {
    private static final Map<Integer, Palm> HANDS = new LinkedHashMap<>();
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Tint GREEN = new Tint(69, 208, 139);
    private static final Tint JADE = new Tint(142, 242, 184);
    private static final Tint LIGHT = new Tint(215, 254, 199);
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private MedicalPalmRenderer() {
    }

    public static void receive(MedicalPalmPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        checkWorld(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        long now = level.getGameTime();
        Palm palm = HANDS.get(packet.actor());
        if (palm == null || packet.phase() == Phase.GATHER) {
            if (palm == null && HANDS.size() >= 24) {
                HANDS.remove(HANDS.keySet().iterator().next());
            }
            palm = new Palm(now);
            HANDS.put(packet.actor(), palm);
        }
        palm.packet = packet;
        palm.updated = now;
        if (packet.phase() == Phase.HEAL && packet.healthGain() > 0) {
            palm.heal = new Pulse(packet.palm(), packet.contact(), packet.healthGain(), now);
        }
    }

    public static float weight(LivingEntity actor, float partial) {
        ClientLevel level = world.get();
        Palm palm = actor.level() == level ? HANDS.get(actor.getId()) : null;
        return palm == null ? 0 : weight(palm, level.getGameTime() + partial);
    }

    private static float weight(Palm palm, double now) {
        if (palm.packet.phase() == Phase.RELEASE) {
            return (float) Mth.clamp(1 - (now - palm.updated) / 5, 0, 1);
        }
        return (float) Mth.clamp((now - palm.start) / MedicalPalm.GATHER, 0, 1);
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        checkWorld(level);
        double now = level.getGameTime() + partial;
        HANDS.values().removeIf(palm -> now - palm.updated > (palm.packet.phase() == Phase.RELEASE ? 8 : 12));
        if (HANDS.isEmpty()) {
            return;
        }
        var mesh = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        boolean fast = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        for (Palm palm : HANDS.values()) {
            MedicalPalmPacket packet = palm.packet;
            Vec3 hand = level.getEntity(packet.actor()) instanceof Player actor && packet.phase() != Phase.RELEASE
                    ? JutsuVfx.handPosition(actor, packet.dominantRight() ? 1 : -1, partial) : packet.palm();
            Vec3 root = hand.subtract(camera);
            if (root.lengthSqr() < 32 * 32) {
                boolean low = fast || root.lengthSqr() > 19 * 19;
                gather(mesh, root, (now - palm.start) * 0.34, weight(palm, now), low);
                if (palm.heal != null && now - palm.heal.time() < 8) {
                    pulse(mesh, palm.heal, camera, now, low);
                }
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static void gather(Mesh mesh, Vec3 root, double rotation, float warm, boolean low) {
        double radius = 0.085 + 0.045 * warm;
        int count = low ? 7 : 12;
        for (int i = 0; i < count; i++) {
            double angle = rotation + i * Math.PI * 2 / count;
            Vec3 a = root.add(Math.cos(angle) * radius, Math.sin(angle * 1.4) * 0.10, Math.sin(angle) * radius);
            Vec3 b = root.add(Math.cos(angle + 0.73) * radius * 0.54, -0.028, Math.sin(angle + 0.73) * radius * 0.54);
            mesh.ribbon(a, b, UP.scale(0.013), JADE, (int) (21 + 124 * warm));
        }
    }

    private static void pulse(Mesh mesh, Pulse pulse, Vec3 camera, double now, boolean low) {
        double t = Mth.clamp((now - pulse.time()) / 8, 0, 1);
        Vec3 palm = pulse.from().subtract(camera);
        Vec3 target = pulse.to().subtract(camera);
        Vec3 aim = target.subtract(palm).normalize();
        Vec3 axis = aim.horizontalDistanceSqr() < 0.002 ? new Vec3(1, 0, 0) : UP.cross(aim).normalize();
        double gain = Mth.clamp(pulse.gain(), 0.2, 1.5);
        int count = low ? 3 : 6;
        for (int i = 0; i < count; i++) {
            double phase = Mth.clamp(t * 2.2 - i * 0.082, 0, 1);
            if (phase == 0 || phase == 1) {
                continue;
            }
            Vec3 drift = axis.scale(Math.sin(phase * 5.2 + i) * 0.11 * (1 - phase));
            Vec3 front = palm.lerp(target, phase).add(drift);
            Vec3 tail = palm.lerp(target, Math.max(0, phase - 0.16)).add(drift.scale(0.29));
            mesh.ribbon(tail, front, UP.scale(0.021), i % 2 == 0 ? JADE : LIGHT, (int) ((175 - i * 8) * (1 - t)));
        }
        local(mesh, target, t, gain, low);
    }

    private static void local(Mesh mesh, Vec3 center, double time, double gain, boolean low) {
        if (time < 0.22) {
            return;
        }
        double t = (time - 0.22) / 0.78;
        int count = low ? 6 : 12;
        for (int i = 0; i < count; i++) {
            double v = i * Math.PI * 2 / count;
            double radius = (0.072 + t * 0.51) * (0.74 + gain * 0.16);
            Vec3 a = center.add(Math.cos(v) * radius, Math.sin(v * 2) * 0.095 + t * 0.22, Math.sin(v) * radius);
            Vec3 b = center.add(Math.cos(v - 0.31) * radius * 0.76, Math.sin(v * 2 - 0.62) * 0.09 + t * 0.15,
                    Math.sin(v - 0.31) * radius * 0.76);
            mesh.ribbon(a, b, UP.scale(0.013), i % 3 == 0 ? LIGHT : GREEN, (int) (161 * (1 - t)));
        }
    }

    private static void checkWorld(ClientLevel level) {
        if (world.get() != level) {
            HANDS.clear();
            world = new WeakReference<>(level);
        }
    }

    private static final class Palm {
        private final long start;
        private long updated;
        private MedicalPalmPacket packet;
        private Pulse heal;

        private Palm(long start) {
            this.start = start;
        }
    }

    private record Pulse(Vec3 from, Vec3 to, float gain, long time) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void ribbon(Vec3 a, Vec3 b, Vec3 width, Tint tint, int alpha) {
            vertex(a.add(width), tint, alpha);
            vertex(b.add(width), tint, alpha);
            vertex(b.subtract(width), tint, alpha);
            vertex(a.subtract(width), tint, alpha);
        }

        private void vertex(Vec3 point, Tint tint, int alpha) {
            this.buffer.vertex(this.matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color(tint.red(), tint.green(), tint.blue(), alpha).endVertex();
        }
    }

    private record Tint(int red, int green, int blue) {
    }
}
