package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.client.model.entity.EnmaModel;
import com.sekwah.narutomod.client.model.entity.GiantSlugModel;
import com.sekwah.narutomod.client.model.entity.GiantSnakeModel;
import com.sekwah.narutomod.client.model.entity.GiantToadModel;
import com.sekwah.narutomod.entity.SummonBeastEntity;
import com.sekwah.narutomod.entity.SummonBeastVariant;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws whichever contract beast answered the summon, using the geometry imported from the
 * 1.12.2 mod rather than one tinted toad silhouette standing in for all of them.
 *
 * All four models are authored with +Y downward, so they need the usual scale(-S,-S,S) flip.
 * None of them puts its origin on the ground either, which is what the variant's feet offset
 * is for: lift by exactly that much, scaled, or the summon stands buried to the knees.
 */
public class SummonBeastRenderer extends EntityRenderer<SummonBeastEntity> {

    private final GiantToadModel toad;
    private final GiantSnakeModel snake;
    private final GiantSlugModel slug;
    private final EnmaModel enma;

    public SummonBeastRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.toad = new GiantToadModel(context.bakeLayer(GiantToadModel.LAYER_LOCATION));
        this.snake = new GiantSnakeModel(context.bakeLayer(GiantSnakeModel.LAYER_LOCATION));
        this.slug = new GiantSlugModel(context.bakeLayer(GiantSlugModel.LAYER_LOCATION));
        this.enma = new EnmaModel(context.bakeLayer(EnmaModel.LAYER_LOCATION));
    }

    @Override
    public void render(SummonBeastEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        SummonBeastVariant variant = entity.getVariant();
        Model model = this.modelFor(variant);

        float ageInTicks = entity.tickCount + partialTick;
        var motion = motion(entity, partialTick, ageInTicks);
        if (model == this.snake) {
            // Set every frame: these models are per-renderer, but the spine keeps whatever
            // rotation the last frame left on it, so a stationary snake would freeze mid-wave.
            this.snake.slither(ageInTicks, Mth.clamp(motion.stride() + Math.abs(motion.lean()) * 0.16f, 0.12f, 1f));
        }

        float scale = variant.getRenderScale();
        poseStack.pushPose();
        poseStack.translate(0.0D, motion.lift(), 0.0D);
        poseStack.scale(0.86f + motion.growth() * 0.14f, motion.growth(), 0.86f + motion.growth() * 0.14f);
        // Lift first, then flip: the translate is in world space, the model is not.
        poseStack.translate(0.0D, variant.getFeetOffset() * scale, 0.0D);
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(motion.lean()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(motion.roll()));
        poseStack.scale(-scale, -scale, scale);

        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityCutoutNoCull(variant.getTexture()));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static Motion motion(SummonBeastEntity summon, float partial, float age) {
        int presence = summon.presence();
        float t = presence >= 0 ? Mth.clamp((presence + partial) / SummonBeastEntity.ARRIVAL, 0, 1)
                : 1 - Mth.clamp((-presence + partial) / SummonBeastEntity.WITHDRAW, 0, 1);
        float growth = 0.11f + 0.89f * t * t * (3 - 2 * t);
        float stride = Mth.clamp(summon.walkAnimation.speed(partial), 0, 1);
        float step = summon.walkAnimation.position(partial);
        float cue = summon.cue() > 0 ? Mth.clamp((9 - summon.cue() + partial) / 8f, 0, 1)
                : summon.cue() < 0 ? -Mth.clamp((-summon.cue() - partial) / 7f, 0, 1) : 0;
        float idle = Mth.sin(age * 0.13f);
        return switch (summon.getVariant()) {
            case GAMABUNTA -> new Motion(growth, stride, (float) Math.abs(Mth.sin(step)) * stride * 0.20f,
                    6.6f * cue + idle, 1.8f * Mth.cos(step) * stride);
            case MANDA -> new Motion(growth, stride, 0.028f * idle + 0.11f * Math.max(cue, 0),
                    4.1f * cue, 2.7f * Mth.sin(step * 0.78f) * stride);
            case KATSUYU -> new Motion(growth * (1 + 0.027f * idle), stride, 0.028f * stride,
                    -3.5f * cue, 1.6f * Mth.sin(step * 0.68f) * stride);
            case ENMA -> new Motion(growth, stride, 0.13f * Math.abs(Mth.sin(step)) * stride,
                    8.4f * cue, 3.9f * Mth.cos(step) * stride);
        };
    }

    private record Motion(float growth, float stride, float lift, float lean, float roll) {
    }

    private Model modelFor(SummonBeastVariant variant) {
        return switch (variant) {
            case GAMABUNTA -> this.toad;
            case MANDA -> this.snake;
            case KATSUYU -> this.slug;
            case ENMA -> this.enma;
        };
    }

    @Override
    public ResourceLocation getTextureLocation(SummonBeastEntity entity) {
        return entity.getVariant().getTexture();
    }
}
