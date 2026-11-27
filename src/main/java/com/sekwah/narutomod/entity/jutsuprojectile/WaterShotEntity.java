package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.ProjectileAxis;
import com.sekwah.narutomod.util.WaterShotShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public class WaterShotEntity extends Projectile {
    private static final EntityDataAccessor<Integer> FRAME =
            SynchedEntityData.defineId(WaterShotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HEAVY =
            SynchedEntityData.defineId(WaterShotEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Direction> FACE =
            SynchedEntityData.defineId(WaterShotEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Vector3f> AXIS =
            SynchedEntityData.defineId(WaterShotEntity.class, EntityDataSerializers.VECTOR3);
    private static final int AGE_BITS = 6;
    private static final int AGE_MASK = (1 << AGE_BITS) - 1;
    private static final Phase[] PHASES = Phase.values();
    private float power = 1;
    private double mouthHeight;

    public WaterShotEntity(EntityType<WaterShotEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void launch(LivingEntity owner, Vec3 direction, float power, boolean heavy) {
        launch(owner, direction, power, heavy, false);
    }

    public static void launchAimed(Player owner, float power) {
        launch(owner, owner.getLookAngle(), power, false, true);
    }

    private boolean followAimWhileGathering;

    private static void launch(LivingEntity owner, Vec3 direction, float power, boolean heavy, boolean followAim) {
        if (!(owner.level() instanceof ServerLevel level) || !owner.isAlive() || owner.isSpectator()) {
            return;
        }
        var shot = new WaterShotEntity(NarutoEntities.WATER_SHOT.get(), level);
        shot.setOwner(owner);
        shot.entityData.set(HEAVY, heavy);
        shot.power = power;
        shot.followAimWhileGathering = followAim;
        shot.mouthHeight = heavy && owner.getBbHeight() > 2.6
                ? owner.getBbHeight() * 0.8 : owner.getEyeY() - owner.getY() - 0.16;
        shot.setHeading(ProjectileAxis.direction(direction, owner.getLookAngle()));
        shot.setPos(shot.mouth(owner));
        level.addFreshEntity(shot);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(FRAME, 0);
        this.entityData.define(HEAVY, false);
        this.entityData.define(FACE, Direction.UP);
        this.entityData.define(AXIS, new Vector3f());
    }

    public Phase phase() {
        return PHASES[this.entityData.get(FRAME) >> AGE_BITS];
    }

    public int age() {
        return this.entityData.get(FRAME) & AGE_MASK;
    }

    public boolean heavy() {
        return this.entityData.get(HEAVY);
    }

    public Direction face() {
        return this.entityData.get(FACE);
    }

    public Vec3 heading() {
        return ProjectileAxis.direction(ProjectileAxis.decode(this.entityData.get(AXIS)), this.getLookAngle());
    }

    private void setHeading(Vec3 heading) {
        this.entityData.set(AXIS, ProjectileAxis.encode(heading));
        var angles = ProjectileAxis.angles(heading);
        this.setRot(angles.yaw(), angles.pitch());
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(3.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Phase phase = this.phase();
        int age = this.age() + 1;
        this.frame(phase, age);
        switch (phase) {
            case GATHER -> this.gather(level, age);
            case FLIGHT -> this.fly(level, age);
            case FADE, BLOCK, ENTITY, WATER -> {
                if (age >= WaterShotShape.SPLASH_TICKS) {
                    this.discard();
                }
            }
        }
    }

    private Vec3 mouth(LivingEntity owner) {
        Vec3 origin = owner.position().add(0, this.mouthHeight, 0);
        Vec3 heading = this.heading();
        Vec3 forward = origin.add(heading.scale(0.42));
        var result = ProjectileVolume.clip(this, origin, forward, ClipContext.Fluid.NONE);
        return result.getType() == HitResult.Type.MISS ? forward
                : result.getLocation().subtract(heading.scale(0.04));
    }

    private void gather(ServerLevel level, int age) {
        if (!(this.getOwner() instanceof LivingEntity owner) || !owner.isAlive() || owner.isSpectator()) {
            this.discard();
            return;
        }
        if (this.followAimWhileGathering) {
            this.setHeading(ProjectileAxis.direction(owner.getLookAngle(), this.heading()));
        }
        this.setPos(this.mouth(owner));
        if (age >= WaterShotShape.PREPARE) {
            this.frame(Phase.FLIGHT, 0);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), NarutoSounds.WATER_BULLET_SHOOT.get(),
                    SoundSource.PLAYERS, 0.92f, this.heavy() ? 0.79f : 1.14f);
        }
    }

    private void fly(ServerLevel level, int age) {
        Vec3 from = this.position();
        Vec3 to = from.add(this.heading().scale(WaterShotShape.velocity(age, this.heavy())));
        if (age > WaterShotShape.MAX_FLIGHT || !level.hasChunkAt(BlockPos.containing(to))) {
            this.frame(Phase.FADE, 0);
            return;
        }
        var wet = ProjectileVolume.clip(this, from, to, ClipContext.Fluid.WATER);
        boolean entersWater = wet.getType() == HitResult.Type.BLOCK
                && !level.getFluidState(BlockPos.containing(from)).is(FluidTags.WATER)
                && level.getFluidState(wet.getBlockPos()).is(FluidTags.WATER);
        Vec3 limit = entersWater ? wet.getLocation() : to;
        LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
        var impact = ProjectileVolume.first(this, level, from, limit, WaterShotShape.radius(this.heavy()),
                candidate -> this.hittable(owner, candidate));
        if (impact != null && !ForgeEventFactory.onProjectileImpact(this, impact.result())) {
            this.setPos(impact.result().getLocation());
            this.entityData.set(FACE, impact.face());
            this.finish(level, owner, impact.target(), impact.target() == null ? Phase.BLOCK : Phase.ENTITY);
        } else if (entersWater) {
            this.setPos(wet.getLocation());
            this.entityData.set(FACE, wet.getDirection());
            this.finish(level, owner, null, Phase.WATER);
        } else {
            this.setPos(to);
        }
    }

    private void finish(ServerLevel level, @Nullable LivingEntity owner, @Nullable LivingEntity target, Phase phase) {
        if (phase == Phase.ENTITY && (target == null
                || !WaterShotContact.damage(this, level, owner, target, 7 * this.power))) {
            this.frame(Phase.FADE, 0);
            return;
        }
        this.frame(phase, 0);
        if (phase != Phase.WATER) {
            WaterShotContact.splash(this, level, owner, target, this.power);
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                phase == Phase.WATER ? SoundEvents.FISHING_BOBBER_SPLASH : NarutoSounds.WATER_BULLET_SPLASH.get(),
                SoundSource.PLAYERS, phase == Phase.WATER ? 0.65f : 0.9f, this.heavy() ? 0.8f : 1.26f);
    }

    boolean hittable(@Nullable LivingEntity owner, LivingEntity target) {
        if (owner == null || target == owner || !target.isAlive() || !target.isPickable() || target.isSpectator()
                || owner.isAlliedTo(target) || Faction.sameSide(owner, target)) {
            return false;
        }
        return !(target instanceof Player player)
                || (!player.isCreative() && (!(owner instanceof Player caster) || caster.canHarmPlayer(player)));
    }

    private void frame(Phase phase, int age) {
        this.entityData.set(FRAME, phase.ordinal() << AGE_BITS | Math.min(AGE_MASK, age));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("FollowAimWhileGathering", this.followAimWhileGathering);
        tag.putInt("ShotFrame", this.entityData.get(FRAME));
        tag.putBoolean("ShotHeavy", this.heavy());
        tag.putByte("ShotFace", (byte) this.face().get3DDataValue());
        tag.putFloat("ShotPower", this.power);
        tag.putDouble("ShotMouth", this.mouthHeight);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.followAimWhileGathering = tag.getBoolean("FollowAimWhileGathering");
        this.entityData.set(FRAME, Mth.clamp(tag.getInt("ShotFrame"), 0, PHASES.length * (1 << AGE_BITS) - 1));
        this.entityData.set(HEAVY, tag.getBoolean("ShotHeavy"));
        this.entityData.set(FACE, Direction.from3DDataValue(tag.getByte("ShotFace")));
        this.power = tag.getFloat("ShotPower");
        this.mouthHeight = tag.getDouble("ShotMouth");
        this.setHeading(this.getLookAngle());
    }

    public enum Phase {
        GATHER, FLIGHT, BLOCK, ENTITY, WATER, FADE
    }
}
