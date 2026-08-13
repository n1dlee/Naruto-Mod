package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.model.entity.ChibakuCoreModel;
import com.sekwah.narutomod.entity.jutsuprojectile.ChibakuTenseiEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The black core, spinning on two axes so a sphere of identical facets does not read as a
 * still image, and scaled from the size the entity is currently carrying.
 *
 * Full-bright: it is a hole in the sky, not a rock, and letting the world's light dim it made
 * it disappear at night - which is exactly when it needs to be readable.
 */
public class ChibakuTenseiRenderer extends EntityRenderer<ChibakuTenseiEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/chibaku_core.png");

    /** Diameter of the imported orb in blocks, at scale 1. */
    private static final float MODEL_DIAMETER = 5f / 16f;

    private final ChibakuCoreModel model;

    public ChibakuTenseiRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ChibakuCoreModel(context.bakeLayer(ChibakuCoreModel.LAYER_LOCATION));
    }

    @Override
    public void render(ChibakuTenseiEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float diameter = entity.getSize() * 2.0f;
        float scale = diameter / MODEL_DIAMETER;
        float spin = (entity.tickCount + partialTick) * 2.5F;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.mulPose(Axis.XP.rotationDegrees(spin * 0.4F));
        poseStack.scale(-scale, -scale, scale);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));
        this.model.renderToBuffer(poseStack, consumer, 15728880, OverlayTexture.NO_OVERLAY,
                0.25f, 0.15f, 0.35f, 0.95f);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ChibakuTenseiEntity entity) {
        return TEXTURE;
    }
}
