package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.anims.PoseBlender;
import com.sekwah.narutomod.network.s2c.TrigramPalmsPacket;
import com.sekwah.narutomod.util.TrigramSequence;
import com.sekwah.narutomod.util.TrigramSequence.Cue;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TrigramPalmsRenderer {
    private static final Map<Integer, Sequence> CASTS = new LinkedHashMap<>();
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Tint FIELD = new Tint(60, 186, 155);
    private static final Tint MINT = new Tint(167, 246, 226);
    private static final Tint WHITE = new Tint(245, 254, 252);
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private TrigramPalmsRenderer() {
    }

    public static void receive(TrigramPalmsPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        checkWorld(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        Sequence prior = CASTS.get(packet.actor());
        if (prior != null && prior.packet().cast() != packet.cast() && packet.cue() != Cue.STANCE) {
            return;
        }
        if (prior == null && CASTS.size() >= 20) {
            CASTS.remove(CASTS.keySet().iterator().next());
        }
        long now = level.getGameTime();
        CASTS.put(packet.actor(), new Sequence(packet, now,
                prior == null || packet.cue() == Cue.STANCE ? now : prior.start()));
    }

    public static void pose(HumanoidModel<?> model, LivingEntity actor, float partial) {
        var level = world.get();
        Sequence track = level == actor.level() ? CASTS.get(actor.getId()) : null;
        if (track == null) {
            return;
        }
        var cue = track.packet().cue();
        float age = level.getGameTime() - track.stamp() + partial;
        float weight = cue == Cue.BREAK || cue == Cue.SEALED ? Mth.clamp(1 - age / 7, 0, 1)
                : cue == Cue.STANCE ? Mth.clamp(age / 3, 0, 1) : 0.91f;
        if (cue == Cue.STANCE) {
            PoseBlender.rotate(model.rightArm, weight, -0.88f, -0.71f, 0.31f);
            PoseBlender.rotate(model.leftArm, weight, -0.65f, 0.47f, -0.38f);
            PoseBlender.rotate(model.rightLeg, weight * 0.65f, -0.21f, 0, 0.18f);
            PoseBlender.rotate(model.leftLeg, weight * 0.65f, 0.12f, 0, -0.17f);
            return;
        }
        float cadence = 1.13f + track.packet().phase() * 0.54f;
        float pulse = cue == Cue.SEALED ? 1 : Mth.sin(age * cadence);
        PoseBlender.rotate(model.rightArm, weight, -1.14f - 0.49f * pulse, -0.31f + pulse * 0.22f, 0.04f);
        PoseBlender.rotate(model.leftArm, weight, -1.14f + 0.49f * pulse, 0.31f + pulse * 0.22f, -0.04f);
        PoseBlender.addRotation(model.body, weight, 0.08f, 0.24f * pulse, 0);
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        checkWorld(level);
        double now = level.getGameTime() + partial;
        CASTS.values().removeIf(sequence -> now - sequence.stamp() > lifetime(sequence.packet().cue()));
        if (CASTS.isEmpty()) {
            return;
        }
        var buffer = buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE);
        boolean fast = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        for (Sequence cast : CASTS.values()) {
            Vec3 ground = cast.packet().anchor().subtract(camera).add(0, 0.038, 0);
            if (ground.lengthSqr() < 36 * 36) {
                drawCast(new Mesh(matrix, buffer), level, new Frame(ground, camera, partial, now, fast || ground.lengthSqr() > 22 * 22), cast);
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static int lifetime(Cue cue) {
        return switch (cue) {
            case STANCE, BURST -> 11;
            case SEALED -> 12;
            case BREAK -> 6;
        };
    }

    private static void drawCast(Mesh mesh, ClientLevel level, Frame frame, Sequence cast) {
        var packet = cast.packet();
        float age = (float) (frame.now() - cast.stamp());
        float approach = Mth.clamp((float) (frame.now() - cast.start()) / 7, 0, 1);
        float fade = switch (packet.cue()) {
            case BREAK -> Mth.clamp(1 - age / 5, 0, 1);
            case SEALED -> Mth.clamp(1 - age / 10, 0, 1);
            default -> 1;
        };
        double angle = (packet.cast() * 0.17) % (2 * Math.PI);
        ring(mesh, frame.ground(), approach, fade, angle, frame.low());
        if (packet.cue() == Cue.STANCE || packet.cue() == Cue.BREAK) {
            return;
        }
        Vec3 center = packet.contact().subtract(frame.camera());
        Vec3 body = level.getEntity(packet.actor()) instanceof LivingEntity actor
                ? actor.getPosition(frame.partial()).add(0, actor.getBbHeight() * 0.69, 0).subtract(frame.camera())
                : frame.ground().add(0, 1.2, 0);
        Vec3 face = center.subtract(body).normalize();
        Vec3 side = frameAxis(face);
        if (packet.cue() == Cue.SEALED) {
            points(mesh, center, side, age, frame.low());
        }
        if (age < (packet.cue() == Cue.SEALED ? 4 : 5)) {
            trails(mesh, new Contact(body, center, side, age, packet.phase(), frame.low()));
        }
    }

    private static void ring(Mesh mesh, Vec3 origin, float reveal, float fade, double yaw, boolean low) {
        int segments = low ? 32 : 56;
        double reach = TrigramSequence.RING;
        for (int i = 0; i < segments && i < segments * reveal; i++) {
            double a = yaw + 2 * Math.PI * i / segments;
            double b = yaw + 2 * Math.PI * (i + 1) / segments;
            mesh.quad(polar(origin, a, reach - 0.075), polar(origin, b, reach - 0.075), polar(origin, b, reach),
                    polar(origin, a, reach), FIELD, (int) (149 * fade));
            if (i % 2 == 0) {
                mesh.quad(polar(origin, a, reach * 0.87), polar(origin, b, reach * 0.87),
                        polar(origin, b, reach * 0.88), polar(origin, a, reach * 0.88), MINT, (int) (101 * fade));
            }
        }
        if (!low && reveal > 0.6f) {
            for (int trigram = 0; trigram < 8; trigram++) {
                glyph(mesh, origin, yaw + trigram * Math.PI / 4, trigram, fade);
            }
        }
    }

    private static void glyph(Mesh mesh, Vec3 base, double angle, int pattern, float fade) {
        for (int bit = 0; bit < 3; bit++) {
            double r = 2.57 + bit * 0.17;
            int alpha = (int) (131 * fade);
            if ((pattern & (1 << bit)) == 0) {
                mesh.ribbon(polar(base, angle - 0.12, r), polar(base, angle + 0.12, r),
                        polar(Vec3.ZERO, angle, 0.03), MINT, alpha);
            } else {
                mesh.ribbon(polar(base, angle - 0.12, r), polar(base, angle - 0.027, r),
                        polar(Vec3.ZERO, angle, 0.03), MINT, alpha);
                mesh.ribbon(polar(base, angle + 0.027, r), polar(base, angle + 0.12, r),
                        polar(Vec3.ZERO, angle, 0.03), MINT, alpha);
            }
        }
    }

    private static void trails(Mesh mesh, Contact c) {
        int streaks = Math.min(c.low() ? 5 : 12, TrigramSequence.strikes(c.phase()) + 1);
        for (int i = 0; i < streaks; i++) {
            float t = Mth.clamp((c.age() - i * (0.47f - c.phase() * 0.057f)) * (0.62f + c.phase() * 0.065f), 0, 1);
            if (t <= 0 || t >= 1) {
                continue;
            }
            double sign = i % 2 == 0 ? -1 : 1;
            Vec3 from = c.body().add(c.side().scale(0.39 * sign)).add(UP.scale(Math.sin(i * 2.14) * 0.24));
            Vec3 to = c.center().add(c.side().scale(sign * (0.10 + 0.037 * (i % 4))))
                    .add(UP.scale(0.21 * Math.cos(i * 2.37)));
            Vec3 slash = from.lerp(to, t);
            Vec3 trail = from.lerp(to, Math.max(0, t - 0.31));
            var band = UP.scale(0.015 + c.phase() * 0.002);
            mesh.ribbon(trail, slash, band, WHITE, (int) (198 * Math.sin(t * Math.PI)));
        }
    }

    private static void points(Mesh mesh, Vec3 center, Vec3 side, float age, boolean low) {
        int count = low ? 4 : 8;
        for (int i = 0; i < count; i++) {
            float flash = Mth.clamp(1 - Math.abs(age - i * 0.23f - 1.45f) / 2.4f, 0, 1);
            double y = (i / 2) * 0.19 - 0.32;
            Vec3 point = center.add(UP.scale(y)).add(side.scale((i % 2 == 0 ? 1 : -1) * 0.19));
            Vec3 a = UP.scale(0.068 * flash);
            Vec3 b = side.scale(0.055 * flash);
            mesh.quad(point.add(a), point.add(b), point.subtract(a), point.subtract(b), WHITE, (int) (255 * flash));
        }
    }

    private static Vec3 polar(Vec3 center, double angle, double radius) {
        return center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
    }

    private static Vec3 frameAxis(Vec3 face) {
        return face.horizontalDistanceSqr() < 0.001 ? new Vec3(1, 0, 0) : UP.cross(face).normalize();
    }

    private static void checkWorld(ClientLevel level) {
        if (world.get() != level) {
            CASTS.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Sequence(TrigramPalmsPacket packet, long stamp, long start) {
    }

    private record Frame(Vec3 ground, Vec3 camera, float partial, double now, boolean low) {
    }

    private record Contact(Vec3 body, Vec3 center, Vec3 side, float age, int phase, boolean low) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void ribbon(Vec3 a, Vec3 b, Vec3 width, Tint tint, int alpha) {
            quad(a.add(width), b.add(width), b.subtract(width), a.subtract(width), tint, alpha);
        }

        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Tint tint, int alpha) {
            vertex(a, tint, alpha);
            vertex(b, tint, alpha);
            vertex(c, tint, alpha);
            vertex(d, tint, alpha);
        }

        private void vertex(Vec3 pos, Tint tint, int alpha) {
            this.buffer.vertex(this.matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                    .color(tint.red(), tint.green(), tint.blue(), alpha).endVertex();
        }
    }

    private record Tint(int red, int green, int blue) {
    }
}
