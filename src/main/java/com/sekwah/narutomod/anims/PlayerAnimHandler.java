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
            } else if (channeledAbility != null && channeledAbility.equals(NarutoAbilities.FIREBALL.getId())) {
                applyFireballPose(playerModel);
            } else if (channeledAbility != null) {
                applyChanneledJutsuPose(playerModel);
            } else if (ninjaData.isChidoriActive()) {
                applyChidoriThrustPose(playerModel);
            } else if (ninjaData.getCastPoseTicks() > 0) {
                var lastCast = ninjaData.getLastCastAbilityId();
                if (lastCast != null && lastCast.equals(NarutoAbilities.AIR_PALM.getId())) {
                    applyAirPalmPose(playerModel);
                } else if (lastCast != null && lastCast.equals(NarutoAbilities.GREAT_BREAKTHROUGH.getId())) {
                    applyWindGatherPose(playerModel);
                } else if (lastCast != null && lastCast.equals(NarutoAbilities.ADAMANTINE_CHAINS.getId())) {
                    applyChainExpulsionPose(playerModel);
                } else {
                    applyHandSealPose(playerModel, ninjaData.isCrossSealPose());
                }
            }
            if (ninjaData.isWallWalkAttached()) {
                applyWallClimbPose(playerModel, limbSwing, limbSwingAmount);
            } else if(entity.isSprinting() && !entity.isVisuallySwimming() && !entity.isCrouching()) {
                applyNarutoSprintPose(playerModel);
            }
            if (ninjaData.isSageModeActive() && !entity.isSprinting() && limbSwingAmount < 0.1F) {
                applySageIdlePose(playerModel);
            }
            if (ninjaData.isKuramaCloakActive()) {
                applyKuramaCloakPose(playerModel);
            }
            if (ninjaData.isSusanooActive() && ninjaData.getSusanooStage() < 4) {
                applySusanooPose(playerModel);
            }
            if (ninjaData.getGatesOpen() >= 5) {
                applyEightGatesShake(playerModel, ninjaData.getGatesOpen(), ageInTicks);
            }
        }));
    }

    /**
     * Pressed-against-the-wall climbing stance: arms splayed up/out as if gripping the
     * surface, legs alternating like a climb-cycle driven by the same limbSwing used for
     * normal walking (so it animates while moving along the wall and holds still when idle).
     */
    private static void applyWallClimbPose(PlayerModel playerModel, float limbSwing, float limbSwingAmount) {
        float cycle = net.minecraft.util.Mth.sin(limbSwing * 0.6662F) * limbSwingAmount;

        playerModel.rightArm.setRotation(-1.9F + cycle * 0.3F, -0.25F, 0F);
        playerModel.rightArm.setPos(-5F, 2.5F, -1.5F);

        playerModel.leftArm.setRotation(-1.9F - cycle * 0.3F, 0.25F, 0F);
        playerModel.leftArm.setPos(5F, 2.5F, -1.5F);

        playerModel.rightLeg.setRotation(-0.6F - cycle * 0.4F, 0F, 0F);
        playerModel.leftLeg.setRotation(-0.6F + cycle * 0.4F, 0F, 0F);

        playerModel.body.setRotation(-0.35F, 0F, 0F);
        playerModel.head.xRot -= 0.15F;
    }

    private static void applyChanneledJutsuPose(PlayerModel playerModel) {
        playerModel.rightArm.setRotation(-1.2F, -0.3F, 0F);
        playerModel.leftArm.setRotation(-1.2F, 0.3F, 0F);
        playerModel.rightArm.setPos(-4.5F, 2.0F, -1.0F);
        playerModel.leftArm.setPos(4.5F, 2.0F, -1.0F);
    }

    /**
     * Brief tiger-seal snap shown on every successful INSTANT jutsu cast (see
     * NinjaData#castPoseTicks) — hands drawn together in front of the chest for a handful
     * of ticks so casting always reads as a deliberate hand-seal, not a silent proc.
     * Shadow Clone opts into the mirrored "cross seal" variant instead of the default.
     */
    private static void applyHandSealPose(PlayerModel playerModel, boolean crossSeal) {
        float yaw = crossSeal ? 0.3F : -0.3F;
        playerModel.rightArm.setRotation(-1.2F, yaw, 0F);
        playerModel.leftArm.setRotation(-1.2F, -yaw, 0F);
        playerModel.rightArm.setPos(-4.5F, 2.0F, -1.0F);
        playerModel.leftArm.setPos(4.5F, 2.0F, -1.0F);
    }

    /**
     * Cupped-hands-to-mouth pose while charging Fireball — distinct from the generic
     * channeled-jutsu hand-seal pose, matching the anime's signature Fire Style tell.
     */
    private static void applyFireballPose(PlayerModel playerModel) {
        playerModel.rightArm.setRotation(-2.1F, -0.35F, 0F);
        playerModel.leftArm.setRotation(-2.1F, 0.35F, 0F);
        playerModel.rightArm.setPos(-3.0F, 0.5F, -2.0F);
        playerModel.leftArm.setPos(3.0F, 0.5F, -2.0F);
        playerModel.head.xRot -= 0.1F;
    }

    /**
     * Crackling hand thrust forward — Chidori's active window (isChidoriActive(), up to
     * 8x20 ticks) is much longer than the generic cast flash, so it gets its own distinct
     * stiff-arm pose instead of sharing the brief hand-seal snap.
     */
    private static void applyChidoriThrustPose(PlayerModel playerModel) {
        playerModel.rightArm.setRotation(-1.55F, -0.1F, 0F);
        playerModel.rightArm.setPos(-4.0F, 2.5F, -2.5F);
        playerModel.leftArm.setRotation(-0.3F, 0.15F, 0F);
    }

    /**
     * One flat palm thrust forward — Air Palm's signature Hyuga tell, asymmetric so it
     * reads distinctly from the two-handed generic hand-seal flash.
     */
    private static void applyAirPalmPose(PlayerModel playerModel) {
        playerModel.rightArm.setRotation(-1.5F, -0.05F, 0F);
        playerModel.rightArm.setPos(-4.0F, 2.0F, -2.0F);
        playerModel.leftArm.setRotation(-0.2F, 0.1F, 0F);
    }

    /**
     * Both hands drawn back near the chest as if compressing/inhaling air before the
     * release — Great Breakthrough's wind-gathering tell.
     */
    private static void applyWindGatherPose(PlayerModel playerModel) {
        playerModel.rightArm.setRotation(-0.9F, -0.6F, 0F);
        playerModel.rightArm.setPos(-3.0F, 1.5F, 0.5F);
        playerModel.leftArm.setRotation(-0.9F, 0.6F, 0F);
        playerModel.leftArm.setPos(3.0F, 1.5F, 0.5F);
    }

    /**
     * Both arms thrust outward to the sides — mirrors Adamantine Chains' actual 4-direction
     * chakra-chain fan instead of the generic forward-facing hand-seal.
     */
    private static void applyChainExpulsionPose(PlayerModel playerModel) {
        playerModel.rightArm.setRotation(0F, -1.4F, 0F);
        playerModel.leftArm.setRotation(0F, 1.4F, 0F);
    }

    /**
     * Wide, grounded power stance while Susanoo is active — sells "channeling a giant
     * spectral avatar" instead of the player silently idling. Only dispatched for stages
     * 1-3 (see sprintingAnim); stage 4 hides the player model entirely via RenderEvents.
     */
    private static void applySusanooPose(PlayerModel playerModel) {
        playerModel.body.setRotation(-0.1F, 0F, 0F);
        playerModel.rightArm.setRotation(-0.3F, -0.5F, -0.15F);
        playerModel.leftArm.setRotation(-0.3F, 0.5F, 0.15F);
        playerModel.rightLeg.setRotation(-0.15F, -0.2F, 0F);
        playerModel.leftLeg.setRotation(-0.15F, 0.2F, 0F);
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
