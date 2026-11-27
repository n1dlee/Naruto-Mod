package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.FireballShape;
import com.sekwah.narutomod.util.WaterDragonFlight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.UUID;

public class GreatFireballEntity extends Projectile {
    private static final EntityDataAccessor<Byte> STAGE =
            SynchedEntityData.defineId(GreatFireballEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> AGE =
            SynchedEntityData.defineId(GreatFireballEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHARGE =
            SynchedEntityData.defineId(GreatFireballEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Direction> FACE =
            SynchedEntityData.defineId(GreatFireballEntity.class, EntityDataSerializers.DIRECTION);
    private static final int IMPACT_TICKS = 9;

    private Vec3 heading = new Vec3(0, 0, 1);
    private float multiplier = 1;
    @Nullable
    private UUID ownerId;
    @Nullable
    private Vec3 clientTarget;

    public GreatFireballEntity(EntityType<GreatFireballEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void launch(LivingEntity owner, Vec3 direction, int charge, float multiplier) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return;
        }
        var fireball = new GreatFireballEntity(NarutoEntities.GREAT_FIREBALL.get(), level);
        fireball.setOwner(owner);
        fireball.ownerId = owner.getUUID();
        fireball.multiplier = multiplier;
        fireball.heading = direction.lengthSqr() > 1.0e-8 ? direction.normalize() : owner.getLookAngle();
        fireball.setRot((float) Math.toDegrees(Math.atan2(fireball.heading.z, fireball.heading.x)) - 90f,
                (float) -Math.toDegrees(Math.atan2(fireball.heading.y, fireball.heading.horizontalDistance())));
        fireball.entityData.set(CHARGE, FireballShape.charge(charge));
        fireball.setPos(fireball.mouth(level, owner));
        level.addFreshEntity(fireball);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(STAGE, (byte) Phase.EXHALE.ordinal());
        this.entityData.define(AGE, 0);
        this.entityData.define(CHARGE, 0);
        this.entityData.define(FACE, Direction.UP);
    }

    public Phase phase() {
        return Phase.values()[this.entityData.get(STAGE)];
    }

    public int phaseAge() {
        return this.entityData.get(AGE);
    }

    public int charge() {
        return this.entityData.get(CHARGE);
    }

    public Direction face() {
        return this.entityData.get(FACE);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(3.3);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        if (teleport) {
            this.clientTarget = null;
            super.lerpTo(x, y, z, yaw, pitch, steps, true);
        } else {
            this.clientTarget = new Vec3(x, y, z);
            this.setRot(yaw, pitch);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            this.tickLocal();
            return;
        }
        int age = this.phaseAge() + 1;
        this.entityData.set(AGE, age);
        switch (this.phase()) {
            case EXHALE -> this.exhale(level, age);
            case FLIGHT -> this.fly(level, age);
            default -> {
                if (age >= IMPACT_TICKS) {
                    this.discard();
                }
            }
        }
    }

    private void tickLocal() {
        if (this.clientTarget != null) {
            this.setPos(this.clientTarget);
            this.clientTarget = null;
        }
        if (this.phase() == Phase.FLIGHT) {
            Vec3 tip = this.position().subtract(this.getLookAngle().scale(FireballShape.radius(this.charge(), this.phaseAge())));
            this.level().addParticle(ParticleTypes.SMOKE, tip.x, tip.y, tip.z, 0, 0.08, 0);
        }
    }

    private void exhale(ServerLevel level, int age) {
        if (!(this.getOwner() instanceof LivingEntity owner) || !owner.isAlive() || owner.isSpectator()) {
            this.discard();
            return;
        }
        this.setPos(this.mouth(level, owner));
        if (age == 1) {
            this.sound(level, NarutoSounds.FIREBALL_EXHALE.get(), 0.86f + this.charge() / 70.0f, 1.2f);
        }
        if (age >= FireballShape.EXHALE_TICKS) {
            this.begin(Phase.FLIGHT);
        }
    }

    private Vec3 mouth(ServerLevel level, LivingEntity owner) {
        Vec3 point = owner.getEyePosition().add(0, -0.16, 0);
        Vec3 ahead = point.add(this.heading.scale(0.44));
        var hit = this.clip(level, point, ahead, ClipContext.Fluid.NONE);
        return hit.getType() == HitResult.Type.BLOCK
                ? hit.getLocation().subtract(this.heading.scale(0.045)) : ahead;
    }

    private void fly(ServerLevel level, int age) {
        if (age > 54 || this.isInWater()) {
            this.impact(level, null, this.isInWater() ? Phase.STEAM : Phase.FADE);
            return;
        }
        Vec3 from = this.position();
        Vec3 to = from.add(this.heading.scale(Math.min(1.46, 0.62 + age * 0.13)));
        if (!level.hasChunkAt(BlockPos.containing(to))) {
            this.discard();
            return;
        }
        BlockHitResult water = this.clip(level, from, to, ClipContext.Fluid.WATER);
        boolean wet = water.getType() == HitResult.Type.BLOCK && level.getFluidState(water.getBlockPos()).is(FluidTags.WATER);
        Vec3 limit = wet ? water.getLocation() : to;
        HitResult hit = FireballContact.first(this, level, from, limit, FireballShape.radius(this.charge(), age));
        if (hit == null && wet) {
            this.impact(level, water, Phase.STEAM);
        } else if (hit != null && !ForgeEventFactory.onProjectileImpact(this, hit)) {
            this.impact(level, hit, hit instanceof EntityHitResult ? Phase.ENTITY : Phase.BLOCK);
        } else {
            this.setPos(to);
        }
    }

    private void impact(ServerLevel level, @Nullable HitResult hit, Phase phase) {
        if (this.phase() != Phase.FLIGHT) {
            return;
        }
        this.begin(phase);
        if (hit != null) {
            this.setPos(hit.getLocation());
        }
        if (hit instanceof BlockHitResult block) {
            this.entityData.set(FACE, block.getDirection());
        } else {
            this.entityData.set(FACE, Direction.getNearest(this.heading.x, this.heading.y, this.heading.z));
        }
        if (phase == Phase.ENTITY || phase == Phase.BLOCK) {
            FireballContact.damage(this, level, this.getOwner(), this.multiplier, hit);
            this.sound(level, NarutoSounds.FIREBALL_EXPLODE.get(), 1.55f, 0.94f);
        } else if (phase == Phase.STEAM) {
            this.sound(level, NarutoSounds.FIREBALL_STEAM.get(), 1.0f, 1.0f);
        }
    }

    boolean canHit(@Nullable Entity owner, LivingEntity target) {
        if (!target.isAlive() || !target.isPickable() || target.isSpectator()
                || target.getUUID().equals(this.ownerId) || Faction.sameSide(owner, target)) {
            return false;
        }
        if (target instanceof Player player) {
            return !player.isCreative() && owner != null
                    && !owner.isAlliedTo(player) && (!(owner instanceof Player caster) || caster.canHarmPlayer(player));
        }
        return owner == null || !owner.isAlliedTo(target);
    }

    BlockHitResult clip(ServerLevel level, Vec3 a, Vec3 b, ClipContext.Fluid fluid) {
        return level.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, fluid, this));
    }

