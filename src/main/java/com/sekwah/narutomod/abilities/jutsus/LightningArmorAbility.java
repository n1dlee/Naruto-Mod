package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.LightningCoat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

public class LightningArmorAbility extends Ability implements Ability.ToggleStartCheck {

    private static final float CHAKRA_COST = 6.0f;

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 211;
    }

    @Override
    public String element() {
        return "lightning";
    }

    @Override
    public int elementLevelRequired() {
        return 10;
    }

    @Override
    public float elementXpReward() {
        return 4f;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.BEACON_POWER_SELECT;
    }

    @Override
    public boolean canStartToggle(Player player, INinjaData ninjaData) {
        if (!LightningCoat.compatible(ninjaData)) {
            player.displayClientMessage(Component.translatable("jutsu.lightning_armor.incompatible"), true);
            return false;
        }
        return validateChakra(player, ninjaData);
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!LightningCoat.active(player, ninjaData) || !validateChakra(player, ninjaData)) {
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 5);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Protection and lift are applied when their gameplay events occur.
    }

    private boolean validateChakra(Player player, INinjaData ninjaData) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }
}
