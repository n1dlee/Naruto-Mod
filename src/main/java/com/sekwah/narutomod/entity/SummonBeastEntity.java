package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.entity.goal.NinjaLeapGoal;
import com.sekwah.narutomod.entity.goal.SummonBeastJutsuGoal;
import com.sekwah.narutomod.entity.goal.SummonFollowOwnerGoal;
import com.sekwah.narutomod.events.SummonRitualEvents;
import com.sekwah.narutomod.util.ContractSummonLedger;
import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.SummoningCircle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.Optional;
import java.util.UUID;

/**
 * Kuchiyose no Jutsu summon - one of the four named contract beasts (see
 * {@link SummonBeastVariant}), called for ninety seconds and then returned to its own realm.
 *
 * The variant drives everything: attributes, hitbox, model, texture and which signature
 * technique the beast brings. Two things about that are easy to get wrong and both were:
 *
 *  - the hitbox is computed per side and is NOT synced, so the client only learns the new
 *    size by recomputing it from the variant itself. That is why getDimensions() reads the
 *    synced variant rather than a field, and why onSyncedDataUpdated refreshes.
 *  - attributes are registered once for the EntityType, so per-variant values have to be
 *    written onto the instance. They are re-applied on load as well as on spawn, but health
 *    is only topped up on spawn - doing it on load would heal every summon on every reload.
 */
public class SummonBeastEntity extends PathfinderMob {

    private static final int LIFESPAN = 90 * 20;

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(SummonBeastEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Byte> VARIANT =
            SynchedEntityData.defineId(SummonBeastEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> PRESENCE =
            SynchedEntityData.defineId(SummonBeastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CUE =
            SynchedEntityData.defineId(SummonBeastEntity.class, EntityDataSerializers.INT);
    public static final int ARRIVAL = 13;
    public static final int WITHDRAW = 9;

    private int aliveTicks = 0;
    private boolean contracted;

    public SummonBeastEntity(EntityType<SummonBeastEntity> entityType, Level level) {
        super(entityType, level);
        NinjaMobMovement.enableWaterWalking(this);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(VARIANT, (byte) 0);
        this.entityData.define(PRESENCE, ARRIVAL);
        this.entityData.define(CUE, 0);
    }

    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(OWNER_UUID);
    }

    public void setOwner(LivingEntity owner) {
        this.entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
    }

    public void beginContract() {
        this.contracted = true;
        this.setPersistenceRequired();
        this.entityData.set(PRESENCE, 0);
        this.setNoAi(true);
    }

    public int presence() { return this.entityData.get(PRESENCE); }

    public boolean isDismissing() { return this.presence() < 0; }

    public boolean usesContractLedger() { return this.contracted; }

    public boolean canUseSignature() { return this.presence() >= ARRIVAL && !this.isNoAi(); }

    public int cue() { return this.entityData.get(CUE); }

    public void signal(int ticks) { this.entityData.set(CUE, ticks); }

    /** The summoner, if they are still loaded in this level. */
    public Player getOwner() {
        return this.getOwnerUUID().map(uuid -> this.level().getPlayerByUUID(uuid)).orElse(null);
    }

    public SummonBeastVariant getVariant() {
        return SummonBeastVariant.byId(this.entityData.get(VARIANT));
    }

    public void setVariant(SummonBeastVariant variant) {
        this.entityData.set(VARIANT, (byte) variant.ordinal());
        this.applyVariantAttributes(variant);
        this.refreshDimensions();
    }

    private void applyVariantAttributes(SummonBeastVariant variant) {
        this.setAttribute(Attributes.MAX_HEALTH, variant.getHealth());
        this.setAttribute(Attributes.ATTACK_DAMAGE, variant.getDamage());
        this.setAttribute(Attributes.MOVEMENT_SPEED, variant.getSpeed());
        this.setAttribute(Attributes.KNOCKBACK_RESISTANCE, variant.getKnockbackResistance());
    }

    private void setAttribute(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        // The client never runs setVariant, so this is the only place it hears that the
        // hitbox changed. Without it every contract would keep the registered default size.
        if (VARIANT.equals(key)) {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        SummonBeastVariant variant = this.getVariant();
        return EntityDimensions.scalable(variant.getWidth(), variant.getHeight());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SummonBeastJutsuGoal(this));
        // Both of these refuse for a support contract, and they have to decide that in
        // canUse rather than here: registerGoals runs from the Mob constructor, before any
        // variant has been set, so at this point every summon still looks like the default.
        //
        // Katsuyu is documented as having no melee role and was given the full melee and leap
        // kit regardless, so the healing slug charged into things and bit them.
        this.goalSelector.addGoal(2, new NinjaLeapGoal(this, 1.0D, 0.5D) {
            @Override
            public boolean canUse() {
                return !SummonBeastEntity.this.getVariant().isSupport() && super.canUse();
            }
        });
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1D, true) {
            @Override
            public boolean canUse() {
                return !SummonBeastEntity.this.getVariant().isSupport() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !SummonBeastEntity.this.getVariant().isSupport() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(4, new SummonFollowOwnerGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Shared sight. Summons called out together see through each other: when one of the
        // summoner's beasts finds something, the rest know without having to spot it
        // themselves.
        //
        // This is the Animal Path's actual hallmark - its summons share vision through the
        // Rinnegan - and it was the piece the mod's version was missing. It applies to every
        // summon rather than only that ability because a pack that ignores what its packmate
        // is fighting looks broken whoever called it.
        this.targetSelector.addGoal(0, new com.sekwah.narutomod.entity.goal.SharedSummonSightGoal(this));
        // Enemy rather than Monster: the Mangekyo bosses are PathfinderMobs that implement
        // Enemy, so a Monster-only filter would have summons stand and watch a boss fight.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class,
                10, true, false, target -> target instanceof Enemy && !this.isSummonerOwned(target)));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class,
                10, true, false, target -> !this.isSummonerOwned(target)));
    }

