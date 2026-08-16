package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.entity.NarutoEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

public class AmaterasuFireEntity extends Entity {

    private static final int MAX_LIFESPAN = 20 * 20;
    private static final float DAMAGE = 4.0F;
    /**
     * Canon: Amaterasu is unquenchable and burns until the target is ash. A literal 7-day
     * burn isn't playable, but the black flames should CLING — victims keep burning long
     * after leaving the flame patch, far beyond a normal fire tick.
     */
    private static final int CLING_FIRE_SECONDS = 15;

    private int lifeSpan = MAX_LIFESPAN;
    /** Transient owner reference — resolved each tick from ownerUUID so it survives chunk reloads. */
    private UUID ownerUUID = null;
    private float damageMultiplier = 1.0F;

    public AmaterasuFireEntity(EntityType<AmaterasuFireEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public AmaterasuFireEntity(Level level, Entity owner, double x, double y, double z) {
        this(NarutoEntities.AMATERASU_FIRE.get(), level);
        this.ownerUUID = owner.getUUID();
        this.setPos(x, y, z);
    }

    /**
     * Tries to resolve the owner entity from the level each time it's needed.
     *
     * Any entity, not only players. Bosses cast Amaterasu too and hand their own UUID in, so
     * a player-only lookup returned null for every NPC caster - which made the flames
     * ownerless and therefore hostile to the boss that lit them, to its clones and to its
     * puppets. Itachi was setting himself on fire.
     */
    private Entity resolveOwner() {
        if (ownerUUID == null) return null;
        if (this.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getEntity(ownerUUID);
        }
        return null;
    }

    public void setDamageMultiplier(float damageMultiplier) {
        this.damageMultiplier = Math.max(0.0F, damageMultiplier);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    this.getX(), this.getY(), this.getZ(),
                    10, 0.35D, 0.35D, 0.35D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(), this.getY(), this.getZ(),
                    4, 0.25D, 0.25D, 0.25D, 0.01D);
        }

        if (!this.level().isClientSide && this.tickCount % 10 == 0) {
            Entity ownerEntity = resolveOwner();
            DamageSource damageSource = NarutoDamageTypes.getDamageSource(this.level(), NarutoDamageTypes.AMATERASU, this, ownerEntity);
            AABB damageBox = this.getBoundingBox().inflate(1.5D);
            this.level().getEntities(this, damageBox, entity -> entity instanceof LivingEntity && entity != ownerEntity)
                    .forEach(entity -> {
                        entity.setSecondsOnFire(CLING_FIRE_SECONDS);
                        entity.hurt(damageSource, DAMAGE * this.damageMultiplier);
                    });
        }

        if (!this.level().isClientSide && this.lifeSpan-- <= 0) {
            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.lifeSpan = tag.getInt("lifeSpan");
        this.damageMultiplier = tag.contains("DamageMultiplier") ? tag.getFloat("DamageMultiplier") : 1.0F;
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("lifeSpan", this.lifeSpan);
        tag.putFloat("DamageMultiplier", this.damageMultiplier);
        if (this.ownerUUID != null) {
            tag.putUUID("OwnerUUID", this.ownerUUID);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
