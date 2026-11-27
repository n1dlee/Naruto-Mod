package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.item.weapons.GunbaiItem;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.GustFront;
import com.sekwah.narutomod.util.GunbaiMotion;
import com.sekwah.narutomod.util.JutsuVfx;
import com.sekwah.narutomod.util.WaterDragonFlight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GustFrontEntity extends Projectile {
    private static final EntityDataAccessor<Integer> AGE =
            SynchedEntityData.defineId(GustFrontEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CompoundTag> BOUNDS =
            SynchedEntityData.defineId(GustFrontEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Boolean> MOB =
            SynchedEntityData.defineId(GustFrontEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(GustFrontEntity.class, EntityDataSerializers.INT);
    private final double[] laneBounds = new double[GustFront.LANES];
    private final Set<UUID> processed = new HashSet<>();
    private float rank;
    private boolean palmContact;

    public GustFrontEntity(EntityType<GustFrontEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        Arrays.fill(this.laneBounds, GustFront.RANGE + 0.05);
    }

    public static boolean launch(LivingEntity owner, Vec3 direction, float rank) {
        return launch(owner, direction, rank, Mode.WIND);
    }

    public static boolean fan(LivingEntity owner, Vec3 direction, InteractionHand hand, float power, boolean signature) {
        Mode mode = owner instanceof Player ? Mode.held(hand, signature) : Mode.BOSS_FAN;
        return launch(owner, direction, power, mode);
    }

    public static boolean palm(LivingEntity owner, Vec3 direction, float rank, boolean focused) {
        return launch(owner, direction, rank, focused ? Mode.FOCUSED_PALM : Mode.PALM);
    }

    private static boolean launch(LivingEntity owner, Vec3 direction, float rank, Mode mode) {
        if (!(owner.level() instanceof ServerLevel level) || !owner.isAlive() || owner.isSpectator()) {
            return false;
        }
        var wave = new GustFrontEntity(NarutoEntities.GUST_FRONT.get(), level);
        wave.setOwner(owner);
        wave.entityData.set(MOB, !(owner instanceof Player));
        wave.entityData.set(MODE, mode.ordinal());
        Arrays.fill(wave.laneBounds, wave.range() + 0.05);
        wave.rank = rank;
        Vec3 heading = direction.lengthSqr() < 1.0e-8 ? owner.getLookAngle() : direction.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(heading.z, heading.x)) - 90;
        int maxPitch = wave.isPalm() ? 70 : 48;
        float pitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(heading.y, heading.horizontalDistance())), -maxPitch, maxPitch);
        wave.setRot(yaw, pitch);
        wave.setPos(wave.muzzle(owner));
        return level.addFreshEntity(wave);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(AGE, 0);
        this.entityData.define(BOUNDS, new CompoundTag());
        this.entityData.define(MOB, false);
        this.entityData.define(MODE, Mode.WIND.ordinal());
    }

    public int age() {
        return this.entityData.get(AGE);
    }

    public double range() {
        if (this.isPalm()) {
            return GustFront.palmRange(this.mode() == Mode.FOCUSED_PALM);
        }
        if (this.isFan()) {
            return this.mode().signature ? GunbaiMotion.SIGNATURE_RANGE : GunbaiMotion.SWEEP_RANGE;
        }
        return this.entityData.get(MOB) ? GustFront.MOB_RANGE : GustFront.RANGE;
    }

    public boolean isFan() {
        return switch (this.mode()) {
            case MAIN, OFF, MAIN_SIGNATURE, OFF_SIGNATURE, BOSS_FAN -> true;
            default -> false;
        };
    }

    public boolean isPalm() {
        return this.mode() == Mode.PALM || this.mode() == Mode.FOCUSED_PALM;
    }

    public GustFront.Shape shape() {
        return this.isPalm() ? GustFront.Shape.PALM : GustFront.Shape.WIND;
    }

    private Mode mode() {
        return Mode.MODES[this.entityData.get(MODE)];
    }

    public double[] bounds() {
        byte[] packed = this.entityData.get(BOUNDS).getByteArray("Limit");
        double[] result = new double[GustFront.LANES];
        for (int i = 0; i < result.length; i++) {
            result[i] = packed.length == result.length ? Byte.toUnsignedInt(packed[i]) / 20.0 : this.range() + 0.05;
        }
        return result;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        if (this.isPalm()) {
            return super.getBoundingBoxForCulling().expandTowards(this.getLookAngle().scale(this.range())).inflate(2.8);
        }
        return super.getBoundingBoxForCulling().expandTowards(this.getLookAngle().scale(GustFront.RANGE)).inflate(7);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        int age = this.age() + 1;
        this.entityData.set(AGE, age);
        var shape = this.shape();
        if (age <= shape.prepare()) {
            if (!(this.getOwner() instanceof LivingEntity owner) || !owner.isAlive() || owner.isSpectator()
                    || this.isFan() && owner instanceof Player player
                    && !(player.getItemInHand(this.mode().hand).getItem() instanceof GunbaiItem)) {
                this.discard();
                return;
            }
            this.setPos(this.muzzle(owner));
            if (this.isFan() && age == shape.prepare() - 2) {
                owner.swing(this.mode().hand, true);
            }
            if (age == shape.prepare()) {
                if (this.isPalm() && !(owner instanceof Player)) {
                    owner.swing(InteractionHand.MAIN_HAND, true);
                }
                var sound = this.isPalm() ? NarutoSounds.AIR_PALM_RELEASE.get()
                        : this.isFan() ? NarutoSounds.GUNBAI_CUT.get() : NarutoSounds.GUST_EDGE.get();
                level.playSound(null, this.getX(), this.getY(), this.getZ(), sound,
                        SoundSource.PLAYERS, 1.1f, owner instanceof Player ? 1.02f : 0.82f);
            }
        } else if (age <= shape.prepare() + shape.flight()) {
            this.propagate(level, age - shape.prepare());
        } else if (age > shape.prepare() + shape.flight() + GustFront.FADE) {
            this.discard();
        }
    }

    private Vec3 muzzle(LivingEntity owner) {
        Vec3 eye = owner.getEyePosition().add(0, -0.2, 0);
        Vec3 next = this.isPalm() ? this.palmOrigin(owner)
                : this.isFan() ? GunbaiMotion.fanOrigin(owner, this.mode().hand, 0.34)
                : eye.add(this.getLookAngle().scale(0.37));
        var clip = ProjectileVolume.clip(this, eye, next, ClipContext.Fluid.NONE);
        return clip.getType() == HitResult.Type.MISS ? next
                : clip.getLocation().subtract(next.subtract(eye).normalize().scale(0.045));
    }

    private Vec3 palmOrigin(LivingEntity owner) {
        if (owner instanceof Player player) {
            return JutsuVfx.handPosition(player, player.getMainArm() == HumanoidArm.LEFT ? -1 : 1, 1);
        }
        var frame = GustFront.frame(this.getLookAngle());
        return owner.position().add(0, owner.getBbHeight() * 0.66, 0)
                .add(frame.right().scale(-owner.getBbWidth() * 0.38))
                .add(frame.forward().scale(0.49));
    }

    private void propagate(ServerLevel level, int age) {
        Vec3 from = this.position();
        GustFront.Frame frame = GustFront.frame(this.getLookAngle());
        var shape = this.shape();
        var wave = new Step(from, frame, GustFront.distance(age - 1, this.range(), shape),
                GustFront.distance(age, this.range(), shape), age);
        this.shorten(level, wave);
        if (this.getOwner() instanceof LivingEntity owner && owner.isAlive()) {
            this.push(level, owner, wave);
        }
    }

    private void shorten(ServerLevel level, Step wave) {
        byte[] packed = new byte[GustFront.LANES];
        for (int lane = 0; lane < this.laneBounds.length; lane++) {
            Vec3 heading = wave.frame().lane(lane, this.shape());
            double horizon = Math.min(this.laneBounds[lane], wave.end());
            Vec3 to = wave.origin().add(heading.scale(horizon));
            if (!level.hasChunkAt(BlockPos.containing(to)) || !level.getWorldBorder().isWithinBounds(BlockPos.containing(to))) {
                this.laneBounds[lane] = Math.min(this.laneBounds[lane], wave.start());
            } else {
                var clip = ProjectileVolume.clip(this, wave.origin(), to, ClipContext.Fluid.NONE);
                if (clip.getType() == HitResult.Type.BLOCK) {
                    double distance = wave.origin().distanceTo(clip.getLocation());
                    if (distance < this.laneBounds[lane] - 0.02) {
                        this.laneBounds[lane] = distance;
                        debris(level, clip.getLocation(), clip.getBlockPos());
                    }
                } else if (!this.isPalm() && (lane + wave.age()) % 4 == 0 && this.laneBounds[lane] > wave.end()) {
                    this.traceGround(level, to);
                }
            }
            packed[lane] = (byte) Mth.clamp(Math.floor(this.laneBounds[lane] * 20), 0, 255);
        }
        var tag = new CompoundTag();
        tag.putByteArray("Limit", packed);
        this.entityData.set(BOUNDS, tag);
    }

    private void traceGround(ServerLevel level, Vec3 tip) {
        var ground = ProjectileVolume.clip(this, tip.add(0, 0.08, 0), tip.add(0, -1.85, 0), ClipContext.Fluid.WATER);
        if (ground.getType() == HitResult.Type.BLOCK) {
            debris(level, ground.getLocation(), ground.getBlockPos());
        }
    }

    private static void debris(ServerLevel level, Vec3 pos, BlockPos block) {
        var state = level.getBlockState(block);
        if (level.getFluidState(block).is(FluidTags.WATER)) {
            level.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y + 0.04, pos.z, 3, 0.12, 0.08, 0.12, 0.035);
        } else if (!state.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.x, pos.y + 0.06, pos.z,
                    3, 0.18, 0.11, 0.18, 0.035);
        }
    }

    private void push(ServerLevel level, LivingEntity owner, Step wave) {
        var bounds = new AABB(wave.origin(), wave.origin()).inflate(wave.end() + 0.27);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds, candidate -> this.eligible(owner, candidate))) {
            AABB targetBox = target.getBoundingBox();
            Vec3 probe = this.isPalm() ? GustFront.palmProbe(targetBox, wave.origin(), wave.frame().forward()) : wave.origin();
            Vec3 contact = WaterDragonFlight.nearestPoint(targetBox, probe);
            Vec3 offset = contact.subtract(wave.origin());
            int lane = GustFront.lane(wave.frame(), offset, this.shape());
            double distance = offset.length();
            if (lane < 0 || !GustFront.crossed(distance, wave.start(), wave.end(), this.laneBounds[lane])
                    || !ProjectileVolume.visible(this, wave.origin(), contact)) {
                continue;
            }
            this.processed.add(target.getUUID());
            if (this.isPalm()) {
                this.palmHit(level, owner, target, wave.frame().forward(), contact, distance);
            } else {
                this.deflect(owner, target, offset, distance);
            }
        }
    }

    private boolean eligible(LivingEntity owner, LivingEntity target) {
        if (target == owner || !target.isAlive() || target.isSpectator() || !target.isPickable()
                || this.processed.contains(target.getUUID()) || owner.isAlliedTo(target) || Faction.sameSide(owner, target)) {
            return false;
        }
        return !(target instanceof Player player)
                || (!player.isCreative() && (!(owner instanceof Player caster) || caster.canHarmPlayer(player)));
    }

    private void deflect(LivingEntity owner, LivingEntity target, Vec3 direction, double distance) {
        boolean mob = !(owner instanceof Player);
        if (this.isFan()) {
            this.fanHit(owner, target, direction, distance);
            return;
        }
        if (mob && !target.hurt(owner.damageSources().mobAttack(owner), 4)) {
            return;
        }
        Vec3 before = target.getDeltaMovement();
        target.knockback(GustFront.strength(distance, this.rank, mob), -direction.x, -direction.z);
        if (!target.getDeltaMovement().equals(before)) {
            target.hurtMarked = true;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 26, 0, false, true));
        }
    }

    private void fanHit(LivingEntity owner, LivingEntity target, Vec3 aim, double distance) {
        Mode mode = this.mode();
        float damage = mode == Mode.BOSS_FAN ? 13f : (mode.signature ? 16f : 8f) * this.rank;
        var source = owner instanceof Player player ? owner.damageSources().playerAttack(player) : owner.damageSources().mobAttack(owner);
        if (target.hurt(source, damage)) {
            target.knockback(1.93 - 0.72 * Mth.clamp(distance / this.range(), 0, 1), -aim.x, -aim.z);
            target.hurtMarked = true;
        }
    }

    private void palmHit(ServerLevel level, LivingEntity owner, LivingEntity target, Vec3 aim, Vec3 contact, double distance) {
        float damage = owner instanceof Player ? 8 * Mth.clamp(this.rank, 0.5f, 2) : 11;
        if (target instanceof Player) {
            damage = Math.min(damage, 7.5f);
        }
        var source = owner instanceof Player player ? owner.damageSources().playerAttack(player) : owner.damageSources().mobAttack(owner);
        if (!target.hurt(source, damage)) {
            return;
        }
        target.knockback(1.52 - 0.66 * Mth.clamp(distance / this.range(), 0, 1), -aim.x, -aim.z);
        target.hurtMarked = true;
        if (!this.palmContact) {
            this.palmContact = true;
            level.playSound(null, contact.x, contact.y, contact.z, NarutoSounds.AIR_PALM_HIT.get(), SoundSource.PLAYERS, 0.86f, 1);
        }
        level.sendParticles(ParticleTypes.POOF, contact.x, contact.y, contact.z, 4, 0.11, 0.11, 0.11, 0.043);
    }

    private enum Mode {
        WIND(InteractionHand.MAIN_HAND, false), MAIN(InteractionHand.MAIN_HAND, false), OFF(InteractionHand.OFF_HAND, false),
        MAIN_SIGNATURE(InteractionHand.MAIN_HAND, true), OFF_SIGNATURE(InteractionHand.OFF_HAND, true),
        BOSS_FAN(InteractionHand.MAIN_HAND, true), PALM(InteractionHand.MAIN_HAND, false), FOCUSED_PALM(InteractionHand.MAIN_HAND, false);

        private static final Mode[] MODES = values();
        private final InteractionHand hand;
        private final boolean signature;

        Mode(InteractionHand hand, boolean signature) {
            this.hand = hand;
            this.signature = signature;
        }

        static Mode held(InteractionHand hand, boolean signature) {
            return hand == InteractionHand.MAIN_HAND ? (signature ? MAIN_SIGNATURE : MAIN) : (signature ? OFF_SIGNATURE : OFF);
        }
    }

    private record Step(Vec3 origin, GustFront.Frame frame, double start, double end, int age) {
    }
}
