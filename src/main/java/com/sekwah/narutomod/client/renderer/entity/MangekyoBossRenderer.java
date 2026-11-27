package com.sekwah.narutomod.client.renderer.entity;

import com.sekwah.narutomod.client.renderer.TrigramPalmsRenderer;
import com.sekwah.narutomod.entity.MangekyoBossEntity;
import com.sekwah.narutomod.util.SusanooFullBody;
import com.sekwah.narutomod.util.SusanooPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws a Mangekyo boss with that wielder's own skin, picked off the variant byte.
 * Reuses the vanilla humanoid rig the same way ShadowCloneRenderer does — these are
 * human ninja, so no custom model geometry is needed.
 */
public class MangekyoBossRenderer extends HumanoidMobRenderer<MangekyoBossEntity, HumanoidModel<MangekyoBossEntity>> {

    public MangekyoBossRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<MangekyoBossEntity>(context.bakeLayer(ModelLayers.PLAYER)) {
            @Override
            public void setupAnim(MangekyoBossEntity boss, float swing, float amount, float age, float yaw, float pitch) {
                super.setupAnim(boss, swing, amount, age, yaw, pitch);
                TrigramPalmsRenderer.pose(this, boss, Mth.frac(age));
            }
        }, 0.5F);
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
        this.getModel().rightArmPose = castArmPose(entity);

        // The player disappears inside their own final form; the boss did not, so a
        // full-height Naruto stood at the foot of an eighteen-block fox and the whole thing
        // read as broken geometry rather than as a transformation.
        //
        // Set on every render call, never assumed: one model instance serves every boss on
        // screen, so whatever the last one left here is what the next one would inherit.
        this.getModel().setAllVisible(!entity.isGiant());
        if (!entity.getVariant().hasSusanoo() || !SusanooFullBody.standing(entity.getSusanooStage())) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            return;
        }
        poseStack.pushPose();
        try {
            double settle = SusanooPose.smooth((entity.getSusanooStageAge() + partialTick) / 9);
            poseStack.translate(0, SusanooFullBody.seatShift(entity.getEyeHeight()) * settle, 0);
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        } finally {
            poseStack.popPose();
        }
    }

    /**
     * The pose a boss holds while it is gathering a technique.
     *
     * The wind-up exists so a player can read the attack coming and answer it, and particles
     * alone do not read as intent — the mod is full of particles. A silhouette does: hand at
     * the eye means a genjutsu is being held on you and you should break line of sight; arm
     * cocked overhead means something heavy is about to land where you are standing.
     *
     * Reusing the vanilla arm poses rather than writing new ones keeps it working with the
     * armour and held-item layers, which already know how to follow these three.
     */
    private static HumanoidModel.ArmPose castArmPose(MangekyoBossEntity entity) {
        return switch (entity.getCastPose()) {
            case STARE -> HumanoidModel.ArmPose.SPYGLASS;       // hand up to the eye
            case REACH -> HumanoidModel.ArmPose.BOW_AND_ARROW;  // both arms out, fixed on you
            case GATHER -> HumanoidModel.ArmPose.THROW_SPEAR;   // cocked overhead
            case NONE -> entity.getMainHandItem().isEmpty()
                    ? HumanoidModel.ArmPose.EMPTY
                    : HumanoidModel.ArmPose.ITEM;
        };
    }

    @Override
    public ResourceLocation getTextureLocation(MangekyoBossEntity entity) {
        return entity.getVariant().texture();
    }
}
