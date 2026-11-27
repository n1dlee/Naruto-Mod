package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.entity.ClayCloneEntity;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.util.ClayCloneState;
import com.sekwah.narutomod.util.ClayCloneState.Phase;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class ClayCloneGlaze extends RenderLayer<ShadowCloneEntity, HumanoidModel<ShadowCloneEntity>> {
    private static final int GLOW = 0xFFE2A8;
    private static final int CHALK = 0xF1E5D6;

    ClayCloneGlaze(RenderLayerParent<ShadowCloneEntity, HumanoidModel<ShadowCloneEntity>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, ShadowCloneEntity entity,
                       float stride, float strideAmount, float partial, float age, float yaw, float pitch) {
        if (!(entity instanceof ClayCloneEntity clone) || clone.phase() != Phase.ARMED) {
            return;
        }
        double pulse = ClayCloneState.seam(clone.stageAge() + partial);
        pose.pushPose();
        this.getParentModel().body.translateAndRotate(pose);
        seam(pose.last().pose(), buffers, pulse);
        pose.popPose();
    }

    private static void seam(Matrix4f matrix, MultiBufferSource buffers, double pulse) {
        var chalk = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        var a = new Vec3(-0.171, 0.074, -0.126);
        var b = new Vec3(-0.082, 0.25, -0.127);
        var c = new Vec3(0.047, 0.306, -0.129);
        var d = new Vec3(0.137, 0.51, -0.127);
        chalk.seam(a, b, 0.018, 0x4E3834, (int) (192 * pulse));
        chalk.seam(b, c, 0.017, 0x4E3834, (int) (192 * pulse));
        chalk.seam(c, d, 0.024, 0x4E3834, (int) (192 * pulse));
        var spark = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW));
        spark.seam(a, b, 0.007, GLOW, (int) (243 * pulse));
        spark.seam(b, c, 0.006, CHALK, (int) (243 * pulse));
        spark.seam(c, d, 0.009, GLOW, (int) (243 * pulse));
        spark.seam(c, new Vec3(-0.04, 0.42, -0.128), 0.006, CHALK, (int) (165 * pulse));
    }

    static void flakes(ClayCloneEntity entity, PoseStack pose, MultiBufferSource buffers, float partial) {
        Phase phase = entity.phase();
        double t = Mth.clamp((entity.stageAge() + partial) / ClayCloneState.SHARDS, 0, 1);
        boolean energetic = phase == Phase.BURST;
        boolean low = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        var mesh = new Mesh(pose.last().pose(), buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        int count = low ? 11 : 26;
        for (int i = 0; i < count; i++) {
            double angle = i * 2.399963 + (entity.getId() & 31) * 0.08;
            double force = energetic ? 0.95 + i % 5 * 0.24 : 0.34 + i % 4 * 0.087;
            double spread = (1 - (1 - t) * (1 - t)) * force;
            Vec3 mid = new Vec3(Math.cos(angle) * spread, 0.25 + (i % 9) * 0.162
                    + (energetic ? Math.sin(t * Math.PI) * 0.84 : 0) - t * t * 0.68, Math.sin(angle) * spread);
            Vec3 aside = new Vec3(Math.cos(angle), -0.32, Math.sin(angle)).scale(0.08 + (i % 4) * 0.033);
            Vec3 vertical = new Vec3(0.02 * Math.sin(angle), 0.078 + (i % 3) * 0.063, 0);
            int tint = energetic && i % 5 == 0 ? GLOW : i % 4 == 0 ? 0xA28A78 : CHALK;
            mesh.quad(mid.add(vertical), mid.add(aside), mid.subtract(vertical), mid.subtract(aside), tint, (int) (232 * (1 - t)));
        }
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void seam(Vec3 a, Vec3 b, double width, int color, int alpha) {
            Vec3 tangent = b.subtract(a);
            Vec3 side = new Vec3(-tangent.y, tangent.x, 0).normalize().scale(width);
            quad(a.add(side), b.add(side.scale(0.78)), b.subtract(side.scale(0.78)), a.subtract(side), color, alpha);
        }

        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, int alpha) {
            vertex(a, color, alpha);
            vertex(b, color, alpha);
            vertex(c, color, alpha);
            vertex(d, color, alpha);
        }

        private void vertex(Vec3 point, int color, int alpha) {
            this.buffer.vertex(this.matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color((color >>> 16) & 255, (color >>> 8) & 255, color & 255, alpha).endVertex();
        }
    }
}
