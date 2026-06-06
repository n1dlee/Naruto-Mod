package com.sekwah.narutomod.anims;

import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class PlayerAnimHandler {

    /**
     * Cleans up parts that may be left over
     * @param entity
     * @param playerModel
     * @param <T>
     */
    public static <T extends LivingEntity> void preSprintingAnim(Entity entity, PlayerModel playerModel) {
        playerModel.head.setPos(0F, 0F, 0F);
        playerModel.body.setPos(0F, 0F, 0F);
        playerModel.rightArm.setPos(-5F, 2F, 0F);
        playerModel.leftArm.setPos(5F, 2F, 0F);
    }
    public static <T extends LivingEntity> void sprintingAnim(Entity entity, PlayerModel playerModel,
                                                              float limbSwing, float limbSwingAmount, float ageInTicks) {
        entity.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent((ninjaData -> {
            if(!ninjaData.isNinjaModeEnabled()) {
                return;
            }
            var channeledAbility = ninjaData.getCurrentlyChanneledAbility();
            if (channeledAbility != null && channeledAbility.equals(NarutoAbilities.CHAKRA_CHARGE.getId())) {
                playerModel.rightArm.setRotation(-1.375616F, -0.5948606F, 0F);
                playerModel.leftArm.setRotation(-1.375616F, 0.5948606F, 0F);
            } else if (channeledAbility != null) {
                applyChanneledJutsuPose(playerModel);
            }
            if(entity.isSprinting() && !entity.isVisuallySwimming() && !entity.isCrouching()) {
                applyNarutoSprintPose(playerModel);
            }
            if (ninjaData.isSageModeActive() && !entity.isSprinting() && limbSwingAmount < 0.1F) {
                applySageIdlePose(playerModel);
            }
            if (ninjaData.isKuramaCloakActive()) {
                applyKuramaCloakPose(playerModel);
            }
            if (ninjaData.getGatesOpen() >= 5) {
                applyEightGatesShake(playerModel, ninjaData.getGatesOpen(), ageInTicks);
            }
        }));
    }

    private static void applyChanneledJutsuPose(PlayerModel playerModel) {
        playerModel.rightArm.setRotation(-1.2F, -0.3F, 0F);
        playerModel.leftArm.setRotation(-1.2F, 0.3F, 0F);
        playerModel.rightArm.setPos(-4.5F, 2.0F, -1.0F);
        playerModel.leftArm.setPos(4.5F, 2.0F, -1.0F);
    }

    private static void applySageIdlePose(PlayerModel playerModel) {
        playerModel.rightArm.setRotation(-0.2F, -0.15F, 0.1F);
        playerModel.leftArm.setRotation(-0.2F, 0.15F, -0.1F);
        playerModel.body.setRotation(0.05F, 0F, 0F);
    }

    private static void applyKuramaCloakPose(PlayerModel playerModel) {
        playerModel.body.setRotation(0.2F, 0F, 0F);
        playerModel.rightArm.setRotation(0.5F, 0F, -0.3F);
        playerModel.leftArm.setRotation(0.5F, 0F, 0.3F);
        playerModel.head.xRot -= 0.08F;
    }

    private static void applyEightGatesShake(PlayerModel playerModel, int gatesOpen, float ageInTicks) {
        float shake = (float) Math.sin(ageInTicks * 3.0F) * 0.02F * gatesOpen;
        playerModel.body.x += shake;
        playerModel.head.x += shake * 0.5F;
    }

    private static void applyNarutoSprintPose(PlayerModel playerModel) {
        playerModel.rightArm.setRotation(1.412787F, 0F, 0F);
        playerModel.rightArm.setPos(-5F, 3.933333F, -3F - 2F);

        playerModel.leftArm.setRotation(1.412787F, 0F, 0F);
        playerModel.leftArm.setPos(5F, 3.266667F, -3F - 2F);

        playerModel.head.xRot = 0F;
        playerModel.head.setPos(0F, 3.133333F - 1F, -5F - 1F);

        playerModel.body.setRotation(0.5435722F, 0F, 0F);
        playerModel.body.setPos(0F, 3F - 1F, -3.5F - 2F);
    }
}
