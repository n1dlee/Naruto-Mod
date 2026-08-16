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
    /**
     * Body size, as a multiple of the base seven-and-a-half-block serpent.
     *
     * Water Dragon and Kirin are not the same animal. One is a bullet you fire down a corridor;
     * the other is the thing that comes out of a thunderhead and is supposed to be the largest
     * object on screen. Drawing both at the same length made Kirin read as a blue streak.
     */
    private static final EntityDataAccessor<Float> SCALE =
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
        this.ownerUUID = owner.getUUID();
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

    /**
     * How large a serpent this is, as a multiple of the base body.
     *
     * Also grows the hitbox, and not only so it collides honestly: an entity is culled against
     * its bounding box, so a thirty-block Kirin hanging off a one-block box vanishes the moment
     * that box leaves the frustum — which, for something descending from above while you are
     * looking at the ground, is most of its flight.
     */
    public ChakraDragonEntity scale(float scale) {
        this.entityData.set(SCALE, Math.max(0.2f, scale));
        this.refreshDimensions();
        return this;
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        float width = 1.2f * this.getScale();
        return net.minecraft.world.entity.EntityDimensions.scalable(width, width);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        // Dimensions are computed per side, so the client has to be told the body grew or it
        // culls the dragon against a box the size of the one it was registered with.
        if (SCALE.equals(key)) {
            this.refreshDimensions();
        }
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
        this.entityData.define(SCALE, 1.0f);
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
                this.getBoundingBox().inflate(1.4 * this.getScale()),
                e -> e != this.resolveOwner() && e.isAlive())) {
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
                e -> e != this.resolveOwner() && e.isAlive());
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
        this.entityData.set(SCALE, tag.contains("Scale") ? tag.getFloat("Scale") : 1.0f);
        this.refreshDimensions();
        // Everything that decides where this goes and what it does on arrival. Held only in
        // memory before, so a chunk unload mid-flight left the dragon with a destination of
        // (0,0,0) and default damage: it turned and flew at the world origin, and whatever it
        // eventually detonated on took a number unrelated to the technique that fired it.
        this.destination = new Vec3(
                tag.getDouble("DestX"), tag.getDouble("DestY"), tag.getDouble("DestZ"));
        this.speed = tag.contains("Speed") ? tag.getDouble("Speed") : 1.1;
        this.damage = tag.contains("Damage") ? tag.getFloat("Damage") : 18f;
        this.blastRadius = tag.contains("Blast") ? tag.getDouble("Blast") : 3.5;
        this.ownerUUID = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Lifetime", this.lifetime);
        tag.putByte("Kind", this.entityData.get(KIND));
        tag.putFloat("Scale", this.entityData.get(SCALE));
        tag.putDouble("DestX", this.destination.x);
        tag.putDouble("DestY", this.destination.y);
        tag.putDouble("DestZ", this.destination.z);
        tag.putDouble("Speed", this.speed);
        tag.putFloat("Damage", this.damage);
        tag.putDouble("Blast", this.blastRadius);
        if (this.owner != null) {
            tag.putUUID("Owner", this.owner.getUUID());
        } else if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
    }

    /**
     * The owner's id, kept separately from the resolved entity.
     *
     * A reloaded dragon has no live reference to whoever fired it, so without this it counted
     * its own summoner as a valid target and could detonate on them.
     */
    @Nullable
    private java.util.UUID ownerUUID;

    /** Resolves the owner lazily, so an unloaded caster does not make the dragon ownerless. */
    @Nullable
    private LivingEntity resolveOwner() {
        if (this.owner != null) {
            return this.owner;
        }
        if (this.ownerUUID != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.ownerUUID) instanceof LivingEntity found) {
            this.owner = found;
        }
        return this.owner;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // Scaled with the body: a Kirin starts its descent well over sixty blocks away, and
        // being cut off at a fixed range is the other half of why it was never seen coming.
        double range = 64 * this.getScale();
        return distance < range * range;
    }
}
