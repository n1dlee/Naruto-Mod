package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * A rogue ninja — the rank-and-file missing-nin a player can actually grind against.
 *
 * The Mangekyo bosses are rare set-pieces; this is the everyday alternative to punching
 * zombies, and it pays out in ninja terms: chakra XP through the normal combat hooks in
 * PlayerEvents, plus its own gear as loot. Five village variants share one entity and are
 * told apart by a synced byte, the same pattern SummonBeastEntity and MangekyoBossEntity
 * already use.
 */
public class RogueNinjaEntity extends Monster {

    private static final EntityDataAccessor<Byte> VARIANT =
            SynchedEntityData.defineId(RogueNinjaEntity.class, EntityDataSerializers.BYTE);

    /** Skins carried over from the 1.12.2 mod's generic village ninja. */
    public static final String[] VARIANT_TEXTURES = {
            "ninja_konoha", "ninja_iwa", "ninja_kiri", "ninja_kumo", "ninja_suna"
    };

    public RogueNinjaEntity(EntityType<? extends RogueNinjaEntity> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, (byte) 0);
    }

    public byte getVariant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(byte variant) {
        this.entityData.set(VARIANT, (byte) Math.floorMod(variant, VARIANT_TEXTURES.length));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        // Tougher than a zombie so the fight is worth having, but nowhere near a boss.
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        RandomSource random = level.getRandom();
        this.setVariant((byte) random.nextInt(VARIANT_TEXTURES.length));
        // Armed like a real ninja — this is also what makes them worth farming.
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                new ItemStack(random.nextInt(4) == 0 ? NarutoItems.KATANA.get() : NarutoItems.KUNAI.get()));
        this.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.12f);
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        RandomSource random = this.random;
        // Ninja tools rather than rotten flesh — the reason to hunt these over vanilla mobs.
        if (random.nextFloat() < 0.55f) {
            this.spawnAtLocation(new ItemStack(NarutoItems.KUNAI.get(), 1 + random.nextInt(2 + looting)));
        }
        if (random.nextFloat() < 0.35f) {
            this.spawnAtLocation(new ItemStack(NarutoItems.SHURIKEN.get(), 1 + random.nextInt(2 + looting)));
        }
        if (random.nextFloat() < 0.15f) {
            this.spawnAtLocation(new ItemStack(NarutoItems.CHAKRA_PAPER.get()));
        }
        if (random.nextFloat() < 0.05f) {
            this.spawnAtLocation(new ItemStack(NarutoItems.SOLDIER_PILL.get()));
        }
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Variant", this.getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(tag.getByte("Variant"));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
