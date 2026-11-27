package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.entity.jutsuprojectile.ClayBombEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.ClayBombEntity.Phase;
import com.sekwah.narutomod.util.ClayFlight;
import com.sekwah.narutomod.util.ProjectileAxis;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class ClayBombRenderer extends EntityRenderer<ClayBombEntity> {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 FRONT = new Vec3(0, 0, 1);
    private static final int CLAY = 0xD8CDC1;
    private static final int EDGE = 0xFBF4E8;
    private static final int SHADE = 0xB4A493;
    private static final int SHADOW = 0x776758;
    private static final int AMBER = 0xFFBC5C;

    public ClayBombRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override
    public void render(ClayBombEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partial, pose, buffers, light);
        var game = Minecraft.getInstance();
        boolean low = game.options.graphicsMode().get() == GraphicsStatus.FAST
                || game.getCameraEntity() != null && entity.distanceToSqr(game.getCameraEntity()) > 27 * 27;
        double age = entity.age() + partial;
        Phase phase = entity.phase();
        Matrix4f matrix = pose.last().pose();
        if (phase == Phase.BURST || phase == Phase.FIZZLE) {
            burst(new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE)), age, entity.getId(), phase == Phase.BURST, low);
            return;
        }
        double amount = phase == Phase.MOLD ? Mth.clamp((age + 1) / ClayFlight.MOLD, 0.16, 1) : 1;
        var basis = Basis.along(entity.axis());
        var figure = new Figure(basis, amount, age, phase == Phase.OUTBOUND, low);
        var clay = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.BOULDER_BODY));
        if (entity.crawler()) {
            spider(clay, figure, entity.grounded());
        } else {
            bird(clay, figure);
        }
        if (phase == Phase.FUSE) {
            var lightMesh = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW));
            crack(lightMesh, basis, ClayFlight.flash(entity.age(), partial));
        }
    }

    private static void bird(Mesh mesh, Figure figure) {
        double t = figure.amount();
        Basis b = figure.basis();
        mesh.body(b, Vec3.ZERO, 0.35 * t, 0.14 * t, 0.13 * t);
        mesh.body(b, b.point(0.24 * t, 0.045 * t, 0), 0.17 * t, 0.106 * t, 0.089 * t);
        double wingSpread = 0.60 * t;
        double flex = figure.moving() ? Math.sin(figure.age() * 1.34) * 0.21 : 0.14 + (1 - t) * 0.29;
        for (int sign : new int[]{-1, 1}) {
            Vec3 joint = b.point(0.05 * t, 0.045 * t, sign * 0.12 * t);
            Vec3 wrist = b.point(-0.12 * t, flex * t, sign * wingSpread * 0.62);
            Vec3 feather = b.point(-0.29 * t, flex * t * 1.36, sign * wingSpread);
            Vec3 sweep = b.point(-0.31 * t, 0.008 * t, sign * 0.16 * t);
            mesh.quad(joint, wrist, feather, sweep, EDGE, 255);
            mesh.quad(joint, sweep, feather, wrist, SHADE, 255);
            if (!figure.low()) {
                Vec3 fold = wrist.lerp(feather, 0.73).add(b.up().scale(0.008));
                mesh.quad(joint.lerp(sweep, 0.33), wrist, fold, sweep.lerp(feather, 0.76), CLAY, 255);
            }
        }
        Vec3 tail = b.point(-0.45 * t, 0, 0);
        mesh.quad(b.point(-0.22 * t, 0, 0.13 * t), tail.add(b.side().scale(0.085 * t)),
                tail.subtract(b.side().scale(0.085 * t)), b.point(-0.22 * t, 0, -0.13 * t), CLAY, 255);
    }

    private static void spider(Mesh mesh, Figure figure, boolean grounded) {
        double t = figure.amount();
        Basis b = figure.basis();
        mesh.body(b, b.point(-0.05 * t, 0.01, 0), 0.255 * t, 0.175 * t, 0.13 * t);
        mesh.body(b, b.point(0.13 * t, 0, 0), 0.166 * t, 0.11 * t, 0.09 * t);
        for (int i = 0; i < 8; i++) {
            int segment = i / 2;
            int sign = i % 2 == 0 ? -1 : 1;
            double step = grounded && figure.moving() ? Math.sin(figure.age() * 1.55 + i * 1.24) : 0;
            double forward = (1.5 - segment) * 0.10 * t;
            Vec3 socket = b.point(forward, 0, sign * 0.11 * t);
            Vec3 knee = b.point(forward + 0.07 * t * step, (0.052 + 0.046 * step) * t, sign * 0.28 * t);
            Vec3 foot = b.point(forward + 0.08 * t * step, (-0.20 + 0.05 * Math.max(0, step)) * t, sign * 0.36 * t);
            mesh.link(socket, knee, b.up().scale(0.037 * t), EDGE);
            mesh.link(knee, foot, b.forward().scale(0.030 * t), SHADE);
        }
    }

    private static void crack(Mesh mesh, Basis b, double flash) {
        Vec3 a = b.point(-0.17, 0.10, -0.04);
        Vec3 mid = b.point(-0.035, 0.139, 0.035);
        Vec3 tip = b.point(0.085, 0.116, 0.055);
        int alpha = (int) (232 * flash);
        mesh.ribbon(a, mid, b.side().scale(0.014), AMBER, alpha);
        mesh.ribbon(mid, tip, b.side().scale(0.013), EDGE, alpha);
    }

    private static void burst(Mesh mesh, double age, int seed, boolean blast, boolean low) {
        double t = Mth.clamp(age / ClayFlight.BURST, 0, 1);
        int count = low ? 10 : 23;
        double spread = blast ? 3.23 : 0.64;
        for (int i = 0; i < count; i++) {
            double spin = i * 2.399963 + (seed & 63) * 0.096;
            double leap = 0.42 + (i % 5) * 0.14;
            Vec3 direction = new Vec3(Math.cos(spin), 0, Math.sin(spin));
            Vec3 at = direction.scale(spread * (1 - Math.pow(1 - t, 1.7)) * leap)
                    .add(0, spread * (0.08 + 0.083 * (i % 5)) * Math.sin(t * Math.PI * 0.86), 0);
            Vec3 side = UP.cross(direction).scale((0.072 + i % 4 * 0.032) * (1 - t * 0.55));
            Vec3 longAxis = direction.add(0, -0.18 + i % 3 * 0.27, 0).scale(blast ? 0.19 : 0.102);
            int opacity = (int) (207 * (1 - t));
            mesh.quad(at.add(longAxis), at.add(side), at.subtract(longAxis), at.subtract(side),
                    i % 4 == 0 && blast ? AMBER : i % 3 == 0 ? SHADE : EDGE, opacity);
        }
        if (blast) {
            wave(mesh, t, low);
        }
    }

    private static void wave(Mesh mesh, double t, boolean low) {
        int parts = low ? 12 : 24;
        double radius = 0.37 + 3.05 * (1 - (1 - t) * (1 - t));
        for (int i = 0; i < parts; i++) {
            double from = Math.PI * 2 * i / parts;
            double to = Math.PI * 2 * (i + 1) / parts;
            Vec3 a = new Vec3(Math.cos(from) * radius, 0.083, Math.sin(from) * radius);
            Vec3 b = new Vec3(Math.cos(to) * radius, 0.083, Math.sin(to) * radius);
            mesh.quad(a, a.scale(0.91), b.scale(0.91), b, i % 4 == 0 ? AMBER : CLAY, (int) (144 * (1 - t)));
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ClayBombEntity entity) { return TextureAtlas.LOCATION_BLOCKS; }

    private record Figure(Basis basis, double amount, double age, boolean moving, boolean low) {
    }

    private record Basis(Vec3 forward, Vec3 side, Vec3 up) {
        private static Basis along(Vec3 facing) {
            Vec3 side = ProjectileAxis.direction((Math.abs(facing.y) > 0.9 ? FRONT : UP).cross(facing), new Vec3(1, 0, 0));
            return new Basis(facing, side, facing.cross(side));
        }

        private Vec3 point(double z, double y, double x) { return this.forward.scale(z).add(this.up.scale(y)).add(this.side.scale(x)); }
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void body(Basis b, Vec3 center, double length, double width, double height) {
            Vec3 nose = center.add(b.forward().scale(length * 0.81));
            Vec3 rear = center.subtract(b.forward().scale(length));
            for (int i = 0; i < 6; i++) {
                double angle = i * Math.PI / 3;
                double next = angle + Math.PI / 3;
                Vec3 a = center.add(b.side().scale(width * Math.cos(angle))).add(b.up().scale(height * Math.sin(angle)));
                Vec3 c = center.add(b.side().scale(width * Math.cos(next))).add(b.up().scale(height * Math.sin(next)));
                int color = i < 3 ? EDGE : i == 5 ? SHADE : CLAY;
                quad(nose, a, c, nose, color, 255);
                quad(a, rear, rear, c, color, 255);
            }
        }

        private void link(Vec3 a, Vec3 b, Vec3 width, int color) {
            ribbon(a, b, width, color, 255);
            ribbon(a, b, b.subtract(a).cross(width).normalize().scale(width.length() * 0.67), SHADOW, 255);
        }

        private void ribbon(Vec3 a, Vec3 b, Vec3 width, int color, int alpha) {
            quad(a.add(width), b.add(width.scale(0.63)), b.subtract(width.scale(0.63)), a.subtract(width), color, alpha);
        }

        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, int alpha) {
            vertex(a, color, alpha);
            vertex(b, color, alpha);
            vertex(c, color, alpha);
            vertex(d, color, alpha);
        }

        private void vertex(Vec3 p, int color, int alpha) {
            this.buffer.vertex(this.matrix, (float) p.x, (float) p.y, (float) p.z)
                    .color((color >>> 16) & 255, (color >>> 8) & 255, color & 255, alpha).endVertex();
        }
    }
}
