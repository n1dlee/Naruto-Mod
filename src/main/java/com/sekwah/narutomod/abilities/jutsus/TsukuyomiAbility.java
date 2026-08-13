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
import net.minecraft.world.phys.Vec3;

/**
 * Tsukuyomi — Itachi's signature Mangekyo genjutsu (combo 1131).
 * Traps a single target in an illusion that lasts an instant outside but days within:
 * heavy blindness, weakness and slowness plus direct spiritual damage that ignores armour.
 * Only an Eternal Mangekyo carrying Itachi's form can cast it.
 */
public class TsukuyomiAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 90f;
    private static final double RANGE = 14.0;
    private static final int EFFECT_TICKS = 10 * 20;
    private static final float SPIRIT_DAMAGE = 14f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** Eye contact held long enough to be unmistakable. */
    @Override
    public int castPoseTicks() {
        return 24;
    }

    @Override
    public long defaultCombo() {
        return 1131;
    }

    @Override
    public String requiredEye() {
        return "sharingan_ems";
    }

    @Override
    public String requiredEyeForm() {
        return "itachi";
    }

    @Override
    public int getCooldown() {
        return 60 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.EVOKER_PREPARE_SUMMON;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
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
        LivingEntity target = EyeTargeting.raycastLiving(player, RANGE);
        if (target == null) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notarget",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, EFFECT_TICKS, 2, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_TICKS, 2, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_TICKS, 2, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, EFFECT_TICKS, 0, false, false));

        // The illusion wounds the mind, not the body — magic damage bypasses armour,
        // same source the other mental techniques use (Kikaichu, Shadow Strangle).
        target.hurt(player.damageSources().magic(), SPIRIT_DAMAGE * ninjaData.getRankDamageMultiplier());

        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 eye = target.getEyePosition();
            // Black-and-red world of Tsukuyomi collapsing around the victim
            for (int i = 0; i < 40; i++) {
                double angle = Math.toRadians(i * 9.0);
                double radius = 0.8 + (i % 5) * 0.15;
                serverLevel.sendParticles(NarutoParticles.GENJUTSU_RED,
                        eye.x + radius * Math.cos(angle), eye.y - 0.3 + (i % 8) * 0.12, eye.z + radius * Math.sin(angle),
                        1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }
}
