package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.entity.jutsuprojectile.IronSandEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.IronSandEntity.Stage;
import com.sekwah.narutomod.util.IronSandFlight;
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

public final class IronSandRenderer extends EntityRenderer<IronSandEntity> {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 RIGHT = new Vec3(1, 0, 0);
    private static final int[] FACETS = {0x171C25, 0x353D4A, 0x606B78, 0x232934, 0x4C5361, 0x292F39};
    private static final int[] GRAINS = {0x758091, 0x4D5D71, 0x232B36, 0x981F36};

    public IronSandRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override
    public void render(IronSandEntity sand, float yaw, float partial, PoseStack poses, MultiBufferSource buffers, int light) {
        super.render(sand, yaw, partial, poses, buffers, light);
        Minecraft game = Minecraft.getInstance();
        boolean low = game.options.graphicsMode().get() == GraphicsStatus.FAST
                || game.getCameraEntity() != null && sand.distanceToSqr(game.getCameraEntity()) > 27 * 27;
        var frame = new Frame(Basis.around(sand.axis()), sand.getId(), sand.age() + partial, low);
        Matrix4f matrix = poses.last().pose();
        Stage stage = sand.stage();
        if (stage == Stage.GATHER || stage == Stage.FLY) {
            double mass = stage == Stage.FLY ? 1 : IronSandFlight.density(frame.age());
            var core = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.BOULDER_BODY));
            wedge(core, frame.basis(), mass);
            var mist = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
            orbit(mist, frame, mass);
        } else {
            var burst = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
            impact(burst, frame, Vec3.atLowerCornerOf(sand.face().getNormal()), stage);
        }
    }

    private static void wedge(Mesh mesh, Basis basis, double mass) {
        if (mass < 0.035) {
            return;
        }
        double width = IronSandFlight.RADIUS * mass;
        Vec3 tip = basis.forward().scale(width);
        Vec3 neck = basis.forward().scale(-0.38 * width);
        Vec3 tail = basis.forward().scale(-2.14 * width);
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3;
            Vec3 a = neck.add(basis.radial(angle).scale(width));
            Vec3 b = neck.add(basis.radial(angle + Math.PI / 3).scale(width));
            mesh.quad(tip, a, b, tip, FACETS[i], 255);
            mesh.quad(a, tail, tail, b, FACETS[(i + 1) % 6], 255);
            if (i == 2 || i == 4) {
                Vec3 edge = a.lerp(b, 0.27).scale(1.018);
                Vec3 inner = a.lerp(b, 0.37).scale(1.018);
                mesh.quad(edge, inner, tip.scale(0.98), tip.scale(0.98), 0x9BA8B9, 255);
            }
        }
    }

    private static void orbit(Mesh mesh, Frame frame, double mass) {
        int count = frame.low() ? 13 : 34;
        Basis frameBasis = frame.basis();
        for (int i = 0; i < count; i++) {
            double value = noise(frame.seed(), i);
            double wave = i * 2.399963 + frame.age() * (0.38 + (i % 4) * 0.09);
            Vec3 turn = frameBasis.radial(wave);
            double distance = (0.43 + 0.62 * value) * (1 - mass * 0.65);
            Vec3 point = frameBasis.forward().scale((value - 0.68) * (0.91 + mass * 0.72))
                    .add(turn.scale(distance * (0.78 + 0.24 * Math.sin(wave * 0.83))));
            double size = 0.013 + value * 0.024;
            mesh.grain(point, turn.scale(size), frameBasis.forward().scale(size * (1.0 + mass * 2.1)),
                    GRAINS[i % GRAINS.length], (int) (113 + 100 * value));
        }
    }

    private static void impact(Mesh mesh, Frame frame, Vec3 normal, Stage stage) {
        Basis aim = frame.basis();
        boolean solid = stage == Stage.BLOCK;
        Basis burst = solid ? Basis.around(normal) : aim;
        double t = Mth.clamp(frame.age() / IronSandFlight.SCATTER, 0, 1);
        int count = frame.low() ? 12 : 31;
        for (int i = 0; i < count; i++) {
            double value = noise(frame.seed() ^ 0x3A61, i);
            Vec3 radial = burst.radial(i * 2.399963 + value * 0.39);
            Vec3 drift = radial.scale((solid ? 0.16 : 0.08) * (0.31 + value))
                    .add(burst.forward().scale(solid ? 0.055 + 0.053 * value : 0.16 * (value - 0.45)));
            Vec3 head = IronSandFlight.scatter(solid ? normal.scale(0.047) : Vec3.ZERO, drift, frame.age());
            double size = (0.017 + value * 0.058) * (1 - t * 0.7);
            Vec3 along = drift.lengthSqr() > 1.0e-5 ? drift.normalize() : burst.forward();
            mesh.grain(head, radial.scale(size), along.scale(size * 1.8), GRAINS[i % GRAINS.length], (int) (207 * (1 - t)));
        }
    }

    private static double noise(int seed, int index) {
        int step = (index + 1) * 0x45D9F3B ^ seed * 0x1973;
        step = (step ^ (step >>> 16)) * 0x45D9F3B;
        return (step & 1023) / 1023.0;
    }

    @Override
    public ResourceLocation getTextureLocation(IronSandEntity entity) { return TextureAtlas.LOCATION_BLOCKS; }

    private record Frame(Basis basis, int seed, double age, boolean low) {
    }

    private record Basis(Vec3 forward, Vec3 side, Vec3 up) {
        private static Basis around(Vec3 axis) {
            Vec3 side = (Math.abs(axis.y) > 0.93 ? RIGHT : UP).cross(axis).normalize();
            return new Basis(axis, side, axis.cross(side).normalize());
        }

        private Vec3 radial(double angle) { return this.side.scale(Math.cos(angle)).add(this.up.scale(Math.sin(angle))); }
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void grain(Vec3 middle, Vec3 side, Vec3 longAxis, int rgb, int alpha) {
            quad(middle.subtract(longAxis), middle.add(side), middle.add(longAxis), middle.subtract(side), rgb, alpha);
        }

        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int rgb, int alpha) {
            vertex(a, rgb, alpha);
            vertex(b, rgb, alpha);
            vertex(c, rgb, alpha);
            vertex(d, rgb, alpha);
        }

        private void vertex(Vec3 point, int rgb, int alpha) {
            this.buffer.vertex(this.matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color((rgb >>> 16) & 255, (rgb >>> 8) & 255, rgb & 255, alpha).endVertex();
        }
    }
}
