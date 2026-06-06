package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.projectile.AbstractNonGlowingHurtingProjectile;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class RasenganEntity extends AbstractNonGlowingHurtingProjectile {

    public int lifeSpan = 80;

    private static final double MULTIPLIER = 0.15D;
    private static final DustParticleOptions CHAKRA_WAVE_PARTICLE = new DustParticleOptions(new Vector3f(0.35F, 0.85F, 1.0F), 1.2F);

    /** Synced to client so the renderer can scale visuals correctly. Min 20, max 60. */
    private static final EntityDataAccessor<Integer> CHARGE_AMOUNT =
            SynchedEntityData.defineId(RasenganEntity.class, EntityDataSerializers.INT);

    // Min 20, max 60 — set by the ability before entity is added to world
    private int chargeAmount = 20;
    private float damageMultiplier = 1.0F;
    private boolean canKillPlayers = false;

    public RasenganEntity(EntityType<RasenganEntity> entityType, Level level) {
        super(entityType, level);
    }

    public RasenganEntity(EntityType<? extends AbstractNonGlowingHurtingProjectile> entityType,
                          double posX, double posY, double posZ,
                          double velX, double velY, double velZ, Level level) {
        super(entityType, level);
        this.moveTo(posX, posY, posZ, this.getYRot(), this.getXRot());
        this.reapplyPosition();
        double d0 = Math.sqrt(velX * velX + velY * velY + velZ * velZ);
        if (d0 != 0.0D) {
            this.xPower = velX / d0 * MULTIPLIER;
            this.yPower = velY / d0 * MULTIPLIER;
            this.zPower = velZ / d0 * MULTIPLIER;
        }
    }

    public RasenganEntity(LivingEntity owner, double xVel, double yVel, double zVel) {
        this(NarutoEntities.RASENGAN.get(), owner.getX(), owner.getEyeY() - 0.1f, owner.getZ(), xVel, yVel, zVel, owner.level());
        this.setOwner(owner);
        this.setRot(owner.getYRot(), owner.getXRot());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CHARGE_AMOUNT, 20);
    }

    public void setChargeAmount(int charge) {
        this.chargeAmount = charge;
        this.entityData.set(CHARGE_AMOUNT, charge);
    }

    public void setDamageMultiplier(float damageMultiplier) {
        this.damageMultiplier = Math.max(0.0F, damageMultiplier);
    }

    public void setCanKillPlayers(boolean canKillPlayers) {
        this.canKillPlayers = canKillPlayers;
    }

    public int getSyncedChargeAmount() {
        return this.entityData.get(CHARGE_AMOUNT);
    }

    /**
     * Rasengan hitbox scales with charge: 0.4 blocks (min) → 2.0 blocks (full charge).
     */
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float t = Math.max(0, Math.min(chargeAmount - 20, 40)) / 40.0f;
        float size = 0.4f + t * 1.6f; // 0.4 → 2.0
        return EntityDimensions.scalable(size, size);
    }

    /**
     * Knockback strength scales with charge: 4.0 at 20 ticks → 10.0 at 60 ticks.
     */
    private double getKnockbackStrength() {
        double t = Math.max(0, Math.min(chargeAmount - 20, 40)) / 40.0;
        return 4.0 + t * 6.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (lifeSpan-- <= 0) {
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(), this.getY(), this.getZ(),
                        60, 0.3, 0.3, 0.3, 0.1);
            }
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (!this.level().isClientSide) {
            Entity target = hitResult.getEntity();
            Entity owner = this.getOwner();
            DamageSource dmgSource = NarutoDamageTypes.getDamageSource(this.level(), NarutoDamageTypes.RASENGAN, this, owner);

            if (target instanceof Player targetPlayer) {
                // Below Kage, leave players at 1 HP. Kage-level Rasengan can be lethal.
                float playerHp = targetPlayer.getHealth();
                float damage = 14.0F * this.damageMultiplier;
                if (!this.canKillPlayers) {
                    damage = Math.min(damage, playerHp - 1.0F);
                }
                if (damage > 0) {
                    target.hurt(dmgSource, damage);
                }
            } else {
                // Mobs: scales 15 → 40 with full charge
                float t = Math.max(0, Math.min(chargeAmount - 20, 40)) / 40.0f;
                float mobDamage = (15.0f + t * 25.0f) * this.damageMultiplier;
                target.hurt(dmgSource, mobDamage);
            }

            // Knockback — direction away from rasengan, scales with charge
            Vec3 diff = target.position().subtract(this.position());
            double horizLen = diff.horizontalDistance();
            if (horizLen > 0) {
                double kbStrength = getKnockbackStrength();
                if (target instanceof LivingEntity living) {
                    living.knockback(kbStrength, -diff.x / horizLen, -diff.z / horizLen);
                    // Extra upward boost to make them fly back dramatically
                    Vec3 motion = living.getDeltaMovement();
                    living.setDeltaMovement(motion.x, Math.min(motion.y + 0.6, 1.2), motion.z);
                }
            }

            if (owner instanceof LivingEntity) {
                this.doEnchantDamageEffects((LivingEntity) owner, target);
            }
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (this.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 36; i++) {
                double angle = (Math.PI * 2.0D * i) / 36.0D;
                double xSpeed = Math.cos(angle) * 0.18D;
                double zSpeed = Math.sin(angle) * 0.18D;
                serverLevel.sendParticles(CHAKRA_WAVE_PARTICLE,
                        this.getX(), this.getY(), this.getZ(),
                        1, xSpeed, 0.04D, zSpeed, 0.35D);
            }
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY(), this.getZ(),
                    24, 0.25D, 0.25D, 0.25D, 0.08D);
        }
        if (!this.level().isClientSide) {
            this.playSound(NarutoSounds.WATER_BULLET_SPLASH.get(), 2f, 1.5f);
            this.discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return CHAKRA_WAVE_PARTICLE;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0F;
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        double d0 = packet.getXa();
        double d1 = packet.getYa();
        double d2 = packet.getZa();
        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
        if (d3 != 0.0D) {
            this.xPower = d0 / d3 * MULTIPLIER;
            this.yPower = d1 / d3 * MULTIPLIER;
            this.zPower = d2 / d3 * MULTIPLIER;
        }
    }
}
