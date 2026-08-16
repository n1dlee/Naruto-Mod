package com.sekwah.narutomod.anims;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.anims.PoseBlender.Curve;
import com.sekwah.narutomod.anims.PoseBlender.Track;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.registries.NarutoRegistries;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
    private static final float RAMP_LANDING = 1.5f;

    /** Movement below this along the wall is standing still, not running. */
    private static final double WALL_RUN_DEAD_ZONE = 0.02;

    /** Below this a drop is a step, not a landing worth animating. */
    private static final float LANDING_MIN_FALL = 2.5f;
    private static final float LANDING_TICKS_MIN = 5f;
    private static final float LANDING_TICKS_MAX = 14f;

    /** Per-player landing memory; see {@link #landingActive}. Client-only and disposable. */
    private static final java.util.Map<Entity, float[]> LANDING_STATE = new java.util.WeakHashMap<>();

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
                applySageIdlePose(playerModel, sageWeight, PoseBlender.elapsed(entity, Track.SAGE));
            }

            float kuramaWeight = PoseBlender.weight(entity, Track.KURAMA,
                    ninjaData.isKuramaCloakActive(), RAMP_KURAMA, ageInTicks);
            if (kuramaWeight > PoseBlender.EPSILON) {
                applyKuramaCloakPose(playerModel, kuramaWeight, PoseBlender.elapsed(entity, Track.KURAMA));
            }

            boolean susanooStance = ninjaData.isSusanooActive() && ninjaData.getSusanooStage() < 4;
            float susanooWeight = PoseBlender.weight(entity, Track.SUSANOO, susanooStance, RAMP_SUSANOO, ageInTicks);
            if (susanooWeight > PoseBlender.EPSILON) {
                applySusanooPose(playerModel, susanooWeight, PoseBlender.elapsed(entity, Track.SUSANOO));
            }

            // ---- Actions, applied LAST ----
            //
            // Order here IS priority. PoseBlender.rotate drives a limb toward an absolute
            // angle, so whichever pose runs last owns that limb - and these used to run
            // first, which meant a transformation stance silently erased them. Holding a
            // Chidori inside a Susanoo showed the Susanoo's idle arms; the lightning was in
            // the hand and the body was standing at rest.
            //
            // Stances describe what you ARE and are applied above; actions describe what you
            // are DOING and win, because doing beats being.
            float channelWeight = PoseBlender.weight(entity, Track.CHANNEL, channelActive, RAMP_CHANNEL, ageInTicks);
            float chidoriWeight = PoseBlender.weight(entity, Track.CHIDORI, chidoriActive, RAMP_CHIDORI, ageInTicks);
            float castWeight = PoseBlender.weight(entity, Track.CAST, castActive, RAMP_CAST, ageInTicks);

            if (channelWeight > PoseBlender.EPSILON) {
                applyChanneledPose(playerModel, channelWeight, channeledAbility,
                        PoseBlender.elapsed(entity, Track.CHANNEL));
            }
            if (chidoriWeight > PoseBlender.EPSILON) {
                applyChidoriThrustPose(playerModel, chidoriWeight,
                        PoseBlender.elapsed(entity, Track.CHIDORI));
            }
            if (castWeight > PoseBlender.EPSILON) {
                applyCastPose(playerModel, castWeight, castPhase(ninjaData),
                        ninjaData.getLastCastAbilityId(), ninjaData.isCrossSealPose());
            }

            // The sword swing overrides the standing stance for as long as it runs - it is the
            // one thing the Susanoo does that has to be readable from across a battlefield.
            int swingTicks = ninjaData.getSusanooSwingTicks();
            float swingWeight = PoseBlender.weight(entity, Track.SUSANOO_SWING,
                    swingTicks > 0, RAMP_CAST, ageInTicks);
            if (swingWeight > PoseBlender.EPSILON) {
                float swingPhase = swingTicks <= 0 ? 1f
                        : Mth.clamp(1f - swingTicks / (float) com.sekwah.narutomod.capabilities
                                .NinjaData.SUSANOO_SWING_TICKS, 0f, 1f);
                applySusanooSwingPose(playerModel, swingWeight, swingPhase);
            }

            int gatesOpen = ninjaData.getGatesOpen();
            float gatesWeight = PoseBlender.weight(entity, Track.GATES, gatesOpen >= 5, RAMP_GATES, ageInTicks);
            if (gatesWeight > PoseBlender.EPSILON) {
                applyEightGatesShake(playerModel, gatesWeight, gatesOpen, ageInTicks);
            }

            // Forcing a gate is its own moment, not a change of stance: the whole body locks
            // and drives downward for about a second. Rides the cast timer of EIGHT_GATES,
            // so it fires once per gate rather than for as long as the gates stay open.
            boolean gateRoar = ninjaData.getCastPoseTicks() > 0
                    && NarutoAbilities.EIGHT_GATES.getId().equals(ninjaData.getLastCastAbilityId());
            float roarWeight = PoseBlender.weight(entity, Track.GATES_OPEN, gateRoar, RAMP_CAST, ageInTicks);
            if (roarWeight > PoseBlender.EPSILON) {
                applyGateRoarPose(playerModel, roarWeight, castPhase(ninjaData));
            }

            float landWeight = PoseBlender.weight(entity, Track.LANDING,
                    landingActive(entity, ageInTicks), RAMP_LANDING, ageInTicks);
            if (landWeight > PoseBlender.EPSILON) {
                applyLandingPose(playerModel, landWeight, PoseBlender.elapsed(entity, Track.LANDING));
            }
        }));
    }

    /**
     * How far through its hold the current cast stance is, 0 at the snap and 1 at the end.
     *
     * {@link INinjaData#getCastPoseTicks()} counts down, and the total it started from is the
     * casting ability's own {@code castPoseTicks()} - so the phase is recoverable from what is
     * already synced and needs no second field travelling alongside it.
     */
    private static float castPhase(INinjaData ninjaData) {
        int remaining = ninjaData.getCastPoseTicks();
        if (remaining <= 0) {
            return 1f;
        }
        ResourceLocation id = ninjaData.getLastCastAbilityId();
        Ability ability = id == null ? null : NarutoRegistries.ABILITIES.getValue(id);
        int total = ability == null ? 8 : ability.castPoseTicks();
        if (total <= 0) {
            return 1f;
        }
        return Mth.clamp(1f - remaining / (float) total, 0f, 1f);
    }

    /**
     * True for a short window after touching down from a fall worth absorbing.
     *
     * fallDistance is already zero by the time the model animates the landing frame, so the
     * drop has to be remembered while the player is still in the air. [0] is the largest fall
     * seen this flight, [1] counts the landing out, [2] is the last ageInTicks, [3] is whether
     * the player was airborne on the previous frame.
     */
    private static boolean landingActive(Entity entity, float ageInTicks) {
        float[] state = LANDING_STATE.computeIfAbsent(entity, e -> new float[]{0f, 0f, ageInTicks, 0f});
        float delta = Mth.clamp(ageInTicks - state[2], 0f, 5f);
        state[2] = ageInTicks;

        boolean airborne = !entity.onGround();
        if (airborne) {
            state[0] = Math.max(state[0], entity.fallDistance);
        } else {
            if (state[3] > 0.5f && state[0] >= LANDING_MIN_FALL) {
                // Deeper drops are absorbed for longer, up to a limit - a ninja stepping off
                // a fence should dip, not go into a full three-point landing.
                state[1] = Math.min(LANDING_TICKS_MAX, LANDING_TICKS_MIN + state[0] * 0.6f);
            }
            state[0] = 0f;
            state[1] = Math.max(0f, state[1] - delta);
        }
        state[3] = airborne ? 1f : 0f;
        return state[1] > 0f;
    }

    /** Picks the stance for whatever is currently being channeled. */
    private static void applyChanneledPose(PlayerModel playerModel, float weight,
                                           ResourceLocation ability, float elapsed) {
        if (ability != null && ability.equals(NarutoAbilities.CHAKRA_CHARGE.getId())) {
            applyChakraChargePose(playerModel, weight, elapsed);
        } else if (ability != null && ability.equals(NarutoAbilities.FIREBALL.getId())) {
            applyFireballPose(playerModel, weight, elapsed);
        } else if (ability != null && (ability.equals(NarutoAbilities.RASENGAN.getId())
                || ability.equals(NarutoAbilities.RASENSHURIKEN.getId()))) {
            applyRasenganPose(playerModel, weight, elapsed);
        } else {
            applyChanneledJutsuPose(playerModel, weight, elapsed);
        }
    }

    /** Picks the stance for the jutsu that just fired, defaulting to the hand-seal snap. */
    private static void applyCastPose(PlayerModel playerModel, float weight, float phase,
                                      ResourceLocation lastCast, boolean crossSeal) {
        if (lastCast != null && lastCast.equals(NarutoAbilities.AIR_PALM.getId())) {
            applyAirPalmPose(playerModel, weight, phase);
        } else if (lastCast != null && lastCast.equals(NarutoAbilities.GREAT_BREAKTHROUGH.getId())) {
            applyWindGatherPose(playerModel, weight, phase);
        } else if (lastCast != null && lastCast.equals(NarutoAbilities.ADAMANTINE_CHAINS.getId())) {
            applyChainExpulsionPose(playerModel, weight, phase);
        } else if (lastCast != null && lastCast.equals(NarutoAbilities.AMATERASU.getId())) {
            applyAmaterasuPose(playerModel, weight, phase);
        } else if (lastCast != null && lastCast.equals(NarutoAbilities.EIGHT_GATES.getId())) {
            // The roar owns this cast outright; the generic seal would fight it.
            return;
        } else if (lastCast != null && (lastCast.equals(NarutoAbilities.EARTH_WALL.getId())
                || lastCast.equals(NarutoAbilities.EARTH_SPIKES.getId()))) {
            applyGroundSlamPose(playerModel, weight, phase);
        } else if (lastCast != null && lastCast.equals(NarutoAbilities.MYSTICAL_PALM.getId())) {
            applyHealingPalmPose(playerModel, weight, phase);
        } else if (lastCast != null && (lastCast.equals(NarutoAbilities.SHADOW_POSSESSION.getId())
                || lastCast.equals(NarutoAbilities.SHADOW_SEWING.getId())
                || lastCast.equals(NarutoAbilities.SHADOW_STRANGLE.getId()))) {
            applyRatSealPose(playerModel, weight, phase);
        } else if (lastCast != null && lastCast.equals(NarutoAbilities.EIGHT_TRIGRAMS_ROTATION.getId())) {
            applyRotationPose(playerModel, weight, phase);
        } else if (lastCast != null && lastCast.equals(NarutoAbilities.SUBSTITUTION.getId())) {
            applySubstitutionPose(playerModel, weight, phase);
        } else if (lastCast != null && (lastCast.equals(NarutoAbilities.KAMUI.getId())
                || lastCast.equals(NarutoAbilities.KAMUI_WARP.getId())
                || lastCast.equals(NarutoAbilities.KAMUI_PHASE.getId()))) {
            applyKamuiPose(playerModel, weight, phase);
        } else if (lastCast != null && lastCast.equals(NarutoAbilities.WOOD_RELEASE.getId())) {
            applyRamSealPose(playerModel, weight, phase);
        } else {
            applyHandSealPose(playerModel, weight, phase, crossSeal);
        }
    }

    /** Earth techniques: both palms driven down into the ground, body dropping with them. */
    private static void applyGroundSlamPose(PlayerModel playerModel, float weight, float phase) {
        SLAM_RIGHT.rotate(playerModel.rightArm, phase, weight);
        SLAM_LEFT.rotate(playerModel.leftArm, phase, weight);
        PoseBlender.addRotation(playerModel.body, weight, SLAM_BODY.sampleX(phase), 0F, 0F);
        playerModel.body.y += 1.6F * weight * SLAM_BODY.sampleY(phase);
    }

    private static final Curve SLAM_RIGHT = Curve.of(
            0.00f, -1.60f, -0.30f, 0.00f,
            0.35f, -2.10f, -0.20f, -0.15f,
            0.55f, 0.35f, -0.10f, -0.20f,
            1.00f, 0.20f, -0.10f, -0.15f);
    private static final Curve SLAM_LEFT = Curve.of(
            0.00f, -1.60f, 0.30f, 0.00f,
            0.35f, -2.10f, 0.20f, 0.15f,
            0.55f, 0.35f, 0.10f, 0.20f,
            1.00f, 0.20f, 0.10f, 0.15f);
    /** x = fold forward, y = how far the body drops into the slam. */
    private static final Curve SLAM_BODY = Curve.of(
            0.00f, -0.10f, 0.00f, 0f,
            0.35f, -0.22f, 0.00f, 0f,
            0.55f, 0.45f, 1.00f, 0f,
            1.00f, 0.30f, 0.70f, 0f);

    /** Medical ninjutsu: one steady palm held out, no snap. Precision, not force. */
    private static void applyHealingPalmPose(PlayerModel playerModel, float weight, float phase) {
        HEAL_ARM.rotate(playerModel.rightArm, phase, weight);
        PoseBlender.position(playerModel.rightArm, weight, -4.5F, 3.0F, -2.0F);
        PoseBlender.rotate(playerModel.leftArm, weight, -0.55F, 0.45F, 0.20F);
    }

    private static final Curve HEAL_ARM = Curve.of(
            0.00f, -0.25f, -0.20f, 0.10f,
            0.60f, -1.30f, -0.30f, 0.15f,
            1.00f, -1.25f, -0.28f, 0.14f);

    /** Nara shadow techniques: the rat seal, held low and pushed forward. */
    private static void applyRatSealPose(PlayerModel playerModel, float weight, float phase) {
        RAT_RIGHT.rotate(playerModel.rightArm, phase, weight);
        RAT_LEFT.rotate(playerModel.leftArm, phase, weight);
        PoseBlender.position(playerModel.rightArm, weight, -3.5F, 4.0F, -1.5F);
        PoseBlender.position(playerModel.leftArm, weight, 3.5F, 4.0F, -1.5F);
        PoseBlender.addRotation(playerModel.body, weight, 0.18F * weight, 0F, 0F);
    }

    private static final Curve RAT_RIGHT = Curve.of(
            0.00f, -0.25f, -0.40f, 0.30f,
            0.40f, -0.95f, -0.18f, 0.45f,
            1.00f, -0.80f, -0.20f, 0.40f);
    private static final Curve RAT_LEFT = Curve.of(
            0.00f, -0.25f, 0.40f, -0.30f,
            0.40f, -0.95f, 0.18f, -0.45f,
            1.00f, -0.80f, 0.20f, -0.40f);

    /** Kaiten: arms flung wide and the whole body whipping around. */
    private static void applyRotationPose(PlayerModel playerModel, float weight, float phase) {
        PoseBlender.rotate(playerModel.rightArm, weight, -0.15F, 0F, -1.45F);
        PoseBlender.rotate(playerModel.leftArm, weight, -0.15F, 0F, 1.45F);
        // A full turn across the cast, so the spin is a spin and not a T-pose.
        PoseBlender.addRotation(playerModel.body, weight, 0F, phase * Mth.TWO_PI, 0F);
        PoseBlender.addRotation(playerModel.rightLeg, weight, 0F, 0F, -0.20F);
        PoseBlender.addRotation(playerModel.leftLeg, weight, 0F, 0F, 0.20F);
    }

    /** Substitution: a hard flinch away, gone before the pose finishes. */
    private static void applySubstitutionPose(PlayerModel playerModel, float weight, float phase) {
        SUB_ARMS.rotate(playerModel.rightArm, phase, weight);
        PoseBlender.rotate(playerModel.leftArm, weight,
                SUB_ARMS.sampleX(phase), -SUB_ARMS.sampleY(phase), -SUB_ARMS.sampleZ(phase));
        PoseBlender.addRotation(playerModel.body, weight, -0.35F * (1F - phase), 0F, 0F);
    }

    private static final Curve SUB_ARMS = Curve.of(
            0.00f, -1.90f, -0.75f, 0.55f,
            0.45f, -1.40f, -0.30f, 0.25f,
            1.00f, -0.60f, -0.15f, 0.10f);

    /** Kamui: two fingers to the eye, head locked on the target. */
    private static void applyKamuiPose(PlayerModel playerModel, float weight, float phase) {
        KAMUI_ARM.rotate(playerModel.rightArm, phase, weight);
        PoseBlender.position(playerModel.rightArm, weight, -3.5F, 0.5F, -1.0F);
        PoseBlender.rotate(playerModel.leftArm, weight, -0.10F, 0.10F, 0.05F);
        PoseBlender.addRotation(playerModel.head, weight, -0.12F * weight, 0F, 0F);
    }

    private static final Curve KAMUI_ARM = Curve.of(
            0.00f, -0.30f, -0.25f, 0.00f,
            0.40f, -2.45f, -0.45f, 0.35f,
            1.00f, -2.30f, -0.40f, 0.30f);

    /** Wood Release: hands clasped in the ram seal, forced together. */
    private static void applyRamSealPose(PlayerModel playerModel, float weight, float phase) {
        RAM_RIGHT.rotate(playerModel.rightArm, phase, weight);
        RAM_LEFT.rotate(playerModel.leftArm, phase, weight);
        PoseBlender.position(playerModel.rightArm, weight, -2.5F, 1.5F, -2.0F);
        PoseBlender.position(playerModel.leftArm, weight, 2.5F, 1.5F, -2.0F);
        PoseBlender.addRotation(playerModel.head, weight, -0.10F * weight, 0F, 0F);
    }

    private static final Curve RAM_RIGHT = Curve.of(
            0.00f, -0.40f, -0.60f, 0.00f,
            0.35f, -1.75f, -0.10f, 0.10f,
            1.00f, -1.60f, -0.12f, 0.08f);
    private static final Curve RAM_LEFT = Curve.of(
            0.00f, -0.40f, 0.60f, 0.00f,
            0.35f, -1.75f, 0.10f, -0.10f,
            1.00f, -1.60f, 0.12f, -0.08f);

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
     * does with this - left the legs frozen mid-stride.
     *
     * But raw deltaMovement.length() is not the travel either. Wall Walk applies a constant
     * push INTO the wall every tick to keep the player attached, so a player standing
     * perfectly still on a wall still reports motion - and the legs kept cycling at a brisk
     * jog while nobody was going anywhere. That is the "fast crawl" this technique was
     * accused of: not the posture, the cadence.
     *
     * The grip component is therefore removed by projecting the movement onto the wall plane
     * and only measuring what is left, and a dead zone keeps sensor noise from twitching the
     * legs. The stride frequency now scales with that speed too, instead of running at a
     * fixed 4.6 cycles a second regardless of how fast the player was actually travelling.
     */
    private static void applyWallRunPose(PlayerModel playerModel, Entity player,
                                         float weight, float ageInTicks) {
        net.minecraft.world.phys.Vec3 motion = player.getDeltaMovement();
        // Everything except the component along the wall normal. The normal is not known
        // here, but the grip is the only thing pushing horizontally into a surface the player
        // is clinging to, so subtracting the collided axes leaves the travel along the wall.
        double tangential = Math.sqrt(
                (player.horizontalCollision ? 0.0 : motion.x * motion.x)
                        + motion.y * motion.y
                        + (player.horizontalCollision ? 0.0 : motion.z * motion.z));
        if (tangential < WALL_RUN_DEAD_ZONE) {
            tangential = 0.0;
        }
        float speed = (float) net.minecraft.util.Mth.clamp(tangential * 7.0D, 0.0D, 1.0D);
        // Cadence follows pace: a walk is not a sprint played at the same tempo.
        float stride = net.minecraft.util.Mth.cos(ageInTicks * (0.45F + 1.15F * speed)) * 0.95F * speed;

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

    /**
     * The default channel: hands come up into a seal and then hold, breathing.
     *
     * The breath is not decoration. A held pose with no motion at all stops reading as a
     * living body after about a second, which is what made every channel in this mod look
     * like the player had been paused rather than like they were concentrating.
     */
    private static void applyChanneledJutsuPose(PlayerModel playerModel, float weight, float elapsed) {
        float entry = Mth.clamp(elapsed / 6f, 0f, 1f);
        float breath = Mth.sin(elapsed * 0.18f) * 0.05f;
        CHANNEL_RIGHT.rotate(playerModel.rightArm, entry, weight);
        CHANNEL_LEFT.rotate(playerModel.leftArm, entry, weight);
        PoseBlender.position(playerModel.rightArm, weight, -4.5F, 2.0F, -1.0F);
        PoseBlender.position(playerModel.leftArm, weight, 4.5F, 2.0F, -1.0F);
        PoseBlender.addRotation(playerModel.rightArm, weight, breath, 0F, 0F);
        PoseBlender.addRotation(playerModel.leftArm, weight, breath, 0F, 0F);
        PoseBlender.addRotation(playerModel.body, weight, breath * 0.4f, 0F, 0F);
    }

    private static final Curve CHANNEL_RIGHT = Curve.of(
            0.00f, -0.30f, -0.10f, 0f,
            0.45f, -1.35f, -0.42f, 0f,
            1.00f, -1.20f, -0.30f, 0f);
    private static final Curve CHANNEL_LEFT = Curve.of(
            0.00f, -0.30f, 0.10f, 0f,
            0.45f, -1.35f, 0.42f, 0f,
            1.00f, -1.20f, 0.30f, 0f);

    /** Both fists drawn in and shaking as raw chakra is forced up. */
    private static void applyChakraChargePose(PlayerModel playerModel, float weight, float elapsed) {
        float entry = Mth.clamp(elapsed / 5f, 0f, 1f);
        float strain = Mth.sin(elapsed * 1.4f) * 0.035f;
        CHARGE_RIGHT.rotate(playerModel.rightArm, entry, weight);
        CHARGE_LEFT.rotate(playerModel.leftArm, entry, weight);
        PoseBlender.addRotation(playerModel.rightArm, weight, strain, 0F, strain);
        PoseBlender.addRotation(playerModel.leftArm, weight, strain, 0F, -strain);
        PoseBlender.addRotation(playerModel.body, weight, -strain * 0.6f, 0F, 0F);
    }

    private static final Curve CHARGE_RIGHT = Curve.of(
            0.00f, -0.40f, -0.20f, 0f,
            1.00f, -1.375616f, -0.5948606f, 0f);
    private static final Curve CHARGE_LEFT = Curve.of(
            0.00f, -0.40f, 0.20f, 0f,
            1.00f, -1.375616f, 0.5948606f, 0f);

    /**
     * Rasengan: the sphere is held out to the side in one hand, the other steadying it.
     *
     * It used to share the generic two-handed seal, which is the pose for making a jutsu, not
     * for carrying one that is already spinning. The wobble is the ball fighting the hand.
     */
    private static void applyRasenganPose(PlayerModel playerModel, float weight, float elapsed) {
        float entry = Mth.clamp(elapsed / 7f, 0f, 1f);
        float spin = Mth.sin(elapsed * 0.55f) * 0.045f;
        RASENGAN_RIGHT.rotate(playerModel.rightArm, entry, weight);
        RASENGAN_LEFT.rotate(playerModel.leftArm, entry, weight);
        PoseBlender.position(playerModel.rightArm, weight, -5.5F, 3.0F, -1.5F);
        PoseBlender.addRotation(playerModel.rightArm, weight, spin, spin * 0.5f, 0F);
        PoseBlender.addRotation(playerModel.body, weight, 0F, -0.12f * weight, 0F);
    }

    private static final Curve RASENGAN_RIGHT = Curve.of(
            0.00f, -0.20f, -0.10f, 0.00f,
            0.55f, -1.05f, -0.75f, -0.35f,
            1.00f, -0.95f, -0.62f, -0.28f);
    private static final Curve RASENGAN_LEFT = Curve.of(
            0.00f, -0.20f, 0.10f, 0.00f,
            0.55f, -0.85f, -0.35f, 0.45f,
            1.00f, -0.78f, -0.28f, 0.38f);

    /**
     * Brief tiger-seal snap shown on every successful INSTANT jutsu cast (see
     * NinjaData#castPoseTicks) — hands drawn together in front of the chest so casting always
     * reads as a deliberate hand-seal, not a silent proc. Shadow Clone opts into the mirrored
     * "cross seal" variant instead of the default.
     */
    private static void applyHandSealPose(PlayerModel playerModel, float weight, float phase,
                                          boolean crossSeal) {
        // Three beats: hands sweep up, clap into the seal, then settle. Before this the seal
        // was one fixed angle for the whole eight ticks, so the "snap" never snapped.
        float sign = crossSeal ? -1F : 1F;
        SEAL_RIGHT.rotate(playerModel.rightArm, phase, weight);
        SEAL_LEFT.rotate(playerModel.leftArm, phase, weight);
        playerModel.rightArm.yRot *= sign;
        playerModel.leftArm.yRot *= sign;
        SEAL_RIGHT_POS.position(playerModel.rightArm, phase, weight);
        SEAL_LEFT_POS.position(playerModel.leftArm, phase, weight);
        PoseBlender.addRotation(playerModel.body, weight, SEAL_LEAN.sampleX(phase), 0F, 0F);
    }

    private static final Curve SEAL_RIGHT = Curve.of(
            0.00f, -0.35f, -0.55f, 0f,
            0.30f, -1.45f, -0.15f, 0f,
            0.45f, -1.20f, -0.30f, 0f,
            1.00f, -1.10f, -0.32f, 0f);
    private static final Curve SEAL_LEFT = Curve.of(
            0.00f, -0.35f, 0.55f, 0f,
            0.30f, -1.45f, 0.15f, 0f,
            0.45f, -1.20f, 0.30f, 0f,
            1.00f, -1.10f, 0.32f, 0f);
    private static final Curve SEAL_RIGHT_POS = Curve.of(
            0.00f, -5.0f, 2.0f, 0.0f,
            0.30f, -4.0f, 1.5f, -1.5f,
            1.00f, -4.5f, 2.0f, -1.0f);
    private static final Curve SEAL_LEFT_POS = Curve.of(
            0.00f, 5.0f, 2.0f, 0.0f,
            0.30f, 4.0f, 1.5f, -1.5f,
            1.00f, 4.5f, 2.0f, -1.0f);
    /** A small recoil through the chest as the seal locks. */
    private static final Curve SEAL_LEAN = Curve.of(
            0.00f, 0.00f, 0f, 0f,
            0.28f, -0.10f, 0f, 0f,
            0.40f, 0.06f, 0f, 0f,
            1.00f, 0.00f, 0f, 0f);

    /**
     * Amaterasu: the hand comes up to shield the eye, then flicks out to mark the target.
     * The technique is aimed with a look, so the head leads and the arm follows it.
     */
    private static void applyAmaterasuPose(PlayerModel playerModel, float weight, float phase) {
        AMATERASU_ARM.rotate(playerModel.rightArm, phase, weight);
        AMATERASU_ARM_POS.position(playerModel.rightArm, phase, weight);
        PoseBlender.rotate(playerModel.leftArm, weight, -0.15F, 0.10F, 0.05F);
        PoseBlender.addRotation(playerModel.head, weight, AMATERASU_HEAD.sampleX(phase), 0F, 0F);
    }

    private static final Curve AMATERASU_ARM = Curve.of(
            0.00f, -0.30f, -0.20f, 0.00f,
            0.35f, -2.30f, -0.55f, 0.30f,
            0.55f, -1.60f, -0.08f, 0.00f,
            1.00f, -1.45f, -0.05f, 0.00f);
    private static final Curve AMATERASU_ARM_POS = Curve.of(
            0.00f, -5.0f, 2.0f, 0.0f,
            0.35f, -3.0f, 0.0f, -1.0f,
            1.00f, -4.0f, 2.0f, -2.5f);
    private static final Curve AMATERASU_HEAD = Curve.of(
            0.00f, 0.00f, 0f, 0f,
            0.35f, -0.18f, 0f, 0f,
            1.00f, 0.04f, 0f, 0f);

    /**
     * Forcing a gate: the body drops into a brace, both fists drive down, and the chest
     * throws back on the release. This is the roar, and it lasts about a second.
     */
    private static void applyGateRoarPose(PlayerModel playerModel, float weight, float phase) {
        GATE_ROAR_RIGHT.rotate(playerModel.rightArm, phase, weight);
        GATE_ROAR_LEFT.rotate(playerModel.leftArm, phase, weight);
        PoseBlender.addRotation(playerModel.body, weight, GATE_ROAR_BODY.sampleX(phase), 0F, 0F);
        PoseBlender.addRotation(playerModel.head, weight, GATE_ROAR_HEAD.sampleX(phase), 0F, 0F);
        PoseBlender.rotate(playerModel.rightLeg, weight, -0.25F, -0.18F, 0F);
        PoseBlender.rotate(playerModel.leftLeg, weight, -0.25F, 0.18F, 0F);
    }

    private static final Curve GATE_ROAR_RIGHT = Curve.of(
            0.00f, -0.40f, -0.30f, -0.20f,
            0.30f, -1.10f, -0.20f, -0.55f,
            0.50f, 0.55f, 0.05f, -0.95f,
            1.00f, 0.30f, 0.00f, -0.70f);
    private static final Curve GATE_ROAR_LEFT = Curve.of(
            0.00f, -0.40f, 0.30f, 0.20f,
            0.30f, -1.10f, 0.20f, 0.55f,
            0.50f, 0.55f, -0.05f, 0.95f,
            1.00f, 0.30f, 0.00f, 0.70f);
    private static final Curve GATE_ROAR_BODY = Curve.of(
            0.00f, 0.00f, 0f, 0f,
            0.30f, 0.22f, 0f, 0f,
            0.52f, -0.26f, 0f, 0f,
            1.00f, -0.10f, 0f, 0f);
    private static final Curve GATE_ROAR_HEAD = Curve.of(
            0.00f, 0.00f, 0f, 0f,
            0.30f, 0.20f, 0f, 0f,
            0.52f, -0.42f, 0f, 0f,
            1.00f, -0.18f, 0f, 0f);

    /**
     * Absorbing a drop: knees fold, the body sinks and one hand goes to the ground, then it
     * all unwinds. Length scales with the fall - see {@link #landingActive}.
     */
    private static void applyLandingPose(PlayerModel playerModel, float weight, float elapsed) {
        float phase = Mth.clamp(1f - elapsed / LANDING_TICKS_MAX, 0f, 1f);
        LANDING_ARM.rotate(playerModel.rightArm, phase, weight);
        PoseBlender.rotate(playerModel.leftArm, weight, -0.25F, 0.30F, 0.45F);
        PoseBlender.addRotation(playerModel.body, weight, LANDING_BODY.sampleX(phase), 0F, 0F);
        PoseBlender.addRotation(playerModel.rightLeg, weight, LANDING_LEG.sampleX(phase), -0.22F, 0F);
        PoseBlender.addRotation(playerModel.leftLeg, weight, LANDING_LEG.sampleX(phase), 0.22F, 0F);
        // Sink the whole figure rather than only bending it: a crouch that never lowers the
        // body reads as the player doing squats in mid-air.
        playerModel.body.y += 2.2F * weight * LANDING_BODY.sampleY(phase);
        playerModel.head.y += 2.2F * weight * LANDING_BODY.sampleY(phase);
    }

    private static final Curve LANDING_ARM = Curve.of(
            0.00f, -0.20f, -0.30f, -0.45f,
            1.00f, -1.15f, -0.55f, -0.30f);
    /** x = chest fold, y = how far the whole body sinks. */
    private static final Curve LANDING_BODY = Curve.of(
            0.00f, 0.00f, 0.00f, 0f,
            1.00f, 0.55f, 1.00f, 0f);
    private static final Curve LANDING_LEG = Curve.of(
            0.00f, 0.00f, 0f, 0f,
            1.00f, -0.85f, 0f, 0f);

    /**
     * Cupped-hands-to-mouth pose while charging Fireball — distinct from the generic
     * channeled-jutsu hand-seal pose, matching the anime's signature Fire Style tell.
     */
    private static void applyFireballPose(PlayerModel playerModel, float weight, float elapsed) {
        // Gather at the chest, then the hands rise to the mouth and stay there swelling.
        float entry = Mth.clamp(elapsed / 8f, 0f, 1f);
        float swell = Mth.sin(elapsed * 0.3f) * 0.06f;
        FIREBALL_RIGHT.rotate(playerModel.rightArm, entry, weight);
        FIREBALL_LEFT.rotate(playerModel.leftArm, entry, weight);
        FIREBALL_RIGHT_POS.position(playerModel.rightArm, entry, weight);
        FIREBALL_LEFT_POS.position(playerModel.leftArm, entry, weight);
        PoseBlender.addRotation(playerModel.head, weight, -0.1F + swell * 0.5f, 0F, 0F);
        PoseBlender.addRotation(playerModel.body, weight, -swell, 0F, 0F);
    }

    private static final Curve FIREBALL_RIGHT = Curve.of(
            0.00f, -0.45f, -0.65f, 0f,
            0.50f, -1.10f, -0.55f, 0f,
            1.00f, -2.10f, -0.35f, 0f);
    private static final Curve FIREBALL_LEFT = Curve.of(
            0.00f, -0.45f, 0.65f, 0f,
            0.50f, -1.10f, 0.55f, 0f,
            1.00f, -2.10f, 0.35f, 0f);
    private static final Curve FIREBALL_RIGHT_POS = Curve.of(
            0.00f, -5.0f, 2.0f, 0.0f,
            0.50f, -3.5f, 1.5f, 0.5f,
            1.00f, -3.0f, 0.5f, -2.0f);
    private static final Curve FIREBALL_LEFT_POS = Curve.of(
            0.00f, 5.0f, 2.0f, 0.0f,
            0.50f, 3.5f, 1.5f, 0.5f,
            1.00f, 3.0f, 0.5f, -2.0f);

    /**
     * Crackling hand thrust forward — Chidori's active window (isChidoriActive(), up to
     * 8x20 ticks) is much longer than the generic cast flash, so it gets its own distinct
     * stiff-arm pose instead of sharing the brief hand-seal snap.
     */
    private static void applyChidoriThrustPose(PlayerModel playerModel, float weight, float elapsed) {
        // Arm cocks low and back, then extends. Once out it never fully settles - the
        // lightning is fighting the hand, and the jitter is the only reason it reads as live.
        float entry = Mth.clamp(elapsed / 6f, 0f, 1f);
        float jitter = Mth.sin(elapsed * 2.7f) * 0.04f + Mth.sin(elapsed * 4.3f) * 0.02f;
        CHIDORI_ARM.rotate(playerModel.rightArm, entry, weight);
        CHIDORI_ARM_POS.position(playerModel.rightArm, entry, weight);
        PoseBlender.rotate(playerModel.leftArm, weight, -0.3F, 0.15F, 0F);
        PoseBlender.addRotation(playerModel.rightArm, weight, jitter, jitter * 0.6f, 0F);
        PoseBlender.addRotation(playerModel.body, weight, 0F, -0.15f * weight, 0F);
    }

    private static final Curve CHIDORI_ARM = Curve.of(
            0.00f, 0.35f, -0.45f, -0.30f,
            0.45f, 0.20f, -0.70f, -0.20f,
            1.00f, -1.55f, -0.10f, 0.00f);
    private static final Curve CHIDORI_ARM_POS = Curve.of(
            0.00f, -5.5f, 3.0f, 1.5f,
            0.45f, -6.0f, 3.5f, 2.0f,
            1.00f, -4.0f, 2.5f, -2.5f);

    /**
     * One flat palm thrust forward — Air Palm's signature Hyuga tell, asymmetric so it
     * reads distinctly from the two-handed generic hand-seal flash.
     */
    private static void applyAirPalmPose(PlayerModel playerModel, float weight, float phase) {
        // Palm withdraws to the ribs and then strikes. A Gentle Fist palm is a strike, and a
        // strike without a wind-up is a wave.
        AIR_PALM_ARM.rotate(playerModel.rightArm, phase, weight);
        AIR_PALM_ARM_POS.position(playerModel.rightArm, phase, weight);
        PoseBlender.rotate(playerModel.leftArm, weight, -0.2F, 0.1F, 0F);
        PoseBlender.addRotation(playerModel.body, weight, 0F, AIR_PALM_TWIST.sampleX(phase), 0F);
    }

    private static final Curve AIR_PALM_ARM = Curve.of(
            0.00f, -0.30f, -0.85f, 0.00f,
            0.30f, -0.55f, -1.15f, 0.10f,
            0.48f, -1.50f, -0.05f, 0.00f,
            1.00f, -1.35f, -0.10f, 0.00f);
    private static final Curve AIR_PALM_ARM_POS = Curve.of(
            0.00f, -5.0f, 2.5f, 1.0f,
            0.30f, -5.5f, 3.0f, 1.5f,
            0.48f, -4.0f, 2.0f, -2.5f,
            1.00f, -4.0f, 2.0f, -2.0f);
    private static final Curve AIR_PALM_TWIST = Curve.of(
            0.00f, 0.22f, 0f, 0f,
            0.48f, -0.20f, 0f, 0f,
            1.00f, -0.10f, 0f, 0f);

    /**
     * Both hands drawn back near the chest as if compressing/inhaling air before the
     * release — Great Breakthrough's wind-gathering tell.
     */
    private static void applyWindGatherPose(PlayerModel playerModel, float weight, float phase) {
        // Draw in, then blow out: the arms compress toward the chest and fling open.
        WIND_RIGHT.rotate(playerModel.rightArm, phase, weight);
        WIND_LEFT.rotate(playerModel.leftArm, phase, weight);
        PoseBlender.position(playerModel.rightArm, weight, -3.0F, 1.5F, 0.5F);
        PoseBlender.position(playerModel.leftArm, weight, 3.0F, 1.5F, 0.5F);
        PoseBlender.addRotation(playerModel.body, weight, WIND_BODY.sampleX(phase), 0F, 0F);
        PoseBlender.addRotation(playerModel.head, weight, WIND_BODY.sampleX(phase) * 0.5f, 0F, 0F);
    }

    private static final Curve WIND_RIGHT = Curve.of(
            0.00f, -0.50f, -0.30f, 0f,
            0.40f, -0.90f, -0.60f, 0f,
            0.60f, -1.25f, -1.05f, 0f,
            1.00f, -1.05f, -0.85f, 0f);
    private static final Curve WIND_LEFT = Curve.of(
            0.00f, -0.50f, 0.30f, 0f,
            0.40f, -0.90f, 0.60f, 0f,
            0.60f, -1.25f, 1.05f, 0f,
            1.00f, -1.05f, 0.85f, 0f);
    private static final Curve WIND_BODY = Curve.of(
            0.00f, 0.00f, 0f, 0f,
            0.40f, 0.18f, 0f, 0f,
            0.60f, -0.20f, 0f, 0f,
            1.00f, -0.06f, 0f, 0f);

    /**
     * Both arms thrust outward to the sides — mirrors Adamantine Chains' actual 4-direction
     * chakra-chain fan instead of the generic forward-facing hand-seal.
     */
    private static void applyChainExpulsionPose(PlayerModel playerModel, float weight, float phase) {
        // Arms clamp in across the chest, then tear outward as the chains leave the back.
        CHAIN_RIGHT.rotate(playerModel.rightArm, phase, weight);
        CHAIN_LEFT.rotate(playerModel.leftArm, phase, weight);
        PoseBlender.addRotation(playerModel.body, weight, CHAIN_BODY.sampleX(phase), 0F, 0F);
    }

    private static final Curve CHAIN_RIGHT = Curve.of(
            0.00f, -0.60f, -0.20f, 0.35f,
            0.32f, -0.75f, 0.35f, 0.60f,
            0.50f, 0.00f, -1.40f, 0.00f,
            1.00f, -0.05f, -1.25f, 0.00f);
    private static final Curve CHAIN_LEFT = Curve.of(
            0.00f, -0.60f, 0.20f, -0.35f,
            0.32f, -0.75f, -0.35f, -0.60f,
            0.50f, 0.00f, 1.40f, 0.00f,
            1.00f, -0.05f, 1.25f, 0.00f);
    private static final Curve CHAIN_BODY = Curve.of(
            0.00f, 0.00f, 0f, 0f,
            0.32f, 0.20f, 0f, 0f,
            0.50f, -0.18f, 0f, 0f,
            1.00f, 0.00f, 0f, 0f);

    /**
     * Wide, grounded power stance while Susanoo is active — sells "channeling a giant
     * spectral avatar" instead of the player silently idling. Only dispatched for stages
     * 1-3 (see sprintingAnim); stage 4 hides the player model entirely via RenderEvents.
     */
    private static void applySusanooPose(PlayerModel playerModel, float weight, float elapsed) {
        // Holding an avatar up is work. The strain oscillates on two frequencies so the loop
        // does not land on an obvious beat - one sine alone reads as a mechanical bob.
        float strain = Mth.sin(elapsed * 0.11f) * 0.05f + Mth.sin(elapsed * 0.29f) * 0.02f;
        PoseBlender.rotate(playerModel.body, weight, -0.1F, 0F, 0F);
        PoseBlender.rotate(playerModel.rightArm, weight, -0.3F, -0.5F, -0.15F);
        PoseBlender.rotate(playerModel.leftArm, weight, -0.3F, 0.5F, 0.15F);
        PoseBlender.rotate(playerModel.rightLeg, weight, -0.15F, -0.2F, 0F);
        PoseBlender.rotate(playerModel.leftLeg, weight, -0.15F, 0.2F, 0F);
        PoseBlender.addRotation(playerModel.rightArm, weight, strain, 0F, -strain * 0.5f);
        PoseBlender.addRotation(playerModel.leftArm, weight, strain, 0F, strain * 0.5f);
        PoseBlender.addRotation(playerModel.body, weight, strain * 0.5f, 0F, 0F);
    }

    /**
     * The Susanoo bringing its blade across.
     *
     * Three beats, and the middle one is short on purpose: the arm hauls back and up over the
     * shoulder, the strike crosses in about a fifth of the swing, and the rest is the body
     * recovering from having swung something the size of a building. A swing whose fastest
     * part is not much faster than its slowest reads as pushing, not cutting.
     *
     * The whole torso turns into it. An arm moving on its own is a wave; what makes a strike
     * land is the shoulders arriving first and the blade catching up.
     */
    private static void applySusanooSwingPose(PlayerModel playerModel, float weight, float phase) {
        SWING_RIGHT.rotate(playerModel.rightArm, phase, weight);
        SWING_LEFT.rotate(playerModel.leftArm, phase, weight);
        PoseBlender.addRotation(playerModel.body, weight, SWING_BODY.sampleX(phase),
                SWING_BODY.sampleY(phase), 0F);
        PoseBlender.addRotation(playerModel.head, weight, SWING_BODY.sampleX(phase) * 0.4f,
                SWING_BODY.sampleY(phase) * 0.6f, 0F);
        // Braced legs, staggered as the weight transfers across the strike.
        PoseBlender.addRotation(playerModel.rightLeg, weight, -0.25F * (1F - phase), -0.15F, 0F);
        PoseBlender.addRotation(playerModel.leftLeg, weight, 0.30F * phase, 0.15F, 0F);
    }

    private static final Curve SWING_RIGHT = Curve.of(
            0.00f, -0.30f, -0.20f, 0.00f,   // guard
            0.35f, -2.60f, -0.85f, -0.70f,  // hauled back over the shoulder
            0.55f, 0.95f, 0.25f, 0.55f,     // the cut, crossing fast
            1.00f, 0.35f, 0.10f, 0.30f);    // follow through, blade low
    private static final Curve SWING_LEFT = Curve.of(
            0.00f, -0.30f, 0.20f, 0.00f,
            0.35f, -1.20f, 0.70f, 0.45f,
            0.55f, -0.20f, -0.35f, -0.25f,
            1.00f, -0.15f, -0.20f, -0.15f);
    /** x = lean into the strike, y = how far the torso has turned through it. */
    private static final Curve SWING_BODY = Curve.of(
            0.00f, 0.00f, 0.00f, 0f,
            0.35f, -0.18f, -0.45f, 0f,
            0.55f, 0.30f, 0.50f, 0f,
            1.00f, 0.10f, 0.30f, 0f);

    /** Sage Mode standing still: slow, deep, deliberately calm breathing. */
    private static void applySageIdlePose(PlayerModel playerModel, float weight, float elapsed) {
        float breath = Mth.sin(elapsed * 0.075f) * 0.07f;
        PoseBlender.rotate(playerModel.rightArm, weight, -0.2F, -0.15F, 0.1F);
        PoseBlender.rotate(playerModel.leftArm, weight, -0.2F, 0.15F, -0.1F);
        PoseBlender.rotate(playerModel.body, weight, 0.05F, 0F, 0F);
        PoseBlender.addRotation(playerModel.rightArm, weight, breath, 0F, breath * 0.3f);
        PoseBlender.addRotation(playerModel.leftArm, weight, breath, 0F, -breath * 0.3f);
        PoseBlender.addRotation(playerModel.body, weight, -breath * 0.5f, 0F, 0F);
        PoseBlender.addRotation(playerModel.head, weight, breath * 0.4f, 0F, 0F);
    }

    /** The cloak prowls: hunched, arms loose and clawed, weight shifting side to side. */
    private static void applyKuramaCloakPose(PlayerModel playerModel, float weight, float elapsed) {
        float prowl = Mth.sin(elapsed * 0.16f);
        PoseBlender.rotate(playerModel.body, weight, 0.2F, 0F, 0F);
        PoseBlender.rotate(playerModel.rightArm, weight, 0.5F, 0F, -0.3F);
        PoseBlender.rotate(playerModel.leftArm, weight, 0.5F, 0F, 0.3F);
        PoseBlender.addRotation(playerModel.head, weight, -0.08F, 0F, 0F);
        PoseBlender.addRotation(playerModel.body, weight, 0F, prowl * 0.06f, 0F);
        PoseBlender.addRotation(playerModel.rightArm, weight, prowl * 0.09f, 0F, -prowl * 0.05f);
        PoseBlender.addRotation(playerModel.leftArm, weight, -prowl * 0.09f, 0F, prowl * 0.05f);
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
