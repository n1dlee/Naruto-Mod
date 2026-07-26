package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

/**
 * Kurama Chakra Mode — Naruto's "just a glow" partnership form (combo 13232, TOGGLE).
 * No shell, no avatar — the player's own body glows gold, granting massive speed.
 * Mutually exclusive with the tailed Kurama Cloak (activating either deactivates the other).
 * Requires Uzumaki clan + Jonin+ rank. Drains kuramaBond, NOT the player's own chakra.
 * Effects and drain are handled in NinjaData.updateKCM().
 */
public class KuramaChakraModeAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck, Ability.HandleEnded {

    private static final float BOND_COST = 10f;

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 13232;
    }

    @Override
    public boolean canStartToggle(Player player, INinjaData ninjaData) {
        return validateAccess(player, ninjaData) && validateBond(player, ninjaData);
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!validateAccess(player, ninjaData) || !validateBond(player, ninjaData)) {
            return false;
        }
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!ninjaData.isKcmActive()) {
            ninjaData.useKuramaBond(BOND_COST);
            ninjaData.setKcmActive(true);
            player.displayClientMessage(
                    Component.literal("Kurama Chakra Mode!").withStyle(ChatFormatting.GOLD), true);
        }
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        ninjaData.setKcmActive(false);
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.BEACON_POWER_SELECT;
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        if (player.tickCount % 4 == 0) {
            player.level().addParticle(
                    new DustParticleOptions(new Vector3f(1.0F, 0.85F, 0.3F), 1.0F),
                    player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(),
                    0.0D, 0.0D, 0.0D);
        }
    }

    private boolean validateAccess(Player player, INinjaData ninjaData) {
        if (!"uzumaki".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.uzumaki",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getNinjaRank() < 3) {
            player.displayClientMessage(Component.translatable("jutsu.fail.rank.jonin",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }

    private boolean validateBond(Player player, INinjaData ninjaData) {
        if (ninjaData.getKuramaBond() < BOND_COST) {
            player.displayClientMessage(Component.literal("Not enough of Kurama's chakra!")
                    .withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        return true;
    }
}
