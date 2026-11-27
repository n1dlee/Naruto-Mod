package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.JutsuVfx;
import com.sekwah.narutomod.util.WaterDragonFlight;
import com.sekwah.narutomod.util.WindVortex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
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

public class RasenshurikenEntity extends Projectile {
    public static final int FORM_TICKS = 7;
    private static final int FLIGHT_TICKS = 34;
    private static final EntityDataAccessor<Byte> PHASE =
            SynchedEntityData.defineId(RasenshurikenEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> AGE =
            SynchedEntityData.defineId(RasenshurikenEntity.class, EntityDataSerializers.INT);

    private final WindVortex vortex = new WindVortex();
    private Vec3 heading = new Vec3(0, 0, 1);
    private Vec3 coverOffset = Vec3.ZERO;
    @Nullable
    private UUID direct;
    @Nullable
    private UUID casterId;
    @Nullable
    private Vec3 nextClientPosition;
    private float multiplier = 1.0f;
    private boolean bossCast;

    public RasenshurikenEntity(EntityType<RasenshurikenEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void form(LivingEntity owner, Vec3 aim, float multiplier, boolean bossCast) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return;
        }
        var disc = new RasenshurikenEntity(NarutoEntities.RASENSHURIKEN.get(), level);
        disc.setOwner(owner);
        disc.casterId = owner.getUUID();
        disc.heading = aim.normalize();
        if (disc.heading.lengthSqr() < 1.0e-8) {
            disc.heading = owner.getLookAngle();
        }
        disc.multiplier = Math.max(0.0f, multiplier);
        disc.bossCast = bossCast;
        disc.setRot((float) Math.toDegrees(Math.atan2(disc.heading.z, disc.heading.x)) - 90.0f,
                (float) -Math.toDegrees(Math.atan2(disc.heading.y, disc.heading.horizontalDistance())));
        disc.setPos(disc.safeHand(level, owner));
        level.addFreshEntity(disc);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PHASE, (byte) Phase.FORM.ordinal());
        this.entityData.define(AGE, 0);
    }

    public Phase phase() {
        return Phase.values()[this.entityData.get(PHASE)];
    }

    public int phaseAge() {
        return this.entityData.get(AGE);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int increments, boolean teleport) {
        if (teleport) {
            this.nextClientPosition = null;
            super.lerpTo(x, y, z, yaw, pitch, increments, true);
        } else {
            this.nextClientPosition = new Vec3(x, y, z);
            this.setRot(yaw, pitch);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.nextClientPosition != null) {
                this.setPos(this.nextClientPosition);
                this.nextClientPosition = null;
            }
            return;
        }
        if (this.level() instanceof ServerLevel level) {
            int age = this.phaseAge() + 1;
            this.entityData.set(AGE, age);
            switch (this.phase()) {
                case FORM -> this.tickForm(level, age);
                case FLIGHT -> this.tickFlight(level, age);
                case VORTEX -> this.tickVortex(level, age);
            }
        }
    }

    private Vec3 safeHand(ServerLevel level, LivingEntity owner) {
        Vec3 eye = owner.getEyePosition().add(0, -0.14, 0);
        Vec3 desired = owner instanceof Player player
                ? JutsuVfx.handPosition(player, player.getMainArm() == HumanoidArm.RIGHT ? 1.0 : -1.0, 1)
                : eye.add(this.heading.scale(0.35));
        BlockHitResult obstruction = this.clip(level, eye, desired);
        if (obstruction.getType() == HitResult.Type.BLOCK) {
            return obstruction.getLocation().add(Vec3.atLowerCornerOf(obstruction.getDirection().getNormal()).scale(0.04));
        }
        return desired;
    }

    private void tickForm(ServerLevel level, int age) {
        if (!(this.getOwner() instanceof LivingEntity owner) || !owner.isAlive() || owner.isSpectator()) {
            this.discard();
            return;
        }
        this.setPos(this.safeHand(level, owner));
        if (age >= FORM_TICKS) {
            this.begin(Phase.FLIGHT);
            this.playAt(level, NarutoSounds.RASENSHURIKEN_FLIGHT.get(), 0.90f);
        }
    }

    private void tickFlight(ServerLevel level, int age) {
        if (age > FLIGHT_TICKS) {
            this.detonate(level, null);
            return;
        }
        Vec3 from = this.position();
        Vec3 end = from.add(WindVortex.velocity(this.heading, age));
        if (!level.hasChunkAt(BlockPos.containing(end))) {
            this.discard();
            return;
        }
        HitResult hit = this.findContact(level, from, end);
        if (hit != null && !ForgeEventFactory.onProjectileImpact(this, hit)) {
            this.detonate(level, hit);
        } else {
            this.setPos(end);
        }
    }

    @Nullable
    private HitResult findContact(ServerLevel level, Vec3 from, Vec3 end) {
        BlockHitResult block = this.clip(level, from, end);
        boolean blocked = block.getType() == HitResult.Type.BLOCK;
        Vec3 limit = blocked ? block.getLocation() : end;
        double best = from.distanceToSqr(limit);
        HitResult nearest = blocked ? block : null;
        Entity owner = this.getOwner();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, limit).inflate(0.24),
                candidate -> this.canDamage(owner, candidate))) {
            var contact = WaterDragonFlight.sweep(from, limit, target.getBoundingBox(), 0.24);
            if (contact != null && contact.center().distanceToSqr(from) < best
                    && this.clip(level, from, contact.point()).getType() == HitResult.Type.MISS) {
                best = contact.center().distanceToSqr(from);
                nearest = new EntityHitResult(target, contact.point());
            }
        }
        return nearest;
    }

    private void detonate(ServerLevel level, @Nullable HitResult hit) {
        if (this.phase() != Phase.FLIGHT) {
            return;
        }
        if (hit != null) {
            this.setPos(hit.getLocation());
            if (hit instanceof EntityHitResult entityHit) {
                this.direct = entityHit.getEntity().getUUID();
            } else if (hit instanceof BlockHitResult block) {
                this.coverOffset = Vec3.atLowerCornerOf(block.getDirection().getNormal()).scale(0.035);
            }
        }
        this.begin(Phase.VORTEX);
        this.playAt(level, NarutoSounds.RASENSHURIKEN_BLOOM.get(), 1.3f);
    }

    private void tickVortex(ServerLevel level, int age) {
        if (age > WindVortex.LIFETIME) {
            this.discard();
            return;
        }
        Vec3 center = this.position();
        Vec3 checkFrom = center.add(this.coverOffset);
        double radius = WindVortex.radius(age);
        Entity owner = this.getOwner();
        var source = owner == null ? this.damageSources().magic() : this.damageSources().indirectMagic(this, owner);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                candidate -> this.canDamage(owner, candidate))) {
            Vec3 near = WaterDragonFlight.nearestPoint(target.getBoundingBox(), checkFrom);
            if (center.distanceToSqr(near) > radius * radius
                    || (checkFrom.distanceToSqr(near) > 1.0e-8
                    && this.clip(level, checkFrom, near).getType() != HitResult.Type.MISS)
                    || !this.vortex.mark(target.getUUID())) {
                continue;
            }
            float base = this.bossCast ? 20f : target.getUUID().equals(this.direct) ? 45f : 25f;
            float amount = base * this.multiplier;
            if (target instanceof Player && owner instanceof Player) {
                amount = Math.min(amount, target.getHealth() - 1.0f);
            }
            if (amount > 0 && target.hurt(source, amount)) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, true));
            }
        }
    }

    private boolean canDamage(@Nullable Entity owner, LivingEntity target) {
        if (!target.isAlive() || !target.isPickable() || target.isSpectator() || target.getUUID().equals(this.casterId)
                || Faction.sameSide(owner, target) || (owner != null && owner.isAlliedTo(target))) {
            return false;
        }
        if (target instanceof Player victim) {
            return !victim.isCreative() && owner != null
                    && (!(owner instanceof Player player) || player.canHarmPlayer(victim));
        }
        return true;
    }

    private void begin(Phase phase) {
        this.entityData.set(PHASE, (byte) phase.ordinal());
        this.entityData.set(AGE, 0);
    }

    private BlockHitResult clip(ServerLevel level, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
    }

    private void playAt(ServerLevel level, net.minecraft.sounds.SoundEvent sound, float volume) {
        level.playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.PLAYERS, volume, 1.0f);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(this.phase() == Phase.VORTEX ? WindVortex.RADIUS : 2);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Phase", (byte) this.phase().ordinal());
        tag.putInt("PhaseAge", this.phaseAge());
        tag.putFloat("WindDamage", this.multiplier);
        tag.putBoolean("BossWind", this.bossCast);
        tag.put("WindHeading", this.newDoubleList(this.heading.x, this.heading.y, this.heading.z));
        tag.put("WindCover", this.newDoubleList(this.coverOffset.x, this.coverOffset.y, this.coverOffset.z));
        if (this.casterId != null) {
            tag.putUUID("WindCaster", this.casterId);
        }
        if (this.direct != null) {
            tag.putUUID("WindDirect", this.direct);
        }
        ListTag marks = new ListTag();
        for (UUID id : this.vortex.marks()) {
            marks.add(NbtUtils.createUUID(id));
        }
        tag.put("WindMarks", marks);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(PHASE, tag.contains("Phase")
                ? (byte) Mth.clamp(tag.getByte("Phase"), 0, Phase.VORTEX.ordinal()) : (byte) Phase.FLIGHT.ordinal());
        this.entityData.set(AGE, Mth.clamp(tag.getInt("PhaseAge"), 0, FLIGHT_TICKS));
        this.multiplier = tag.contains("WindDamage") ? Math.max(0, tag.getFloat("WindDamage")) : 1.0f;
        this.bossCast = tag.getBoolean("BossWind");
        this.heading = readVector(tag, tag.contains("WindHeading") ? "WindHeading" : "power", this.getLookAngle())
                .normalize();
        this.coverOffset = readVector(tag, "WindCover", Vec3.ZERO);
        this.casterId = tag.hasUUID("WindCaster") ? tag.getUUID("WindCaster")
                : tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        this.direct = tag.hasUUID("WindDirect") ? tag.getUUID("WindDirect") : null;
        for (Tag mark : tag.getList("WindMarks", Tag.TAG_INT_ARRAY)) {
            this.vortex.mark(NbtUtils.loadUUID(mark));
        }
    }

    private static Vec3 readVector(CompoundTag tag, String key, Vec3 fallback) {
        ListTag values = tag.getList(key, Tag.TAG_DOUBLE);
        return values.size() == 3 ? new Vec3(values.getDouble(0), values.getDouble(1), values.getDouble(2)) : fallback;
    }

    public enum Phase {
        FORM, FLIGHT, VORTEX
    }
}
