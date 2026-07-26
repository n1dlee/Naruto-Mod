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

import java.util.UUID;

public class RasenganEntity extends AbstractNonGlowingHurtingProjectile {

    public int lifeSpan = 80;

    private static final double MULTIPLIER = 0.15D;
    private static final DustParticleOptions CHAKRA_WAVE_PARTICLE = new DustParticleOptions(new Vector3f(0.35F, 0.85F, 1.0F), 1.2F);
    private static final int GRIND_TICK_INTERVAL = 4;

    /** Synced to client so the renderer can scale visuals correctly. Min 20, max 60. */
    private static final EntityDataAccessor<Integer> CHARGE_AMOUNT =
            SynchedEntityData.defineId(RasenganEntity.class, EntityDataSerializers.INT);

    // Min 20, max 60 — set by the ability before entity is added to world
    private int chargeAmount = 20;
    private float damageMultiplier = 1.0F;
    private boolean canKillPlayers = false;

    // --- Grinding state (anime-style: rasengan connects and grinds into the target for a
    // couple seconds with continuous chip damage + a gentle shove, instead of one instant hit) ---
    private boolean grinding = false;
    private UUID grindTargetId = null;
    private int grindTicksRemaining = 0;
    private int grindTotalTicks = 0;

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
        if (this.grinding) {
            tickGrind();
            return;
        }
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

    /**
     * While grinding, the entity stops flying and instead glues itself to the target's position,
     * dealing periodic chip damage + a small continuous shove — like Naruto grinding a Rasengan
     * into an opponent — for a fixed duration before releasing with a bigger final knockback.
     */
    private void tickGrind() {
        if (this.level().isClientSide) {
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity targetEntity = this.grindTargetId != null ? serverLevel.getEntity(this.grindTargetId) : null;
        if (!(targetEntity instanceof LivingEntity target) || !target.isAlive()) {
            releaseGrind(null);
            return;
        }

        Vec3 stickPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
        this.setPos(stickPos.x, stickPos.y, stickPos.z);
        this.setDeltaMovement(Vec3.ZERO);

        this.grindTicksRemaining--;

        if (this.grindTicksRemaining % GRIND_TICK_INTERVAL == 0) {
            applyGrindDamageTick(target);

            // Gentle continuous shove away from the caster while grinding (the big launch happens on release)
            Entity owner = this.getOwner();
            if (owner != null) {
                Vec3 push = target.position().subtract(owner.position());
                double len = push.horizontalDistance();
                if (len > 0.001) {
                    target.knockback(0.25, -push.x / len, -push.z / len);
                }
            }

            serverLevel.sendParticles(CHAKRA_WAVE_PARTICLE,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    10, 0.3, 0.3, 0.3, 0.05);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    6, 0.25, 0.25, 0.25, 0.1);
        }

        if (this.grindTicksRemaining <= 0 || !target.isAlive()) {
            releaseGrind(target);
        }
    }

    private void applyGrindDamageTick(LivingEntity target) {
        DamageSource dmgSource = NarutoDamageTypes.getDamageSource(this.level(), NarutoDamageTypes.RASENGAN, this, this.getOwner());
        int tickCount = Math.max(1, this.grindTotalTicks / GRIND_TICK_INTERVAL);

        if (target instanceof Player targetPlayer) {
            // Same overall budget as before (14 HP at Chunin baseline), split across the grind —
            // rechecking currentHP-1 every tick still naturally caps cumulative damage below Kage.
            float totalDamage = 14.0F * this.damageMultiplier;
            float damage = totalDamage / tickCount;
            if (!this.canKillPlayers) {
                damage = Math.min(damage, targetPlayer.getHealth() - 1.0F);
            }
            if (damage > 0) {
                target.hurt(dmgSource, damage);
            }
        } else {
            // Mobs: same 15→40 charge-scaled budget as before, split across the grind so tankier
            // mobs visibly get chipped down + shoved rather than eating one lump sum.
            float t = Math.max(0, Math.min(chargeAmount - 20, 40)) / 40.0f;
            float totalDamage = (15.0f + t * 25.0f) * this.damageMultiplier;
            target.hurt(dmgSource, totalDamage / tickCount);
        }

        if (this.getOwner() instanceof LivingEntity ownerLiving) {
            this.doEnchantDamageEffects(ownerLiving, target);
        }
    }

    private void releaseGrind(LivingEntity target) {
        if (target != null && this.level() instanceof ServerLevel serverLevel) {
            Vec3 diff = target.position().subtract(this.position());
            double horizLen = diff.horizontalDistance();
            if (horizLen > 0.001) {
                double kbStrength = getKnockbackStrength();
                target.knockback(kbStrength, -diff.x / horizLen, -diff.z / horizLen);
                Vec3 motion = target.getDeltaMovement();
                target.setDeltaMovement(motion.x, Math.min(motion.y + 0.6, 1.2), motion.z);
            }
            spawnReleaseBurst(serverLevel);
            this.playSound(NarutoSounds.WATER_BULLET_SPLASH.get(), 2f, 1.5f);
        }
        this.discard();
    }

    private void spawnReleaseBurst(ServerLevel serverLevel) {
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

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (this.level().isClientSide || this.grinding) {
            return;
        }
        Entity target = hitResult.getEntity();
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }

        // Start grinding instead of an instant single hit — matches Rasengan's anime usage of
        // continuously drilling into the target for a couple seconds before releasing them.
        this.grinding = true;
        this.grindTargetId = living.getUUID();
        float t = Math.max(0, Math.min(chargeAmount - 20, 40)) / 40.0f;
        this.grindTotalTicks = (int) (30 + t * 20); // ~1.5s at min charge -> ~2.5s at max charge
        this.grindTicksRemaining = this.grindTotalTicks;
        this.setDeltaMovement(Vec3.ZERO);
        this.xPower = 0.0D;
        this.yPower = 0.0D;
        this.zPower = 0.0D;
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (this.grinding) {
            // onHitEntity already put us into the grind state — no burst/despawn yet, tick() drives it
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            spawnReleaseBurst(serverLevel);
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
