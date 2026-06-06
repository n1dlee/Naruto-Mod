package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.client.model.jutsu.RasenganJutsuModel;
import com.sekwah.narutomod.entity.jutsuprojectile.RasenganEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class RasenganRenderer extends EntityRenderer<RasenganEntity> {

    public static final ResourceLocation TEX = new ResourceLocation("narutomod", "textures/entity/jutsu/projectiles/rasengan_2.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucent(TEX);
    private final RasenganJutsuModel model;

    public RasenganRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new RasenganJutsuModel(context.bakeLayer(RasenganJutsuModel.LAYER_LOCATION));
    }

    @Override
    public void render(RasenganEntity entity, float yaw, float partial, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        VertexConsumer consumer = bufferSource.getBuffer(RENDER_TYPE);
        poseStack.pushPose();
        poseStack.translate(0, entity.getBbHeight() / 2f, 0);

        // Apply charge-based scale (synced from server): 1.0 at 20 ticks → 5.0 at 60 ticks
        int charge = entity.getSyncedChargeAmount();
        float t = Math.max(0, Math.min(charge - 20, 40)) / 40.0f;
        float chargeScale = 1.0f + t * 4.0f; // 1x → 5x
        poseStack.scale(chargeScale, chargeScale, chargeScale);

        float time = (entity.tickCount + partial) * 12.0F;
        // Spin faster than fireball to suggest rapid chakra rotation
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 2.5f));
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 2.5f));

        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                0.55F, 0.9F, 1.0F, 0.92F);
        poseStack.popPose();
        super.render(entity, yaw, partial, poseStack, bufferSource, packedLight);
    }

    @Override
    protected int getBlockLightLevel(RasenganEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(RasenganEntity entity) {
        return TEX;
    }
}
