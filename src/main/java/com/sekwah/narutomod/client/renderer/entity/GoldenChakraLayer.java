package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.util.SpectralSword;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class GoldenChakraLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public GoldenChakraLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float swing, float stride, float partial, float age, float yaw, float pitch) {
        if (player.isInvisible() || player.isDeadOrDying()) {
            return;
        }
        int stage = player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                .map(data -> data.isKcmActive() && !data.isKuramaCloakActive() ? data.getKcmFormAge() : -1).orElse(-1);
        if (stage < 0) {
            return;
        }
        int alpha = (int) ((NarutoConfig.reducedBodyRiteEffects ? 98 : 184) * SpectralSword.formation(stage + partial));
        var model = getParentModel();
        var paint = new Paint(pose, buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW), alpha);
        paint.mark(model.body, new Vec3(-0.201, 0.18, -0.167), new Vec3(0, 0.34, -0.167), new Vec3(0.201, 0.18, -0.167));
        paint.mark(model.head, new Vec3(-0.209, -0.266, -0.292), new Vec3(0, -0.322, -0.292), new Vec3(0.209, -0.266, -0.292));
        if (stage >= 5) {
            paint.mark(model.rightArm, new Vec3(-0.106, 0.06, -0.162), new Vec3(-0.106, 0.34, -0.162), new Vec3(-0.023, 0.51, -0.162));
            paint.mark(model.leftArm, new Vec3(0.106, 0.06, -0.162), new Vec3(0.106, 0.34, -0.162), new Vec3(0.023, 0.51, -0.162));
        }
    }

    private record Paint(PoseStack poses, VertexConsumer sink, int alpha) {
        private void mark(ModelPart part, Vec3 first, Vec3 joint, Vec3 end) {
            if (!part.visible) {
                return;
            }
            poses.pushPose();
            try {
                part.translateAndRotate(poses);
                Matrix4f matrix = poses.last().pose();
                line(matrix, first, joint);
                line(matrix, joint, end);
            } finally {
                poses.popPose();
            }
        }

        private void line(Matrix4f matrix, Vec3 a, Vec3 b) {
            Vec3 edge = new Vec3(a.y - b.y, b.x - a.x, 0).normalize().scale(0.0125);
            put(matrix, a.add(edge));
            put(matrix, b.add(edge));
            put(matrix, b.subtract(edge));
            put(matrix, a.subtract(edge));
        }

        private void put(Matrix4f matrix, Vec3 at) {
            sink.vertex(matrix, (float) at.x, (float) at.y, (float) at.z).color(255, 222, 125, alpha).endVertex();
        }
    }
}