    private void begin(Phase phase) {
        this.entityData.set(STAGE, (byte) phase.ordinal());
        this.entityData.set(AGE, 0);
    }

    private void sound(ServerLevel level, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level.playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FlameAge", this.phaseAge());
        tag.putByte("FlameStage", (byte) this.phase().ordinal());
        tag.putInt("FlameCharge", this.charge());
        tag.putByte("FlameFace", (byte) this.face().ordinal());
        tag.put("FlameHeading", this.newDoubleList(this.heading.x, this.heading.y, this.heading.z));
        tag.putFloat("FlameDamage", this.multiplier);
        if (this.ownerId != null) {
            tag.putUUID("FlameOwner", this.ownerId);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(AGE, tag.getInt("FlameAge"));
        this.entityData.set(STAGE, (byte) Mth.clamp(tag.getByte("FlameStage"), 0, Phase.FADE.ordinal()));
        this.entityData.set(CHARGE, FireballShape.charge(tag.getInt("FlameCharge")));
        this.entityData.set(FACE, Direction.from3DDataValue(tag.getByte("FlameFace")));
        var aim = tag.getList("FlameHeading", Tag.TAG_DOUBLE);
        this.heading = aim.size() == 3 ? new Vec3(aim.getDouble(0), aim.getDouble(1), aim.getDouble(2)) : this.getLookAngle();
        this.multiplier = tag.getFloat("FlameDamage");
        this.ownerId = tag.hasUUID("FlameOwner") ? tag.getUUID("FlameOwner") : null;
    }

    public enum Phase {
        EXHALE, FLIGHT, BLOCK, ENTITY, STEAM, FADE
    }
}
