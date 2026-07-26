package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.entity.goal.BossJutsuGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/**
 * An S-rank Mangekyo wielder roaming the world — Itachi, Sasuke, Madara, Shisui or
 * Obito, told apart by a variant byte (see {@link MangekyoBossVariant}).
 *
 * Unlike the player, a boss has no ninja capability, so its chakra reserve is a plain
 * field on the entity: it regenerates slowly and gates how often the AI can throw a
 * technique. Its Susanoo is likewise entity-local state, synced as a byte so
 * MangekyoBossRenderer can draw the shell in that wielder's canon colour.
 *
 * Killing one hands its Mangekyo to the killer — see PlayerEvents.onMangekyoBossKill.
 */
public class MangekyoBossEntity extends PathfinderMob implements Enemy {

    private static final EntityDataAccessor<Byte> VARIANT =
            SynchedEntityData.defineId(MangekyoBossEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> SUSANOO_STAGE =
            SynchedEntityData.defineId(MangekyoBossEntity.class, EntityDataSerializers.BYTE);

    private static final float MAX_CHAKRA = 300f;
    private static final float CHAKRA_REGEN = 0.6f; // per tick — a boss casts often
    /** Shell absorbs more the further the fight has gone; index = susanoo stage 0-3. */
    private static final float[] SUSANOO_ABSORB = {0f, 0.25f, 0.45f, 0.65f};

    private float chakra = MAX_CHAKRA;

    public MangekyoBossEntity(EntityType<MangekyoBossEntity> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.xpReward = 120;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, (byte) 0);
        this.entityData.define(SUSANOO_STAGE, (byte) 0);
    }

    public byte getVariantId() {
        return this.entityData.get(VARIANT);
    }

    public MangekyoBossVariant getVariant() {
        return MangekyoBossVariant.byId(this.getVariantId());
    }

    /**
     * Applies a wielder's identity: variant byte, their own health/damage/speed, and the
     * display name. Call right after creating the entity and before adding it to the level.
     */
    public void applyVariant(MangekyoBossVariant variant) {
        this.entityData.set(VARIANT, (byte) variant.ordinal());
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(variant.maxHealth());
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(variant.attackDamage());
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(variant.movementSpeed());
        this.setHealth(variant.maxHealth());
        this.setCustomName(Component.translatable(variant.translationKey()));
    }

    public int getSusanooStage() {
        return this.entityData.get(SUSANOO_STAGE);
    }

    public void setSusanooStage(int stage) {
        this.entityData.set(SUSANOO_STAGE, (byte) Math.min(Math.max(stage, 0), 3));
    }

    public float getChakra() {
        return this.chakra;
    }

    /** Spends chakra if the reserve covers it; returns false when the boss is drained. */
    public boolean useChakra(float amount) {
        if (this.chakra < amount) {
            return false;
        }
        this.chakra -= amount;
        return true;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BossJutsuGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        // Overwritten per wielder in applyVariant — these are the shared baseline.
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 180.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.chakra < MAX_CHAKRA) {
            this.chakra = Math.min(MAX_CHAKRA, this.chakra + CHAKRA_REGEN);
        }
        // The shell rises as the fight turns against them, exactly like a player's Susanoo
        // ramping with the power meter — here it tracks missing health instead.
        if (!this.getVariant().hasSusanoo()) {
            return; // missing-nin have no Sharingan, so no spectral armour
        }
        float healthFraction = this.getHealth() / this.getMaxHealth();
        int stage = healthFraction > 0.75f ? 0
                : healthFraction > 0.5f ? 1
                : healthFraction > 0.25f ? 2
                : 3;
        if (stage != this.getSusanooStage()) {
            this.setSusanooStage(stage);
            if (stage > 0 && this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(),
                        30, 0.6, 0.9, 0.6, 0.03);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // The Susanoo tanks part of every blow, same idea as the player-side damage sponge.
        int stage = this.getSusanooStage();
        if (stage > 0 && !source.isCreativePlayer()) {
            amount *= (1f - SUSANOO_ABSORB[Math.min(stage, SUSANOO_ABSORB.length - 1)]);
        }
        return super.hurt(source, amount);
    }

    /** Bosses are hunted deliberately — never let them wander off and vanish. */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Variant", this.getVariantId());
        tag.putByte("SusanooStage", (byte) this.getSusanooStage());
        tag.putFloat("BossChakra", this.chakra);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(VARIANT, tag.getByte("Variant"));
        this.setSusanooStage(tag.getByte("SusanooStage"));
        this.chakra = tag.contains("BossChakra") ? tag.getFloat("BossChakra") : MAX_CHAKRA;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
