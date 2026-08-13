package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.client.model.entity.*;
import com.sekwah.narutomod.entity.TailedBeastEntity;
import com.sekwah.narutomod.entity.TailedBeastVariant;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

/**
 * Draws whichever tailed beast this is, at the size its hitbox says it is.
 *
 * All eight models came out of the 1.12.2 bytecode with +Y downward, so they need the usual
 * scale(-S,-S,S) flip, and none of them puts its origin on the ground - hence the per-variant
 * feet offset, lifted before the flip. Get either wrong and the beast is buried or upside
 * down; both happened during the port.
 */
public class TailedBeastRenderer extends EntityRenderer<TailedBeastEntity> {

    private final Map<TailedBeastVariant, Model> models = new EnumMap<>(TailedBeastVariant.class);

    public TailedBeastRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.models.put(TailedBeastVariant.SHUKAKU,
                new OneTailModel(context.bakeLayer(OneTailModel.LAYER_LOCATION)));
        this.models.put(TailedBeastVariant.MATATABI,
                new TwoTailsModel(context.bakeLayer(TwoTailsModel.LAYER_LOCATION)));
        this.models.put(TailedBeastVariant.ISOBU,
                new ThreeTailsModel(context.bakeLayer(ThreeTailsModel.LAYER_LOCATION)));
        this.models.put(TailedBeastVariant.SON_GOKU,
                new FourTailsModel(context.bakeLayer(FourTailsModel.LAYER_LOCATION)));
        this.models.put(TailedBeastVariant.KOKUO,
                new FiveTailsModel(context.bakeLayer(FiveTailsModel.LAYER_LOCATION)));
        this.models.put(TailedBeastVariant.SAIKEN,
                new SixTailsModel(context.bakeLayer(SixTailsModel.LAYER_LOCATION)));
        this.models.put(TailedBeastVariant.CHOMEI,
                new SevenTailsModel(context.bakeLayer(SevenTailsModel.LAYER_LOCATION)));
        this.models.put(TailedBeastVariant.GYUKI,
                new EightTailsModel(context.bakeLayer(EightTailsModel.LAYER_LOCATION)));
    }

    @Override
    public void render(TailedBeastEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        TailedBeastVariant variant = entity.getVariant();
        Model model = this.models.get(variant);
        if (model == null) {
            return;
        }
        float scale = variant.getRenderScale();

        poseStack.pushPose();
        poseStack.translate(0.0D, variant.getFeetOffset() * scale, 0.0D);
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.scale(-scale, -scale, scale);

        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityCutoutNoCull(variant.getTexture()));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TailedBeastEntity entity) {
        return entity.getVariant().getTexture();
    }
}
