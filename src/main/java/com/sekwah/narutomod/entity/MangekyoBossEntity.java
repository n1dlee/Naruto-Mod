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

    private static final float MAX_CHAKRA = 400f;
    /**
     * Per tick. Sized so a wielder can sustain a technique roughly every 2-3 seconds: the
     * old 0.6 paid for a cast every seven seconds or so, which combined with the goal's
     * melee-range blind spot meant a boss you had actually closed with never cast at all.
     */
    private static final float CHAKRA_REGEN = 1.2f;
    /** Shell absorbs more the further the fight has gone; index = susanoo stage 0-3. */
    private static final float[] SUSANOO_ABSORB = {0f, 0.25f, 0.45f, 0.65f};

    private float chakra = MAX_CHAKRA;

    /** Set once a player actually trades blows, so a fight in progress can't despawn. */
    private boolean engagedByPlayer = false;

    public MangekyoBossEntity(EntityType<MangekyoBossEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 120;
    }

    /**
     * Always outlined. A boss that spawns naturally somewhere in a 128-block radius is
     * effectively impossible to find in wooded or hilly terrain, and the sighting message
     * is worthless without something to home in on. Overriding this instead of using
     * setGlowingTag keeps it true on both sides with no sync or NBT round-trip.
     */
    @Override
    public boolean isCurrentlyGlowing() {
        return true;
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
        this.equipSignatureWeapon(variant);
    }

    /**
     * Puts the wielder's own weapon in their hand. Vanilla applies the item's attack-damage
     * modifier to any mob holding it, so this is a real combat change as well as a visual
     * one — Madara's fan and Zabuza's cleaver add on top of the variant's base damage.
     *
     * The drop chance is deliberately low but non-zero: killing Zabuza and walking away with
     * Kubikiribocho is a memorable outcome, and it should stay rare enough to feel like one.
     */
    private void equipSignatureWeapon(MangekyoBossVariant variant) {
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                new net.minecraft.world.item.ItemStack(variant.weapon().get()));
        this.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.10f);
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
                .add(Attributes.MAX_HEALTH, 260.0D)
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

    /**
     * Picks which wielder this is and announces the sighting. Runs for natural spawns, so
     * the boss now arrives through the ordinary mob-spawning pipeline (rare weight in the
     * biome modifier) instead of a bespoke timer.
     */
    @javax.annotation.Nullable
    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.MobSpawnType reason,
            @javax.annotation.Nullable net.minecraft.world.entity.SpawnGroupData data,
            @javax.annotation.Nullable CompoundTag tag) {
        MangekyoBossVariant variant = MangekyoBossVariant.values()[
                level.getRandom().nextInt(MangekyoBossVariant.values().length)];
        this.applyVariant(variant);
        this.announceSighting(variant);
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    /** Everyone nearby should know an S-rank just walked into the region. */
    private void announceSighting(MangekyoBossVariant variant) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // Coordinates are handed over deliberately: without them a 160-block search radius
        // in forest or hills means the announcement just taunts the player.
        Component message = Component.translatable("mangekyo.boss.sighted",
                        Component.translatable(variant.translationKey())
                                .withStyle(net.minecraft.ChatFormatting.RED),
                        Component.literal(this.blockPosition().getX() + ", "
                                        + this.blockPosition().getY() + ", "
                                        + this.blockPosition().getZ())
                                .withStyle(net.minecraft.ChatFormatting.GOLD))
                .withStyle(net.minecraft.ChatFormatting.DARK_RED);
        for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
            if (player.blockPosition().closerThan(this.blockPosition(), 160)) {
                player.displayClientMessage(message, false);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player) {
            this.engagedByPlayer = true;
        }
        // The Susanoo tanks part of every blow, same idea as the player-side damage sponge.
        int stage = this.getSusanooStage();
        if (stage > 0 && !source.isCreativePlayer()) {
            amount *= (1f - SUSANOO_ABSORB[Math.min(stage, SUSANOO_ABSORB.length - 1)]);
        }
        return super.hurt(source, amount);
    }

    /**
     * Despawns like any other monster until a player actually engages it.
     *
     * These used to be flagged persistent forever, which quietly broke spawning: a boss the
     * player never found would sit in the world permanently, and once a couple had piled up
     * they filled the spawn cap and no further boss ever appeared. Letting unengaged ones
     * despawn keeps the rotation alive; once you've hit it, it stays and the fight is real.
     */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.engagedByPlayer;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Variant", this.getVariantId());
        tag.putByte("SusanooStage", (byte) this.getSusanooStage());
        tag.putFloat("BossChakra", this.chakra);
        tag.putBoolean("EngagedByPlayer", this.engagedByPlayer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(VARIANT, tag.getByte("Variant"));
        this.setSusanooStage(tag.getByte("SusanooStage"));
        this.chakra = tag.contains("BossChakra") ? tag.getFloat("BossChakra") : MAX_CHAKRA;
        this.engagedByPlayer = tag.getBoolean("EngagedByPlayer");
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
