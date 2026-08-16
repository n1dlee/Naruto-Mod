package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Yasaka Magatama - the comma-shaped seal a Susanoo throws.
 *
 * The 1.12.2 version is a scalable projectile with no model at all: a flat billboard of
 * yasaka_magatama.png that spins as it flies and does area damage where it lands. There was
 * nothing for the model converter to extract, so what is ported here is the behaviour and the
 * texture, and the renderer draws the billboard the same way the original did.
 *
 * Susanoo's only technique in this mod was the sword, which meant a Complete Body had nothing
 * to do against anything that stayed out of reach.
 */
public class YasakaMagatamaEntity extends Entity {

    /** Drives the size of the billboard; a bigger Susanoo throws a bigger seal. */
    private static final EntityDataAccessor<Float> SCALE =
            SynchedEntityData.defineId(YasakaMagatamaEntity.class, EntityDataSerializers.FLOAT);
    /** The wielder's Susanoo colour, so the seal matches the shell that threw it. */
    private static final EntityDataAccessor<Integer> TINT =
            SynchedEntityData.defineId(YasakaMagatamaEntity.class, EntityDataSerializers.INT);

    private static final int MAX_LIFETIME = 20 * 8;
    private static final double SPEED = 1.4;

    private int lifetime;
    private float damage = 20f;
    private double blastRadius = 4.0;
    @Nullable
    private LivingEntity owner;

    public YasakaMagatamaEntity(EntityType<YasakaMagatamaEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public YasakaMagatamaEntity(LivingEntity owner, Vec3 from, Vec3 aim, float scale, int tint) {
        this(NarutoEntities.YASAKA_MAGATAMA.get(), owner.level());
        this.owner = owner;
        this.setPos(from.x, from.y, from.z);
        this.setDeltaMovement(aim.normalize().scale(SPEED));
        this.entityData.set(SCALE, scale);
        this.entityData.set(TINT, tint);
    }

    public YasakaMagatamaEntity damage(float damage, double blastRadius) {
        this.damage = damage;
        this.blastRadius = blastRadius;
        return this;
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    public int getTint() {
        return this.entityData.get(TINT);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SCALE, 1.5f);
        this.entityData.define(TINT, 0x8C40D9);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        if (this.level().isClientSide) {
            return;
        }
        if (++this.lifetime > MAX_LIFETIME) {
            this.discard();
            return;
        }

        // Anything it passes through, and anything it hits.
        double reach = this.getScale() * 0.6;
        for (LivingEntity caught : this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(reach),
                e -> e != this.owner && e.isAlive())) {
            this.detonate();
            return;
        }

        Vec3 ahead = this.position().add(motion);
        HitResult blockHit = this.level().clip(new ClipContext(this.position(), ahead,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) {
            this.detonate();
        }
    }

    private void detonate() {
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(this.position(), this.position()).inflate(this.blastRadius),
                e -> e != this.owner && e.isAlive())) {
            victim.hurt(this.damageSources().magic(), this.damage);
            Vec3 push = victim.position().subtract(this.position()).normalize().scale(0.8);
            victim.setDeltaMovement(victim.getDeltaMovement().add(push.x, 0.35, push.z));
            victim.hurtMarked = true;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(0.55f, 0.25f, 0.85f), 2.4f),
                    this.getX(), this.getY(), this.getZ(), 40,
                    this.blastRadius * 0.4, this.blastRadius * 0.4, this.blastRadius * 0.4, 0.1);
            serverLevel.playSound(null, this.blockPosition(),
                    net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_BREAK,
                    net.minecraft.sounds.SoundSource.PLAYERS, 3.0f, 0.6f);
        }
        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.lifetime = tag.getInt("Lifetime");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Lifetime", this.lifetime);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
