package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Lightning Style: Chidori Nagashi (combo 221) — Sasuke's omnidirectional release:
 * instead of concentrating the current in one hand, it's discharged through the whole
 * body, electrocuting and paralysing everything in melee range in every direction.
 *
 * Canon flow preserved: it's an EXTENSION of an active Chidori — you must have the
 * Chidori buff window running (combo 22 first), and releasing the current consumes it.
 */
public class ChidoriNagashiAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 35f;
    private static final double RADIUS = 3.5;
    private static final float DAMAGE = 10.0f;
    private static final int PARALYSIS_TICKS = 2 * 20;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 221;
    }

    @Override
    public int getCooldown() {
        return 15 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return NarutoSounds.CHIDORI.get();
    }
    // --- Phase 15: Nature Release ---
    @Override
    public String element() {
        return "lightning";
    }

    @Override
    public int elementLevelRequired() {
        return 12;
    }

    @Override
    public float elementXpReward() {
        return 35f;
    }


    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        // Phase 15: lightning-nature mastery gates this now (was Uchiha only)
        // Canon: Nagashi is the body-wide release of an already-formed Chidori
        if (!ninjaData.isChidoriActive()) {
            player.displayClientMessage(Component.literal("Form a Chidori first — Nagashi releases its current!")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 25);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Discharging the current through the body consumes the formed Chidori
        ninjaData.setChidoriTicks(0);

        float damage = DAMAGE * ninjaData.getRankDamageMultiplier() * ninjaData.getClanLightningDamageMultiplier();
        DamageSource source = NarutoDamageTypes.getDamageSource(player.level(), NarutoDamageTypes.CHIDORI, player, player);

        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(RADIUS, 1.5, RADIUS), e -> e != player && e.isAlive())) {
            target.hurt(source, damage);
            // Lightning paralysis — the current locks up the muscles of anyone it runs through
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PARALYSIS_TICKS, 3, false, true));
            Vec3 away = target.position().subtract(player.position()).normalize();
            target.knockback(1.2, -away.x, -away.z);
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);
            NarutoParticles.spawnRing(serverLevel, center, RADIUS * 0.6, 20, NarutoParticles.CHIDORI_CYAN);
            NarutoParticles.spawnRing(serverLevel, center.add(0, -0.6, 0), RADIUS, 26, NarutoParticles.CHIDORI_CYAN);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    center.x, center.y, center.z, 50, RADIUS * 0.5, 1.0, RADIUS * 0.5, 0.15);
        }
    }
}
