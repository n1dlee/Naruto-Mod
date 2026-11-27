package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

public class ByakuganAbility extends Ability implements Ability.ToggleStartCheck {

    /** Exempt from the free-hands gate: this is an eye being opened, not a hand-cast technique. */
    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    private static final float CHAKRA_COST = 0.5F;
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
        return validateByakuganAccess(player, ninjaData) && validateChakra(player, ninjaData);
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!validateByakuganAccess(player, ninjaData) || !validateChakra(player, ninjaData)) {
            return false;
        }
        ninjaData.useChakra(chakraCost(ninjaData), CHAKRA_COOLDOWN);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // The local HUD owns vision markers. Combat modifiers follow the active eye.
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.AMETHYST_CLUSTER_BREAK;
    }

    private boolean validateByakuganAccess(Player player, INinjaData ninjaData) {
        if (!"hyuga".equals(ninjaData.getClanId()) && ninjaData.getByakuganLevel() <= 0) {
            player.displayClientMessage(Component.translatable("jutsu.fail.hyuga",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }

    private boolean validateChakra(Player player, INinjaData ninjaData) {
        if (ninjaData.getChakra() < chakraCost(ninjaData)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }

    private static float chakraCost(INinjaData data) {
        return "hyuga".equals(data.getClanId()) ? CHAKRA_COST : CHAKRA_COST * 2;
    }
}
