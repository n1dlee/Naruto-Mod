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
 * Preta Path — chakra absorption (combo 1232, TOGGLE).
 * While active the user drinks incoming ninjutsu instead of taking it: the damage is
 * blunted and the chakra behind it is added to their own reserve. Applied centrally in
 * PlayerEvents, which reads this toggle off the ability set the way Chakra Scalpel does.
 * Nearly free to hold — the cost is that it does nothing against plain steel.
 */
public class PretaPathAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck {

    private static final float CHAKRA_COST = 0.4f;
    private static final DustParticleOptions PRETA_VIOLET =
            new DustParticleOptions(new Vector3f(0.62F, 0.55F, 0.82F), 1.0F);

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 1232;
    }

    @Override
    public String requiredEye() {
        return "rinnegan_path:preta";
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.AMETHYST_BLOCK_RESONATE;
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
        // The absorption itself happens in PlayerEvents.applyPretaAbsorption.
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        if (player.tickCount % 5 == 0) {
            double angle = (player.tickCount % 40) / 40.0 * Math.PI * 2;
            player.level().addParticle(PRETA_VIOLET,
                    player.getX() + Math.cos(angle) * 0.8,
                    player.getY() + player.getBbHeight() * 0.5,
                    player.getZ() + Math.sin(angle) * 0.8,
                    0.0D, 0.01D, 0.0D);
        }
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
