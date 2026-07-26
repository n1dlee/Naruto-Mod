package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

/**
 * Kurama Cloak — Jinchuriki transformation (combo 13231).
 * Only Uzumaki clan + Jonin+ rank. Cost: 40 Kurama bond (Kurama's own chakra, NOT the
 * player's chakra pool — see NinjaData.kuramaBond). No fixed duration — lasts as long as
 * the bond holds out (~5 minutes at base tier from a full 1,000,000-point pool; see
 * NinjaData.updateKuramaCloak() for the drain math), and also tops up the player's own
 * chakra while active. Grants Strength III, Resistance I, and a big uncapped speed bonus.
 * Melee hits create a mini AoE explosion (2 damage, 2 block radius).
 * Visual: orange-red aura particles, red vignette overlay.
 *
 * Activation and effects are handled in NinjaData.updateKuramaCloak().
 */
public class KuramaCloakAbility extends Ability implements Ability.Cooldown {

    private static final float BOND_COST = 40f;

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

        if (ninjaData.getKuramaBond() < BOND_COST) {
            player.displayClientMessage(Component.literal("Not enough of Kurama's chakra!")
                    .withStyle(ChatFormatting.YELLOW), true);
            return false;
        }

        ninjaData.useKuramaBond(BOND_COST);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        ninjaData.setKuramaCloakActive(true);
        player.displayClientMessage(
                Component.literal("Kurama Cloak activated!")
                        .withStyle(ChatFormatting.GOLD), true);
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.RESPAWN_ANCHOR_CHARGE;
    }
}
