package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.block.MokutonTimberBlock;
import com.sekwah.narutomod.block.MokutonTimberBlockEntity;
import com.sekwah.narutomod.util.WoodCagePlan.Profile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class MokutonTimberRenderer implements BlockEntityRenderer<MokutonTimberBlockEntity> {
    private static final Vec3 X = new Vec3(1, 0, 0);
    private static final Vec3 Y = new Vec3(0, 1, 0);
    private static final Vec3 Z = new Vec3(0, 0, 1);
    private static final Tint SAP = new Tint(170, 211, 146);
    private static final Tint BARK = new Tint(219, 157, 104);
    private static final Tint DEEP = new Tint(93, 119, 69);

    public MokutonTimberRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MokutonTimberBlockEntity timber, float partial, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        var level = timber.getLevel();
        if (level == null || timber.rise() == 0 || !timber.getBlockState().hasProperty(MokutonTimberBlock.PROFILE)) {
            return;
        }
        double t = (level.getGameTime() - timber.rise() + partial) / 9;
        if (t < 0 || t > 1) {
            return;
        }
        Profile profile = timber.getBlockState().getValue(MokutonTimberBlock.PROFILE);
        var basis = Basis.from(profile);
        var shape = new Growth(basis, profile.shape().bounds(), t, (timber.getBlockPos().asLong() & 63) * 0.13);
        var mesh = new Mesh(pose.last().pose(), buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        boolean low = Minecraft.getInstance().options.graphicsMode().get() == net.minecraft.client.GraphicsStatus.FAST;
        fibres(mesh, shape, low);
    }

    @Override
    public int getViewDistance() {
        return 28;
    }

    private static void fibres(Mesh mesh, Growth growth, boolean low) {
        int length = low ? 6 : 11;
        int count = low ? 1 : 3;
        double reveal = Mth.clamp(growth.time() * 1.7, 0, 1);
        double fade = Math.sin(Math.PI * growth.time());
        for (int branch = 0; branch < count; branch++) {
            for (int i = 0; i < length && i / (double) length < reveal; i++) {
                double from = i / (double) length;
                double to = Math.min(reveal, (i + 1) / (double) length);
                Vec3 a = growth.point(from, branch);
                Vec3 b = growth.point(to, branch);
                Vec3 width = a.subtract(growth.bounds().getCenter()).normalize().scale(0.019 + branch * 0.004);
                mesh.ribbon(a, b, width, branch == 0 ? SAP : branch == 1 ? BARK : DEEP, (int) (167 * fade));
            }
        }
    }

    private record Basis(Vec3 axis, Vec3 side, Vec3 normal) {
        private static Basis from(Profile profile) {
            return switch (profile) {
                case POST, ROOT_X, ROOT_Z -> new Basis(Y, X, Z);
                case RAIL_Z -> new Basis(Z, X, Y);
                default -> new Basis(X, Z, Y);
            };
        }
    }

    private record Growth(Basis basis, AABB bounds, double time, double seed) {
        private Vec3 point(double progress, int strand) {
            double length = extent(this.basis.axis);
            double a = (progress * 6.9 + this.seed) + strand * 2.1 - this.time * 1.2;
            return this.bounds.getCenter().add(this.basis.axis.scale((progress - 0.5) * length))
                    .add(this.basis.side.scale(Math.cos(a) * (extent(this.basis.side) * 0.54 + 0.026)))
                    .add(this.basis.normal.scale(Math.sin(a) * (extent(this.basis.normal) * 0.54 + 0.025)));
        }

        private double extent(Vec3 axis) {
            return axis.x * this.bounds.getXsize() + axis.y * this.bounds.getYsize() + axis.z * this.bounds.getZsize();
        }
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void ribbon(Vec3 from, Vec3 to, Vec3 width, Tint tint, int alpha) {
            vertex(from.add(width), tint, alpha);
            vertex(to.add(width), tint, alpha);
            vertex(to.subtract(width), tint, alpha);
            vertex(from.subtract(width), tint, alpha);
        }

        private void vertex(Vec3 point, Tint tint, int alpha) {
            this.buffer.vertex(this.matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color(tint.red(), tint.green(), tint.blue(), alpha).endVertex();
        }
    }

    private record Tint(int red, int green, int blue) {
    }
}
