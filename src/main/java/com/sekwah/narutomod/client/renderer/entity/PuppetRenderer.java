package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.client.model.entity.*;
import com.sekwah.narutomod.entity.PuppetEntity;
import com.sekwah.narutomod.entity.PuppetVariant;
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
 * Draws whichever of Sasori's puppets this is.
 *
 * All five came out of the 1.12.2 bytecode with +Y downward, so they need the usual
 * scale(-S,-S,S) flip, and each is lifted by its own measured feet offset - none of these
 * models puts its origin on the ground.
 */
public class PuppetRenderer extends EntityRenderer<PuppetEntity> {

    private final Map<PuppetVariant, Model> models = new EnumMap<>(PuppetVariant.class);
    private final HirukoModel hiruko;

    public PuppetRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.hiruko = new HirukoModel(context.bakeLayer(HirukoModel.LAYER_LOCATION));
        this.models.put(PuppetVariant.HIRUKO, this.hiruko);
        this.models.put(PuppetVariant.KARASU,
                new KarasuModel(context.bakeLayer(KarasuModel.LAYER_LOCATION)));
        this.models.put(PuppetVariant.SANSHOUO,
                new SanshouoModel(context.bakeLayer(SanshouoModel.LAYER_LOCATION)));
        this.models.put(PuppetVariant.THIRD_KAZEKAGE,
                new ThirdKazekageModel(context.bakeLayer(ThirdKazekageModel.LAYER_LOCATION)));
        this.models.put(PuppetVariant.HUNDRED,
                new HundredPuppetModel(context.bakeLayer(HundredPuppetModel.LAYER_LOCATION)));
    }

    @Override
    public void render(PuppetEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        PuppetVariant variant = entity.getVariant();
        Model model = this.models.get(variant);
        if (model == null) {
            return;
        }
        if (model == this.hiruko) {
            // Set every frame: one model instance serves every Hiruko on screen, and the tail
            // keeps whatever rotation the last one left on it.
            this.hiruko.lashTail(entity.tickCount + partialTick);
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
    public ResourceLocation getTextureLocation(PuppetEntity entity) {
        return entity.getVariant().getTexture();
    }
}
