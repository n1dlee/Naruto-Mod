package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.entity.goal.PuppetAttackGoal;
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
 * One of Sasori's puppets, on the strings of whoever summoned it.
 *
 * A puppet is not alive, and several things follow from that rather than being balance
 * choices: it does not drown, it takes no fall damage worth speaking of, poison does nothing
 * to it, and it never flees. What it does do is die permanently - there is no repair here, so
 * a puppet lost in a fight is lost.
 *
 * The variant drives stats, hitbox, model and role. As everywhere else in this mod: the
 * hitbox is derived from the synced variant because dimensions are per-side and never sent,
 * attributes are written onto the instance because they are registered per EntityType, and
 * no goal branches on the variant at registration time because registerGoals runs from the
 * Mob constructor before any variant exists.
 */
public class PuppetEntity extends PathfinderMob implements Enemy {

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(PuppetEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Byte> VARIANT =
            SynchedEntityData.defineId(PuppetEntity.class, EntityDataSerializers.BYTE);

    /**
     * Puppets summoned into a fight are on a clock. Chakra strings do not hold forever, and
     * without this a long fight would leave a field of abandoned puppets behind it.
     */
    private static final int LIFESPAN = 150 * 20;

    private int aliveTicks;

    public PuppetEntity(EntityType<PuppetEntity> entityType, Level level) {
        super(entityType, level);
        NinjaMobMovement.enableWaterWalking(this);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER, 0.0F);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(VARIANT, (byte) 0);
    }

    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(OWNER_UUID);
    }

    public void setOwner(LivingEntity owner) {
        this.entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
    }

    public PuppetVariant getVariant() {
        return PuppetVariant.byId(this.entityData.get(VARIANT));
    }

    public void setVariant(PuppetVariant variant) {
        this.entityData.set(VARIANT, (byte) variant.ordinal());
        this.applyVariantAttributes(variant);
        this.refreshDimensions();
    }

    private void applyVariantAttributes(PuppetVariant variant) {
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
        if (VARIANT.equals(key)) {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        PuppetVariant variant = this.getVariant();
        return EntityDimensions.scalable(variant.getWidth(), variant.getHeight());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PuppetAttackGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class,
                10, true, false, target -> !this.isPuppeteerOwned(target)));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class,
                10, true, false, target -> !(target instanceof PuppetEntity)));
    }

    /** The summoner and their other puppets. A collection does not fight itself. */
    private boolean isPuppeteerOwned(LivingEntity candidate) {
        UUID owner = this.getOwnerUUID().orElse(null);
        if (owner == null) {
            return false;
        }
        if (owner.equals(candidate.getUUID())) {
            return true;
        }
        return candidate instanceof PuppetEntity other
                && owner.equals(other.getOwnerUUID().orElse(null));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ATTACK_DAMAGE, 13.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        NinjaMobMovement.tickWaterWalk(this);
        if (++this.aliveTicks >= LIFESPAN) {
            this.collapse();
        }
    }

    /** The strings are cut: the puppet falls apart where it stands. */
    public void collapse() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                    40, 0.6, 0.8, 0.6, 0.03);
        }
        this.level().playSound(null, this.blockPosition(),
                SoundEvents.WOOD_BREAK, SoundSource.HOSTILE, 1.0f, 0.7f);
        this.discard();
    }

    // --- A puppet is not alive ------------------------------------------------------------

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        // Poison, wither and the rest are for bodies. Sasori's own weapon would otherwise
        // work on his own collection.
        net.minecraft.world.effect.MobEffect type = effect.getEffect();
        if (type == net.minecraft.world.effect.MobEffects.POISON
                || type == net.minecraft.world.effect.MobEffects.WITHER
                || type == net.minecraft.world.effect.MobEffects.HUNGER) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    protected boolean isAffectedByFluids() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    /** Never turns on its own puppeteer. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker && this.isPuppeteerOwned(attacker)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOOD_HIT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.WOOD_BREAK;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AliveTicks", this.aliveTicks);
        tag.putByte("Variant", this.entityData.get(VARIANT));
        this.getOwnerUUID().ifPresent(uuid -> tag.putUUID("OwnerUUID", uuid));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.aliveTicks = tag.getInt("AliveTicks");
        this.setVariant(PuppetVariant.byId(tag.getByte("Variant")));
        if (tag.hasUUID("OwnerUUID")) {
            this.entityData.set(OWNER_UUID, Optional.of(tag.getUUID("OwnerUUID")));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
