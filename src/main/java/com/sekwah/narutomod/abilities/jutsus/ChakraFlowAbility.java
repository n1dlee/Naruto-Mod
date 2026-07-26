package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

/**
 * Chakra Flow (Chakura Nagashi) — combo 123, TOGGLE.
 *
 * Canon: a ninja channels chakra straight into whatever they are holding, making the
 * weapon cut far beyond what its material should allow. Here that means any weapon you
 * hold hits harder while this is up — a plain iron kunai starts biting like diamond.
 *
 * The damage bonus is applied centrally in PlayerEvents.applyChakraFlowHit, which also
 * decides what counts as a weapon: building blocks, bows, arrows and potions are
 * deliberately excluded, so this sharpens your blade rather than buffing everything.
 */
public class ChakraFlowAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck {

    /** Drain per tick while held — cheap enough to keep up through a fight. */
    private static final float CHAKRA_COST = 0.6f;

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 123;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    public boolean canStartToggle(Player player, INinjaData ninjaData) {
        return validateChakra(player, ninjaData);
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!validateChakra(player, ninjaData)) {
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 5);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // The bonus itself lives in PlayerEvents so it applies to every melee source.
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        // Chakra running along the blade in the held hand
        if (player.getMainHandItem().isEmpty() || player.tickCount % 4 != 0) {
            return;
        }
        double angle = Math.toRadians(player.getYRot() + 45);
        player.level().addParticle(NarutoParticles.CHIDORI_CYAN,
                player.getX() - Math.sin(angle) * 0.55,
                player.getY() + player.getBbHeight() * 0.75,
                player.getZ() + Math.cos(angle) * 0.55,
                0.0D, 0.01D, 0.0D);
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
