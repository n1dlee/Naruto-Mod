package com.sekwah.narutomod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Wood Release: Wood Golem - a giant of living timber the summoner climbs onto and drives.
 *
 * Not a pet that fights for you. The legacy mod built it as a mount with no attack AI of
 * its own, and that is the right read: it is a walking siege platform, and the ninja on
 * its shoulder is still the one doing the fighting. It does swing at monsters that come
 * within reach while nobody is riding it, so a summoned golem left standing is not inert.
 *
 * Kept alive by its summoner's chakra (billed by WoodGolemAbility's per-tick toggle cost),
 * and it dissolves when that runs out or the technique is released.
 */
public class WoodGolemEntity extends PathfinderMob {

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(WoodGolemEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    /** 0-30, drives the "growing out of the ground" scale-up in the renderer. */
    private static final EntityDataAccessor<Integer> GROWTH =
            SynchedEntityData.defineId(WoodGolemEntity.class, EntityDataSerializers.INT);

    public static final int GROWTH_TICKS = 30;

    public WoodGolemEntity(EntityType<WoodGolemEntity> entityType, Level level) {
        super(entityType, level);
        // Something this size should stride over fences and ledges, not be stopped by them.
        this.setMaxUpStep(1.5f);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(GROWTH, 0);
    }

    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(OWNER_UUID);
    }

    public void setOwner(LivingEntity owner) {
        this.entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
    }

    public boolean isOwnedBy(Player player) {
        return this.getOwnerUUID().map(uuid -> uuid.equals(player.getUUID())).orElse(false);
    }

    /** 0 while erupting, 1 once fully grown. Read by the renderer for the rise animation. */
    public float getGrowthProgress() {
        return Math.min(1.0f, this.entityData.get(GROWTH) / (float) GROWTH_TICKS);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.LivingEntity.class, 10, true, false,
                        // Enemy, not Monster. Every custom boss and tailed beast in this mod
                        // is a PathfinderMob implementing Enemy rather than a vanilla Monster,
                        // so a clone or golem sent in against Madara or Kurama simply stood
                        // there and picked a zombie three hundred blocks away instead.
                        target -> target instanceof net.minecraft.world.entity.monster.Enemy
                                && !com.sekwah.narutomod.util.Faction.sameSide(this, target)));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 16.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        int growth = this.entityData.get(GROWTH);
        if (growth < GROWTH_TICKS) {
            this.entityData.set(GROWTH, growth + 1);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.COMPOSTER,
                        this.getX(), this.getY(), this.getZ(), 6, 1.2, 0.2, 1.2, 0.02);
            }
        }
    }

    // --- Being ridden -------------------------------------------------------------

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        // Only its summoner can drive it; anyone else is just cargo.
        return this.getFirstPassenger() instanceof Player rider && this.isOwnedBy(rider) ? rider : null;
    }

    @Override
    protected void tickRidden(Player rider, Vec3 travelVector) {
        super.tickRidden(rider, travelVector);
        // The golem turns to look wherever the rider is looking.
        this.setRot(rider.getYRot(), rider.getXRot() * 0.5f);
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
        this.yRotO = this.getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player rider, Vec3 travelVector) {
        return new Vec3(rider.xxa * 0.5f, 0.0, rider.zza);
    }

    @Override
    protected float getRiddenSpeed(Player rider) {
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.6f;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** Dissolves back into timber rather than dropping a corpse. */
    public void dissolve() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.COMPOSTER,
                    this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                    50, 1.0, 1.5, 1.0, 0.05);
        }
        this.ejectPassengers();
        this.discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Growth", this.entityData.get(GROWTH));
        this.getOwnerUUID().ifPresent(uuid -> tag.putUUID("OwnerUUID", uuid));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(GROWTH, tag.getInt("Growth"));
        if (tag.hasUUID("OwnerUUID")) {
            this.entityData.set(OWNER_UUID, Optional.of(tag.getUUID("OwnerUUID")));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
