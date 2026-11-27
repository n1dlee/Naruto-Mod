package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.network.s2c.BodyRitePacket;
import com.sekwah.narutomod.network.s2c.BodyRitePacket.Phase;
import com.sekwah.narutomod.util.EightGateCycle;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;

public final class BodyRiteRenderer {
    private static final int REED = 0xC8B87B;
    private static final int AMBER = 0xF0B557;
    private static final int CHALK = 0xD2D4C7;
    private static final Deque<Beat> BEATS = new ArrayDeque<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private BodyRiteRenderer() {
    }

    public static void receive(BodyRitePacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        use(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (packet.phase().ordinal() <= Phase.SAGE_EXIT.ordinal()) {
            BEATS.removeIf(beat -> beat.packet().actor() == packet.actor() && focus(beat.packet().phase()));
        }
        if (BEATS.size() >= 44) {
            BEATS.removeFirst();
        }
        BEATS.addLast(new Beat(packet, level.getGameTime()));
    }

    static void draw(ClientLevel level, Matrix4f pose, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        use(level);
        double now = level.getGameTime() + partial;
        BEATS.removeIf(beat -> now - beat.born() >= length(beat.packet().phase()));
        if (BEATS.isEmpty()) {
            return;
        }
        var mesh = new Mesh(pose, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        for (Beat beat : BEATS) {
            Vec3 at = beat.packet().at().subtract(camera).add(0, 0.028, 0);
            if (at.lengthSqr() < 44 * 44) {
                double age = (now - beat.born()) / length(beat.packet().phase());
                boolean reduced = lowDetail() || at.lengthSqr() > 24 * 24;
                cue(mesh, at, beat.packet(), Math.max(0, age), reduced);
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static boolean focus(Phase phase) {
        return phase == Phase.SAGE_DRAW || phase == Phase.SAGE_BALANCE || phase == Phase.SAGE_OVERFLOW;
    }

    private static int length(Phase phase) {
        return switch (phase) {
            case SAGE_DRAW, SAGE_BALANCE, SAGE_OVERFLOW -> 10;
            case GATE_OPEN, GATE_DEATH, SAGE_STONE -> 18;
            case SAGE_ENTER -> 20;
            default -> 12;
        };
    }

    private static void cue(Mesh mesh, Vec3 at, BodyRitePacket packet, double age, boolean low) {
        var phase = packet.phase();
        int power = packet.intensity();
        if (focus(phase)) {
            gather(mesh, at, phase, power, age, low);
            return;
        }
        boolean gate = phase.ordinal() >= Phase.GATE_OPEN.ordinal();
        int color = gate ? EightGateCycle.profile(Math.max(1, power)).tint()
                : phase == Phase.SAGE_ENTER ? AMBER : CHALK;
        double ease = 1 - (1 - age) * (1 - age);
        double span = gate ? 0.48 + power * 0.135 : 0.96;
        if (phase == Phase.SAGE_BREAK || phase == Phase.GATE_SPENT) {
            span *= 0.70;
        }
        int alpha = (int) ((low ? 95 : 180) * (1 - age) * (phase == Phase.GATE_WARNING ? 0.60 : 1));
        mesh.ring(new Ring(at, 0.30 + ease * span, 0.065 * (1 - age), 0.2 + age * 0.27), new Tone(color, alpha), low);
        if (!low && phase != Phase.GATE_WARNING) {
            mesh.ring(new Ring(at.add(0, 0.1 + age * 0.23, 0), 0.28 + ease * span * 0.69, 0.036, age), new Tone(color, alpha / 2), false);
        }
        if (phase == Phase.SAGE_STONE || phase == Phase.GATE_DEATH) {
            fractures(mesh, at, age, color, alpha, low);
        }
    }

    private static void gather(Mesh mesh, Vec3 at, Phase phase, int power, double age, boolean low) {
        double load = power / 100.0;
        int color = phase == Phase.SAGE_OVERFLOW ? CHALK : phase == Phase.SAGE_BALANCE ? AMBER : REED;
        int alpha = (int) ((low ? 82 : 132) * (0.40 + load * 0.58) * Math.sin(Math.PI * (0.11 + age * 0.89)));
        double radius = 0.55 + ((1 - age) * (0.72 + load * 0.47));
        mesh.ring(new Ring(at, radius, 0.048, age * -0.44), new Tone(color, alpha), low);
        if (!low) {
            mesh.ring(new Ring(at.add(0, 0.04 + 0.19 * age, 0), radius * 0.73, 0.034, age * 0.49), new Tone(color, alpha / 2), false);
        }
        if (phase == Phase.SAGE_OVERFLOW && power >= 45) {
            fractures(mesh, at, 0.1 + age * 0.4, CHALK, (int) (alpha * load), low);
        }
    }

    private static void fractures(Mesh mesh, Vec3 at, double age, int tint, int alpha, boolean low) {
        int shards = low ? 5 : 10;
        for (int i = 0; i < shards; i++) {
            double a = i * 2.399963;
            Vec3 radial = horizontal(a);
            Vec3 base = at.add(radial.scale(0.34 + age * 0.62)).add(0, 0.14 + (i % 3) * 0.22 + age * 0.32, 0);
            mesh.blade(new Stroke(base, base.add(radial.scale(0.12)).add(0, 0.31 * (1 - age), 0),
                    horizontal(a + Math.PI / 2).scale(0.037)), new Tone(tint, alpha));
        }
    }

    static boolean standing(Matrix4f pose, VertexConsumer buffer, Player actor, int gates, boolean sage, float partial) {
        if ((!sage && gates == 0) || actor.isInvisible() || actor.isDeadOrDying()) {
            return false;
        }
        double age = actor.tickCount + partial;
        var mesh = new Mesh(pose, buffer);
        Vec3 feet = actor.getPosition(partial);
        boolean low = lowDetail();
        if (sage && gates < EightGateCycle.LAST) {
            drawSageAura(mesh, feet, age, low);
        }
        if (gates > 0) {
            steam(mesh, feet, EightGateCycle.profile(gates), age, low);
        }
        return true;
    }

    private static void drawSageAura(Mesh mesh, Vec3 feet, double age, boolean low) {
        int count = low ? 3 : 7;
        for (int i = 0; i < count; i++) {
            double a = i * 2.399963 - age * 0.036;
            Vec3 start = feet.add(horizontal(a).scale(0.58)).add(0, 0.67 + 0.55 * Math.sin(a), 0);
            Vec3 tip = start.add(horizontal(a - 0.32).scale(0.13)).add(0, 0.29, 0);
            mesh.blade(new Stroke(start, tip, horizontal(a + Math.PI / 2).scale(0.026)), new Tone(AMBER, low ? 73 : 118));
        }
    }

    private static void steam(Mesh mesh, Vec3 feet, EightGateCycle.Palette profile, double age, boolean low) {
        int count = low ? 5 : 10;
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count;
            double breath = low ? 0.86 : 0.82 + 0.18 * Math.sin(age * 0.23 + angle * 4);
            Vec3 base = feet.add(horizontal(angle).scale(0.16 + profile.span() * 0.35)).add(0, 0.08, 0);
            Vec3 middle = base.add(horizontal(angle + 0.14).scale(profile.span() * 0.22)).add(0, breath * profile.height() * 0.56, 0);
            Vec3 tip = middle.add(horizontal(angle - 0.28).scale(0.12)).add(0, breath * profile.height() * 0.60, 0);
            Vec3 side = horizontal(angle + Math.PI / 2).scale(0.034 + profile.span() * 0.075);
            int alpha = (int) ((low ? 79 : 131) * breath);
            mesh.ribbon(new Ribbon(base, middle, tip, side), new Tone(profile.tint(), alpha));
        }
    }

    private static boolean lowDetail() {
        return NarutoConfig.reducedBodyRiteEffects || Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
    }

    private static Vec3 horizontal(double angle) {
        return new Vec3(Math.cos(angle), 0, Math.sin(angle));
    }

    private static void use(ClientLevel level) {
        if (world.get() != level) {
            BEATS.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Beat(BodyRitePacket packet, long born) {
    }

    private record Tone(int rgb, int alpha) {
    }

    private record Ring(Vec3 center, double radius, double thickness, double spin) {
    }

    private record Stroke(Vec3 base, Vec3 tip, Vec3 width) {
    }

    private record Ribbon(Vec3 base, Vec3 middle, Vec3 tip, Vec3 width) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void ring(Ring ring, Tone tone, boolean low) {
            int pieces = low ? 13 : 24;
            for (int i = 0; i < pieces; i++) {
                double a = ring.spin() + i * Math.PI * 2 / pieces;
                double b = a + (i % 3 == 0 ? 0.48 : 0.85) * Math.PI * 2 / pieces;
                Vec3 first = horizontal(a);
                Vec3 last = horizontal(b);
                vertex(ring.center().add(first.scale(ring.radius())), tone, tone.alpha());
                vertex(ring.center().add(last.scale(ring.radius())), tone, tone.alpha());
                vertex(ring.center().add(last.scale(ring.radius() - ring.thickness())), tone, 0);
                vertex(ring.center().add(first.scale(ring.radius() - ring.thickness())), tone, 0);
            }
        }

        private void stroke(Vec3 base, Vec3 tip, Vec3 width, Tone tone, int lower, int upper) {
            vertex(base.add(width), tone, lower);
            vertex(tip.add(width.scale(0.34)), tone, upper);
            vertex(tip.subtract(width.scale(0.34)), tone, upper);
            vertex(base.subtract(width), tone, lower);
        }

        private void blade(Stroke blade, Tone tone) {
            stroke(blade.base(), blade.tip(), blade.width(), tone, tone.alpha(), 0);
        }

        private void ribbon(Ribbon ribbon, Tone tone) {
            stroke(ribbon.base(), ribbon.middle(), ribbon.width(), tone, 0, tone.alpha());
            blade(new Stroke(ribbon.middle(), ribbon.tip(), ribbon.width().scale(0.45)), tone);
        }

        private void vertex(Vec3 at, Tone tone, int alpha) {
            int rgb = tone.rgb();
            buffer.vertex(matrix, (float) at.x, (float) at.y, (float) at.z)
                    .color((rgb >>> 16) & 255, (rgb >>> 8) & 255, rgb & 255, alpha).endVertex();
        }
    }
}
