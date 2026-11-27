package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.SpiritProjectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class ByakuganScoutAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck {
    private static final float COST = 2.6f;

    @Override
    public boolean requiresFreeHands() { return false; }

    @Override
    public ActivationType activationType() { return ActivationType.TOGGLE; }

    @Override
    public long defaultCombo() { return 1112; }

    @Override
    public String requiredEye() { return "byakugan"; }

    @Override
    public SoundEvent castingSound() { return null; }

    @Override
    public boolean canStartToggle(Player player, INinjaData data) {
        if (data.getByakuganLevel() < 2) {
            player.displayClientMessage(Component.translatable("jutsu.spirit.eye").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (!SpiritProjectionEvents.canStart(player) || data.isWallWalkAttached() || data.getCurrentlyChanneledAbility() != null) {
            player.displayClientMessage(Component.translatable("jutsu.spirit.stance").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (data.getChakra() >= COST) {
            return true;
        }
        player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
        return false;
    }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (data.getByakuganLevel() < 2 || data.getChakra() < COST) {
            return false;
        }
        data.useChakra(COST, 8);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
    }

    @Override
    public void performToggleClient(Player player, INinjaData data) {
    }
}
