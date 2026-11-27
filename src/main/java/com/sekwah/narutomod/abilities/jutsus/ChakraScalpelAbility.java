package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.ScalpelEvents;
import com.sekwah.narutomod.util.ScalpelEdge;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class ChakraScalpelAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck, Ability.HandleEnded {
    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 133;
    }

    @Override
    public boolean canStartToggle(Player player, INinjaData ninjaData) {
        if (!"haruno".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.haruno",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (!ScalpelEvents.freeHand(player)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.scalpel_hand").withStyle(ChatFormatting.RED), true);
            return false;
        }
        return ninjaData.getChakra() >= ScalpelEdge.UPKEEP;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int age) {
        if (!"haruno".equals(ninjaData.getClanId()) || !ScalpelEvents.freeHand(player) || ninjaData.getChakra() < ScalpelEdge.UPKEEP) {
            return false;
        }
        ninjaData.useChakra(ScalpelEdge.UPKEEP, 10);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int age) {
        ScalpelEvents.maintain(player);
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int age) {
        ScalpelEvents.end(player);
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }
}
