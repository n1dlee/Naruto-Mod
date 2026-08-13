package com.sekwah.narutomod.anims;

import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.anims.PoseBlender.Track;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Drives every player-body pose in the mod.
 *
 * Each stance is dispatched with a blend weight from {@link PoseBlender} rather than being
 * written straight onto the model. Poses used to snap on and off within a single frame, which
 * is why casting read as unanimated no matter how many stances existed — the arms never
 * travelled anywhere, they just teleported. Every applyXxx method below now takes a weight and
 * lerps from whatever vanilla already animated toward its target, so the same stances now
 * ease in, hold, and ease out.
 *
 * How long a cast stance holds is the casting ability's own business — see
 * Ability#castPoseTicks. A substitution should be gone before you register it; a summoning
 * should hold long enough to look like effort.
 */
public class PlayerAnimHandler {

    // How long each stance takes to blend fully in or out, in ticks. Cast snaps are quick by
    // design; the transformation stances are slow because they represent settling into a mode.
    private static final float RAMP_CAST = 2.5f;
    private static final float RAMP_CHANNEL = 4f;
    private static final float RAMP_CHIDORI = 3f;
    private static final float RAMP_WALL_CLIMB = 3f;
    private static final float RAMP_SPRINT = 3f;
    private static final float RAMP_SAGE = 8f;
    private static final float RAMP_KURAMA = 6f;
    private static final float RAMP_SUSANOO = 6f;
    private static final float RAMP_GATES = 4f;

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
            ResourceLocation channeledAbility = ninjaData.getCurrentlyChanneledAbility();
            boolean chidoriActive = ninjaData.isChidoriActive();
            // The three upper-body stances are mutually exclusive at the source, so only one
            // is ever driven toward 1 - but the other two still need to be ticked so they ease
            // back out instead of freezing at whatever weight they were abandoned on.
            boolean channelActive = channeledAbility != null;
            boolean castActive = !channelActive && !chidoriActive && ninjaData.getCastPoseTicks() > 0;

            float channelWeight = PoseBlender.weight(entity, Track.CHANNEL, channelActive, RAMP_CHANNEL, ageInTicks);
            float chidoriWeight = PoseBlender.weight(entity, Track.CHIDORI, chidoriActive, RAMP_CHIDORI, ageInTicks);
            float castWeight = PoseBlender.weight(entity, Track.CAST, castActive, RAMP_CAST, ageInTicks);

            if (channelWeight > PoseBlender.EPSILON) {
                applyChanneledPose(playerModel, channelWeight, channeledAbility);
            }
            if (chidoriWeight > PoseBlender.EPSILON) {
                applyChidoriThrustPose(playerModel, chidoriWeight);
            }
            if (castWeight > PoseBlender.EPSILON) {
                applyCastPose(playerModel, castWeight, ninjaData.getLastCastAbilityId(),
                        ninjaData.isCrossSealPose());
            }

            boolean wallClimbing = ninjaData.isWallWalkAttached();
            boolean sprinting = !wallClimbing && entity.isSprinting()
                    && !entity.isVisuallySwimming() && !entity.isCrouching();
            float wallWeight = PoseBlender.weight(entity, Track.WALL_CLIMB, wallClimbing, RAMP_WALL_CLIMB, ageInTicks);
            float sprintWeight = PoseBlender.weight(entity, Track.SPRINT, sprinting, RAMP_SPRINT, ageInTicks);
            if (wallWeight > PoseBlender.EPSILON) {
                applyWallRunPose(playerModel, entity, wallWeight, ageInTicks);
            }
            if (sprintWeight > PoseBlender.EPSILON) {
                applyNarutoSprintPose(playerModel, sprintWeight);
            }

            boolean sageIdle = ninjaData.isSageModeActive() && !entity.isSprinting() && limbSwingAmount < 0.1F;
            float sageWeight = PoseBlender.weight(entity, Track.SAGE, sageIdle, RAMP_SAGE, ageInTicks);
            if (sageWeight > PoseBlender.EPSILON) {
                applySageIdlePose(playerModel, sageWeight);
            }

