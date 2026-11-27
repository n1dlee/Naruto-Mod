package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.ShinranshinEvents;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class MindDisturbanceAbility extends Ability implements Ability.Cooldown {
    private static final float CHAKRA_COST = 55;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int castPoseTicks() {
        return 12;
    }

    @Override
    public long defaultCombo() {
        return 322;
    }

    @Override
    public int getCooldown() {
        return 25 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return NarutoSounds.SHINRANSHIN_FOCUS.get();
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!"yamanaka".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.yamanaka",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ShinranshinEvents.select(player) == null) {
            player.displayClientMessage(Component.translatable("jutsu.fail.shinranshin.target"), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 40);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        var target = ShinranshinEvents.select(player);
        if (target != null) {
            ShinranshinEvents.start(player, target);
        }
    }
}
