package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.network.s2c.GatsugaPacket;
import com.sekwah.narutomod.util.GatsugaMotion;
import com.sekwah.narutomod.util.GatsugaMotion.Stage;
import com.sekwah.narutomod.util.ProjectileAxis;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GatsugaRenderer {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 FRONT = new Vec3(0, 0, 1);
    private static final Tint AMBER = new Tint(226, 184, 138);
    private static final Tint BLUE = new Tint(118, 197, 220);
    private static final Tint WHITE = new Tint(242, 251, 247);
    private static final Map<Integer, Rig> RIGS = new LinkedHashMap<>();
    private static final List<Trail> TRAILS = new ArrayList<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private GatsugaRenderer() {
    }

    public static void receive(GatsugaPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        use(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        long now = level.getGameTime();
        if (TRAILS.size() >= 144) {
            TRAILS.remove(0);
        }
        TRAILS.add(new Trail(packet, now));
        if (packet.stage() == Stage.CONTACT) {
            return;
        }
        Rig old = RIGS.get(packet.body());
        if (old == null && RIGS.size() >= 30) {
            RIGS.remove(RIGS.keySet().iterator().next());
        }
        long start = old == null || old.cast() != packet.cast() || packet.stage() == Stage.BRACE ? now : old.start();
        RIGS.put(packet.body(), new Rig(packet.cast(), packet.stage(), packet.wolf(), start, now));
    }

    public static void orient(PoseStack pose, LivingEntity actor, float partial) {
        var level = world.get();
        Rig rig = actor.level() == level ? RIGS.get(actor.getId()) : null;
        if (rig == null) {
            return;
        }
        float age = level.getGameTime() - rig.start() + partial;
        float since = level.getGameTime() - rig.seen() + partial;
        float weight = switch (rig.stage()) {
            case BRACE -> Mth.clamp(age / GatsugaMotion.WINDUP, 0, 1) * 0.49f;
            case TRAVEL -> 1;
            default -> Mth.clamp(1 - since / GatsugaMotion.FADE, 0, 1);
        };
        if (weight <= 0) {
            return;
        }
        double pivot = actor.getBbHeight() * 0.47;
        pose.translate(0, pivot, 0);
        pose.mulPose(Axis.XP.rotationDegrees((rig.wolf() ? 15 : 30) * weight));
        if (rig.stage() != Stage.BRACE) {
            float angle = Mth.wrapDegrees(Math.max(0, age - GatsugaMotion.WINDUP) * (rig.wolf() ? -102 : 86));
            pose.mulPose((rig.wolf() ? Axis.ZP : Axis.YP).rotationDegrees(angle * weight));
        }
        pose.translate(0, -pivot, 0);
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        use(level);
        double now = level.getGameTime() + partial;
        TRAILS.removeIf(trail -> now - trail.time() >= lifetime(trail.packet().stage()));
        RIGS.values().removeIf(rig -> now - rig.seen() > 9);
        if (TRAILS.isEmpty()) {
            return;
        }
        var mesh = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        boolean cheap = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        for (Trail sample : TRAILS) {
            var packet = sample.packet();
            Vec3 from = packet.start().subtract(camera);
            if (from.lengthSqr() > 34 * 34) {
                continue;
            }
            float age = (float) ((now - sample.time()) / lifetime(packet.stage()));
            Vec3 to = packet.end().subtract(camera);
            var frame = new Frame(from, to, packet.wolf(), packet.cast(), age);
            if (packet.stage() == Stage.TRAVEL) {
                wind(mesh, frame, cheap || from.lengthSqr() > 20 * 20, from.lengthSqr() < 1.1 ? 0.46f : 1);
            } else {
                ring(mesh, frame, packet.stage());
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static int lifetime(Stage stage) {
        return switch (stage) {
            case BRACE -> GatsugaMotion.WINDUP;
            case TRAVEL -> 6;
            case BLOCK, CONTACT -> 5;
            case STOP -> GatsugaMotion.FADE;
        };
    }

    private static void wind(Mesh mesh, Frame frame, boolean low, float near) {
        int count = low ? 10 : 16;
        var basis = frame.basis();
        for (int strand = 0; strand < 2; strand++) {
            for (int q = 0; q < count; q++) {
                double a = q / (double) count;
                double b = (q + 1) / (double) count;
                Vec3 start = helix(frame, basis, a, strand);
                Vec3 end = helix(frame, basis, b, strand);
                Vec3 rim = start.subtract(frame.from().lerp(frame.to(), a)).normalize().scale(0.024 + 0.019 * a);
                float envelope = (float) (Math.sin(a * Math.PI) * 0.45 + 0.55);
                mesh.ribbon(start, end, rim, strand == 0 ? frame.tint() : WHITE,
                        (int) ((strand == 0 ? 183 : 123) * envelope * (1 - frame.age()) * near));
            }
        }
    }

    private static Vec3 helix(Frame frame, Basis basis, double progress, int strand) {
        double spin = frame.seed() * 0.16 + strand * Math.PI + progress * 6.8 - frame.age() * 7.1;
        double radius = (frame.wolf() ? 0.43 : 0.61) * (0.72 + 0.25 * Math.sin(progress * Math.PI))
                * (1 + 0.21 * frame.age());
        return frame.from().lerp(frame.to(), progress).add(basis.right().scale(Math.cos(spin) * radius))
                .add(basis.up().scale(Math.sin(spin) * radius));
    }

    private static void ring(Mesh mesh, Frame frame, Stage stage) {
        var basis = frame.basis();
        Vec3 center = stage == Stage.CONTACT ? frame.to() : frame.from();
        float alpha = stage == Stage.BRACE ? (float) Math.sin(frame.age() * Math.PI) : 1 - frame.age();
        double radius = (frame.wolf() ? 0.30 : 0.43) + frame.age() * (stage == Stage.BRACE ? 0.20 : 0.52);
        for (int i = 0; i < 9; i++) {
            double angle = 2 * Math.PI * i / 9 + frame.age() * 0.7;
            Vec3 a = basis.right().scale(Math.cos(angle)).add(basis.up().scale(Math.sin(angle)));
            Vec3 b = basis.right().scale(Math.cos(angle + 0.37)).add(basis.up().scale(Math.sin(angle + 0.37)));
            mesh.ribbon(center.add(a.scale(radius)), center.add(b.scale(radius)), a.scale(0.033),
                    i % 2 == 0 ? WHITE : frame.tint(), (int) (176 * alpha));
        }
    }

    private static void use(ClientLevel level) {
        if (world.get() != level) {
            RIGS.clear();
            TRAILS.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Trail(GatsugaPacket packet, long time) {
    }

    private record Rig(int cast, Stage stage, boolean wolf, long start, long seen) {
    }

    private record Basis(Vec3 right, Vec3 up) {
    }

    private record Frame(Vec3 from, Vec3 to, boolean wolf, int seed, float age) {
        private Basis basis() {
            Vec3 forward = ProjectileAxis.direction(this.to.subtract(this.from), FRONT);
            Vec3 right = forward.cross(Math.abs(forward.y) > 0.94 ? new Vec3(1, 0, 0) : UP).normalize();
            return new Basis(right, right.cross(forward));
        }

        private Tint tint() { return this.wolf ? BLUE : AMBER; }
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void ribbon(Vec3 from, Vec3 to, Vec3 width, Tint tint, int alpha) {
            vertex(from.add(width), tint, alpha);
            vertex(to.add(width), tint, alpha);
            vertex(to.subtract(width), tint, alpha);
            vertex(from.subtract(width), tint, alpha);
        }

        private void vertex(Vec3 point, Tint color, int alpha) {
            this.buffer.vertex(this.matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color(color.r(), color.g(), color.b(), alpha).endVertex();
        }
    }

    private record Tint(int r, int g, int b) {
    }
}
