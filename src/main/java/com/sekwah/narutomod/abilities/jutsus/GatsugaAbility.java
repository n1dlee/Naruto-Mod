package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.GatsugaEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class GatsugaAbility extends Ability implements Ability.Cooldown {
    private static final float CHAKRA_COST = 35f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 223;
    }

    @Override
    public int getCooldown() {
        return 12 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int age) {
        if (!"inuzuka".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.inuzuka",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (GatsugaEvents.readyPartner(player) == null) {
            player.displayClientMessage(Component.translatable("jutsu.fail.gatsuga.partner"), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 20);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int age) {
        var dog = GatsugaEvents.readyPartner(player);
        if (dog != null) {
            GatsugaEvents.start(player, dog, ninjaData.getRankDamageMultiplier());
        }
    }
}
