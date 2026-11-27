package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.WaterDragonImpactPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.ProjectileAxis;
import com.sekwah.narutomod.util.WaterDragonFlight;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.UUID;

/** Water Dragon projectile. Legacy lightning entities are discarded. */
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
    /**
     * Body size, as a multiple of the base seven-and-a-half-block serpent.
     *
     * Water Dragon and Kirin are not the same animal. One is a bullet you fire down a corridor;
     * the other is the thing that comes out of a thunderhead and is supposed to be the largest
     * object on screen. Drawing both at the same length made Kirin read as a blue streak.
     */
    private static final EntityDataAccessor<Float> SCALE =
            SynchedEntityData.defineId(ChakraDragonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> WATER_PHASE =
            SynchedEntityData.defineId(ChakraDragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> WATER_AXIS =
            SynchedEntityData.defineId(ChakraDragonEntity.class, EntityDataSerializers.VECTOR3);

    private static final int MAX_LIFETIME = 20 * 6;
    public static final int WATER_WINDUP = 9;
    public static final double WATER_RANGE = 25.0;
    private static final double WATER_SPEED = 2.4;
    private static final double WATER_RADIUS = 3.0;
    private static final float WATER_SCALE = 1.5f;
    private static final double HEAD_RADIUS = 0.30;

    private int lifetime;
    private Vec3 destination = Vec3.ZERO;
    private double speed = 1.1;
    private float damage = 18f;
    private double blastRadius = 3.5;
    @Nullable
    private LivingEntity owner;
    @Nullable
    private Vec3 queuedWaterPosition;
    private final DragonTrail waterTrail = new DragonTrail();

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

    public static void launchWaterAlongLook(LivingEntity caster, float damage) {
        Vec3 from = waterOrigin(caster);
        Vec3 axis = ProjectileAxis.direction(caster.getLookAngle(), Vec3.ZERO);
        launchWater(caster, from, from.add(axis.scale(WATER_RANGE)), damage);
    }

    public static void launchWater(LivingEntity caster, Vec3 target, float damage) {
        launchWater(caster, waterOrigin(caster), target, damage);
    }

    private static Vec3 waterOrigin(LivingEntity caster) {
        return caster.getEyePosition().add(0, -0.2, 0);
    }

    private static void launchWater(LivingEntity caster, Vec3 from, Vec3 target, float damage) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 destination = WaterDragonFlight.advance(from, target, WATER_RANGE);
        ChakraDragonEntity dragon = new ChakraDragonEntity(caster, from, destination, Kind.WATER)
                .speed(WATER_SPEED).scale(WATER_SCALE).damage(damage, WATER_RADIUS);
        dragon.faceWater(ProjectileAxis.direction(destination.subtract(from), caster.getLookAngle()));
        dragon.entityData.set(WATER_PHASE, WATER_WINDUP);
        if (level.addFreshEntity(dragon)) {
            level.playSound(null, from.x, from.y, from.z, NarutoSounds.WATER_DRAGON_FORM.get(),
                    SoundSource.PLAYERS, 0.9f, 1.0f);
        }
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

    @Override
    protected void defineSynchedData() {
        this.entityData.define(KIND, (byte) 0);
        this.entityData.define(SCALE, 1.0f);
        this.entityData.define(WATER_PHASE, 0);
        this.entityData.define(WATER_AXIS, new Vector3f());
    }

    public int waterPhase() {
        return this.entityData.get(WATER_PHASE);
    }

    public Vec3 waterHeading() {
        return ProjectileAxis.direction(ProjectileAxis.decode(this.entityData.get(WATER_AXIS)), this.getLookAngle());
    }

    public DragonTrail waterTrail() {
        return this.waterTrail;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        AABB head = super.getBoundingBoxForCulling();
        if (this.getKind() == Kind.WATER) {
            return this.waterTrail.bounds(head.inflate(1.5 * this.getScale()), 1.1 * this.getScale());
        }
        return head;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int increments, boolean teleport) {
        if (this.getKind() != Kind.WATER || teleport) {
            super.lerpTo(x, y, z, yaw, pitch, increments, teleport);
            if (teleport) {
                this.queuedWaterPosition = null;
                this.waterTrail.clear();
            }
            return;
        }
        this.queuedWaterPosition = new Vec3(x, y, z);
        this.setRot(yaw, pitch);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.getKind() == Kind.WATER) {
                this.tickWaterClient();
                return;
            }
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

        if (this.getKind() == Kind.WATER && this.level() instanceof ServerLevel serverLevel) {
            this.tickWater(serverLevel);
            return;
        }
        // Obsolete lightning projectiles from old saves must not discharge.
        this.discard();
    }

    private void tickWaterClient() {
        if (this.queuedWaterPosition != null) {
            if (this.position().distanceToSqr(this.queuedWaterPosition) <= 16.0) {
                this.waterTrail.push(this.position());
            } else {
                this.waterTrail.clear();
            }
            this.setPos(this.queuedWaterPosition);
            this.queuedWaterPosition = null;
        }
    }

    private void tickWater(ServerLevel level) {
        int phase = this.waterPhase();
        if (phase < 0) {
            if (phase == -2) {
                this.discard();
            } else {
                this.entityData.set(WATER_PHASE, -2);
            }
            return;
        }
        if (phase > 0) {
            this.entityData.set(WATER_PHASE, phase - 1);
            if (phase == 1) {
                level.playSound(null, this.getX(), this.getY(), this.getZ(), NarutoSounds.WATER_DRAGON_LAUNCH.get(),
                        SoundSource.PLAYERS, 1.05f, 1.0f);
            }
            return;
        }

        Vec3 from = this.position();
        int flightTick = Math.max(1, this.lifetime - WATER_WINDUP);
        Vec3 to = WaterDragonFlight.advance(from, this.destination, this.speed * Math.min(1.0, 0.4 + 0.2 * flightTick));
        if (!level.hasChunkAt(BlockPos.containing(to))) {
            this.discard();
            return;
        }
        LivingEntity caster = this.resolveOwner();
        var impact = WaterDragonCollision.sweep(this, level, from, to, HEAD_RADIUS * this.getScale(), caster);
        if (impact != null) {
            this.setPos(impact.center());
            this.finishWater(level, caster, impact);
        } else {
            this.setPos(to);
            if (to.equals(this.destination)) {
                this.finishWater(level, caster, new WaterDragonCollision.Impact(to, to,
                        WaterDragonCollision.flightAxis(this, from, to), null, WaterDragonImpactPacket.Kind.DISSIPATE));
            }
        }
    }

    private void finishWater(ServerLevel level, @Nullable LivingEntity caster, WaterDragonCollision.Impact contact) {
        this.entityData.set(WATER_PHASE, -1);
        WaterDragonCollision.damage(this, level, caster, contact, this.damage, this.blastRadius);
        Vec3 point = contact.point();
        PacketHandler.sendNear(new WaterDragonImpactPacket(level.dimension().location(), point, contact.axis(),
                contact.kind(), this.random.nextInt()), level, point, 64.0);
        level.playSound(null, point.x, point.y, point.z, NarutoSounds.WATER_DRAGON_IMPACT.get(), SoundSource.PLAYERS,
                contact.kind() == WaterDragonImpactPacket.Kind.DISSIPATE ? 0.48f : 1.25f, 1.0f);
    }

    private void faceWater(Vec3 heading) {
        this.entityData.set(WATER_AXIS, ProjectileAxis.encode(heading));
        var angles = ProjectileAxis.angles(heading);
        this.setRot(angles.yaw(), angles.pitch());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.lifetime = tag.getInt("Lifetime");
        this.entityData.set(KIND, tag.getByte("Kind"));
        this.entityData.set(WATER_PHASE, Mth.clamp(tag.getInt("WaterPhase"), -2, WATER_WINDUP));
        this.entityData.set(SCALE, tag.contains("Scale") ? tag.getFloat("Scale") : 1.0f);
        this.refreshDimensions();
        // Everything that decides where this goes and what it does on arrival. Held only in
        // memory before, so a chunk unload mid-flight left the dragon with a destination of
        // (0,0,0) and default damage: it turned and flew at the world origin, and whatever it
        // eventually detonated on took a number unrelated to the technique that fired it.
        this.destination = new Vec3(
                tag.getDouble("DestX"), tag.getDouble("DestY"), tag.getDouble("DestZ"));
        if (this.getKind() == Kind.WATER) {
            Vec3 axis = this.waterPhase() < 0 ? this.getLookAngle() : this.destination.subtract(this.position());
            this.faceWater(ProjectileAxis.direction(axis, this.getLookAngle()));
        }
        this.speed = tag.contains("Speed") ? tag.getDouble("Speed") : 1.1;
        this.damage = tag.contains("Damage") ? tag.getFloat("Damage") : 18f;
        this.blastRadius = tag.contains("Blast") ? tag.getDouble("Blast") : 3.5;
        this.ownerUUID = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Lifetime", this.lifetime);
        tag.putByte("Kind", this.entityData.get(KIND));
        tag.putInt("WaterPhase", this.waterPhase());
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
    private UUID ownerUUID;

    @Nullable
    UUID ownerId() {
        return this.ownerUUID;
    }

    /** Resolves the owner lazily, so an unloaded caster does not make the dragon ownerless. */
    @Nullable
    private LivingEntity resolveOwner() {
        if (this.owner != null && !this.owner.isRemoved()) {
            return this.owner;
        }
        this.owner = null;
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
