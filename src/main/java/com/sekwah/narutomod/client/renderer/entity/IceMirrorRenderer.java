package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.model.entity.IceMirrorModel;
import com.sekwah.narutomod.entity.jutsuprojectile.IceMirrorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws a mirror standing where it was raised, turned to face the middle of the ring.
 *
 * A damaged pane fades toward transparent rather than cracking - there is one texture, and
 * "you can nearly see through this one" reads as "this one is about to go" without needing a
 * second set of art.
 */
public class IceMirrorRenderer extends EntityRenderer<IceMirrorEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/ice_mirror.png");

    private static final float FULL_HEALTH = 12f;
    private static final float MIN_ALPHA = 0.35f;
    private static final float MAX_ALPHA = 0.85f;

    private final IceMirrorModel model;

    public IceMirrorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new IceMirrorModel(context.bakeLayer(IceMirrorModel.LAYER_LOCATION));
    }

    @Override
    public void render(IceMirrorEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        // The pane hangs below its own origin in model space, so lift by its height before the
        // flip and it stands on the ground rather than under it.
        poseStack.translate(0.0D, IceMirrorModel.HEIGHT_BLOCKS, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getFacing()));
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        float health = Math.max(0f, Math.min(FULL_HEALTH, entity.getMirrorHealth()));
        float alpha = MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * (health / FULL_HEALTH);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));
        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0f, 1.0f, 1.0f, alpha);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(IceMirrorEntity entity) {
        return TEXTURE;
    }
}
