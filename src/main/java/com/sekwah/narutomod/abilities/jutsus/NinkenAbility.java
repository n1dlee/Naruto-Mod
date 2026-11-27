package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.NinkenEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class NinkenAbility extends Ability implements Ability.Cooldown {
    private static final float CHAKRA_COST = 40f;

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
        return 323;
    }

    @Override
    public int getCooldown() {
        return 60 * 20;
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
        if (NinkenEvents.hasPartner(player)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.ninken.duplicate"), true);
            return false;
        }
        if (NinkenEvents.placement(player) == null) {
            player.displayClientMessage(Component.translatable("jutsu.fail.ninken.space"), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int age) {
        var feet = NinkenEvents.placement(player);
        if (feet != null) {
            NinkenEvents.start(player, feet, ninjaData.getRankDamageMultiplier());
        }
    }
}
