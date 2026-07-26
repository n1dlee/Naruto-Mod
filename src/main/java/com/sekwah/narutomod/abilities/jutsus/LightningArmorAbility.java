package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Lightning Release Armour (Raiton no Yoroi) — combo 211, TOGGLE.
 *
 * The Fourth Raikage's technique: lightning chakra wrapped around the body, boosting the
 * user's reflexes to the point that nothing on the battlefield keeps up. This is the
 * fastest movement in the mod on purpose — it feeds the biggest single contribution into
 * NinjaData.updateNinjaSpeed's uncapped attribute modifier, well past Chakra Dash or the
 * Eight Gates. The price is a heavy, continuous chakra burn.
 *
 * Gated on lightning nature rather than a clan, since the Raikage line is Kumo's, not a
 * bloodline — any lightning user can learn it.
 */
public class LightningArmorAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck {

    private static final float CHAKRA_COST = 6.0f;

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 211;
    }

    @Override
    public String element() {
        return "lightning";
    }

    @Override
    public int elementLevelRequired() {
        return 10;
    }

    @Override
    public float elementXpReward() {
        return 4f;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.BEACON_POWER_SELECT;
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
        // Reflexes, not just speed: the armour also blunts hits and steadies the landing.
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 1, false, false));
        player.resetFallDistance();
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        if (player.tickCount % 2 == 0) {
            player.level().addParticle(NarutoParticles.CHIDORI_CYAN,
                    player.getX() + (player.getRandom().nextDouble() - 0.5) * 0.9,
                    player.getY() + player.getRandom().nextDouble() * player.getBbHeight(),
                    player.getZ() + (player.getRandom().nextDouble() - 0.5) * 0.9,
                    0.0D, 0.02D, 0.0D);
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
