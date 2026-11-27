package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.GreatFireballEntity;
import com.sekwah.narutomod.util.FireballShape;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/** Paid charge determines the Gōkakyū projectile's size and damage. */
public class FireballJutsuAbility extends Ability implements Ability.Channeled, Ability.Cooldown {

    private static final float BASE_CHAKRA = 30f;
    private static final float CHAKRA_PER_TICK = 1.5f;

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public long defaultCombo() {
        return 121;
    }

    // Allow instant fire — tap is valid
    @Override
    public boolean canActivateBelowMinCharge() {
        return true;
    }

    // Show "charging" message, not "channeling"
    @Override
    public boolean useChargedMessages() {
        return true;
    }

    @Override
    public int getCooldown() {
        return 2 * 20; // short enough to stay the workhorse fire technique, long enough to cost something
    }
    @Override
    public String element() {
        return "fire";
    }

    @Override
    public int elementLevelRequired() {
        return 1;
    }

    @Override
    public float elementXpReward() {
        return 20f;
    }


    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        /*
         * chargeAmount <= 0, not == 0. The tap-to-cast path passes -1, which fell through to
         * the per-tick branch below and charged a tapped fireball 1.5 chakra instead of 30 -
         * so the cheapest way to use this technique was to never charge it at all.
         */
        if (chargeAmount <= 0) {
            // Initial activation — pay base cost
            if (ninjaData.getChakra() < BASE_CHAKRA) {
                player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                        Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
                return false;
            }
            ninjaData.useChakra(BASE_CHAKRA, 30);
        } else {
            // Drain per tick while charging
            if (ninjaData.getChakra() < CHAKRA_PER_TICK) {
                return false; // stop charging, fire immediately
            }
            ninjaData.useChakra(CHAKRA_PER_TICK, 5);
        }
        return true;
    }

    /**
     * While charging: spiral of FLAME + LAVA particles around the player's mouth area.
     */
    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        if (ticksChanneled % 3 == 0 && player.level() instanceof ServerLevel level) {
            var mouth = player.getEyePosition().add(0, -0.16, 0).add(player.getLookAngle().scale(0.38));
            float spread = 0.055f + FireballShape.charge(ticksChanneled) / 1000.0f;
            level.sendParticles(ParticleTypes.SMALL_FLAME, mouth.x, mouth.y, mouth.z, 3, spread, spread, spread, 0.015);
        }
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        float clan = "uchiha".equals(ninjaData.getClanId()) ? 1.25f : 1.0f;
        GreatFireballEntity.launch(player, player.getLookAngle(), ticksActive, clan * ninjaData.getRankDamageMultiplier());
    }
}
