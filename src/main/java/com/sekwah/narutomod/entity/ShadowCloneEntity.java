package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
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

    private int aliveTicks = 0;
    private boolean dispelled = false;

    public ShadowCloneEntity(EntityType<ShadowCloneEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(BOSS_VARIANT, (byte) -1);
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
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, true));
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
        super.tick();
        if (!level().isClientSide) {
            if (++aliveTicks >= LIFESPAN) {
                poof();
                discard();
            }
        }
    }

    /** Set when the clone is destroyed by its own owner — dispelling your own clones for
     *  chakra would otherwise be a free "cast 20 clones, punch them all" chakra pump. */
    private boolean killedByOwner = false;

    /** Minimum lifetime before a dispelling clone returns chakra — pairs with the
     *  owner-kill check above to close the summon-and-instantly-harvest loop. */
    private static final int MIN_TICKS_FOR_REFUND = 100;

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && source.getEntity() != null
                && this.getOwnerUUID().map(id -> id.equals(source.getEntity().getUUID())).orElse(false)) {
            this.killedByOwner = true;
        }
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
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    getX(), getY() + getBbHeight() / 2, getZ(),
                    30, 0.3, 0.4, 0.3, 0.05);

            // Canon: when a shadow clone dispels, everything it gathered flows back to the
            // original — translated here as a small chakra refund to the owner per clone.
            // No refund for clones the owner destroyed themselves or that barely existed,
            // so the mechanic rewards clones that actually fought, not chakra farming.
            if (this.killedByOwner || this.aliveTicks < MIN_TICKS_FOR_REFUND) {
                return;
            }
            this.getOwnerUUID().ifPresent(ownerId -> {
                if (serverLevel.getEntity(ownerId) instanceof Player owner && owner.isAlive()) {
                    owner.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                            .ifPresent(ownerData -> ownerData.addChakra(8f));
                }
            });
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AliveTicks", aliveTicks);
        tag.putByte("BossVariant", this.getBossVariant());
        this.getOwnerUUID().ifPresent(uuid -> tag.putUUID("OwnerUUID", uuid));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        aliveTicks = tag.getInt("AliveTicks");
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
