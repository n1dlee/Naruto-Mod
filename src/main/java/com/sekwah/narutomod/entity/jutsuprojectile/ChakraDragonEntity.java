package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The serpent a dragon technique actually summons.
 *
 * Water Dragon and Kirin both described themselves as summoning a dragon and then did not:
 * one span a few particles at the impact point and hit-scanned, the other called a vanilla
 * lightning bolt. Neither put a creature on the field, so the two techniques with the most
 * distinctive silhouette in the game looked like every other point-and-damage jutsu.
 *
 * This is one entity for both, because they differ in colour, speed and approach rather than
 * in kind - a long body that swims through the air toward a point and detonates on arrival.
 * The body itself is procedural geometry drawn by the renderer; nothing here is a model file,
 * so the shape can flex and coil while it travels.
 */
public class ChakraDragonEntity extends Entity {

    /** Which dragon this is. Colour and behaviour both key off it. */
    public enum Kind {
        /** Water Release: Water Dragon Bullet. Comes in level and fast. */
        WATER,
        /** Kirin. Rides the thunderhead down, so it arrives from directly above. */
        LIGHTNING
    }

    private static final EntityDataAccessor<Byte> KIND =
            SynchedEntityData.defineId(ChakraDragonEntity.class, EntityDataSerializers.BYTE);
    /** Synced so the renderer can taper and coil the body by how far along the flight it is. */
    private static final EntityDataAccessor<Float> PROGRESS =
            SynchedEntityData.defineId(ChakraDragonEntity.class, EntityDataSerializers.FLOAT);

    private static final int MAX_LIFETIME = 20 * 6;
    /** How close to the destination counts as arrival. */
    private static final double ARRIVAL = 1.2;

    private int lifetime;
    private Vec3 destination = Vec3.ZERO;
    private double speed = 1.1;
    private float damage = 18f;
    private double blastRadius = 3.5;
    @Nullable
    private LivingEntity owner;

    public ChakraDragonEntity(EntityType<ChakraDragonEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public ChakraDragonEntity(LivingEntity owner, Vec3 from, Vec3 destination, Kind kind) {
        this(NarutoEntities.CHAKRA_DRAGON.get(), owner.level());
        this.owner = owner;
        this.destination = destination;
        this.setPos(from.x, from.y, from.z);
        this.entityData.set(KIND, (byte) kind.ordinal());
    }

    public ChakraDragonEntity speed(double speed) {
        this.speed = speed;
        return this;
    }

    public ChakraDragonEntity damage(float damage, double blastRadius) {
        this.damage = damage;
        this.blastRadius = blastRadius;
        return this;
    }

    public Kind getKind() {
        return Kind.values()[Math.floorMod(this.entityData.get(KIND), Kind.values().length)];
    }

    /** 0 at launch, 1 on arrival. The renderer uses it to swell the body as it closes. */
    public float getProgress() {
        return this.entityData.get(PROGRESS);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(KIND, (byte) 0);
        this.entityData.define(PROGRESS, 0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            // The client has no destination, so it just carries on along its last heading;
            // the server's position updates keep it honest.
            this.setPos(this.getX() + this.getDeltaMovement().x,
                    this.getY() + this.getDeltaMovement().y,
                    this.getZ() + this.getDeltaMovement().z);
            return;
        }

        if (++this.lifetime > MAX_LIFETIME) {
            this.discard();
            return;
        }

        Vec3 toTarget = this.destination.subtract(this.position());
        double remaining = toTarget.length();
        if (remaining <= ARRIVAL) {
            this.detonate();
            return;
        }

        Vec3 step = toTarget.scale(this.speed / remaining);
        this.setDeltaMovement(step);
        this.setPos(this.getX() + step.x, this.getY() + step.y, this.getZ() + step.z);

        // Face the way it is going, so the renderer's body follows the flight path.
        this.setYRot((float) (Math.atan2(step.z, step.x) * (180.0 / Math.PI)) - 90.0f);
        this.setXRot((float) (-Math.atan2(step.y, step.horizontalDistance()) * (180.0 / Math.PI)));

        this.entityData.set(PROGRESS, 1.0f - (float) Math.min(1.0, remaining / Math.max(1.0, this.startDistance())));

        // A dragon is a body, not a beam: anything it swims through is hit on the way past.
        for (LivingEntity caught : this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(1.4),
                e -> e != this.owner && e.isAlive())) {
            caught.hurt(this.damageSources().magic(), this.damage * 0.5f);
            this.detonate();
            return;
        }
    }

    private double cachedStartDistance = -1;

    private double startDistance() {
        if (this.cachedStartDistance < 0) {
            this.cachedStartDistance = Math.max(1.0, this.destination.distanceTo(this.position()));
        }
        return this.cachedStartDistance;
    }

    /** Arrival: everything nearby takes the full hit, and the body comes apart. */
    private void detonate() {
        List<LivingEntity> caught = this.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(this.position(), this.position()).inflate(this.blastRadius),
                e -> e != this.owner && e.isAlive());
        for (LivingEntity victim : caught) {
            victim.hurt(this.damageSources().magic(), this.damage);
            if (this.getKind() == Kind.WATER) {
                // Water throws; lightning stuns rather than moves.
                Vec3 push = victim.position().subtract(this.position()).normalize().scale(0.9);
                victim.setDeltaMovement(victim.getDeltaMovement().add(push.x, 0.45, push.z));
                victim.hurtMarked = true;
            }
        }
        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.lifetime = tag.getInt("Lifetime");
        this.entityData.set(KIND, tag.getByte("Kind"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Lifetime", this.lifetime);
        tag.putByte("Kind", this.entityData.get(KIND));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64 * 64;
    }
}
