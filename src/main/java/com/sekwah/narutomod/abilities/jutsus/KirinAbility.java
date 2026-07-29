package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.util.EyeTargeting;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Kirin - Sasuke's signature Mangekyo technique (combo 1121).
 *
 * Canon Kirin is not a spell you fire, it is a thunderstorm you spend time building and
 * then aim. So this is CHANNELED: hold the cast and the sky above you fills with static
 * discharge, the weather turns, and only after a full three seconds does the bolt come
 * down. Let go early and the charge simply dissipates - the whole point of the technique
 * is that it is devastating precisely because it is slow and telegraphed.
 *
 * Requires open sky over the target, as before: the bolt rides a real thunderhead.
 */
public class KirinAbility extends Ability implements Ability.Cooldown, Ability.Channeled {

    private static final float CHARGE_TICK_COST = 1.5f;
    private static final double RANGE = 24.0;
    private static final float DIRECT_DAMAGE = 34f;
    private static final float SPLASH_DAMAGE = 10f;
    private static final double SPLASH_RADIUS = 4.0;

    /** Three seconds, matching the legacy mod's own wind-up exactly. */
    private static final int CHARGE_TICKS = 60;
    /** Where the thunderhead sits. The bolt is drawn down from here. */
    private static final int CLOUD_HEIGHT = 40;

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public long defaultCombo() {
        return 1121;
    }

    @Override
    public String requiredEye() {
        return "sharingan_ems";
    }

    @Override
    public String requiredEyeForm() {
        return "sasuke";
    }

    @Override
    public int getCooldown() {
        return 45 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.LIGHTNING_BOLT_THUNDER;
    }

    /** Tapping the combo does nothing but tell you to hold it - there is no quick Kirin. */
    @Override
    public boolean canActivateBelowMinCharge() {
        return false;
    }

    /** A charge released early never became a strike, so it must not burn the cooldown. */
    @Override
    public boolean channelCommittedAt(int ticksChanneled) {
        return ticksChanneled >= CHARGE_TICKS;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        // Billed per tick of channelling rather than as a lump sum, so an interrupted
        // charge costs you only what you actually spent holding it.
        if (ninjaData.getChakra() < CHARGE_TICK_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHARGE_TICK_COST, 40);
        return true;
    }

    /**
     * The wind-up. Static discharge gathers in a dome overhead that widens and intensifies
     * as the charge builds, and at four-fifths the storm actually breaks.
     */
    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float progress = Math.min(1.0f, ticksChanneled / (float) CHARGE_TICKS);

        // A couple of arcs per tick, more as it builds, scattered across a widening dome.
        int arcs = 1 + (int) (progress * 3);
        double spread = 6.0 + progress * 22.0;
        for (int i = 0; i < arcs; i++) {
            Vec3 base = player.position().add(
                    (serverLevel.getRandom().nextDouble() - 0.5) * spread,
                    CLOUD_HEIGHT + serverLevel.getRandom().nextDouble() * 8.0,
                    (serverLevel.getRandom().nextDouble() - 0.5) * spread);
            Vec3 end = base.add(
                    (serverLevel.getRandom().nextDouble() - 0.5) * 10.0,
                    -serverLevel.getRandom().nextDouble() * 6.0,
                    (serverLevel.getRandom().nextDouble() - 0.5) * 10.0);
            NarutoParticles.spawnBolt(serverLevel, base, end, 3, 1.5, NarutoParticles.CHIDORI_CYAN);
        }

        // Sparks gathering on the caster themselves, so the charge is visible from the ground.
        if (ticksChanneled % 4 == 0) {
            NarutoParticles.spawnRing(serverLevel, player.position().add(0, 1.0, 0),
                    0.8 + progress, 12, NarutoParticles.CHIDORI_CYAN);
        }

        if (ticksChanneled == (int) (CHARGE_TICKS * 0.8f)) {
            // The storm breaks. Brief, and only a nudge - permanently reshaping the
            // player's weather because they charged a jutsu would be obnoxious.
            serverLevel.setWeatherParameters(0, 600, true, true);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.WEATHER, 4.0f, 0.6f);
        }
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (ticksActive < CHARGE_TICKS) {
            player.displayClientMessage(Component.translatable("jutsu.kirin.uncharged")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        LivingEntity target = EyeTargeting.raycastLiving(player, RANGE);
        if (target == null) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notarget",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        // Kirin rides a thunderhead - it needs sky above the target to come down.
        BlockPos targetPos = target.blockPosition();
        if (!player.level().canSeeSky(targetPos)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.nosky",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        float multiplier = ninjaData.getRankDamageMultiplier();
        DamageSource source = NarutoDamageTypes.getDamageSource(
                player.level(), NarutoDamageTypes.CHIDORI, player, player);

        target.hurt(source, DIRECT_DAMAGE * multiplier);
        target.setSecondsOnFire(3);

        for (LivingEntity splashed : player.level().getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(SPLASH_RADIUS),
                e -> e != player && e != target && e.isAlive())) {
            splashed.hurt(source, SPLASH_DAMAGE * multiplier);
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            strike(serverLevel, target.position());
            serverLevel.playSound(null, targetPos, SoundEvents.LIGHTNING_BOLT_IMPACT,
                    SoundSource.PLAYERS, 4.0f, 0.8f);
        }
    }

    /**
     * The strike itself: several branching bolts torn down out of the cloud onto the
     * target, all converging on the same point so they read as one enormous fork rather
     * than as separate hits.
     */
    private void strike(ServerLevel level, Vec3 impact) {
        Vec3 cloud = impact.add(0, CLOUD_HEIGHT, 0);
        for (int i = 0; i < 3; i++) {
            Vec3 start = cloud.add(
                    (level.getRandom().nextDouble() - 0.5) * 8.0,
                    level.getRandom().nextDouble() * 6.0,
                    (level.getRandom().nextDouble() - 0.5) * 8.0);
            NarutoParticles.spawnBolt(level, start, impact, 6, 5.0, NarutoParticles.CHIDORI_CYAN);
        }
        NarutoParticles.spawnBurst(level, impact.add(0, 0.5, 0), 60, 1.2, NarutoParticles.CHIDORI_CYAN);
        NarutoParticles.spawnRing(level, impact.add(0, 0.2, 0), SPLASH_RADIUS, 48,
                NarutoParticles.CHIDORI_CYAN);
        level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y + 1.0, impact.z, 4, 1.0, 0.6, 1.0, 0.0);
    }
}
