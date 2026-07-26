package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.projectile.AbstractNonGlowingHurtingProjectile;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Wind Style: Rasenshuriken — the thrown evolution of the Rasengan (see
 * RasenshurikenAbility). Flies as a spinning disc of wind blades; on impact it expands
 * into a sphere of countless microscopic wind needles that sever the chakra network —
 * AoE damage plus heavy Weakness on everything caught in the blast.
 *
 * No baked model: the spinning particle disc IS the visual, drawn every tick in flight.
 */
public class RasenshurikenEntity extends AbstractNonGlowingHurtingProjectile {

    private static final double SPEED = 0.55D;
    private static final float DIRECT_DAMAGE = 20.0F;
    private static final float BLAST_DAMAGE = 25.0F;
    private static final double BLAST_RADIUS = 4.5D;
    private static final int SEVER_TICKS = 10 * 20;

    private int lifeSpan = 40;
    private float damageMultiplier = 1.0F;

    public RasenshurikenEntity(EntityType<RasenshurikenEntity> entityType, Level level) {
        super(entityType, level);
    }

    public RasenshurikenEntity(LivingEntity owner, double velX, double velY, double velZ) {
        this(NarutoEntities.RASENSHURIKEN.get(), owner.level());
        this.setOwner(owner);
        this.moveTo(owner.getX(), owner.getEyeY() - 0.2D, owner.getZ(), owner.getYRot(), owner.getXRot());
        this.reapplyPosition();
        double length = Math.sqrt(velX * velX + velY * velY + velZ * velZ);
        if (length != 0.0D) {
            this.xPower = velX / length * SPEED;
            this.yPower = velY / length * SPEED;
            this.zPower = velZ / length * SPEED;
        }
    }

    public void setDamageMultiplier(float damageMultiplier) {
        this.damageMultiplier = Math.max(0.0F, damageMultiplier);
    }

    @Override
    public void tick() {
        super.tick();

        // Spinning wind-blade disc around the core
        if (this.level().isClientSide || this.level() instanceof ServerLevel) {
            double angle = this.tickCount * 1.1;
            for (int blade = 0; blade < 4; blade++) {
                double a = angle + (Math.PI * 2 * blade) / 4;
                double px = this.getX() + Math.cos(a) * 0.9;
                double pz = this.getZ() + Math.sin(a) * 0.9;
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(NarutoParticles.RASENGAN_BLUE, px, this.getY(), pz, 1, 0.05, 0.02, 0.05, 0.0);
                    serverLevel.sendParticles(ParticleTypes.CLOUD, px, this.getY(), pz, 1, 0.03, 0.01, 0.03, 0.0);
                }
            }
        }

        if (!this.level().isClientSide && this.lifeSpan-- <= 0) {
            detonate();
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide) {
            detonate();
        }
    }

    /**
     * The expanding sphere of wind needles: everything in the blast takes heavy damage and
     * has its chakra network severed (long Weakness) — canonically the technique attacks on
     * a cellular level, which is why even survivors are left crippled.
     */
    private void detonate() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            this.discard();
            return;
        }
        Entity owner = this.getOwner();
        DamageSource source = owner instanceof LivingEntity livingOwner
                ? this.damageSources().indirectMagic(this, livingOwner)
                : this.damageSources().magic();

        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(BLAST_RADIUS), e -> e != owner && e.isAlive())) {
            float damage = (target.position().distanceTo(this.position()) < 1.2 ? DIRECT_DAMAGE + BLAST_DAMAGE : BLAST_DAMAGE)
                    * this.damageMultiplier;
            if (target instanceof Player targetPlayer && owner instanceof Player) {
                damage = Math.min(damage, targetPlayer.getHealth() - 1.0F);
                if (damage <= 0.0F) {
                    continue;
                }
            }
            target.hurt(source, damage);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, SEVER_TICKS, 2, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SEVER_TICKS / 2, 1, false, true));
        }

        // Expanding needle-sphere flash
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 3, 0.5, 0.5, 0.5, 0.0);
        serverLevel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 60, 1.8, 1.8, 1.8, 0.25);
        NarutoParticles.spawnBurst(serverLevel, this.position(), 50, 2.5, NarutoParticles.RASENGAN_BLUE);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.6F);

        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.END_ROD;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }
}
