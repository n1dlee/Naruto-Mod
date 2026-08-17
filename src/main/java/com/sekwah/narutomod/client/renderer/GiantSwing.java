package com.sekwah.narutomod.client.renderer;

import net.minecraft.util.Mth;

/**
 * How far through its swing a manifested giant is.
 *
 * The full forms hide the player and draw a Susanoo or a Kurama in their place, and the pose
 * work that animates a strike lives on the player's own model - which at that point is not
 * being drawn at all. So the swing existed as damage, as an arc of particles and as a sound,
 * with the thing holding the sword perfectly still.
 *
 * The first attempt at fixing that turned the whole body, on the assumption that the ported
 * bodies were single meshes with no joint to drive. They are not. The converted Susanoo rig
 * carries its sword as a child of the right arm, exactly as the 1.12.2 model did, and the fox
 * model has all nine tails as separate parts. Rotating an entire twenty-block figure to swing
 * looks like it is being shoved rather than striking.
 *
 * So what is shared here is only the clock. Each form animates the limb that actually does the
 * work - the arm for a sword, the tails for a fox - off the same synced counter the damage
 * uses, so what you see and what hits are one event rather than two that resemble each other.
 */
public final class GiantSwing {

    private GiantSwing() {
    }

    /**
     * Progress through the swing: 0 on the first frame, 1 on the last, negative when no swing
     * is running.
     *
     * Interpolated across the partial tick rather than stepping once per tick, because a limb
     * this large covers a visible distance between two server ticks.
     *
     * @param ticksLeft the synced counter, which runs DOWN to zero
     * @param totalTicks how long a full swing lasts
     */
    public static float progress(int ticksLeft, int totalTicks, float partialTick) {
        if (ticksLeft <= 0 || totalTicks <= 0) {
            return -1f;
        }
        float remaining = Math.max(0f, ticksLeft - partialTick);
        return Mth.clamp(1f - remaining / totalTicks, 0f, 1f);
    }

    /** The swing counter off a player's capability, or zero if they have none. */
    public static int ticksLeft(net.minecraft.world.entity.player.Player player) {
        return player.getCapability(com.sekwah.narutomod.capabilities.NinjaCapabilityHandler.NINJA_DATA)
                .map(com.sekwah.narutomod.capabilities.INinjaData::getSusanooSwingTicks)
                .orElse(0);
    }

    /** Convenience for the renderers: the current swing progress for this player. */
    public static float progressFor(net.minecraft.world.entity.player.Player player, float partialTick) {
        return progress(ticksLeft(player),
                com.sekwah.narutomod.capabilities.NinjaData.SUSANOO_SWING_TICKS, partialTick);
    }
}
