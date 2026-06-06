package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Kurama Cloak — Jinchuriki transformation (combo 13231).
 * Only Uzumaki clan + Jonin+ rank. Cost: 150 chakra. Duration: 20 seconds.
 * Drains 5 chakra/tick while active. Grants Strength III, Speed II, Resistance I.
 * Melee hits create a mini AoE explosion (2 damage, 2 block radius).
 * Visual: orange-red aura particles, red vignette overlay.
 *
 * Activation and effects are handled in NinjaData.updateKuramaCloak().
 */
public class KuramaCloakAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 200f;
    private static final int DURATION = 20 * 20; // 20 seconds

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 13231;
    }

    @Override
    public int getCooldown() {
        return 60 * 20; // 60 seconds
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        // Toggle off if already active
        if (ninjaData.isKuramaCloakActive()) {
            ninjaData.setKuramaCloakActive(false);
            player.displayClientMessage(Component.literal("Kurama Cloak deactivated!")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        // Uzumaki only
        if (!"uzumaki".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.uzumaki",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }

        // Jonin+ (rank 3+)
        if (ninjaData.getNinjaRank() < 3) {
            player.displayClientMessage(Component.translatable("jutsu.fail.rank.jonin",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
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
        ninjaData.setKuramaCloakActive(true);
        ninjaData.setKuramaCloakTicks(DURATION);
        player.displayClientMessage(
                Component.literal("Kurama Cloak activated! Duration: 20s")
                        .withStyle(ChatFormatting.GOLD), true);
    }
}
