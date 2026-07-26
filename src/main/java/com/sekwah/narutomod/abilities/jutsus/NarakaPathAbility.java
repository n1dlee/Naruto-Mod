package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.EyeTargeting;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Naraka Path — the King of Hell restores the broken (combo 1233).
 * Canon's Naraka Path drags a body back from ruin; here it heals the user outright and
 * puts regeneration on every ally standing with them. No resurrection — restoration.
 */
public class NarakaPathAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 95f;
    private static final double ALLY_RADIUS = 8.0;
    private static final float SELF_HEAL = 12f;
    private static final int REGEN_TICKS = 10 * 20;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 1233;
    }

    @Override
    public String requiredEye() {
        return "rinnegan_path:naraka";
    }

    @Override
    public int getCooldown() {
        return 90 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.BEACON_POWER_SELECT;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 60);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        player.heal(SELF_HEAL * ninjaData.getRankDamageMultiplier());
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_TICKS, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, REGEN_TICKS, 1, false, true));

        // Fellow players standing close enough are mended too — mobs are not.
        for (LivingEntity nearby : EyeTargeting.livingAround(player, ALLY_RADIUS)) {
            if (nearby instanceof Player) {
                nearby.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_TICKS, 0, false, true));
            }
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnSpiral(serverLevel, player.position(), 1.0, 0.12, 40,
                    NarutoParticles.SAGE_GOLD);
            NarutoParticles.spawnRing(serverLevel, player.position().add(0, 0.2, 0), ALLY_RADIUS, 40,
                    NarutoParticles.SAGE_GOLD);
        }
    }
}
