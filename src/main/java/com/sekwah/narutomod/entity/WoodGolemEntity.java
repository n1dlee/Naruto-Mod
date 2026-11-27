package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.WaterDragonFlight;
import com.sekwah.narutomod.util.WoodGolemMotion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class WoodGolemEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(WoodGolemEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> GROWTH =
            SynchedEntityData.defineId(WoodGolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SWIPE =
            SynchedEntityData.defineId(WoodGolemEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DISMANTLE =
            SynchedEntityData.defineId(WoodGolemEntity.class, EntityDataSerializers.INT);
    private static final int DISMANTLE_TICKS = 11;
    private final Set<UUID> struck = new HashSet<>();
    private long lease;
    private float attackYaw;
    private double stride;
    private Vec3 lastStep = Vec3.ZERO;

    public WoodGolemEntity(EntityType<WoodGolemEntity> entityType, Level level) {
        super(entityType, level);
        this.setMaxUpStep(1f);
        this.setPersistenceRequired();
        this.setNoAi(true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(GROWTH, 0);
        this.entityData.define(SWIPE, -1);
        this.entityData.define(DISMANTLE, 0);
    }

    public Optional<UUID> getOwnerUUID() { return this.entityData.get(OWNER_UUID); }

    public void setOwner(Player owner) { this.entityData.set(OWNER_UUID, Optional.of(owner.getUUID())); }

    public boolean isOwnedBy(Player player) { return this.getOwnerUUID().filter(player.getUUID()::equals).isPresent(); }

    public void keepStanding(long until) { this.lease = until; }

    public long lease() { return this.lease; }

    public int swipeAge() { return this.entityData.get(SWIPE); }

    public boolean dismantling() { return this.entityData.get(DISMANTLE) > 0; }

    public float dismantleProgress(float partial) {
        return dismantling() ? Math.min(1, (this.entityData.get(DISMANTLE) - 1 + partial) / DISMANTLE_TICKS) : 0;
    }

    public float growth(float partial) {
        return Math.min(1, (this.entityData.get(GROWTH) + partial) / WoodGolemMotion.GROWTH);
    }

    private boolean ready() { return this.entityData.get(GROWTH) >= WoodGolemMotion.GROWTH && !dismantling(); }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new SwipeGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 13f));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 12, true, false, this::hostile));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 200).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 16).add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.FOLLOW_RANGE, 25);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (!WoodGolemMotion.validLease(this.getOwnerUUID().isPresent(), true, this.isAlive(), level.getGameTime(), this.lease)) {
            dissolve();
        }
        if (dismantling()) {
            int phase = this.entityData.get(DISMANTLE);
            if (phase >= DISMANTLE_TICKS) {
                this.discard();
            } else {
                this.entityData.set(DISMANTLE, phase + 1);
            }
            return;
        }
        if (!ready()) {
            grow(level);
        } else {
            animateStride(level);
            tickSwipe(level);
        }
    }

    private void grow(ServerLevel level) {
        int age = this.entityData.get(GROWTH) + 1;
        this.entityData.set(GROWTH, age);
        if (age == 1 || age == 8 || age == WoodGolemMotion.GROWTH) {
            chips(level, this.position().add(0, age / 8d, 0), 8, 0.64, 0.30);
            tone(level, this.position(), age == WoodGolemMotion.GROWTH ? 1.05f : 0.76f, 0.65f + age * 0.017f);
        }
        if (age >= WoodGolemMotion.GROWTH) {
            this.setNoAi(false);
        }
    }

    private void animateStride(ServerLevel level) {
        Vec3 feet = this.position();
        double movement = lastStep.equals(Vec3.ZERO) ? 0 : feet.subtract(lastStep).horizontalDistance();
        lastStep = feet;
        if (this.onGround() && movement > 0.035 && movement < 0.75) {
            stride += movement;
            if (stride >= 1.35) {
                stride -= 1.35;
                chips(level, feet, 4, 0.55, 0.10);
                tone(level, feet, 0.33f, 0.59f);
            }
        }
    }

    private boolean hostile(@Nullable LivingEntity target) {
        return target instanceof Enemy && target.isAlive() && !target.noPhysics && !Faction.sameSide(this, target);
    }

    private void beginSwipe(LivingEntity target) {
        if (!ready() || this.swipeAge() >= 0 || this.isVehicle() || !hostile(target) || !this.hasLineOfSight(target)) {
            return;
        }
        Vec3 aim = JutsuAim.center(target).subtract(this.position());
        float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
        this.attackYaw = yaw;
        this.struck.clear();
        this.entityData.set(SWIPE, 0);
        if (this.level() instanceof ServerLevel level) {
            tone(level, this.position().add(0, 2.2, 0), 0.75f, 0.87f);
        }
    }

    private void tickSwipe(ServerLevel level) {
        int previous = this.swipeAge();
        if (previous < 0) {
            return;
        }
        this.getNavigation().stop();
        this.setYRot(this.attackYaw);
        this.yBodyRot = this.attackYaw;
        this.yHeadRot = this.attackYaw;
        Vec3 velocity = this.getDeltaMovement();
        this.setDeltaMovement(velocity.x * 0.18, velocity.y, velocity.z * 0.18);
        int now = previous + 1;
        if (WoodGolemMotion.active(now) && !sweep(level, previous, now)) {
            now = WoodGolemMotion.WINDUP + WoodGolemMotion.SWING;
        }
        this.entityData.set(SWIPE, now >= WoodGolemMotion.WINDUP + WoodGolemMotion.SWING + WoodGolemMotion.RECOVERY ? -1 : now);
    }

    private boolean sweep(ServerLevel level, int previous, int now) {
        Vec3 from = WoodGolemMotion.fist(this.position(), this.attackYaw, previous);
        Vec3 to = WoodGolemMotion.fist(this.position(), this.attackYaw, now);
        BlockPos at = BlockPos.containing(to);
        if (!level.hasChunkAt(at)) {
            return false;
        }
        Vec3 shoulder = WoodGolemMotion.shoulder(this.position(), this.attackYaw);
        var barrier = level.clip(new ClipContext(shoulder, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        var sweptBlock = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (barrier.getType() != HitResult.Type.MISS || sweptBlock.getType() != HitResult.Type.MISS) {
            Vec3 stop = barrier.getType() != HitResult.Type.MISS ? barrier.getLocation() : sweptBlock.getLocation();
            chips(level, stop, 5, 0.23, 0.21);
            tone(level, stop, 0.71f, 0.67f);
            return false;
        }
        if (now == WoodGolemMotion.WINDUP + 2) {
            tone(level, to, 0.94f, 0.73f);
        }
        var owner = this.getOwnerUUID().map(level::getPlayerByUUID).orElse(null);
        if (owner == null || !owner.isAlive()) {
            return false;
        }
        AABB zone = new AABB(from, to).inflate(WoodGolemMotion.FIST);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, zone, this::hostile)) {
            if (this.struck.contains(target.getUUID()) || !JutsuAim.attackable(owner, target)
                    || !level.mayInteract(owner, target.blockPosition()) || !this.hasLineOfSight(target)) {
                continue;
            }
            var contact = WaterDragonFlight.sweep(from, to, target.getBoundingBox(), WoodGolemMotion.FIST);
            if (contact != null && this.struck.add(target.getUUID())
                    && target.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE))) {
                chips(level, contact.point(), 9, 0.30, 0.27);
                tone(level, contact.point(), 0.91f, 0.51f);
            }
        }
        return true;
    }

    @Override
    public InteractionResult mobInteract(Player owner, InteractionHand hand) {
        if (!ready() || this.swipeAge() >= 0 || !isOwnedBy(owner) || !owner.getItemInHand(hand).isEmpty() || this.isVehicle()) {
            return super.mobInteract(owner, hand);
        }
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return owner.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    public double getPassengersRidingOffset() { return 3.77; }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return ready() && this.getFirstPassenger() instanceof Player rider && isOwnedBy(rider) ? rider : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return ready() && this.swipeAge() < 0 && this.getPassengers().isEmpty() && passenger instanceof Player owner && isOwnedBy(owner);
    }

    @Override
    protected void tickRidden(Player rider, Vec3 travelVector) {
        super.tickRidden(rider, travelVector);
        this.setYRot(rider.getYRot());
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player rider, Vec3 travelVector) {
        return new Vec3(rider.xxa * 0.42f, 0, rider.zza * 0.86f);
    }

    @Override
    protected float getRiddenSpeed(Player rider) {
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    @Override
    public boolean isPushable() { return false; }

    public void dissolve() {
        if (!dismantling() && this.level() instanceof ServerLevel level) {
            this.ejectPassengers();
            this.setNoAi(true);
            this.setTarget(null);
            this.entityData.set(SWIPE, -1);
            this.entityData.set(DISMANTLE, 1);
            chips(level, this.position().add(0, 1.6, 0), 24, 0.89, 0.79);
            tone(level, this.position(), 0.89f, 0.49f);
        }
    }

    private static void chips(ServerLevel level, Vec3 at, int count, double width, double height) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DARK_OAK_LOG.defaultBlockState()),
                at.x, at.y, at.z, count, width, height, width, 0.043);
    }

    private static void tone(ServerLevel level, Vec3 at, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, NarutoSounds.MOKUTON_GOLEM.get(), SoundSource.BLOCKS, volume, pitch);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private static final class SwipeGoal extends MeleeAttackGoal {
        private final WoodGolemEntity golem;

        private SwipeGoal(WoodGolemEntity golem) {
            super(golem, 1.0, false);
            this.golem = golem;
        }

        @Override
        protected double getAttackReachSqr(LivingEntity target) {
            return 2.55 * 2.55;
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target, double dist) {
            if (this.isTimeToAttack() && dist <= getAttackReachSqr(target) && golem.swipeAge() < 0) {
                this.resetAttackCooldown();
                golem.beginSwipe(target);
            }
        }
    }
}
