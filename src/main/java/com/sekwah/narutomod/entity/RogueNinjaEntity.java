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
    private static final EntityDataAccessor<Byte> ELEMENT =
            SynchedEntityData.defineId(RogueNinjaEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> RANK =
            SynchedEntityData.defineId(RogueNinjaEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> CLAN =
            SynchedEntityData.defineId(RogueNinjaEntity.class, EntityDataSerializers.BYTE);

    /**
     * Rank uses the same scale the player is measured on, so a Jonin missing-nin is a Jonin
     * in exactly the sense the player understands. Only these two appear in the wild:
     * Genin are not worth deserting over, and a Kage-level defector is a boss, not a mob.
     */
    public static final int RANK_CHUNIN = 2;
    public static final int RANK_JONIN = 3;

    /**
     * Bloodlines that show up among missing-nin. NONE is the common case - most rogues are
     * ordinary ninja - and each of the others brings the technique that clan is known for
     * (see RogueNinjaClanGoal).
     */
    public static final String[] CLANS = {"none", "hyuga", "nara", "akimichi"};
    public static final int CLAN_NONE = 0;
    public static final int CLAN_HYUGA = 1;
    public static final int CLAN_NARA = 2;
    public static final int CLAN_AKIMICHI = 3;

    /** Skins carried over from the 1.12.2 mod's generic village ninja. */
    public static final String[] VARIANT_TEXTURES = {
            "ninja_konoha", "ninja_iwa", "ninja_kiri", "ninja_kumo", "ninja_suna"
    };

    /**
     * One nature each, rolled at spawn. A rank-and-file ninja never had the years or the
     * talent for a second - that is what separates them from the player, who collects
     * natures, and from the S-rank bosses, who have signature techniques instead.
     */
    public static final String[] ELEMENTS = {"fire", "water", "earth", "wind", "lightning"};

    /**
     * Dirt raised by an Earth Release volley, cleared on a timer so a world full of these
     * mobs doesn't slowly fill with abandoned pillars. Server-side only and deliberately
     * not persisted: if the chunk unloads mid-volley the blocks are ordinary dirt anyway.
     */
    private final java.util.List<net.minecraft.core.BlockPos> raisedSpikes = new java.util.ArrayList<>();
    private int spikeClearTimer;

    public RogueNinjaEntity(EntityType<? extends RogueNinjaEntity> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, (byte) 0);
        this.entityData.define(ELEMENT, (byte) 0);
        this.entityData.define(RANK, (byte) RANK_CHUNIN);
        this.entityData.define(CLAN, (byte) CLAN_NONE);
    }

    public int getNinjaRank() {
        return Math.min(Math.max(this.entityData.get(RANK), RANK_CHUNIN), RANK_JONIN);
    }

    /**
     * Sets the rank and scales the body to match. Attributes are written here rather than
     * in createAttributes because a Jonin has to be measurably tougher than a Chunin, and
     * the registry only gets one shared baseline.
     */
    public void setNinjaRank(int rank) {
        int clamped = Math.min(Math.max(rank, RANK_CHUNIN), RANK_JONIN);
        this.entityData.set(RANK, (byte) clamped);
        boolean jonin = clamped >= RANK_JONIN;
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(jonin ? 60.0D : 34.0D);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(jonin ? 9.0D : 5.5D);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(jonin ? 0.34D : 0.3D);
        this.setHealth(this.getMaxHealth());
        this.xpReward = jonin ? 18 : 10;
    }

    public int getClanId() {
        return Math.floorMod(this.entityData.get(CLAN), CLANS.length);
    }

    public String getClanName() {
        return CLANS[this.getClanId()];
    }

    public void setClanId(int clan) {
        this.entityData.set(CLAN, (byte) Math.floorMod(clan, CLANS.length));
    }

    public byte getVariant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(byte variant) {
        this.entityData.set(VARIANT, (byte) Math.floorMod(variant, VARIANT_TEXTURES.length));
    }

    /** Index into {@link #ELEMENTS}. */
    public byte getElementId() {
        return this.entityData.get(ELEMENT);
    }

    public String getElement() {
        return ELEMENTS[Math.floorMod(this.getElementId(), ELEMENTS.length)];
    }

    public void setElementId(byte element) {
        this.entityData.set(ELEMENT, (byte) Math.floorMod(element, ELEMENTS.length));
    }

    /**
     * Raises a run of dirt and remembers it so it can be taken back down again. Calling
     * this while an earlier volley is still standing clears that one first, so a single
     * ninja can never leave more than one volley of terrain behind.
     */
    public void trackRaisedSpike(net.minecraft.core.BlockPos pos, int lifespanTicks) {
        if (this.spikeClearTimer <= 0 && !this.raisedSpikes.isEmpty()) {
            this.clearRaisedSpikes();
        }
        this.raisedSpikes.add(pos);
        this.spikeClearTimer = lifespanTicks;
    }

    private void clearRaisedSpikes() {
        if (this.level() instanceof ServerLevel serverLevel) {
            for (net.minecraft.core.BlockPos pos : this.raisedSpikes) {
                if (serverLevel.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.DIRT)) {
                    serverLevel.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
            }
        }
        this.raisedSpikes.clear();
        this.spikeClearTimer = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.spikeClearTimer > 0 && --this.spikeClearTimer <= 0) {
            this.clearRaisedSpikes();
        }
    }

    /** Terrain must not outlive the ninja who raised it. */
    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && !this.raisedSpikes.isEmpty()) {
            this.clearRaisedSpikes();
        }
        super.remove(reason);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Above melee: at range they open with ninjutsu and only close in once it is on
        // cooldown, which is what makes them read as ninja rather than as armed zombies.
        this.goalSelector.addGoal(1, new com.sekwah.narutomod.entity.goal.RogueNinjaJutsuGoal(this));
        // Above the elemental jutsu: a bloodline technique is what that clan reaches for
        // first. Self-disables for clanless rogues, so it costs them nothing.
        this.goalSelector.addGoal(1, new com.sekwah.narutomod.entity.goal.RogueNinjaClanGoal(this));
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
        this.setElementId((byte) random.nextInt(ELEMENTS.length));
        // Jonin are the minority: most people who desert are competent, not exceptional.
        this.setNinjaRank(random.nextInt(4) == 0 ? RANK_JONIN : RANK_CHUNIN);
        this.rollClan(random);
        // Armed like a real ninja — this is also what makes them worth farming.
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                new ItemStack(random.nextInt(4) == 0 ? NarutoItems.KATANA.get() : NarutoItems.KUNAI.get()));
        this.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.12f);
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    /**
     * Most rogues are clanless. A bloodline is meant to be a nasty surprise, so roughly one
     * in four carries one - common enough to meet, rare enough to notice.
     */
    protected void rollClan(RandomSource random) {
        if (random.nextInt(4) != 0) {
            this.setClanId(CLAN_NONE);
            return;
        }
        this.setClanId(1 + random.nextInt(CLANS.length - 1));
        applyClanIdentity();
    }

    /**
     * Names a clan rogue after their bloodline. There is no separate skin per clan - these
     * are generic village ninja - so the nameplate is what tells the player which technique
     * is about to be used on them, which they need before it lands rather than after.
     */
    protected void applyClanIdentity() {
        int clan = this.getClanId();
        if (clan == CLAN_NONE) {
            return;
        }
        this.setCustomName(net.minecraft.network.chat.Component.translatable(
                "entity.narutomod.rogue_ninja." + CLANS[clan]));
        // Bloodline users are trained fighters: never below Jonin.
        if (this.getNinjaRank() < RANK_JONIN) {
            this.setNinjaRank(RANK_JONIN);
        }
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
        tag.putByte("Element", this.getElementId());
        tag.putByte("NinjaRank", (byte) this.getNinjaRank());
        tag.putByte("RogueClan", (byte) this.getClanId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(tag.getByte("Variant"));
        this.setElementId(tag.getByte("Element"));
        // Rogues saved before ranks existed have no key; Chunin is the right default for
        // them, and setNinjaRank re-derives the attribute scaling either way.
        this.setNinjaRank(tag.contains("NinjaRank") ? tag.getByte("NinjaRank") : RANK_CHUNIN);
        this.setClanId(tag.getByte("RogueClan"));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
