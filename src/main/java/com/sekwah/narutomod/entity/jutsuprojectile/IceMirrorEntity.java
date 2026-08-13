package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * One pane of Haku's Demonic Mirroring Ice Crystals.
 *
 * The mirrors are the technique, so they are entities rather than a particle flourish: each
 * one stands, each one can be broken, and breaking enough of them ends it. That is the whole
 * counterplay - the technique is not a timer you wait out, it is a structure you take apart,
 * and until you do, Haku is inside any of them.
 *
 * Twenty seconds is a long time to be surrounded, so the ring is deliberately fragile: a
 * mirror dies to a couple of solid hits and there are never more than ten.
 */
public class IceMirrorEntity extends Entity {

    private static final EntityDataAccessor<Float> HEALTH =
            SynchedEntityData.defineId(IceMirrorEntity.class, EntityDataSerializers.FLOAT);
    /** Which way the pane faces, in degrees; the renderer turns it to face the ring's centre. */
    private static final EntityDataAccessor<Float> FACING =
            SynchedEntityData.defineId(IceMirrorEntity.class, EntityDataSerializers.FLOAT);

    private static final float MAX_HEALTH = 12f;
    private static final int LIFESPAN = 20 * 20;

    /** How many panes go up around a target, and how far out they stand. */
    public static final int RING_SIZE = 8;
    public static final double RING_RADIUS = 4.0;

    private Optional<UUID> ownerUUID = Optional.empty();
    private int age;

    public IceMirrorEntity(EntityType<IceMirrorEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public IceMirrorEntity(LivingEntity caster, Vec3 position, float facing) {
        this(NarutoEntities.ICE_MIRROR.get(), caster.level());
        this.setPos(position.x, position.y, position.z);
        this.ownerUUID = Optional.of(caster.getUUID());
        this.entityData.set(FACING, facing);
    }

    /**
     * Raises the full ring around a point and hands back the panes.
     *
     * Shared by the boss kit and the player ability so the two cannot drift: one place decides
     * how many mirrors there are, how far out they stand and how tall they are.
     */
    public static List<IceMirrorEntity> raiseRing(LivingEntity caster, Vec3 centre) {
        java.util.ArrayList<IceMirrorEntity> mirrors = new java.util.ArrayList<>();
        for (int i = 0; i < RING_SIZE; i++) {
            double angle = (Math.PI * 2 * i) / RING_SIZE;
            Vec3 spot = centre.add(Math.cos(angle) * RING_RADIUS, 0.5, Math.sin(angle) * RING_RADIUS);
            // Facing measured back toward the centre, so every pane looks inward.
            float facing = (float) Math.toDegrees(-angle) + 90f;
            IceMirrorEntity mirror = new IceMirrorEntity(caster, spot, facing);
            caster.level().addFreshEntity(mirror);
            mirrors.add(mirror);
        }
        if (caster.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, centre.add(0, 0.2, 0), RING_RADIUS, 40,
                    NarutoParticles.ICE_PALE);
            serverLevel.playSound(null, caster.blockPosition(), SoundEvents.GLASS_PLACE,
                    SoundSource.HOSTILE, 2.0f, 0.7f);
        }
        return mirrors;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(HEALTH, MAX_HEALTH);
        this.entityData.define(FACING, 0f);
    }

    public float getMirrorHealth() {
        return this.entityData.get(HEALTH);
    }

    public float getFacing() {
        return this.entityData.get(FACING);
    }

    public Optional<UUID> getOwnerUUID() {
        return this.ownerUUID;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.random.nextInt(4) == 0) {
                this.level().addParticle(ParticleTypes.SNOWFLAKE,
                        this.getX() + (this.random.nextDouble() - 0.5),
                        this.getY() + this.random.nextDouble() * 2.0,
                        this.getZ() + (this.random.nextDouble() - 0.5), 0, -0.02, 0);
            }
            return;
        }
        if (++this.age >= LIFESPAN) {
            this.shatter();
        }
    }

    /**
     * Mirrors take damage from anything except the ninja standing inside them.
     *
     * Attacked rather than invulnerable on purpose: a technique that cannot be interfered
     * with is a timer, and this one is supposed to be a structure.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || this.isRemoved()) {
            return false;
        }
        if (source.getEntity() instanceof LivingEntity attacker
                && this.ownerUUID.map(attacker.getUUID()::equals).orElse(false)) {
            return false;
        }
        float left = this.getMirrorHealth() - amount;
        this.entityData.set(HEALTH, Math.max(0f, left));
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GLASS_HIT,
                    SoundSource.HOSTILE, 1.0f, 1.2f);
        }
        if (left <= 0f) {
            this.shatter();
        }
        return true;
    }

    public void shatter() {
        if (this.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel, this.position().add(0, 1.0, 0), 30, 0.9,
                    NarutoParticles.ICE_PALE);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK,
                    SoundSource.HOSTILE, 1.4f, 1.0f);
        }
        this.discard();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("Age");
        this.entityData.set(HEALTH, tag.getFloat("MirrorHealth"));
        this.entityData.set(FACING, tag.getFloat("Facing"));
        this.ownerUUID = tag.hasUUID("OwnerUUID")
                ? Optional.of(tag.getUUID("OwnerUUID")) : Optional.empty();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.age);
        tag.putFloat("MirrorHealth", this.getMirrorHealth());
        tag.putFloat("Facing", this.getFacing());
        this.ownerUUID.ifPresent(uuid -> tag.putUUID("OwnerUUID", uuid));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
