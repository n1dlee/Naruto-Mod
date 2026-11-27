package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.entity.goal.ShadowCloneJutsuGoal;
import com.sekwah.narutomod.events.ShadowCloneEffects;
import com.sekwah.narutomod.util.ShadowCloneFormation;
import com.sekwah.narutomod.util.ShadowCloneChakra;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.Optional;
import java.util.UUID;

public class ShadowCloneEntity extends PathfinderMob {

    private static final int LIFESPAN = 600; // 30 seconds

    /** Synced to client so the renderer can look up the player skin. */
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    /**
     * Which Mangekyo wielder's skin to wear, or -1 for a player's clone.
     *
     * The renderer resolves a player owner by UUID and falls back to Steve when it cannot -
     * which is every clone a boss makes, because a boss is not a player and never will be
     * found by getPlayerByUUID. Naruto's clones were therefore always Steve. Carrying the
     * variant on the clone is cheaper than searching the entity list every frame, and it is
     * synced, which the renderer needs.
     */
    private static final EntityDataAccessor<Byte> BOSS_VARIANT =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> FORMATION_BORN =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.LONG);

    private int aliveTicks = 0;
    private boolean dispelled = false;
    private boolean restoredVessel;
    private boolean replicaEquipment;
    private int ownerMissing;

    public ShadowCloneEntity(EntityType<? extends ShadowCloneEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(BOSS_VARIANT, (byte) -1);
        this.entityData.define(FORMATION_BORN, -1L);
    }

    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(OWNER_UUID);
    }

    /** Ordinal of the wielder whose skin this clone wears, or -1 when a player made it. */
    public byte getBossVariant() {
        return this.entityData.get(BOSS_VARIANT);
    }

    public void setBossVariant(MangekyoBossVariant variant) {
        this.entityData.set(BOSS_VARIANT, variant == null ? (byte) -1 : (byte) variant.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ShadowCloneJutsuGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.LivingEntity.class, 10, true, false,
                        // Enemy, not Monster. Every custom boss and tailed beast in this mod
                        // is a PathfinderMob implementing Enemy rather than a vanilla Monster,
                        // so a clone or golem sent in against Madara or Kurama simply stood
                        // there and picked a zombie three hundred blocks away instead.
                        target -> target instanceof net.minecraft.world.entity.monster.Enemy
                                && !com.sekwah.narutomod.util.Faction.sameSide(this, target)));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    public void setOwner(LivingEntity owner) {
        this.entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
    }

    /**
     * Turns a clone against players, for the ones a boss makes rather than a player.
     *
     * The default target selector only looks for Monster, which is right for a clone fighting
     * at your side and useless for one Naruto just threw at you - it would stand there hunting
     * zombies while its owner fought you. Goal selectors are protected, so the switch has to
     * live on the entity; the vanilla monster-hunting goal is left in place so a boss clone
     * still defends itself if something else picks a fight.
     */
    public void makeHostileToPlayers(float health, float attackDamage) {
        this.targetSelector.addGoal(0,
                new NearestAttackableTargetGoal<>(this, Player.class, true));
        var maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(health);
            this.setHealth(health);
        }
        var damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(attackDamage);
        }
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel serverLevel && isPlayerFormation() && retirePlayerFormation(serverLevel)) {
            return;
        }
        super.tick();
        if (!level().isClientSide) {
            if (++aliveTicks >= LIFESPAN) {
                poof();
                discard();
            }
        }
    }

    public void configurePlayerFormation(long now) {
        this.entityData.set(FORMATION_BORN, now);
        this.replicaEquipment = true;
        this.xpReward = 0;
    }

    public ServerPlayer playerOwner() {
        if (!(level() instanceof ServerLevel level) || !isPlayerFormation()) {
            return null;
        }
        return getOwnerUUID().map(level.getServer().getPlayerList()::getPlayer).orElse(null);
    }

    @Override
    public boolean isAlliedTo(Entity target) {
        var owner = playerOwner();
        return super.isAlliedTo(target) || isPlayerFormation()
                && (target instanceof Player || owner != null && owner.isAlliedTo(target));
    }

    public boolean isPlayerFormation() {
        return formationBorn() >= 0;
    }

    public long formationBorn() {
        return this.entityData.get(FORMATION_BORN);
    }

    public boolean playerFormationExpired(long now) {
        return now - formationBorn() >= ShadowCloneFormation.LIFETIME;
    }

    private boolean retirePlayerFormation(ServerLevel level) {
        var owner = playerOwner();
        if (owner == null) {
            if (++this.ownerMissing <= 40) {
                return false;
            }
        } else if (owner.level() == level && owner.isAlive()) {
            this.ownerMissing = 0;
            if (!playerFormationExpired(level.getGameTime())) {
                return false;
            }
        }
        poof();
        discard();
        return true;
    }

    @Override
    protected void dropEquipment() {
        if (!this.replicaEquipment) {
            super.dropEquipment();
        }
    }

    public void markRestoredVessel() {
        this.restoredVessel = true;
        this.aliveTicks = LIFESPAN - 200;
    }

    public boolean isRestorationVessel() { return this.restoredVessel; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (!level().isClientSide && !isAlive()) {
            poof();
        }
        return result;
    }

    private void poof() {
        if (this.dispelled) {
            return;
        }
        this.dispelled = true;
        if (level() instanceof ServerLevel serverLevel) {
            if (isPlayerFormation()) {
                ShadowCloneEffects.dispel(serverLevel, this);
                ShadowCloneChakra.of(serverLevel.getServer()).release(getUUID());
            } else {
                serverLevel.sendParticles(ParticleTypes.CLOUD, getX(), getY() + getBbHeight() / 2, getZ(),
                        30, 0.3, 0.4, 0.3, 0.05);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AliveTicks", aliveTicks);
        tag.putByte("BossVariant", this.getBossVariant());
        this.getOwnerUUID().ifPresent(uuid -> tag.putUUID("OwnerUUID", uuid));
        tag.putBoolean("NarakaVessel", this.restoredVessel);
        tag.putBoolean("CloneDispelled", this.dispelled);
        tag.putBoolean("ReplicaEquipment", this.replicaEquipment);
        if (isPlayerFormation()) {
            tag.putLong("FormationBorn", formationBorn());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        aliveTicks = tag.getInt("AliveTicks");
        this.restoredVessel = tag.getBoolean("NarakaVessel");
        this.dispelled = tag.getBoolean("CloneDispelled");
        this.entityData.set(FORMATION_BORN, tag.contains("FormationBorn") ? tag.getLong("FormationBorn") : -1L);
        this.replicaEquipment = tag.getBoolean("ReplicaEquipment") || isPlayerFormation();
        if (this.replicaEquipment) {
            this.xpReward = 0;
        }
        this.entityData.set(BOSS_VARIANT, tag.contains("BossVariant") ? tag.getByte("BossVariant") : (byte) -1);
        if (tag.hasUUID("OwnerUUID")) {
            this.entityData.set(OWNER_UUID, Optional.of(tag.getUUID("OwnerUUID")));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket entityPacket) {
        super.recreateFromPacket(entityPacket);
        this.setYBodyRot(entityPacket.getYRot());
    }
}
