package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

/**
 * Hyuga Clan — Gentle Fist Art: Eight Trigrams Sixty-Four Palms (combo 112).
 * The clan's signature offensive taijutsu: six escalating waves of palm strikes
 * (2 -> 4 -> 8 -> 16 -> 32 -> 64) delivered to a single target in melee range,
 * sealing their tenketsu. Canon requires the Byakugan's near-360° vision to place
 * the strikes, so the Byakugan must be active to cast.
 *
 * Final wave "blocks the chakra points": heavy Weakness + Slowness, knockback, and
 * if the target is another ninja (player), half of their remaining chakra is sealed.
 */
public class EightTrigramsSixtyFourPalmsAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 45f;
    private static final double CAST_RANGE = 3.5;
    private static final double FOLLOW_RANGE = 6.0;
    private static final int[] WAVE_STRIKES = {2, 4, 8, 16, 32, 64};
    private static final int TICKS_BETWEEN_WAVES = 5;
    private static final float DAMAGE_PER_STRIKE = 0.22f;
    private static final int SEAL_TICKS = 8 * 20;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 112;
    }

    @Override
    public int getCooldown() {
        return 20 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.PLAYER_ATTACK_SWEEP;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!"hyuga".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.hyuga",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        // Canon: the strike pattern depends on the Byakugan's vision of the tenketsu
        if (!ninjaData.isByakuganActive()) {
            player.displayClientMessage(Component.literal("The Byakugan must be active to see the chakra points!")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (findTarget(player) == null) {
            player.displayClientMessage(Component.literal("No target within striking range!")
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }
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
        LivingEntity target = findTarget(player);
        if (target == null) {
            return;
        }

        float damageMultiplier = ninjaData.getRankDamageMultiplier();
        strikeWave(player, target, 0, damageMultiplier);
        for (int wave = 1; wave < WAVE_STRIKES.length; wave++) {
            final int waveIndex = wave;
            ninjaData.scheduleDelayedTickEvent(
                    p -> strikeWave(p, target, waveIndex, damageMultiplier),
                    wave * TICKS_BETWEEN_WAVES);
        }
    }

    private void strikeWave(Player player, LivingEntity target, int waveIndex, float damageMultiplier) {
        // Target escaped the trigram's perimeter or died mid-sequence — the sequence breaks
        if (!target.isAlive() || player.distanceTo(target) > FOLLOW_RANGE) {
            return;
        }
        int strikes = WAVE_STRIKES[waveIndex];

        player.displayClientMessage(Component.literal("Eight Trigrams: " + strikes + " Palms!")
                .withStyle(waveIndex >= WAVE_STRIKES.length - 1 ? ChatFormatting.GOLD : ChatFormatting.WHITE), true);
        target.hurt(player.damageSources().playerAttack(player), strikes * DAMAGE_PER_STRIKE * damageMultiplier);

        Vec3 center = target.position().add(0, target.getBbHeight() * 0.5, 0);
        if (player.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel, center, 4 + strikes / 4, 0.4, NarutoParticles.ROTATION_WHITE);
        }
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.7f, 1.0f + waveIndex * 0.1f);

        if (waveIndex == WAVE_STRIKES.length - 1) {
            sealTenketsu(player, target);
        }
    }

    /**
     * The 64th strike closes the tenketsu: chakra flow stops (heavy Weakness/Slowness,
     * and half of a ninja target's remaining chakra is sealed away) and the target is
     * knocked out of the trigram's perimeter.
     */
    private void sealTenketsu(Player player, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, SEAL_TICKS, 1, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SEAL_TICKS, 2, false, true));
        Vec3 away = target.position().subtract(player.position()).normalize();
        target.knockback(1.5, -away.x, -away.z);

        // 25% of remaining chakra (was 50% — too brutal in PvP for a 20s-cooldown technique)
        target.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(targetData ->
                targetData.useChakra(targetData.getChakra() * 0.25f, 60));

        if (player.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, target.position().add(0, 0.1, 0), 1.2, 16,
                    NarutoParticles.ROTATION_WHITE);
        }
    }

    private LivingEntity findTarget(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(CAST_RANGE)).inflate(1.0);
        return player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                        e -> e != player && e.isAlive()).stream()
                .filter(e -> {
                    Vec3 toE = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
                    return toE.length() <= CAST_RANGE && toE.normalize().dot(look) >= 0.5;
                })
                .min(Comparator.comparingDouble(e -> e.position().distanceTo(eye)))
                .orElse(null);
    }
}
