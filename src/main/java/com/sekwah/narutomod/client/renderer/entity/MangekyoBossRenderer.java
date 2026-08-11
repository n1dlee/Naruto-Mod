package com.sekwah.narutomod.client.renderer.entity;

import com.sekwah.narutomod.entity.MangekyoBossEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws a Mangekyo boss with that wielder's own skin, picked off the variant byte.
 * Reuses the vanilla humanoid rig the same way ShadowCloneRenderer does — these are
 * human ninja, so no custom model geometry is needed.
 */
public class MangekyoBossRenderer extends HumanoidMobRenderer<MangekyoBossEntity, HumanoidModel<MangekyoBossEntity>> {

    public MangekyoBossRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new BossSusanooLayer(this, context));
    }

    /**
     * Without an arm pose the vanilla humanoid rig lets its arms swing free and the held
     * weapon just floats alongside the fist. Switching to ITEM whenever the boss is actually
     * carrying something makes Madara grip the fan and Zabuza shoulder the cleaver.
     */
    @Override
    public void render(MangekyoBossEntity entity, float entityYaw, float partialTick,
                       com.mojang.blaze3d.vertex.PoseStack poseStack,
                       net.minecraft.client.renderer.MultiBufferSource bufferSource, int packedLight) {
        this.getModel().rightArmPose = entity.getMainHandItem().isEmpty()
                ? HumanoidModel.ArmPose.EMPTY
                : HumanoidModel.ArmPose.ITEM;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MangekyoBossEntity entity) {
        return entity.getVariant().texture();
    }
}
