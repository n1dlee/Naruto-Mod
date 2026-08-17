package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;

/**
 * The swing a manifested giant makes, applied to the whole body.
 *
 * The full forms hide the player and draw a Susanoo or a Kurama in their place, and the pose
 * work that animates a sword strike lives on the player's own model — which at that point is
 * not being drawn at all. So the sword swing existed as damage, as an arc of particles and as
 * a sound, and the thing holding the sword never moved. At Complete Body scale that is the
 * most conspicuous possible place for nothing to happen.
 *
 * The ported bodies are single meshes rather than rigged skeletons; there is no shoulder joint
 * to turn. What there is, is the whole giant, and a creature the size of a tower swinging a
 * sword longer than a house does in fact move its whole body to do it. So the swing is a turn
 * of the entire form: wind up away from the target, come through fast, and settle back.
 *
 * Driven off the same synced tick counter the damage uses, so what you see and what hits are
 * the same event rather than two things that happen to look alike.
 */
public final class GiantSwing {

    /** Wind-up ends, strike ends. The rest of the swing is recovery. */
    private static final float WINDUP_END = 0.25f;
    private static final float STRIKE_END = 0.55f;

    /** How far the body turns away before the blow, and how far through it carries. */
    private static final float WINDUP_YAW = -38f;
    private static final float FOLLOW_YAW = 58f;
    /** The lean: back on the wind-up, down through the strike. */
    private static final float WINDUP_PITCH = -12f;
    private static final float FOLLOW_PITCH = 26f;

    private GiantSwing() {
    }

    /**
     * Turns the body into whatever part of the swing it is currently in.
     *
     * @param ticksLeft the synced counter, which runs DOWN to zero
     * @param totalTicks how long a full swing lasts
     */
    public static void apply(PoseStack poseStack, int ticksLeft, int totalTicks, float partialTick) {
        if (ticksLeft <= 0 || totalTicks <= 0) {
            return;
        }
        // Interpolated so the strike is smooth at giant scale rather than stepping once per
        // tick - a body this large moves far enough in one tick for the difference to show.
        float remaining = Math.max(0f, ticksLeft - partialTick);
        float t = Mth.clamp(1f - remaining / totalTicks, 0f, 1f);

        float yaw;
        float pitch;
        if (t < WINDUP_END) {
            // Loading up. Eased so it starts slowly and gathers, which is what sells the
            // weight - a linear wind-up reads as a twitch.
            float progress = ease(t / WINDUP_END);
            yaw = WINDUP_YAW * progress;
            pitch = WINDUP_PITCH * progress;
        } else if (t < STRIKE_END) {
            // The blow itself: straight through, no easing. This is the fast part.
            float progress = (t - WINDUP_END) / (STRIKE_END - WINDUP_END);
            yaw = Mth.lerp(progress, WINDUP_YAW, FOLLOW_YAW);
            pitch = Mth.lerp(progress, WINDUP_PITCH, FOLLOW_PITCH);
        } else {
            // Settling back out of the follow-through.
            float progress = ease((t - STRIKE_END) / (1f - STRIKE_END));
            yaw = FOLLOW_YAW * (1f - progress);
            pitch = FOLLOW_PITCH * (1f - progress);
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
    }

    /** Smoothstep. Slow at both ends, quick through the middle. */
    private static float ease(float t) {
        float clamped = Mth.clamp(t, 0f, 1f);
        return clamped * clamped * (3f - 2f * clamped);
    }
}
