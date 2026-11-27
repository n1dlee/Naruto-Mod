package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class SharinganAbility extends Ability implements Ability.ToggleStartCheck {

    /** Exempt from the free-hands gate: this is an eye being opened, not a hand-cast technique. */
    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    /**
     * Billed every tick the toggle is on, so this is 12 chakra/second, not a one-off.
     * Cut 40% from the original 1.0 - an eye you cannot afford to leave open is an eye
     * you never actually use.
     */
    private static final float CHAKRA_COST = 0.6F;
    private static final int CHAKRA_COOLDOWN = 15;

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return -1;
    }

    @Override
    public boolean canStartToggle(Player player, INinjaData ninjaData) {
        return validateSharinganAccess(player, ninjaData) && validateChakra(player, ninjaData);
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!validateSharinganAccess(player, ninjaData) || !validateChakra(player, ninjaData)) {
            return false;
        }
        ninjaData.useChakra(toggleCost(ninjaData), CHAKRA_COOLDOWN);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Movement and damage modifiers follow the live toggle in NinjaData.
    }

    @Override
    public net.minecraft.sounds.SoundEvent castingSound() {
        return NarutoSounds.SHARINGAN_ACTIVATE.get();
    }

    /**
     * Gated on owning the eye rather than on the clan, so a transplanted Sharingan works
     * for a non-Uchiha (Kakashi's case) without a second code path.
     */
    private boolean validateSharinganAccess(Player player, INinjaData ninjaData) {
        if (!ninjaData.hasSharinganEye()) {
            player.displayClientMessage(Component.translatable("jutsu.fail.uchiha",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getSharinganLevel() < 1) {
            player.displayClientMessage(Component.translatable("jutsu.fail.rank.genin",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }

    /**
     * A foreign eye is far more expensive to actively drive — the host has no Uchiha body
     * to run it efficiently. This is on TOP of the passive per-second drain the transplant
     * already charges in NinjaData.
     */
    private static float toggleCost(INinjaData ninjaData) {
        return ninjaData.isTransplantedSharingan() ? CHAKRA_COST * 3f : CHAKRA_COST;
    }

    private boolean validateChakra(Player player, INinjaData ninjaData) {
        if (ninjaData.getChakra() < toggleCost(ninjaData)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }
}
