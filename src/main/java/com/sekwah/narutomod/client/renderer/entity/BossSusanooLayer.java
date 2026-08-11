package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.model.entity.SusanooModel;
import com.sekwah.narutomod.entity.MangekyoBossEntity;
import com.sekwah.narutomod.entity.MangekyoBossVariant;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Wraps a boss in their own Susanoo once the fight has worn them down, tinted with that
 * wielder's canon colour from {@link MangekyoBossVariant} - Itachi red-orange, Madara
 * blue, and so on.
 *
 * Draws the SAME detailed bodies ported from the 1.12.2 mod that the player's Susanoo
 * uses. It used to fall back to the old blocky SusanooModel unconditionally, so an
 * Uchiha boss manifested a visibly cruder Susanoo than the player standing in front of
 * them - which read as a bug rather than as a difference. All the geometry constants
 * below are shared with SusanooRenderer for exactly that reason.
 */
public class BossSusanooLayer extends RenderLayer<MangekyoBossEntity, HumanoidModel<MangekyoBossEntity>> {

    private static final ResourceLocation FALLBACK_TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/susanoo.png");
    private static final RenderType FALLBACK_TYPE = RenderType.entityTranslucent(FALLBACK_TEXTURE);
    /** Bosses stay in the hovering-ribcage range - they never grow the Complete Body giant. */
    private static final float[] FALLBACK_STAGE_SCALE = {0f, 1.8f, 2.4f, 3.2f};
    private static final float ALPHA = 0.55f;
    private static final float DETAILED_ALPHA = 0.85f;

    private final SusanooModel fallbackModel;

    public BossSusanooLayer(RenderLayerParent<MangekyoBossEntity, HumanoidModel<MangekyoBossEntity>> parent,
                            EntityRendererProvider.Context context) {
        super(parent);
        this.fallbackModel = new SusanooModel(context.bakeLayer(SusanooModel.LAYER_LOCATION));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       MangekyoBossEntity boss, float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        int stage = boss.getSusanooStage();
        if (stage <= 0) {
            return;
        }
        MangekyoBossVariant variant = boss.getVariant();

        poseStack.pushPose();
        if (SusanooRenderer.detailedReady()) {
            renderDetailed(poseStack, bufferSource, packedLight, variant, stage);
        } else {
            renderFallback(poseStack, bufferSource, packedLight, variant, stage);
        }
        poseStack.popPose();
    }

    /**
     * A render layer inherits the pose LivingEntityRenderer already set up: rotated into the
     * body's facing, flipped by scale(-1,-1,1), and translated down by 1.501. So none of that
     * may be applied a second time.
     *
     * Doing it anyway is what buried the boss's Susanoo underground - the second flip turned
     * these +Y-downward models upside down, so instead of rising from the boss's feet they
     * grew into the floor. Here the model only needs a positive scale and a vertical offset
     * measured in that inherited space.
     */
    private void renderDetailed(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                MangekyoBossVariant variant, int stage) {
        int clamped = Mth.clamp(stage, 1, 3);
        Model body = SusanooRenderer.detailedBodyForStage(clamped);
        ResourceLocation texture = SusanooRenderer.detailedTextureForStage(clamped);
        float modelHeightU = SusanooRenderer.detailedHeightForStage(clamped);
        float modelBottomU = SusanooRenderer.detailedBottomForStage(clamped);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));
        float scale = SusanooRenderer.targetHeightForStage(clamped) / (modelHeightU / 16f);

        // Origin sits 1.501 above the feet in this space; back it off by however far the
        // model's lowest geometry hangs below its own origin, so the Susanoo stands on the
        // ground at any scale.
        poseStack.translate(0.0D, 1.501D - (modelBottomU / 16f) * scale, 0.0D);
        poseStack.scale(scale, scale, scale);

        // Tint only part-way toward white, or the painted artwork flattens to one hue.
        body.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                0.5f + variant.susanooRed() * 0.5f,
                0.5f + variant.susanooGreen() * 0.5f,
                0.5f + variant.susanooBlue() * 0.5f,
                DETAILED_ALPHA);
    }

    /** Only reached if the ported bodies failed to bake; keeps bosses from rendering nothing. */
    private void renderFallback(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                MangekyoBossVariant variant, int stage) {
        VertexConsumer consumer = bufferSource.getBuffer(FALLBACK_TYPE);
        float scale = FALLBACK_STAGE_SCALE[Mth.clamp(stage, 1, FALLBACK_STAGE_SCALE.length - 1)];
        // The blocky fallback is authored centred on the body rather than standing on the
        // ground, so it only needs the scale.
        poseStack.scale(scale, scale, scale);

        this.fallbackModel.setStage(stage);
        this.fallbackModel.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                variant.susanooRed(), variant.susanooGreen(), variant.susanooBlue(), ALPHA);
    }
}