    /**
     * True for the summoner and for anything else that summoner has on the field.
     *
     * Delegates to the shared faction test. It used to recognise only other SummonBeastEntity,
     * so a toad would happily maul its owner's puppets, clones and wood golems.
     */
    private boolean isSummonerOwned(LivingEntity candidate) {
        return com.sekwah.narutomod.util.Faction.sameSide(this, candidate);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 220.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 22.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    public void tick() {
        if (this.contracted && this.level() instanceof ServerLevel level) {
            contractTick(level);
            if (this.isRemoved()) {
                return;
            }
        }
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        int cue = this.cue();
        if (cue > 0) {
            this.signal(cue - 1);
        } else if (cue < 0) {
            this.signal(cue + 1);
        }
        NinjaMobMovement.tickWaterWalk(this);
        if (!this.contracted && ++this.aliveTicks >= LIFESPAN) {
            this.dispel();
        }
    }

    private void contractTick(ServerLevel level) {
        int presence = this.presence();
        if (presence < 0) {
            if (presence <= -WITHDRAW) {
                this.discard();
            } else {
                this.entityData.set(PRESENCE, presence - 1);
            }
            return;
        }
        var ownerId = this.getOwnerUUID().orElse(null);
        var owner = this.getOwner();
        if (ownerId == null || !ContractSummonLedger.of(level).owns(ownerId, this.getUUID(), level.dimension().location(), ContractSummonLedger.time(level))
                || !this.isAlive() || owner != null && !owner.isAlive()) {
            this.dispel();
            return;
        }
        if (presence < ARRIVAL) {
            this.entityData.set(PRESENCE, presence + 1);
        }
        boolean idle = owner == null || owner.isSpectator() || owner.distanceToSqr(this) > 64 * 64 || presence < ARRIVAL;
        if (idle && !this.isNoAi()) {
            this.getNavigation().stop();
            this.setTarget(null);
            this.signal(0);
        }
        this.setNoAi(idle);
    }

    public void dispel() {
        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.contracted && this.isDismissing()) {
                return;
            }
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(),
                    29, 0.85, 0.87, 0.85, 0.05);
            if (this.contracted) {
                releaseContract(serverLevel);
                this.setNoAi(true);
                this.getNavigation().stop();
                this.setTarget(null);
                this.signal(0);
                this.entityData.set(PRESENCE, -1);
                if (this.getOwner() instanceof ServerPlayer owner) {
                    SummonRitualEvents.publish(owner, serverLevel, this.position(), this.getVariant().sealStyle(),
                            SummoningCircle.Stage.FADE, this.getVariant().getWidth());
                }
                return;
            }
        }
        this.discard();
    }

    private void releaseContract(ServerLevel level) {
        this.getOwnerUUID().ifPresent(owner -> ContractSummonLedger.of(level).release(owner, this.getUUID()));
    }

    /** A contract beast never turns on the ninja who called it. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker && this.isSummonerOwned(attacker)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity victim) {
        Player owner = this.getOwner();
        if (victim instanceof LivingEntity actor && this.level() instanceof ServerLevel level && owner != null
                && (!JutsuAim.attackable(owner, actor) || actor instanceof Player && !level.getServer().isPvpAllowed()
                || !level.mayInteract(owner, actor.blockPosition()))) {
            return false;
        }
        return super.doHurtTarget(victim);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AliveTicks", this.aliveTicks);
        tag.putByte("Variant", this.entityData.get(VARIANT));
        tag.putBoolean("Contracted", this.contracted);
        tag.putInt("Presence", this.presence());
        this.getOwnerUUID().ifPresent(uuid -> tag.putUUID("OwnerUUID", uuid));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.aliveTicks = tag.getInt("AliveTicks");
        this.contracted = tag.getBoolean("Contracted");
        if (this.contracted) {
            this.entityData.set(PRESENCE, tag.contains("Presence") ? tag.getInt("Presence") : ARRIVAL);
        }
        // Attributes and hitbox come back with the variant; health does not, so the saved
        // Health super already read stays exactly as it was.
        this.setVariant(SummonBeastVariant.byId(tag.getByte("Variant")));
        if (tag.hasUUID("OwnerUUID")) {
            this.entityData.set(OWNER_UUID, Optional.of(tag.getUUID("OwnerUUID")));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
