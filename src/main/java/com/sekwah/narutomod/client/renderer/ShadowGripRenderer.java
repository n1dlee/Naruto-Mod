package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.ShadowGripPacket;
import com.sekwah.narutomod.network.s2c.ShadowGripPacket.Step;
import com.sekwah.narutomod.util.KamuiStep;
import com.sekwah.narutomod.util.ShadowGrip;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ShadowGripRenderer {
    private static final Map<Integer, Shape> SHAPES = new LinkedHashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private ShadowGripRenderer() {
    }

    public static void receive(ShadowGripPacket event) {
        ClientLevel level = Minecraft.getInstance().level;
        useLevel(level);
        if (level == null || !level.dimension().location().equals(event.dimension())) {
            return;
        }
        if (SHAPES.size() >= 24 && !SHAPES.containsKey(event.id())) {
            SHAPES.remove(SHAPES.keySet().iterator().next());
        }
        SHAPES.put(event.id(), new Shape(event, level.getGameTime()));
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        useLevel(level);
        long now = level.getGameTime();
        SHAPES.values().removeIf(shape -> now - shape.start() >= duration(shape.packet().step()));
        if (SHAPES.isEmpty()) {
            return;
        }
        boolean cheap = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        VertexConsumer dark = buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE);
        for (Shape shape : SHAPES.values()) {
            float age = now - shape.start() + partial;
            ShadowGripPacket packet = shape.packet();
            var frame = silhouette(level, packet, camera, partial);
            if (frame.feet().lengthSqr() <= 38 * 38) {
                var mesh = new Mesh(matrix, dark, frame);
                drawHand(mesh, packet.step(), age, cheap);
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static int duration(Step step) {
        return switch (step) {
            case LIFT -> ShadowGrip.LIFT + 4;
            case CLENCH, HOLD -> ShadowGrip.INTERVAL + 3;
            case DROP -> ShadowGrip.DROP;
        };
    }

    private static Frame silhouette(ClientLevel level, ShadowGripPacket packet, Vec3 camera, float partial) {
        Vec3 origin = packet.feet();
        float height = packet.height();
        float yaw = packet.id() * 17f;
        if (packet.step() != Step.DROP && level.getEntity(packet.target()) instanceof LivingEntity actor) {
            origin = actor.getPosition(partial);
            height = actor.getBbHeight();
            yaw = actor.getYRot();
        }
        Vec3 front = KamuiStep.facing(yaw);
        return new Frame(origin.subtract(camera), height, front, new Vec3(front.z, 0, -front.x));
    }

    private static void drawHand(Mesh mesh, Step step, float age, boolean cheap) {
        float rise = step == Step.LIFT ? Mth.clamp((age + 0.25f) / ShadowGrip.LIFT, 0, 1)
                : step == Step.DROP ? Mth.clamp(1 - age / ShadowGrip.DROP, 0, 1) : 1;
        double pulse = step == Step.CLENCH ? Math.exp(-age * 0.69) : 0;
        float alpha = step == Step.DROP ? rise * 0.84f : 0.87f;
        int pieces = cheap ? 5 : 9;
        for (int i = 0; i < pieces; i++) {
            double a = i / (double) pieces;
            double b = (i + 1.0) / pieces;
            Vec3 from = wrist(mesh.frame(), a * rise, pulse);
            Vec3 to = wrist(mesh.frame(), b * rise, pulse);
            Vec3 width = mesh.frame().right().scale(0.068 + Math.sin(a * Math.PI) * 0.109);
            mesh.line(from, to, width, alpha, false);
            if (!cheap) {
                mesh.line(from.add(width), to.add(width.scale(0.89)), width.scale(0.23), alpha * 0.54f, true);
            }
        }
        if (rise < 0.65f) {
            return;
        }
        int digits = cheap ? 3 : 5;
        for (int finger = 0; finger < digits; finger++) {
            double lane = finger / (digits - 1.0) - 0.5;
            finger(mesh, new Digit(lane, rise, pulse, alpha), cheap);
        }
    }

    private static Vec3 wrist(Frame frame, double rise, double pulse) {
        return frame.feet().add(frame.front().scale(-0.34 + 0.22 * rise))
                .add(frame.right().scale(Math.sin(rise * 7.8) * (0.17 - rise * 0.065)))
                .add(0, 0.048 + frame.height() * (0.78 * rise + pulse * 0.017), 0);
    }

    private static void finger(Mesh mesh, Digit digit, boolean cheap) {
        Frame frame = mesh.frame();
        Vec3 root = wrist(frame, digit.rise(), digit.pulse()).add(frame.right().scale(digit.lane() * 0.31));
        double radius = 0.26 - digit.pulse() * 0.064;
        Vec3 bend = frame.feet().add(frame.front().scale(-radius * 0.53))
                .add(frame.right().scale(digit.lane() * (0.38 + radius))).add(0, frame.height() * digit.rise() * 0.90, 0);
        Vec3 curl = bend.add(frame.front().scale(radius * 1.61)).add(frame.right().scale(-digit.lane() * 0.18))
                .add(0, -frame.height() * (0.09 + Math.abs(digit.lane()) * 0.06), 0);
        Vec3 width = frame.right().scale(0.043 - Math.abs(digit.lane()) * 0.018).add(0, 0.018, 0);
        mesh.line(root, bend, width, digit.alpha(), false);
        mesh.line(bend, curl, width.scale(0.58), digit.alpha() * 0.95f, false);
        if (!cheap) {
            mesh.line(bend.add(width), curl, width.scale(0.26), digit.alpha() * 0.60f, true);
        }
    }

    private static void useLevel(ClientLevel level) {
        if (world.get() != level) {
            SHAPES.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Shape(ShadowGripPacket packet, long start) {
    }

    private record Frame(Vec3 feet, float height, Vec3 front, Vec3 right) {
    }

    private record Digit(double lane, float rise, double pulse, float alpha) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer mesh, Frame frame) {
        private void line(Vec3 a, Vec3 b, Vec3 edge, float alpha, boolean rim) {
            vertex(a.subtract(edge), alpha, rim);
            vertex(b.subtract(edge.scale(0.54)), alpha * 0.78f, rim);
            vertex(b.add(edge.scale(0.58)), alpha * 0.54f, rim);
            vertex(a.add(edge), alpha * 0.83f, rim);
        }

        private void vertex(Vec3 point, float alpha, boolean rim) {
            float near = (float) Mth.clamp((point.length() - 0.24) * 2.1, 0, 1);
            mesh.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color(rim ? 0.20f : 0.029f, rim ? 0.13f : 0.019f, rim ? 0.27f : 0.052f, alpha * near).endVertex();
        }
    }
}
