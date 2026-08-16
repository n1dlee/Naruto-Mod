package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

/**
 * Rasengan — held in hand (combo 212, TOGGLE), like Naruto actually uses it: formed, carried,
 * resized on the fly, and rammed into a target or a wall rather than thrown. Toggle on to form
 * it, scroll the mouse wheel to grow/shrink it (20-60, same scale the old charge used), melee
 * an entity to slam it into them (see PlayerEvents.applyRasenganMeleeHit), or punch a block to
 * blast a small crater (see PlayerEvents.onLeftClickBlock). Either action consumes it.
 */
public class RasenganJutsuAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck, Ability.HandleEnded {

    private static final float CHAKRA_PER_TICK = 1.5f;
    private static final float ACTIVATE_COST = 15f;

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 212;
    }

    /**
     * Rasengan is Wind Nature - it is the technique the Rasenshuriken is built out of, and
     * Naruto's whole wind affinity runs through it. The level requirement is deliberately
     * zero: he learned it long before he had any nature training at all, so it belongs to the
     * element for scaling purposes without being gated behind it.
     */
    @Override
    public String element() {
        return "wind";
    }

    @Override
    public int elementLevelRequired() {
        return 0;
    }

    @Override
    public float elementXpReward() {
        return 8f;
    }

    @Override
    public boolean canStartToggle(Player player, INinjaData ninjaData) {
        return validateChakra(player, ninjaData, ACTIVATE_COST);
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        // Refusing here is how a toggle ends itself: the tick loop drops the ability the
        // moment handleCost says no, which then runs handleAbilityEnded below.
        if (ninjaData.isRasenganConsumed()) {
            return false;
        }
        return validateChakra(player, ninjaData, CHAKRA_PER_TICK);
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!ninjaData.isRasenganHeld()) {
            ninjaData.useChakra(ACTIVATE_COST, 10);
            ninjaData.setRasenganHeld(true);
            player.displayClientMessage(
                    Component.literal("Rasengan formed!").withStyle(ChatFormatting.AQUA), true);
        } else {
            ninjaData.useChakra(CHAKRA_PER_TICK, 5);
        }
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        ninjaData.setRasenganHeld(false);
        // Cleared only here, so the refusal above survives exactly one tick - long enough to
        // drop the toggle, not long enough to stop the next deliberate cast.
        ninjaData.setRasenganConsumed(false);
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.END_PORTAL_FRAME_FILL;
    }

    /**
     * The sphere itself is drawn by JutsuVfxHandler, not here.
     *
     * This used to orbit two particles and an eight-point ring around the PLAYER's centre at
     * eye height, which put a flat halo through the chest rather than a ball in the hand - and
     * it only ran for the local player, so nobody ever saw anyone else's Rasengan. The
     * replacement is a real spinning point cloud positioned at the hand, drawn for every
     * player in view from one client-side place.
     */
    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
    }

    private boolean validateChakra(Player player, INinjaData ninjaData, float cost) {
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }
}
