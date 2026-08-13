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
        // Armour has to be skipped explicitly rather than left to setAllVisible below:
        // HumanoidModel.copyPropertiesTo copies part transforms, not part visibility, so a
        // hidden wielder would still have their cloak and headband floating in the giant.
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()) {
            @Override
            public void render(com.mojang.blaze3d.vertex.PoseStack poseStack,
                               net.minecraft.client.renderer.MultiBufferSource bufferSource,
                               int packedLight, MangekyoBossEntity boss, float limbSwing,
                               float limbSwingAmount, float partialTick, float ageInTicks,
                               float netHeadYaw, float headPitch) {
                if (boss.isGiant()) {
                    return;
                }
                super.render(poseStack, bufferSource, packedLight, boss, limbSwing,
                        limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
            }
        });
        // Held weapons vanish with the body once the wielder is a giant - otherwise a kunai
        // hangs in mid-air inside the Susanoo's ribs.
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()) {
            @Override
            public void render(com.mojang.blaze3d.vertex.PoseStack poseStack,
                               net.minecraft.client.renderer.MultiBufferSource bufferSource,
                               int packedLight, MangekyoBossEntity boss, float limbSwing,
                               float limbSwingAmount, float partialTick, float ageInTicks,
                               float netHeadYaw, float headPitch) {
                if (boss.isGiant()) {
                    return;
                }
                super.render(poseStack, bufferSource, packedLight, boss, limbSwing,
                        limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
            }
        });
        this.addLayer(new BossSusanooLayer(this, context));
        this.addLayer(new BossKuramaLayer(this));
        this.addLayer(new BossSandLayer(this, context));
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

        // The player disappears inside their own final form; the boss did not, so a
        // full-height Naruto stood at the foot of an eighteen-block fox and the whole thing
        // read as broken geometry rather than as a transformation.
        //
        // Set on every render call, never assumed: one model instance serves every boss on
        // screen, so whatever the last one left here is what the next one would inherit.
        this.getModel().setAllVisible(!entity.isGiant());
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MangekyoBossEntity entity) {
        return entity.getVariant().texture();
    }
}
