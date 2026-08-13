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
     * Per tick. The old 0.6 paid for a cast every seven seconds or so, which combined with
     * the goal's melee-range blind spot meant a boss you had actually closed with never
     * cast at all.
     *
     * 0.9 rather than a flat 1.2: at 1.2 the reserve refilled faster than any cooldown ran
     * down, so chakra stopped being a constraint and the expensive finishers — Kirin,
     * Kotoamatsukami, Tsukuyomi — came out as freely as a fireball. Now the cheap techniques
     * are still effectively unlimited and the expensive ones genuinely cost a pause.
     */
    private static final float CHAKRA_REGEN = 0.9f;
    /** Shell absorbs more the further the fight has gone; index = susanoo stage 0-4. */
    private static final float[] SUSANOO_ABSORB = {0f, 0.25f, 0.45f, 0.65f, 0.85f};

    /** How tall the Complete Body stands. Shared with every renderer that draws a final form. */
    private static final float COMPLETE_BODY_HEIGHT = com.sekwah.narutomod.util.GiantForm.HEIGHT_BLOCKS;
    /**
     * Clear blocks needed overhead before the giant can rise.
     *
     * Without this the boss would try to grow to thirteen blocks inside a cave and spend the
     * rest of the fight embedded in stone. Checked only on the way in - once it is standing,
     * walking under an overhang must not shrink it back, or it would flicker between forms.
     */
    private static final int COMPLETE_BODY_HEADROOM = com.sekwah.narutomod.util.GiantForm.HEADROOM;
    /** Standing army ceiling for a boss's Shadow Clones. */
    private static final int MAX_BOSS_CLONES = 8;
    private static final float BOSS_CLONE_HEALTH = 24f;
    /** A clone hits for a fraction of its summoner - dangerous in a group, not alone. */
    private static final float BOSS_CLONE_DAMAGE_SHARE = 0.55f;

    /** Ticks between crush pulses once the giant is up. */
    private static final int CRUSH_INTERVAL = 10;
    private static final double CRUSH_RADIUS = 7.0;

    // Obito's reactive Kamui window - see absorbWithKamui.
    private static final float KAMUI_PHASE_COST = 30f;
    private static final int KAMUI_PHASE_TICKS = 22;
    private static final int KAMUI_PHASE_COOLDOWN = 90;
    private static final float KAMUI_PHASE_CHANCE = 0.45f;

    /**
     * Hashirama's regeneration, fired once each time he drops past a threshold.
     *
     * He is the only boss with 420 health and no Susanoo, which made "the strongest shinobi
     * who ever lived" the easiest of the heavyweights: Madara's shell turns 320 health into
     * roughly 717 effective, while Hashirama's 420 was simply 420. Rather than inflate his
     * health bar, he gets the thing he was actually famous for - he does not stay wounded.
     * Three guaranteed heals bring him to about 700 effective, level with his rival.
     */
    private static final float[] SENJU_HEAL_THRESHOLDS = {0.50f, 0.30f, 0.15f};
    private static final float SENJU_HEAL_FRACTION = 0.22f;

    private float chakra = MAX_CHAKRA;
    private int phaseTicks = 0;
    private int phaseCooldown = 0;
    private int senjuHealsUsed = 0;
    /** Kakashi opens his Mangekyo partway through; before that he cannot phase. */
    private boolean kamuiUnlocked = false;

    /** True while this wielder is standing as its thirteen-block final form. */
    public boolean isGiant() {
        return this.getSusanooStage() >= 4 && this.getVariant().hasGiantForm();
    }

    /** Set once a player actually trades blows, so a fight in progress can't despawn. */
    private boolean engagedByPlayer = false;

    public MangekyoBossEntity(EntityType<MangekyoBossEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 120;
        NinjaMobMovement.enableWaterWalking(this);
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

    /**
     * The stage byte changes the entity's size, and the client has to be told so.
     *
     * Dimensions are computed locally on each side, not synced - so without this the server
     * would know the giant is thirteen blocks tall while the client still thought it was two.
     * Every attack you aimed at the avatar's chest would ray-trace through empty air and
     * miss, which is the exact failure this whole change exists to avoid.
     */
    @Override
    public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> key) {
        if (SUSANOO_STAGE.equals(key)) {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
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
        var weapon = variant.weapon();
        if (weapon == null) {
            // Hashirama, Nagato and Hinata fight empty-handed on purpose - see weapon().
            this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                    net.minecraft.world.item.ItemStack.EMPTY);
            return;
        }
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                new net.minecraft.world.item.ItemStack(weapon.get()));
        this.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.10f);
    }

    public int getSusanooStage() {
        return this.entityData.get(SUSANOO_STAGE);
    }

    public void setSusanooStage(int stage) {
        int clamped = Math.min(Math.max(stage, 0), 4);
        if (clamped == this.getSusanooStage()) {
            return;
        }
        this.entityData.set(SUSANOO_STAGE, (byte) clamped);
        // Stage 4 is the only one that changes the entity's actual size, but refreshing
        // unconditionally keeps the box correct when a boss is restored from NBT mid-fight.
        this.refreshDimensions();
    }

    /**
     * Complete Body is a thirteen-block avatar, so the hitbox has to become one too.
     *
     * Leaving the box at human size was the reason bosses were capped at stage 3 in the
     * first place: you would swing at the giant's leg and connect with nothing, because the
     * only thing you could actually hit was the man standing between its feet. Growing the
     * box means the silhouette you can see is the silhouette you can hit.
     */
    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        net.minecraft.world.entity.EntityDimensions base = super.getDimensions(pose);
        if (this.isGiant() && base.height > 0.01f) {
            return base.scale(COMPLETE_BODY_HEIGHT / base.height);
        }
        return base;
    }

    /**
     * A hitbox that size will clip terrain constantly. The wielder is sealed inside a shell
     * of chakra, so being pressed against a hillside is not what kills them.
     */
    @Override
    public boolean isInWall() {
        return !this.isGiant() && super.isInWall();
    }

    /** Nothing staggers the Complete Body, same rule the player's stage 4 plays by. */
    @Override
    public void knockback(double strength, double x, double z) {
        if (this.isGiant()) {
            return;
        }
        super.knockback(strength, x, z);
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
        // Above the melee chase and holding the same MOVE flag, so it can genuinely pull the
        // boss back out to throwing distance instead of being overruled by it every tick.
        this.goalSelector.addGoal(2, new com.sekwah.narutomod.entity.goal.BossRepositionGoal(this));
        // Between the stand-off and the chase: closing a gap is a bound, not a jog.
        this.goalSelector.addGoal(2,
                new com.sekwah.narutomod.entity.goal.NinjaLeapGoal(this, 1.25D, 0.6D));
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
        NinjaMobMovement.tickWaterWalk(this);
        if (this.chakra < MAX_CHAKRA) {
            this.chakra = Math.min(MAX_CHAKRA, this.chakra + CHAKRA_REGEN);
        }
        if (this.phaseTicks > 0) {
            this.phaseTicks--;
            if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                        4, 0.3, 0.6, 0.3, 0.05);
            }
        }
        if (this.phaseCooldown > 0) {
            this.phaseCooldown--;
        }
        if (this.getVariant() == MangekyoBossVariant.HASHIRAMA) {
            this.tickSenjuRegeneration();
        }
        // The shell rises as the fight turns against them, exactly like a player's Susanoo
        // ramping with the power meter - here it tracks missing health instead. The Uchiha
        // grow a Susanoo, Naruto grows the fox; the missing-nin have neither and fight flat.
        if (!this.getVariant().transforms()) {
            return;
        }
        this.tickTransformation();
    }

    /**
     * The escalation ladder every transforming boss shares.
     *
     * The stage byte was written for Susanoo, but it is really just "how far into the fight
     * is this wielder" - so Naruto rides the same four steps with the fox instead of the
     * shell, and the giant at the end reuses the Complete Body machinery wholesale. Keeping
     * one field means the hitbox growth, the NBT, the client refresh and the render layers
     * all stay in step instead of being reimplemented per character.
     */
    private void tickTransformation() {
        float healthFraction = this.getHealth() / this.getMaxHealth();
        int stage = healthFraction > 0.75f ? 0
                : healthFraction > 0.5f ? 1
                : healthFraction > 0.28f ? 2
                : healthFraction > 0.12f ? 3
                : 4;
        int current = this.getSusanooStage();
        // Only the transition INTO the giant is gated on space. Once it is standing, walking
        // under a ledge must not fold it back down - that would flicker the hitbox every
        // few ticks and make the fight unreadable.
        if (stage >= 4 && current < 4 && this.getVariant().hasGiantForm()
                && !hasRoomForCompleteBody()) {
            stage = 3;
        }
        if (stage != current) {
            this.setSusanooStage(stage);
            if (stage > 0 && this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(),
                        30, 0.6, 0.9, 0.6, 0.03);
            }
            this.onStageEntered(stage);
        }

        if (this.isGiant() && this.tickCount % CRUSH_INTERVAL == 0) {
            crushUnderfoot();
        }
    }

    /**
     * Obito phasing out of a blow that had already landed.
     *
     * The scripted rotation could raise intangibility, but only on its own timer, which is
     * the opposite of how the technique works: Obito is not periodically untouchable, he is
     * untouchable exactly when you swing at him. Reacting to incoming damage instead of to a
     * cooldown is the whole character, and it is what "using Kamui properly" means for him.
     *
     * Costs chakra, has its own cooldown and does not fire every time - otherwise he would
     * simply be immortal rather than maddening.
     */
    private boolean absorbWithKamui(DamageSource source) {
        // Obito always; Kakashi only once his own Mangekyo has opened at stage 3. He spent
        // most of the war with a Sharingan he could not do this with, and the fight should
        // change when that stops being true rather than starting there.
        boolean canPhase = this.getVariant() == MangekyoBossVariant.OBITO
                || (this.getVariant() == MangekyoBossVariant.KAKASHI && this.kamuiUnlocked);
        if (!canPhase || source.isCreativePlayer()) {
            return false;
        }
        if (this.phaseTicks > 0) {
            return true; // already intangible; the whole flurry passes through
        }
        if (this.phaseCooldown > 0 || this.chakra < KAMUI_PHASE_COST
                || this.random.nextFloat() > KAMUI_PHASE_CHANCE) {
            return false;
        }
        this.chakra -= KAMUI_PHASE_COST;
        this.phaseTicks = KAMUI_PHASE_TICKS;
        this.phaseCooldown = KAMUI_PHASE_COOLDOWN;
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                    40, 0.4, 0.8, 0.4, 0.4);
            serverLevel.playSound(null, this.blockPosition(),
                    net.minecraft.sounds.SoundEvents.SHULKER_TELEPORT,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.2f, 0.7f);
        }
        return true;
    }

    /**
     * Heals Hashirama once per threshold crossed. Guaranteed rather than rolled: an ability
     * that only sometimes fires is not what made him unkillable, and leaving it to chance is
     * what left him softer than Madara despite the bigger health bar.
     */
    private void tickSenjuRegeneration() {
        if (this.senjuHealsUsed >= SENJU_HEAL_THRESHOLDS.length) {
            return;
        }
        float fraction = this.getHealth() / this.getMaxHealth();
        if (fraction > SENJU_HEAL_THRESHOLDS[this.senjuHealsUsed]) {
            return;
        }
        this.senjuHealsUsed++;
        this.heal(this.getMaxHealth() * SENJU_HEAL_FRACTION);
        this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.REGENERATION, 6 * 20, 1, false, true));
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.COMPOSTER,
                    this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                    50, 0.6, 1.0, 0.6, 0.05);
            serverLevel.playSound(null, this.blockPosition(),
                    net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.6f, 1.3f);
        }
    }

    /** Enough clear sky for a thirteen-block avatar to stand up in. */
    private boolean hasRoomForCompleteBody() {
        net.minecraft.core.BlockPos base = this.blockPosition();
        for (int dy = 2; dy <= COMPLETE_BODY_HEADROOM; dy++) {
            if (!this.level().getBlockState(base.above(dy)).isAir()) {
                return false;
            }
        }
        return true;
    }

    /**
     * At this size the wielder's own reach is irrelevant - the avatar itself is the weapon,
     * and anything standing in its footprint gets stamped on. Mirrors the crush aura the
     * player's Complete Body uses, for the same reason: a giant that can only hit what a
     * man could reach does not read as a giant.
     */
    private void crushUnderfoot() {
        net.minecraft.world.phys.AABB footprint = new net.minecraft.world.phys.AABB(
                this.getX() - CRUSH_RADIUS, this.getY() - 1, this.getZ() - CRUSH_RADIUS,
                this.getX() + CRUSH_RADIUS, this.getY() + 4, this.getZ() + CRUSH_RADIUS);
        float damage = this.getVariant().attackDamage() * 1.6f;
        for (net.minecraft.world.entity.LivingEntity caught : this.level().getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class, footprint,
                entity -> entity != this && entity.isAlive())) {
            caught.hurt(this.damageSources().mobAttack(this), damage);
            net.minecraft.world.phys.Vec3 push = caught.position().subtract(this.position())
                    .normalize().scale(1.1).add(0, 0.45, 0);
            caught.setDeltaMovement(caught.getDeltaMovement().add(push));
            caught.hurtMarked = true;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    this.getX(), this.getY() + 0.2, this.getZ(), 3, CRUSH_RADIUS * 0.5, 0.2,
                    CRUSH_RADIUS * 0.5, 0.0);
        }
    }

    /**
     * Everything that happens the moment a wielder steps up a stage: the buffs that make the
     * new form mean something, the announcement, and for Naruto the clones that come with it.
     *
     * Buffs are transient effects rather than permanent attribute edits so that a boss which
     * somehow drops back a stage does not keep them, and so a relog cannot stack them twice.
     */
    private void onStageEntered(int stage) {
        if (stage <= 0) {
            return;
        }
        MangekyoBossVariant variant = this.getVariant();

        // The baseline every wielder gets: a little faster and a little harder to put down
        // each time the fight turns. Deliberately mild - it is the floor under the signature
        // escalation below, not a replacement for it. Transient rather than an attribute edit
        // so a relog cannot stack two copies.
        this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 99999, (stage - 1) / 2, false, false));
        if (!variant.hasSusanoo()) {
            // Susanoo carriers already have the damage sponge; giving them this on top would
            // double-count the same idea.
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 99999,
                    Math.min(stage - 1, 2), false, false));
        }

        applySignatureEscalation(variant, stage);
        announceStage(variant, stage);
    }

    /**
     * What reaching a new stage actually means for this particular wielder.
     *
     * Each of these is the thing the character reaches for when they start losing, so the
     * fight has the same shape as the one in the show: a first gear that works, and then
     * something worse when it stops working.
     */
    private void applySignatureEscalation(MangekyoBossVariant variant, int stage) {
        int amplifier = stage - 1;
        switch (variant) {
            case NARUTO -> {
                // The fox lends chakra, and it shows.
                this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 99999, amplifier, false, false));
                if (stage >= 3) {
                    this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.REGENERATION, 99999, 1, false, false));
                }
                this.summonShadowClones(stage + 1);
            }
            // Sage Mode: senjutsu makes every strike land like a tree falling.
            case HASHIRAMA -> this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 99999, amplifier, false, false));
            // Nagato cycles the Paths: Preta soaks, then Asura hits, then Deva pushes.
            case NAGATO -> {
                if (stage >= 2) {
                    this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 99999, amplifier - 1, false, false));
                }
                if (stage >= 3) {
                    this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.REGENERATION, 99999, 0, false, false));
                }
            }
            // Kakashi opens the Mangekyo late, and from then on he phases like Obito.
            case KAKASHI -> {
                if (stage >= 3) {
                    this.kamuiUnlocked = true;
                }
                this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 99999, amplifier, false, false));
            }
            // The Byakugan does not make her stronger, it makes her impossible to miss with.
            case HINATA -> this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 99999, amplifier, false, false));
            // Shikamaru buys time rather than power - he gets harder to corner.
            case SHIKAMARU -> this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 99999, amplifier, false, false));
            // Samehada feeds him: the longer it goes, the faster he comes back.
            case KISAME -> this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.REGENERATION, 99999, Math.min(amplifier, 2), false, false));
            // Silent Killing: the mist thickens and he stops being visible in it.
            case ZABUZA -> {
                if (stage >= 3) {
                    this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.INVISIBILITY, 99999, 0, false, false));
                }
                this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 99999, amplifier, false, false));
            }
            // Hidan's ritual runs on his own blood - the worse it goes for him, the worse
            // it goes for you.
            case HIDAN -> this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 99999, stage, false, false));
            // Sasori does not fight with his hands. Each stage he reaches into the collection
            // and puts another puppet on the field - Karasu, then Sanshouo, then the Third
            // Kazekage, and finally the Hundred, which is where he stops being a person in
            // the fight at all. Without this he was a man throwing senbon, which is the one
            // thing the character is not.
            case SASORI -> this.summonPuppet(PuppetVariant.forSasoriStage(stage));
            // Kankuro fights the same way his teacher does, with a smaller collection.
            case KANKURO -> this.summonPuppet(stage >= 3
                    ? PuppetVariant.HUNDRED : PuppetVariant.KARASU);
            /*
             * Might Guy opens a gate each time he is pushed, and the last one is the one that
             * kills him. Speed and damage climb far past anything else on the roster, and the
             * wither is the cost - a Guy left alone long enough at stage 4 burns himself out,
             * which is exactly how the technique is supposed to end.
             */
            case MIGHT_GUY -> {
                this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 99999, stage, false, false));
                this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 99999, stage, false, false));
                if (stage >= 4) {
                    this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.WITHER, 99999, 0, false, false));
                }
            }
            /*
             * Kakuzu has five hearts, so he does not stay dead the way other people do: every
             * step down the ladder puts one back. It is not regeneration - it is a lump of
             * health returning at the moment you thought you had him.
             */
            case KAKUZU -> {
                this.heal(this.getMaxHealth() * 0.18f);
                this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 99999, amplifier, false, false));
            }
            // A medic's stored chakra: she comes back from further down than she should.
            case SAKURA -> {
                this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 99999, amplifier, false, false));
                if (stage >= 3) {
                    this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.REGENERATION, 99999, 0, false, false));
                }
            }
            // Haku does not get stronger, he gets harder to touch.
            case HAKU -> this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 99999, stage, false, false));
            // There is always another Zetsu. Being hurt is how they multiply.
            case WHITE_ZETSU -> this.summonShadowClones(stage);
            // More sand leaves the gourd every time he is pushed, so the shield thickens
            // (see blockedBySand) and the sand starts carrying him instead of the ground.
            case GAARA -> {
                this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 99999, amplifier, false, false));
                if (stage >= 2) {
                    // Sand Levitation: he stops walking anywhere he does not want to.
                    this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.SLOW_FALLING, 99999, 0, false, false));
                    this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.JUMP, 99999, 2, false, false));
                }
            }
            default -> this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 99999, amplifier, false, false));
        }
    }

    /** Announces the final form; the earlier steps speak for themselves through the buffs. */
    private void announceStage(MangekyoBossVariant variant, int stage) {
        if (stage < 4 || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean kurama = variant.hasKuramaCloak();
        boolean sand = variant.hasSandCloak();
        String key = kurama ? "mangekyo.boss.kuramaavatar"
                : sand ? "mangekyo.boss.shukaku"
                : variant.hasSusanoo() ? "mangekyo.boss.completebody"
                : "mangekyo.boss.finalform";
        serverLevel.playSound(null, this.blockPosition(),
                kurama || sand ? net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL
                        : net.minecraft.sounds.SoundEvents.WITHER_SPAWN,
                net.minecraft.sounds.SoundSource.HOSTILE, 3.0f, kurama ? 0.5f : sand ? 0.8f : 0.6f);
        Component message = Component.translatable(key,
                        Component.translatable(variant.translationKey())
                                .withStyle(net.minecraft.ChatFormatting.RED))
                .withStyle(net.minecraft.ChatFormatting.DARK_RED);
        for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
            if (player.blockPosition().closerThan(this.blockPosition(), 80)) {
                player.displayClientMessage(message, false);
            }
        }
    }

    /**
     * Real Shadow Clones, as separate entities you can cut down one at a time.
     *
     * The rotation's clone flurry is a damage pattern with particles - fine as a flourish,
     * useless as the technique Naruto is actually known for, because there is nothing there
     * to fight. These are the entity, hostile to players, scaled off their summoner so they
     * stay a threat without being a second boss each.
     */
    public void summonShadowClones(int count) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // Hard ceiling: a jinchuriki who has escalated three times must not leave a standing
        // army behind, and the clone entity has no cap of its own.
        long existing = serverLevel.getEntities(
                        com.sekwah.narutomod.entity.NarutoEntities.SHADOW_CLONE.get(),
                        clone -> clone.getOwnerUUID().map(this.getUUID()::equals).orElse(false))
                .size();
        int room = (int) Math.max(0, MAX_BOSS_CLONES - existing);
        int spawning = Math.min(count, room);

        for (int i = 0; i < spawning; i++) {
            double angle = (Math.PI * 2 * i) / Math.max(1, spawning) + this.random.nextDouble();
            ShadowCloneEntity clone = new ShadowCloneEntity(
                    com.sekwah.narutomod.entity.NarutoEntities.SHADOW_CLONE.get(), serverLevel);
            clone.moveTo(this.getX() + Math.cos(angle) * 2.5, this.getY(),
                    this.getZ() + Math.sin(angle) * 2.5, this.getYRot(), 0);
            clone.setOwner(this);
            clone.makeHostileToPlayers(BOSS_CLONE_HEALTH,
                    this.getVariant().attackDamage() * BOSS_CLONE_DAMAGE_SHARE);
            serverLevel.addFreshEntity(clone);
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    clone.getX(), clone.getY() + 1.0, clone.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
        }
        if (spawning > 0) {
            serverLevel.playSound(null, this.blockPosition(),
                    net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.6f, 1.2f);
        }
    }

    /** Standing limit on a puppeteer's collection, for the same reason clones have one. */
    private static final int MAX_BOSS_PUPPETS = 7;

    /**
     * Puts one of Sasori's puppets on the field, on his strings.
     *
     * The Hundred arrive in fours because that is the whole idea of them; everything else is
     * a single piece. Capped like the clones are - a fight that runs long must not leave a
     * standing army behind it.
     */
    public void summonPuppet(PuppetVariant variant) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        long existing = serverLevel.getEntities(
                        com.sekwah.narutomod.entity.NarutoEntities.PUPPET.get(),
                        puppet -> puppet.getOwnerUUID().map(this.getUUID()::equals).orElse(false))
                .size();
        int room = (int) Math.max(0, MAX_BOSS_PUPPETS - existing);
        int spawning = Math.min(variant.summonCount(), room);

        for (int i = 0; i < spawning; i++) {
            double angle = (Math.PI * 2 * i) / Math.max(1, spawning) + this.random.nextDouble();
            double reach = 2.5 + variant.getWidth();
            PuppetEntity puppet = new PuppetEntity(
                    com.sekwah.narutomod.entity.NarutoEntities.PUPPET.get(), serverLevel);
            puppet.moveTo(this.getX() + Math.cos(angle) * reach, this.getY(),
                    this.getZ() + Math.sin(angle) * reach, this.getYRot(), 0);
            puppet.setOwner(this);
            // Order matters: setVariant rewrites max health, so the top-up comes after it.
            puppet.setVariant(variant);
            puppet.setHealth(puppet.getMaxHealth());
            puppet.setCustomName(Component.literal(variant.getDisplayName()));
            serverLevel.addFreshEntity(puppet);
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    puppet.getX(), puppet.getY() + 1.0, puppet.getZ(), 24, 0.5, 0.7, 0.5, 0.02);
        }
        if (spawning > 0) {
            serverLevel.playSound(null, this.blockPosition(),
                    net.minecraft.sounds.SoundEvents.WOOD_PLACE,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.8f, 0.7f);
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
        MangekyoBossVariant variant = MangekyoBossVariant.weightedRandom(level.getRandom());
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
        if (this.absorbWithKamui(source)) {
            return false;
        }
        // The Susanoo tanks part of every blow, same idea as the player-side damage sponge.
        int stage = this.getSusanooStage();
        if (stage > 0 && this.getVariant().hasSusanoo() && !source.isCreativePlayer()) {
            // Gated on hasSusanoo deliberately: every boss climbs the stage ladder now, and
            // handing a swordsman an 85% shell at stage 4 because he shares the counter
            // would make the last twelve percent of his health longer than the rest of him.
            amount *= (1f - SUSANOO_ABSORB[Math.min(stage, SUSANOO_ABSORB.length - 1)]);
        }
        if (this.blockedBySand(source, stage)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    /** Odds the Shield of Sand catches a blow, by escalation stage. */
    private static final float[] SAND_SHIELD_CHANCE = {0.30f, 0.38f, 0.46f, 0.54f, 0.62f};

    /**
     * The Shield of Sand: it moves without being told to, and it stops what he never saw.
     *
     * Written as a chance to negate a hit outright rather than as flat damage reduction,
     * because that is what it does in the story and because it plays differently: a
     * percentage shave is invisible, while a blow that simply does not land is something you
     * see and learn to work around. Never blocks starvation, drowning or a creative player.
     */
    private boolean blockedBySand(DamageSource source, int stage) {
        if (!this.getVariant().hasAutomaticDefence() || source.isCreativePlayer()) {
            return false;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            return false;
        }
        float chance = SAND_SHIELD_CHANCE[Math.min(stage, SAND_SHIELD_CHANCE.length - 1)];
        if (this.random.nextFloat() >= chance) {
            return false;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            com.sekwah.narutomod.util.NarutoParticles.spawnRing(serverLevel,
                    this.position().add(0, this.getBbHeight() * 0.5, 0), 1.4, 20, SAND_TAN);
            serverLevel.playSound(null, this.blockPosition(),
                    net.minecraft.sounds.SoundEvents.SAND_BREAK,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 0.7f);
        }
        return true;
    }

    private static final net.minecraft.core.particles.DustParticleOptions SAND_TAN =
            new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(0.85F, 0.75F, 0.45F), 1.2F);

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
        tag.putInt("SenjuHealsUsed", this.senjuHealsUsed);
        tag.putBoolean("KamuiUnlocked", this.kamuiUnlocked);
        tag.putBoolean("EngagedByPlayer", this.engagedByPlayer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(VARIANT, tag.getByte("Variant"));
        this.setSusanooStage(tag.getByte("SusanooStage"));
        this.chakra = tag.contains("BossChakra") ? tag.getFloat("BossChakra") : MAX_CHAKRA;
        // Persisted so relogging mid-fight cannot hand Hashirama his heals back.
        this.senjuHealsUsed = tag.getInt("SenjuHealsUsed");
        this.kamuiUnlocked = tag.getBoolean("KamuiUnlocked");
        this.engagedByPlayer = tag.getBoolean("EngagedByPlayer");
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