            float kuramaWeight = PoseBlender.weight(entity, Track.KURAMA,
                    ninjaData.isKuramaCloakActive(), RAMP_KURAMA, ageInTicks);
            if (kuramaWeight > PoseBlender.EPSILON) {
                applyKuramaCloakPose(playerModel, kuramaWeight);
            }

            boolean susanooStance = ninjaData.isSusanooActive() && ninjaData.getSusanooStage() < 4;
            float susanooWeight = PoseBlender.weight(entity, Track.SUSANOO, susanooStance, RAMP_SUSANOO, ageInTicks);
            if (susanooWeight > PoseBlender.EPSILON) {
                applySusanooPose(playerModel, susanooWeight);
            }

            int gatesOpen = ninjaData.getGatesOpen();
            float gatesWeight = PoseBlender.weight(entity, Track.GATES, gatesOpen >= 5, RAMP_GATES, ageInTicks);
            if (gatesWeight > PoseBlender.EPSILON) {
                applyEightGatesShake(playerModel, gatesWeight, gatesOpen, ageInTicks);
            }
        }));
    }

    /** Picks the stance for whatever is currently being channeled. */
    private static void applyChanneledPose(PlayerModel playerModel, float weight, ResourceLocation ability) {
        if (ability != null && ability.equals(NarutoAbilities.CHAKRA_CHARGE.getId())) {
            PoseBlender.rotate(playerModel.rightArm, weight, -1.375616F, -0.5948606F, 0F);
            PoseBlender.rotate(playerModel.leftArm, weight, -1.375616F, 0.5948606F, 0F);
        } else if (ability != null && ability.equals(NarutoAbilities.FIREBALL.getId())) {
            applyFireballPose(playerModel, weight);
        } else {
            applyChanneledJutsuPose(playerModel, weight);
        }
    }

    /** Picks the stance for the jutsu that just fired, defaulting to the hand-seal snap. */
    private static void applyCastPose(PlayerModel playerModel, float weight,
                                      ResourceLocation lastCast, boolean crossSeal) {
        if (lastCast != null && lastCast.equals(NarutoAbilities.AIR_PALM.getId())) {
            applyAirPalmPose(playerModel, weight);
        } else if (lastCast != null && lastCast.equals(NarutoAbilities.GREAT_BREAKTHROUGH.getId())) {
            applyWindGatherPose(playerModel, weight);
        } else if (lastCast != null && lastCast.equals(NarutoAbilities.ADAMANTINE_CHAINS.getId())) {
            applyChainExpulsionPose(playerModel, weight);
        } else {
            applyHandSealPose(playerModel, weight, crossSeal);
        }
    }

    /**
     * Running up the wall, not clinging to it.
     *
     * The old pose was a climb: arms splayed overhead gripping the surface, legs barely
     * moving. That reads as Spider-Man, which is the one thing this technique is not - a
     * ninja goes up a wall at a run, upright, arms trailing behind.
     *
     * The model is deliberately NOT rotated ninety degrees. A full rotation is right for
     * "gravity now points at the wall"; for a wall run it lays the player out horizontally
     * and reads as clinging again. The body only leans a little toward the surface.
     *
     * The leg cycle is driven by deltaMovement rather than limbSwing on purpose: limbSwing
     * tracks horizontal travel, so going straight up a wall - the most common thing anyone
     * does with this - left the legs frozen mid-stride. deltaMovement includes the vertical
     * component, so the run keeps its rhythm going up, down or sideways, and falls to zero
     * when the player stops, which stops the legs on its own.
     */
    private static void applyWallRunPose(PlayerModel playerModel, Entity player,
                                         float weight, float ageInTicks) {
        float speed = (float) net.minecraft.util.Mth.clamp(
                player.getDeltaMovement().length() * 7.0D, 0.0D, 1.0D);
        float stride = net.minecraft.util.Mth.cos(ageInTicks * 1.45F) * 0.95F * speed;

        // A lean toward the wall, not a lie-down on it.
        PoseBlender.rotate(playerModel.body, weight, 0.24F, 0F, 0F);
        PoseBlender.addRotation(playerModel.head, weight, -0.10F, 0F, 0F);

        PoseBlender.rotate(playerModel.rightLeg, weight, stride, 0F, 0F);
        PoseBlender.rotate(playerModel.leftLeg, weight, -stride, 0F, 0F);

        // Arms back, the way he runs on the ground - not reaching for the blocks.
        PoseBlender.rotate(playerModel.rightArm, weight, 1.35F, -0.12F, 0F);
        PoseBlender.rotate(playerModel.leftArm, weight, 1.35F, 0.12F, 0F);
        PoseBlender.position(playerModel.rightArm, weight, -5.0F, 3.7F, -4.0F);
        PoseBlender.position(playerModel.leftArm, weight, 5.0F, 3.4F, -4.0F);
    }

    private static void applyChanneledJutsuPose(PlayerModel playerModel, float weight) {
        PoseBlender.rotate(playerModel.rightArm, weight, -1.2F, -0.3F, 0F);
        PoseBlender.rotate(playerModel.leftArm, weight, -1.2F, 0.3F, 0F);
        PoseBlender.position(playerModel.rightArm, weight, -4.5F, 2.0F, -1.0F);
        PoseBlender.position(playerModel.leftArm, weight, 4.5F, 2.0F, -1.0F);
    }

    /**
     * Brief tiger-seal snap shown on every successful INSTANT jutsu cast (see
     * NinjaData#castPoseTicks) — hands drawn together in front of the chest so casting always
     * reads as a deliberate hand-seal, not a silent proc. Shadow Clone opts into the mirrored
     * "cross seal" variant instead of the default.
     */
    private static void applyHandSealPose(PlayerModel playerModel, float weight, boolean crossSeal) {
        float yaw = crossSeal ? 0.3F : -0.3F;
        PoseBlender.rotate(playerModel.rightArm, weight, -1.2F, yaw, 0F);
        PoseBlender.rotate(playerModel.leftArm, weight, -1.2F, -yaw, 0F);
        PoseBlender.position(playerModel.rightArm, weight, -4.5F, 2.0F, -1.0F);
        PoseBlender.position(playerModel.leftArm, weight, 4.5F, 2.0F, -1.0F);
    }

    /**
     * Cupped-hands-to-mouth pose while charging Fireball — distinct from the generic
     * channeled-jutsu hand-seal pose, matching the anime's signature Fire Style tell.
     */
    private static void applyFireballPose(PlayerModel playerModel, float weight) {
        PoseBlender.rotate(playerModel.rightArm, weight, -2.1F, -0.35F, 0F);
        PoseBlender.rotate(playerModel.leftArm, weight, -2.1F, 0.35F, 0F);
        PoseBlender.position(playerModel.rightArm, weight, -3.0F, 0.5F, -2.0F);
        PoseBlender.position(playerModel.leftArm, weight, 3.0F, 0.5F, -2.0F);
        PoseBlender.addRotation(playerModel.head, weight, -0.1F, 0F, 0F);
    }

    /**
     * Crackling hand thrust forward — Chidori's active window (isChidoriActive(), up to
     * 8x20 ticks) is much longer than the generic cast flash, so it gets its own distinct
     * stiff-arm pose instead of sharing the brief hand-seal snap.
     */
    private static void applyChidoriThrustPose(PlayerModel playerModel, float weight) {
        PoseBlender.rotate(playerModel.rightArm, weight, -1.55F, -0.1F, 0F);
        PoseBlender.position(playerModel.rightArm, weight, -4.0F, 2.5F, -2.5F);
        PoseBlender.rotate(playerModel.leftArm, weight, -0.3F, 0.15F, 0F);
    }

    /**
     * One flat palm thrust forward — Air Palm's signature Hyuga tell, asymmetric so it
     * reads distinctly from the two-handed generic hand-seal flash.
     */
    private static void applyAirPalmPose(PlayerModel playerModel, float weight) {
        PoseBlender.rotate(playerModel.rightArm, weight, -1.5F, -0.05F, 0F);
        PoseBlender.position(playerModel.rightArm, weight, -4.0F, 2.0F, -2.0F);
        PoseBlender.rotate(playerModel.leftArm, weight, -0.2F, 0.1F, 0F);
    }

    /**
     * Both hands drawn back near the chest as if compressing/inhaling air before the
     * release — Great Breakthrough's wind-gathering tell.
     */
    private static void applyWindGatherPose(PlayerModel playerModel, float weight) {
        PoseBlender.rotate(playerModel.rightArm, weight, -0.9F, -0.6F, 0F);
        PoseBlender.position(playerModel.rightArm, weight, -3.0F, 1.5F, 0.5F);
        PoseBlender.rotate(playerModel.leftArm, weight, -0.9F, 0.6F, 0F);
        PoseBlender.position(playerModel.leftArm, weight, 3.0F, 1.5F, 0.5F);
    }

    /**
     * Both arms thrust outward to the sides — mirrors Adamantine Chains' actual 4-direction
     * chakra-chain fan instead of the generic forward-facing hand-seal.
     */
    private static void applyChainExpulsionPose(PlayerModel playerModel, float weight) {
        PoseBlender.rotate(playerModel.rightArm, weight, 0F, -1.4F, 0F);
        PoseBlender.rotate(playerModel.leftArm, weight, 0F, 1.4F, 0F);
    }

    /**
     * Wide, grounded power stance while Susanoo is active — sells "channeling a giant
     * spectral avatar" instead of the player silently idling. Only dispatched for stages
     * 1-3 (see sprintingAnim); stage 4 hides the player model entirely via RenderEvents.
     */
    private static void applySusanooPose(PlayerModel playerModel, float weight) {
        PoseBlender.rotate(playerModel.body, weight, -0.1F, 0F, 0F);
        PoseBlender.rotate(playerModel.rightArm, weight, -0.3F, -0.5F, -0.15F);
        PoseBlender.rotate(playerModel.leftArm, weight, -0.3F, 0.5F, 0.15F);
        PoseBlender.rotate(playerModel.rightLeg, weight, -0.15F, -0.2F, 0F);
        PoseBlender.rotate(playerModel.leftLeg, weight, -0.15F, 0.2F, 0F);
    }

    private static void applySageIdlePose(PlayerModel playerModel, float weight) {
        PoseBlender.rotate(playerModel.rightArm, weight, -0.2F, -0.15F, 0.1F);
        PoseBlender.rotate(playerModel.leftArm, weight, -0.2F, 0.15F, -0.1F);
        PoseBlender.rotate(playerModel.body, weight, 0.05F, 0F, 0F);
    }

    private static void applyKuramaCloakPose(PlayerModel playerModel, float weight) {
        PoseBlender.rotate(playerModel.body, weight, 0.2F, 0F, 0F);
        PoseBlender.rotate(playerModel.rightArm, weight, 0.5F, 0F, -0.3F);
        PoseBlender.rotate(playerModel.leftArm, weight, 0.5F, 0F, 0.3F);
        PoseBlender.addRotation(playerModel.head, weight, -0.08F, 0F, 0F);
    }

    private static void applyEightGatesShake(PlayerModel playerModel, float weight,
                                             int gatesOpen, float ageInTicks) {
        float shake = (float) Math.sin(ageInTicks * 3.0F) * 0.02F * Math.max(gatesOpen, 5) * weight;
        playerModel.body.x += shake;
        playerModel.head.x += shake * 0.5F;
    }

    private static void applyNarutoSprintPose(PlayerModel playerModel, float weight) {
        PoseBlender.rotate(playerModel.rightArm, weight, 1.412787F, 0F, 0F);
        PoseBlender.position(playerModel.rightArm, weight, -5F, 3.933333F, -3F - 2F);

        PoseBlender.rotate(playerModel.leftArm, weight, 1.412787F, 0F, 0F);
        PoseBlender.position(playerModel.leftArm, weight, 5F, 3.266667F, -3F - 2F);

        playerModel.head.xRot = net.minecraft.util.Mth.lerp(weight, playerModel.head.xRot, 0F);
        PoseBlender.position(playerModel.head, weight, 0F, 3.133333F - 1F, -5F - 1F);

        PoseBlender.rotate(playerModel.body, weight, 0.5435722F, 0F, 0F);
        PoseBlender.position(playerModel.body, weight, 0F, 3F - 1F, -3.5F - 2F);
    }
}
