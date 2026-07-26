package com.sekwah.narutomod.entity.goal;

import com.sekwah.narutomod.entity.MangekyoBossEntity;
import com.sekwah.narutomod.entity.MangekyoBossVariant;
import com.sekwah.narutomod.entity.jutsuprojectile.AmaterasuFireEntity;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;

/**
 * Lets a Mangekyo boss actually fight like one. Nothing in this mod casts jutsu from the
 * mob side — everything else is player-input driven — so this goal is the mob-facing
 * equivalent: on a cooldown, while it has line of sight and chakra to spare, the boss
 * throws whichever technique its wielder is known for.
 *
 * Damage is applied the same way the player-side jutsu do it (direct hurt + effects,
 * reusing AmaterasuFireEntity for Itachi/Sasuke's black flame) rather than inventing a
 * parallel combat path.
 */
public class BossJutsuGoal extends Goal {

    private static final double MIN_RANGE = 3.0;
    private static final double MAX_RANGE = 24.0;
    private static final int COOLDOWN_TICKS = 70;
    private static final float CHAKRA_PER_CAST = 45f;

    private static final DustParticleOptions CROW_BLACK =
            new DustParticleOptions(new Vector3f(0.08F, 0.08F, 0.12F), 1.4F);

    private final MangekyoBossEntity boss;
    private int cooldown;

    public BossJutsuGoal(MangekyoBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.boss.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distance = this.boss.distanceTo(target);
        return distance >= MIN_RANGE && distance <= MAX_RANGE
                && this.boss.getSensing().hasLineOfSight(target)
                && this.boss.getChakra() >= CHAKRA_PER_CAST;
    }

    @Override
    public boolean canContinueToUse() {
        return false; // single-shot: fire once, then hand control back
    }

    @Override
    public void start() {
        LivingEntity target = this.boss.getTarget();
        if (target == null || !this.boss.useChakra(CHAKRA_PER_CAST)) {
            return;
        }
        this.cooldown = COOLDOWN_TICKS;
        this.boss.getLookControl().setLookAt(target, 30f, 30f);

        switch (this.boss.getVariant().kit()) {
            case CROWS_AND_FLAME -> castBlackFlame(target, true);
            case LIGHTNING -> castLightning(target);
            case GUNBAI -> castShockwave(target);
            case ILLUSION -> castIllusion(target);
            case PHASE -> castPhaseStrike(target);
            case SWORDSMAN -> castBladeRush(target);
            case EXPLOSIVE -> castDetonation(target);
        }
    }

    /** Kisame/Zabuza/Hidan: close in and land a brutal blade hit. */
    private void castBladeRush(LivingEntity target) {
        Vec3 toTarget = target.position().subtract(this.boss.position()).normalize();
        this.boss.setDeltaMovement(toTarget.scale(1.6).add(0, 0.35, 0));
        this.boss.hurtMarked = true;
        target.hurt(this.boss.damageSources().mobAttack(this.boss), 13f);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3 * 20, 0, false, true));
        playCastSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel, target.position().add(0, 1.0, 0), 20, 0.8,
                    NarutoParticles.METAL_GRAY);
        }
    }

    /** Deidara/Sasori: a detonation on the target, damaging everything around it. */
    private void castDetonation(LivingEntity target) {
        for (LivingEntity caught : this.boss.level().getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(3.0), e -> e != this.boss && e.isAlive())) {
            caught.hurt(this.boss.damageSources().explosion(this.boss, this.boss), 11f);
            caught.addEffect(new MobEffectInstance(MobEffects.POISON, 5 * 20, 0, false, true));
        }
        playCastSound(SoundEvents.GENERIC_EXPLODE, 1.1f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    target.getX(), target.getY() + 1.0, target.getZ(), 6, 0.6, 0.6, 0.6, 0.0);
        }
    }

    /** Itachi: Amaterasu, with a blinding crow burst on the side. */
    private void castBlackFlame(LivingEntity target, boolean withCrows) {
        AmaterasuFireEntity fire = new AmaterasuFireEntity(this.boss.level(), this.boss,
                target.getX(), target.getY(), target.getZ());
        fire.setDamageMultiplier(1.2f);
        this.boss.level().addFreshEntity(fire);
        playCastSound(SoundEvents.WARDEN_SONIC_BOOM, 1.0f);

        if (withCrows && this.boss.level() instanceof ServerLevel serverLevel) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 4 * 20, 0, false, true));
            NarutoParticles.spawnBurst(serverLevel, target.position().add(0, 1.2, 0), 25, 1.0, CROW_BLACK);
        }
    }

    /** Sasuke: a lightning strike straight down onto the target. */
    private void castLightning(LivingEntity target) {
        target.hurt(this.boss.damageSources().mobAttack(this.boss), 12f);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3 * 20, 1, false, true));
        playCastSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.2f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            Vec3 pos = target.position();
            for (int y = 0; y < 16; y++) {
                serverLevel.sendParticles(NarutoParticles.CHIDORI_CYAN,
                        pos.x, pos.y + y, pos.z, 2, 0.12, 0.1, 0.12, 0.0);
            }
        }
    }

    /** Madara: a fan sweep that hurls the target back. */
    private void castShockwave(LivingEntity target) {
        target.hurt(this.boss.damageSources().mobAttack(this.boss), 14f);
        Vec3 push = target.position().subtract(this.boss.position()).normalize().scale(2.2).add(0, 0.5, 0);
        target.setDeltaMovement(target.getDeltaMovement().add(push));
        target.hurtMarked = true;
        playCastSound(SoundEvents.PHANTOM_SWOOP, 0.9f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, this.boss.position().add(0, 1.0, 0), 3.0, 30,
                    ParticleTypes.CLOUD);
        }
    }

    /** Shisui: genjutsu that takes the target out of the fight rather than damaging them. */
    private void castIllusion(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 6 * 20, 0, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 8 * 20, 1, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 8 * 20, 0, false, false));
        playCastSound(SoundEvents.EVOKER_CAST_SPELL, 1.0f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, target.getEyePosition(), 0.8, 24,
                    NarutoParticles.GENJUTSU_RED);
        }
    }

    /** Obito: phases out and re-emerges beside the target, striking as he lands. */
    private void castPhaseStrike(LivingEntity target) {
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel, this.boss.position().add(0, 1.0, 0), 20, 0.6,
                    NarutoParticles.SHARINGAN_RED);
        }
        Vec3 behind = target.position().subtract(target.getLookAngle().scale(1.5));
        this.boss.teleportTo(behind.x, target.getY(), behind.z);
        target.hurt(this.boss.damageSources().mobAttack(this.boss), 10f);
        playCastSound(SoundEvents.SHULKER_TELEPORT, 1.0f);
    }

    private void playCastSound(net.minecraft.sounds.SoundEvent sound, float pitch) {
        this.boss.level().playSound(null, this.boss.blockPosition(), sound, SoundSource.HOSTILE, 1.4f, pitch);
    }
}
