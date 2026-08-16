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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Kotoamatsukami — Shisui's signature Mangekyo genjutsu (combo 1211).
 * The victim is turned without ever knowing it: a hostile mob switches sides and fights
 * for the caster; a player is left completely neutralised instead. Canon's strongest
 * genjutsu, and priced like it — enormous cost and a multi-minute cooldown.
 */
public class KotoamatsukamiAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 150f;
    private static final double RANGE = 16.0;
    private static final int CONTROL_TICKS = 30 * 20;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** The slowest genjutsu in the mod, and it should look it. */
    @Override
    public int castPoseTicks() {
        return 24;
    }

    @Override
    public long defaultCombo() {
        return 1211;
    }

    @Override
    public String requiredEye() {
        return "sharingan_ms";
    }

    @Override
    public String requiredEyeForm() {
        return "shisui";
    }

    @Override
    public int getCooldown() {
        return 300 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.ENCHANTMENT_TABLE_USE;
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
        LivingEntity target = EyeTargeting.raycastLiving(player, RANGE);
        if (target == null) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notarget",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (target instanceof Mob mob) {
            // Turned without knowing it — the mob drops its grudge and hunts the caster's enemies
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
            for (LivingEntity nearby : EyeTargeting.livingAround(player, 16.0)) {
                if (nearby != mob && nearby instanceof Mob && nearby.isAlive()) {
                    mob.setTarget(nearby);
                    break;
                }
            }
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, CONTROL_TICKS, 0, false, true));
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, CONTROL_TICKS, 0, false, true));
            player.displayClientMessage(Component.translatable("jutsu.kotoamatsukami.turned",
                    target.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        } else {
            // Players can't be puppeteered — they are simply switched off for the duration
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, CONTROL_TICKS, 0, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, CONTROL_TICKS, 3, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CONTROL_TICKS, 3, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, CONTROL_TICKS, 3, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, CONTROL_TICKS, 0, false, false));
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 eye = target.getEyePosition();
            NarutoParticles.spawnRing(serverLevel, eye, 0.9, 32, NarutoParticles.GENJUTSU_RED);
            NarutoParticles.spawnSpiral(serverLevel, eye.subtract(0, 1.0, 0), 0.7, 0.1, 30,
                    NarutoParticles.SHARINGAN_RED);
        }
    }
}
