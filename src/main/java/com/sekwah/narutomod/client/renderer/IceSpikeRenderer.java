package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.block.IceSpikeBlock;
import com.sekwah.narutomod.block.IceSpikeBlockEntity;
import com.sekwah.narutomod.util.IceSpikeFormation;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class IceSpikeRenderer implements BlockEntityRenderer<IceSpikeBlockEntity> {
    private static final int[] FACETS = {0x2B6E97, 0x71C2E1, 0xABF0FF, 0x416D99, 0x55B6CF, 0x286C91};
    private static final int CORE = 0xD4FAFC;
    private static final int VEIN = 0x93C8FF;

    public IceSpikeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(IceSpikeBlockEntity ice, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = ice.getLevel();
        if (level == null || ice.end() <= 0) {
            return;
        }
        double now = level.getGameTime() + partial;
        if (now >= ice.end() || now < ice.rise() - IceSpikeFormation.WARNING) {
            return;
        }
        boolean low = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        var mesh = new Mesh(pose.last().pose(), buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        double fade = Mth.clamp((ice.end() - now) / IceSpikeFormation.MELT, 0, 1);
        double seed = (ice.getBlockPos().asLong() & 31) * 0.12;
        if (IceSpikeBlock.isDormant(ice.getBlockState())) {
            if (ice.row() == 0) {
                frost(mesh, seed, Mth.clamp(1 + (now - ice.rise()) / IceSpikeFormation.WARNING, 0, 1), low);
            }
        } else {
            double rise = ice.appeared() > 0 ? Mth.clamp((now - ice.appeared() + 0.8) / 3.0, 0.12, 1) : 1;
            crystal(mesh, ice.getBlockState().getValue(IceSpikeBlock.THICKNESS), new Light(seed, rise, fade, low));
        }
    }

    private static void frost(Mesh mesh, double seed, double progress, boolean low) {
        Vec3 root = new Vec3(0.5, 0.034, 0.5);
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3 + seed;
            Vec3 forward = radial(angle);
            Vec3 side = radial(angle + Math.PI / 2);
            double length = (0.34 + i % 2 * 0.12) * progress;
            Vec3 tip = root.add(forward.scale(length));
            mesh.ribbon(root, tip, side.scale(0.021), VEIN, (int) (181 * progress));
            if (!low) {
                Vec3 join = root.lerp(tip, 0.55);
                mesh.ribbon(join, join.add(radial(angle + 0.8).scale(length * 0.31)), side.scale(0.009), CORE,
                        (int) (140 * progress));
                mesh.ribbon(join, join.add(radial(angle - 0.8).scale(length * 0.31)), side.scale(0.009), CORE,
                        (int) (140 * progress));
            }
        }
    }

    private static void crystal(Mesh mesh, DripstoneThickness shape, Light state) {
        boolean point = IceSpikeFormation.height(shape) < 1;
        double height = IceSpikeFormation.height(shape) * state.rise();
        double width = IceSpikeFormation.radius(shape) * (0.76 + 0.24 * state.fade());
        Vec3 bottom = new Vec3(0.5, 0, 0.5);
        Vec3 top = bottom.add(0.025 * Math.sin(state.seed()), height, 0.018 * Math.cos(state.seed()));
        Vec3 core = bottom.add(0, height * 0.84, 0);
        int sides = state.low() ? 4 : 6;
        for (int i = 0; i < sides; i++) {
            Vec3 a = radial(state.seed() + i * Math.PI * 2 / sides);
            Vec3 b = radial(state.seed() + (i + 1) * Math.PI * 2 / sides);
            Vec3 lowA = bottom.add(a.scale(width));
            Vec3 lowB = bottom.add(b.scale(width));
            Vec3 highA = point ? top : top.add(a.scale(width * 0.79));
            Vec3 highB = point ? top : top.add(b.scale(width * 0.79));
            int alpha = (int) (213 * (0.4 + 0.6 * state.fade()));
            mesh.quad(lowA, highA, highB, lowB, FACETS[i], alpha);
            if (!state.low() && i % 2 == 0) {
                mesh.quad(lowA.lerp(lowB, 0.61), highA.lerp(highB, 0.82), core, core, CORE,
                        (int) (91 * state.fade() * state.rise()));
            }
        }
    }

    private static Vec3 radial(double angle) { return new Vec3(Math.cos(angle), 0, Math.sin(angle)); }

    @Override
    public int getViewDistance() { return 40; }

    private record Light(double seed, double rise, double fade, boolean low) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void ribbon(Vec3 a, Vec3 b, Vec3 width, int tint, int alpha) {
            quad(a.add(width), b.add(width), b.subtract(width), a.subtract(width), tint, alpha);
        }

        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int tint, int alpha) {
            vertex(a, tint, alpha);
            vertex(b, tint, alpha);
            vertex(c, tint, alpha);
            vertex(d, tint, alpha);
        }

        private void vertex(Vec3 at, int tint, int alpha) {
            this.buffer.vertex(this.matrix, (float) at.x, (float) at.y, (float) at.z)
                    .color((tint >>> 16) & 255, (tint >>> 8) & 255, tint & 255, alpha).endVertex();
        }
    }
}
