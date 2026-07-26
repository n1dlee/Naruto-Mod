package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.model.entity.SummonBeastModel;
import com.sekwah.narutomod.entity.SummonBeastEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Renders the Kuchiyose summon beast: shared toad-silhouette geometry tinted per clan
 * contract — green Giant Toad (Uzumaki), violet Giant Serpent (Uchiha), pale Giant Slug
 * (Senju). Same manual-render pattern as SusanooRenderer, including the vanilla
 * model-space flip via scale(-S,-S,S).
 */
public class SummonBeastRenderer extends EntityRenderer<SummonBeastEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/kurama_tail.png");
    private static final float SCALE = 2.4f;

    // r,g,b per variant: toad green / serpent violet / slug pale
    private static final float[][] VARIANT_TINT = {
            {0.35f, 0.75f, 0.3f},
            {0.55f, 0.3f, 0.8f},
            {0.9f, 0.88f, 0.8f}
    };

    private final SummonBeastModel model;

    public SummonBeastRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new SummonBeastModel(context.bakeLayer(SummonBeastModel.LAYER_LOCATION));
    }

    @Override
    public void render(SummonBeastEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.scale(-SCALE, -SCALE, SCALE);

        int variant = Math.min(Math.max(entity.getVariant(), 0), VARIANT_TINT.length - 1);
        float[] tint = VARIANT_TINT[variant];
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                tint[0], tint[1], tint[2], 1.0f);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SummonBeastEntity entity) {
        return TEXTURE;
    }
}
