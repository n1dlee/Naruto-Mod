package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket.Phase;
import com.sekwah.narutomod.util.BijudamaProfile;
import com.sekwah.narutomod.util.SpectralSword;
import com.sekwah.narutomod.util.SusanooPose;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;

public final class FinalChakraRenderer {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 EAST = new Vec3(1, 0, 0);
    private static final int GOLD = 0xFFD67A;
    private static final Deque<Cue> CUES = new ArrayDeque<>();
    private static final Deque<Cue> SHELL_CONTACTS = new ArrayDeque<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private FinalChakraRenderer() {
    }

    public static void receive(FinalChakraPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        use(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (packet.phase() == Phase.SHELL_BREAK) {
            CUES.removeIf(cue -> cue.packet.subject() == packet.subject()
                    && (cue.packet.phase() == Phase.SWORD || cue.packet.phase() == Phase.YASAKA_GATHER));
        }
        if (packet.phase() == Phase.YASAKA_STOP || packet.phase() == Phase.SWORD_STOP) {
            Phase action = packet.phase() == Phase.YASAKA_STOP ? Phase.YASAKA_GATHER : Phase.SWORD;
            CUES.removeIf(cue -> cue.packet.subject() == packet.subject() && cue.packet.phase() == action
                    && cue.born() <= packet.started());
            return;
        }
        long born = packet.started() >= 0 ? packet.started() : level.getGameTime();
        if (packet.phase() == Phase.SHELL_CRACK || packet.phase() == Phase.SHELL_BREAK) {
            if (level.getGameTime() - born < life(packet)) {
                if (SHELL_CONTACTS.size() >= 32) SHELL_CONTACTS.removeFirst();
                SHELL_CONTACTS.addLast(new Cue(packet, born));
            }
            return;
        }
        if (packet.phase() == Phase.YASAKA_GATHER || packet.phase() == Phase.SWORD) {
            if (level.getGameTime() - born >= life(packet) || CUES.stream().anyMatch(cue ->
                    cue.packet.subject() == packet.subject() && cue.packet.phase() == packet.phase() && cue.born() >= born)) {
                return;
            }
            CUES.removeIf(cue -> cue.packet.subject() == packet.subject() && cue.packet.phase() == packet.phase());
        }
        if (CUES.size() >= 54) {
            CUES.removeFirst();
        }
        CUES.addLast(new Cue(packet, born));
    }

    public static float swordProgress(Player actor, float partial) {
        Action pose = action(actor, Phase.SWORD, partial);
        return pose == null ? -1 : (float) (pose.age() / SpectralSword.length(pose.stage()));
    }

    static Action action(LivingEntity actor, Phase phase, float partial) {
        if (actor.level() != world.get()) {
            return null;
        }
        var descending = CUES.descendingIterator();
        while (descending.hasNext()) {
            Cue cue = descending.next();
            if (cue.packet().subject() == actor.getId() && cue.packet().phase() == phase) {
                double age = actor.level().getGameTime() - cue.born() + partial;
                if (age < life(cue.packet())) {
                    var shape = cue.packet().shape();
                    return new Action(Math.max(0, age), cue.packet().yaw(), shape.axis(), shape.point(), (int) shape.size());
                }
            }
        }
        return null;
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        use(level);
        double time = level.getGameTime() + partial;
        CUES.removeIf(cue -> time - cue.born >= life(cue.packet));
        SHELL_CONTACTS.removeIf(cue -> time - cue.born >= life(cue.packet));
        if (CUES.isEmpty() && SHELL_CONTACTS.isEmpty()) {
            return;
        }
        var mesh = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW));
        for (Cue cue : CUES) {
            var packet = cue.packet;
            Vec3 at = packet.shape().point().subtract(camera);
            int range = packet.phase() == Phase.SWORD && packet.shape().size() == 4 ? 104 : 82;
            if (at.lengthSqr() < range * range) {
                var frame = new Frame(at, Math.max(0, time - cue.born), lowDetail() || at.lengthSqr() > 36 * 36);
                paint(mesh, packet, frame);
            }
        }
        for (Cue cue : SHELL_CONTACTS) {
            Vec3 at = cue.packet.shape().point().subtract(camera);
            if (at.lengthSqr() < 76 * 76) {
                shellContact(mesh, cue, new Frame(at, Math.max(0, time - cue.born), lowDetail() || at.lengthSqr() > 36 * 36));
            }
        }
        buffers.endBatch(NarutoRenderTypes.CHAKRA_GLOW);
    }

    private static void paint(Mesh mesh, FinalChakraPacket packet, Frame frame) {
        float size = packet.shape().size();
        int rgb = packet.shape().tint();
        Vec3 axis = packet.shape().axis();
        if (packet.phase() == Phase.SWORD) {
            sword(mesh, frame, axis, (int) size, rgb);
        } else if (packet.phase() != Phase.YASAKA_GATHER) {
            double t = frame.age() / life(packet);
            double reach = size * (0.13 + 0.87 * Math.min(1, t * 1.8));
            int alpha = (int) ((frame.low() ? 92 : 167) * (1 - t));
            var tone = new Tone(rgb, alpha);
            if (packet.phase() == Phase.BIJU_HIT) {
                sphere(mesh, frame.at(), reach, tone, frame.low());
            } else {
                var plane = basis(packet.phase() == Phase.YASAKA_RELEASE ? axis : UP);
                mesh.ring(new Ring(frame.at().add(0, 0.045, 0), plane, reach, 0.05 + reach * 0.027), tone, frame.low());
                if (packet.phase() == Phase.YASAKA_HIT || packet.phase() == Phase.BIJU_CHARGE_BREAK) {
                    fragments(mesh, frame.at(), reach, t, tone, frame.low());
                }
            }
        }
    }

    private static void shellContact(Mesh mesh, Cue cue, Frame frame) {
        var packet = cue.packet();
        var shape = packet.shape();
        boolean broken = packet.phase() == Phase.SHELL_BREAK;
        double t = frame.age() / life(packet);
        double dim = frame.at().lengthSqr() < 2.25 ? .42 : 1;
        var plane = basis(shape.axis());
        Vec3 at = frame.at().add(shape.axis().scale(.02 + .13 * t));
        double reach = shape.size() * (.22 + (broken ? .93 : .68) * t);
        var echo = new Tone(shape.tint(), (int) ((broken ? 160 : 123) * (1 - t) * dim));
        mesh.ring(new Ring(at, plane, reach, Math.min(reach * .18, .035 + shape.size() * .013)), echo, frame.low());
        var flash = new Tone(0xFFF0CD, (int) (182 * (1 - t) * (1 - t) * dim));
        mesh.ring(new Ring(at, plane, reach * .46, reach * .066), flash, frame.low());
        var hit = new PanelHit(at, plane, shape.axis(), reach, t, cue);
        panelFracture(mesh, hit, echo, frame.low());
    }

    private static void panelFracture(Mesh mesh, PanelHit hit, Tone echo, boolean low) {
        int count = low ? 3 : hit.broken() ? 8 : 5;
        for (int i = 0; i < count; i++) {
            double turn = (hit.cue().born() % 17) * .15 + i * Math.PI * 2 / count;
            Vec3 line = hit.plane().heading(turn);
            Vec3 bend = hit.plane().heading(turn + (i % 2 == 0 ? .39 : -.42));
            double radius = hit.reach() * (.60 + (i % 3) * .16);
            Vec3 root = hit.at().add(line.scale(radius * (hit.broken() ? .2 + .95 * hit.age() : .14)));
            Vec3 joint = root.add(line.scale(radius * .57));
            Vec3 tip = joint.add(bend.scale(radius * .29));
            if (hit.broken()) {
                Vec3 lift = hit.normal().scale(hit.age() * radius * (1 + (i % 3) * .28));
                joint = joint.add(lift);
                tip = tip.add(lift.scale(1.55));
                mesh.quad(root, joint, tip, root.add(bend.scale(radius * .18)), echo);
            } else {
                mesh.stroke(root, joint, Math.min(.047, radius * .07), echo);
                mesh.stroke(joint, tip, Math.min(.034, radius * .048), echo);
            }
        }
    }

    private static void sword(Mesh mesh, Frame frame, Vec3 axis, int stage, int tint) {
        float yaw = (float) Math.toDegrees(Math.atan2(-axis.x, axis.z));
        int first = SpectralSword.startTick(stage);
        int last = SpectralSword.endTick(stage);
        double edge = Mth.clamp(frame.age(), first, last);
        var head = SpectralSword.sample(frame.at(), yaw, stage, edge);
        var before = SpectralSword.sample(frame.at(), yaw, stage, edge - 1);
        int alpha = frame.low() ? 76 : 139;
        if (frame.age() >= first && frame.age() < last + 2) {
            var tail = SpectralSword.sample(frame.at(), yaw, stage, edge - (frame.low() ? 1 : 3));
            mesh.quad(head.guard(), head.tip(), tail.tip(), tail.guard(), new Tone(tint, alpha / 3));
            mesh.quad(head.guard(), head.tip(), before.tip(), before.guard(), new Tone(tint, alpha));
        }
    }

    static boolean wearer(Matrix4f matrix, VertexConsumer sink, Player actor, INinjaData data, float partial) {
        if (actor.isInvisible() || actor.isDeadOrDying()) {
            return false;
        }
        if (data.isKcmActive() && !data.isKuramaCloakActive()) {
            kcmTrails(new Mesh(matrix, sink), actor, data.getKcmFormAge(), partial);
            return true;
        }
        return false;
    }

    private static void kcmTrails(Mesh mesh, Player actor, int phase, float partial) {
        Vec3 at = actor.getPosition(partial);
        Vec3 drift = actor.position().subtract(actor.xo, actor.yo, actor.zo).multiply(1, 0, 1);
        double speed = Math.min(drift.length(), 0.92);
        float show = SpectralSword.formation(phase + partial);
        if (actor.isSprinting() && speed > 0.045) {
            Vec3 heading = drift.normalize();
            Vec3 side = heading.cross(UP).scale(0.25);
            for (int sign : new int[]{-1, 1}) {
                Vec3 shoe = at.add(side.scale(sign)).add(0, 0.19, 0);
                Vec3 shoulder = at.add(side.scale(sign * 1.35)).add(0, 1.37, 0);
                mesh.stroke(shoe, shoe.subtract(heading.scale(0.38 + speed * 3)), 0.06, new Tone(GOLD, (int) (122 * show)));
                if (!lowDetail()) {
                    mesh.stroke(shoulder, shoulder.subtract(heading.scale(speed * 2)), 0.032, new Tone(GOLD, (int) (81 * show)));
                }
            }
        }
    }

    public static void bijuCore(Matrix4f matrix, MultiBufferSource buffers, Vec3 point, Vec3 direction, Core frame) {
        double radius = BijudamaProfile.core(frame.blast(), frame.progress());
        var dark = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        dark.orb(point, radius, new Tone(0x10081D, 231));
        var glow = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW));
        sphere(glow, point, radius, new Tone(frame.tint(), frame.charging() ? 140 : 170), lowDetail());
        if (frame.charging()) {
            spiral(glow, point, direction, radius, frame);
        }
    }

    private static void spiral(Mesh mesh, Vec3 at, Vec3 axis, double radius, Core frame) {
        var plane = basis(axis);
        int count = lowDetail() ? 8 : 17;
        for (int sign : new int[]{-1, 1}) {
            Vec3 previous = null;
            for (int i = 0; i <= count; i++) {
                double t = (double) i / count;
                double angle = sign * (frame.age() * 0.21 + t * 6.6) + (sign > 0 ? 0 : Math.PI);
                Vec3 point = at.add(plane.heading(angle).scale(radius + t * (1.65 + radius * 0.55)))
                        .add(axis.scale((t - 0.5) * (radius + 0.86) * sign));
                if (previous != null) {
                    mesh.stroke(previous, point, 0.055, new Tone(sign > 0 ? frame.tint() : 0xCC668D, (int) (145 * (1 - t))));
                }
                previous = point;
            }
        }
    }

    private static void fragments(Mesh mesh, Vec3 at, double radius, double age, Tone ink, boolean low) {
        for (int i = 0; i < (low ? 6 : 15); i++) {
            double angle = i * 2.399963;
            Vec3 signed = new Vec3(Math.cos(angle), ((i % 5) - 2) * 0.31, Math.sin(angle));
            Vec3 root = at.add(signed.scale(radius));
            mesh.stroke(root, root.add(signed.scale(0.3 + (1 - age) * 0.66)), 0.08, ink);
        }
    }

    private static void sphere(Mesh mesh, Vec3 at, double radius, Tone tone, boolean low) {
        double band = 0.04 + radius * 0.021;
        mesh.ring(new Ring(at, basis(UP), radius, band), tone, low);
        mesh.ring(new Ring(at, basis(EAST), radius, band), tone, low);
        mesh.ring(new Ring(at, basis(new Vec3(0.23, 0.18, 0.95)), radius, band), tone, low);
    }

    private static Plane basis(Vec3 normal) {
        Vec3 axis = normal.normalize();
        Vec3 right = axis.cross(Math.abs(axis.y) > 0.9 ? EAST : UP).normalize();
        if (right.lengthSqr() < 0.5) {
            return new Plane(EAST, UP);
        }
        return new Plane(right, axis.cross(right).normalize());
    }

    private static int life(FinalChakraPacket packet) {
        return switch (packet.phase()) {
            case SWORD -> SpectralSword.length((int) packet.shape().size());
            case SHELL_BREAK, KCM_ENTER, KCM_EXIT -> 17;
            case SHELL_CRACK -> 12;
            case YASAKA_GATHER -> SusanooPose.THROW_LENGTH;
            case BIJU_HIT -> 26;
            default -> 11;
        };
    }

    private static boolean lowDetail() {
        return NarutoConfig.reducedBodyRiteEffects || Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
    }

    private static void use(ClientLevel level) {
        if (world.get() != level) {
            CUES.clear();
            SHELL_CONTACTS.clear();
            world = new WeakReference<>(level);
        }
    }

    public record Core(float progress, float blast, int tint, float age, boolean charging) {
    }

    private record Cue(FinalChakraPacket packet, long born) {
    }

    record Action(double age, float yaw, Vec3 aim, Vec3 origin, int stage) {
    }

    private record Frame(Vec3 at, double age, boolean low) {
    }

    private record Plane(Vec3 first, Vec3 second) {
        private Vec3 heading(double angle) {
            return first.scale(Math.cos(angle)).add(second.scale(Math.sin(angle)));
        }
    }

    private record Ring(Vec3 at, Plane plane, double radius, double thickness) {
    }

    private record PanelHit(Vec3 at, Plane plane, Vec3 normal, double reach, double age, Cue cue) {
        private boolean broken() {
            return cue.packet().phase() == Phase.SHELL_BREAK;
        }
    }

    private record Tone(int tint, int alpha) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer sink) {
        private void ring(Ring ring, Tone ink, boolean low) {
            int steps = low ? 14 : 30;
            for (int i = 0; i < steps; i++) {
                Vec3 a = ring.plane().heading(i * Math.PI * 2 / steps);
                Vec3 b = ring.plane().heading((i + 1) * Math.PI * 2 / steps);
                quad(ring.at().add(a.scale(ring.radius())), ring.at().add(b.scale(ring.radius())),
                        ring.at().add(b.scale(ring.radius() - ring.thickness())),
                        ring.at().add(a.scale(ring.radius() - ring.thickness())), ink);
            }
        }

        private void orb(Vec3 at, double radius, Tone tone) {
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * i / 6;
                double b = Math.PI * (i + 1) / 6;
                for (int j = 0; j < 12; j++) {
                    double c = Math.PI * 2 * j / 12;
                    double d = Math.PI * 2 * (j + 1) / 12;
                    quad(at.add(unit(a, c).scale(radius)), at.add(unit(a, d).scale(radius)),
                            at.add(unit(b, d).scale(radius)), at.add(unit(b, c).scale(radius)), tone);
                }
            }
        }

        private static Vec3 unit(double elevation, double turn) {
            return new Vec3(Math.sin(elevation) * Math.cos(turn), Math.cos(elevation), Math.sin(elevation) * Math.sin(turn));
        }

        private void stroke(Vec3 a, Vec3 b, double width, Tone tone) {
            Vec3 line = b.subtract(a);
            Vec3 side = line.cross(Math.abs(line.normalize().y) > 0.93 ? EAST : UP).normalize().scale(width);
            quad(a.add(side), b.add(side), b.subtract(side), a.subtract(side), tone);
        }

        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Tone tone) {
            vertex(a, tone);
            vertex(b, tone);
            vertex(c, tone);
            vertex(d, tone);
        }

        private void vertex(Vec3 p, Tone tone) {
            int tint = tone.tint();
            sink.vertex(matrix, (float) p.x, (float) p.y, (float) p.z)
                    .color((tint >>> 16) & 255, (tint >>> 8) & 255, tint & 255, tone.alpha()).endVertex();
        }
    }
}
