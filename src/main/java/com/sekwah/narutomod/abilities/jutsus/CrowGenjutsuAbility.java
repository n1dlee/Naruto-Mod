package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.EyeTargeting;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

/**
 * Crow Clone Genjutsu — Itachi's flock (combo 1133).
 * A murder of crows bursts out of the caster and scatters everyone nearby: blindness
 * and nausea in a radius, no damage. The crowd-control half of Itachi's kit, where
 * Tsukuyomi is the single-target execution.
 */
public class CrowGenjutsuAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 55f;
    private static final double RADIUS = 8.0;
    private static final int EFFECT_TICKS = 6 * 20;
    private static final DustParticleOptions CROW_BLACK =
            new DustParticleOptions(new Vector3f(0.08F, 0.08F, 0.12F), 1.4F);

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** Same as the Sharingan genjutsu it is built on. */
    @Override
    public int castPoseTicks() {
        return 14;
    }

    @Override
    public long defaultCombo() {
        return 1133;
    }

    @Override
    public String requiredEye() {
        return "sharingan_ms";
    }

    @Override
    public String requiredEyeForm() {
        return "itachi";
    }

    @Override
    public int getCooldown() {
        return 25 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.PARROT_IMITATE_VEX;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
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
        for (LivingEntity target : EyeTargeting.livingAround(player, RADIUS)) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, EFFECT_TICKS, 0, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, EFFECT_TICKS, 0, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_TICKS, 0, false, true));
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            // The flock scattering outward — a widening spiral of black feathers
            NarutoParticles.spawnSpiral(serverLevel, player.position().add(0, 1.0, 0),
                    RADIUS * 0.5, 0.12, 40, CROW_BLACK);
            NarutoParticles.spawnRing(serverLevel, player.position().add(0, 1.4, 0),
                    RADIUS * 0.75, 36, CROW_BLACK);
            NarutoParticles.spawnBurst(serverLevel, player.position().add(0, 1.2, 0),
                    30, 1.2, NarutoParticles.GENJUTSU_RED);
        }
    }
}
