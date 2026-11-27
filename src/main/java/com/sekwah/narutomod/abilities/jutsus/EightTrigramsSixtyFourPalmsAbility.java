package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.TrigramPalmsEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class EightTrigramsSixtyFourPalmsAbility extends Ability implements Ability.Cooldown {
    private static final float CHAKRA_COST = 45f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 112;
    }

    @Override
    public int getCooldown() {
        return 20 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!"hyuga".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.hyuga",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (!ninjaData.isByakuganActive()) {
            player.displayClientMessage(Component.translatable("jutsu.fail.trigram_byakugan").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (TrigramPalmsEvents.select(player) == null) {
            player.displayClientMessage(Component.translatable("jutsu.fail.trigram_target").withStyle(ChatFormatting.GRAY), true);
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
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        var target = TrigramPalmsEvents.select(player);
        if (target != null) {
            TrigramPalmsEvents.start(player, target, ninjaData.getRankDamageMultiplier());
        }
    }
}
