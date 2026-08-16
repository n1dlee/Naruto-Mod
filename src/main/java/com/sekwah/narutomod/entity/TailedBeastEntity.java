package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.entity.goal.BijuHoverGoal;
import com.sekwah.narutomod.entity.goal.TailedBeastJutsuGoal;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.network.NetworkHooks;

/**
 * A tailed beast: the largest thing in this mod and the only fight built around not being
 * able to out-manoeuvre the enemy.
 *
 * Everything specific lives in {@link TailedBeastVariant}. Three consequences of that are
 * worth stating, because each is a bug this codebase has already produced once:
 *
 *  - the hitbox is derived from the synced variant in getDimensions and refreshed in
 *    onSyncedDataUpdated, because dimensions are computed per side and never sent;
 *  - attributes are registered once per EntityType, so per-variant values are written onto
 *    the instance, on spawn and again on load, while health is only topped up on spawn;
 *  - registerGoals runs from the Mob constructor before any variant exists, so no goal here
 *    branches on the variant at registration time - each one asks in canUse instead.
 *
 * These are hostile to players but not to each other and not to summons: a fight against a
 * tailed beast is between the beast and the ninja who found it.
 */
public class TailedBeastEntity extends PathfinderMob implements Enemy {

    private static final EntityDataAccessor<Byte> VARIANT =
            SynchedEntityData.defineId(TailedBeastEntity.class, EntityDataSerializers.BYTE);
    /** Rises as the beast is worn down; drives the aura and how often it throws a Bijudama. */
    /**
     * How far through a Bijudama charge the beast is, 0 to 1.
     *
     * Synced because the CAST is the part worth watching: the beast rears back, the sphere
     * forms at its mouth and then it drives its head forward to spit. The bomb used to appear
     * out of a perfectly still animal, which gave the biggest attack in the mod no tell at all
     * and no way to know it was coming.
     */
    private static final EntityDataAccessor<Float> BIJUDAMA_CHARGE =
            SynchedEntityData.defineId(TailedBeastEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Integer> RAGE =
            SynchedEntityData.defineId(TailedBeastEntity.class, EntityDataSerializers.INT);

    /** Health fractions at which the beast steps up a stage. */
    private static final float[] RAGE_THRESHOLDS = {0.72f, 0.45f, 0.20f};

    private static final int ROAR_INTERVAL = 180;

    private int roarCooldown = 60;
    /** Monotonic count of landed hits; never persisted, only compared within a fight. */
    private int hurtCount;

    public TailedBeastEntity(EntityType<TailedBeastEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 250;
        NinjaMobMovement.enableWaterWalking(this);
        // Nothing this size should be balking at a pond, and the two aquatic ones actively
        // belong in one. Set here rather than per variant because it is harmless either way.
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.LAVA, 8.0F);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, (byte) 0);
        this.entityData.define(RAGE, 0);
        this.entityData.define(BIJUDAMA_CHARGE, 0f);
    }

    public TailedBeastVariant getVariant() {
        return TailedBeastVariant.byId(this.entityData.get(VARIANT));
    }

    public void setVariant(TailedBeastVariant variant) {
        this.entityData.set(VARIANT, (byte) variant.ordinal());
        this.applyVariantAttributes(variant);
        this.refreshDimensions();
    }

    public int getRage() {
        return this.entityData.get(RAGE);
    }

    private void applyVariantAttributes(TailedBeastVariant variant) {
        this.setAttribute(Attributes.MAX_HEALTH, variant.getHealth());
        this.setAttribute(Attributes.ATTACK_DAMAGE, variant.getDamage());
        this.setAttribute(Attributes.MOVEMENT_SPEED, variant.getSpeed());
    }

    private void setAttribute(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    /** 0 when idle, climbing to 1 as the sphere finishes forming, then released. */
    public float getBijudamaCharge() {
        return this.entityData.get(BIJUDAMA_CHARGE);
    }

    public void setBijudamaCharge(float charge) {
        this.entityData.set(BIJUDAMA_CHARGE, Math.max(0f, Math.min(1f, charge)));
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
        TailedBeastVariant variant = this.getVariant();
        return EntityDimensions.scalable(variant.getWidth(), variant.getHeight());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TailedBeastJutsuGoal(this));
        this.goalSelector.addGoal(2, new BijuHoverGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.5D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // Not each other, and not the ninja's summons: a beast has no quarrel with a toad.
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class,
                10, true, false, target -> !(target instanceof TailedBeastEntity)));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 435.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 20.0D)
                // A tailed beast is not knocked back. Not reduced - not at all.
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        NinjaMobMovement.tickWaterWalk(this);
        this.updateRage();
        if (--this.roarCooldown <= 0 && this.getTarget() != null) {
            this.roarCooldown = ROAR_INTERVAL + this.random.nextInt(120);
            this.playSound(this.getVariant().getRoar(), 3.0f, 1.0f);
        }
    }

    /**
     * Rage is a health ladder, not a damage counter.
     *
     * Reading a running total of damage taken would make a beast that regenerates or gets
     * healed keep an escalation it no longer has the wounds for; the fraction of health left
     * is the thing the fight is actually about.
     */
    private void updateRage() {
        float fraction = this.getHealth() / this.getMaxHealth();
        int stage = 0;
        for (float threshold : RAGE_THRESHOLDS) {
            if (fraction <= threshold) {
                stage++;
            }
        }
        if (stage != this.getRage()) {
            this.entityData.set(RAGE, stage);
            this.playSound(this.getVariant().getRoar(), 4.0f, 0.85f);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                        this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                        12, this.getBbWidth(), this.getBbHeight() * 0.4, this.getBbWidth(), 0.0);
            }
        }
    }

    /** Beasts ignore each other and the ninja's summons entirely. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof TailedBeastEntity) {
            return false;
        }
        boolean landed = super.hurt(source, amount);
        if (landed) {
            this.hurtCount++;
        }
        return landed;
    }

    /**
     * How many blows have actually landed on this beast.
     *
     * The Bijudama goal snapshots this when it starts gathering and compares each tick, which
     * is how hitting a beast mid-charge cancels the sphere. Vanilla's own hurt clocks are not
     * enough: getLastHurtByMobTimestamp ignores players entirely, and hurtTime is a countdown
     * that a second hit resets rather than advances.
     */
    public int getHurtCount() {
        return this.hurtCount;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return NarutoSounds.BIJU_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return NarutoSounds.BIJU_DEATH.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null; // the roar is driven from tick() so it can be loud and rare
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        // Deliberately routed through the level rather than Entity#playSound: these carry far
        // further than the default attenuation allows, which is most of the dread.
        //
        // Server side only. Level#playSound with a null player broadcasts from the server and
        // plays locally on the client, so letting both run would double every roar - and
        // LivingEntity calls this for its own hurt and death sounds.
        if (this.level().isClientSide || this.isSilent()) {
            return;
        }
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                sound, SoundSource.HOSTILE, volume, pitch);
    }

    /** Nothing this size should be shoved by an explosion either. */
    @Override
    public boolean ignoreExplosion() {
        return false;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    protected boolean isImmobile() {
        return this.isDeadOrDying();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // A beast is not jostled out of position by walking into it.
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false; // found once, still there when you come back with a plan
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Variant", this.entityData.get(VARIANT));
        tag.putInt("Rage", this.getRage());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(TailedBeastVariant.byId(tag.getByte("Variant")));
        this.entityData.set(RAGE, tag.getInt("Rage"));
    }

    @Override
    public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
                                        net.minecraft.world.DifficultyInstance difficulty,
                                        MobSpawnType spawnType,
                                        SpawnGroupData spawnData,
                                        CompoundTag tag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnData, tag);
        // Which of the eight this is gets decided here, for every way one can appear. Anything
        // that wants a specific beast calls setVariant afterwards and tops the health up again.
        this.setVariant(TailedBeastVariant.random(this.random));
        this.setHealth(this.getMaxHealth());
        this.setCustomName(net.minecraft.network.chat.Component.literal(this.getVariant().getDisplayName()));
        this.setCustomNameVisible(true);
        this.playSound(this.getVariant().getRoar(), 5.0f, 1.0f);
        return data;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
