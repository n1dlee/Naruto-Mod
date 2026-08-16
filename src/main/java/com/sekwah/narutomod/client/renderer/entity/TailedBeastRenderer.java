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

        applyBijudamaCharge(poseStack, entity.getBijudamaCharge(),
                entity.tickCount + partialTick);

        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityCutoutNoCull(variant.getTexture()));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    /**
     * Rearing back to spit a Bijudama.
     *
     * Applied to the whole body rather than to a head part, because these eight models were
     * each converted separately from the 1.12.2 mod and their parts are named differently -
     * there is no "head" every one of them agrees on. Leaning the entire animal is cruder than
     * a neck rig, but it works identically for a tanuki, a turtle and an octopus, and at the
     * size these things are it reads perfectly well.
     *
     * The curve is deliberately lopsided: most of the charge is spent hauling back, and the
     * last fifth snaps forward. That snap is the moment the bomb leaves, so the tell and the
     * attack line up.
     *
     * The model is in the flipped, +Y-downward space by this point, so the pitch sign here is
     * inverted relative to what "lean back" would mean in world space.
     */
    private static void applyBijudamaCharge(PoseStack poseStack, float charge, float age) {
        if (charge <= 0.001f) {
            return;
        }
        float lean;
        if (charge < 0.8f) {
            // Winding up: rock backward, accelerating.
            float t = charge / 0.8f;
            lean = -22.0f * t * t;
        } else {
            // The spit: through the rest position and out the far side.
            float t = (charge - 0.8f) / 0.2f;
            lean = -22.0f + 46.0f * t;
        }
        // A tremor through the build-up - it is holding a great deal of chakra in its mouth.
        float shudder = Mth.sin(age * 1.9f) * 1.6f * Math.min(1f, charge * 1.4f);

        poseStack.mulPose(Axis.XP.rotationDegrees(lean + shudder));
        // Settles back on its haunches as it draws in, then drives up as it fires.
        poseStack.translate(0.0D, charge < 0.8f ? charge * 0.6D : (1.0f - charge) * 2.4D, 0.0D);
    }

    @Override
    public ResourceLocation getTextureLocation(TailedBeastEntity entity) {
        return entity.getVariant().getTexture();
    }
}
