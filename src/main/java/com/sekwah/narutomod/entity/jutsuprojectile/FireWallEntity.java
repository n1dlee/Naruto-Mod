package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.FireFront;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FireWallEntity extends Entity {
    private static final EntityDataAccessor<Integer> TIME =
            SynchedEntityData.defineId(FireWallEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CompoundTag> STOPS =
            SynchedEntityData.defineId(FireWallEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Boolean> PRIMARY =
            SynchedEntityData.defineId(FireWallEntity.class, EntityDataSerializers.BOOLEAN);
    private static final double[] PROBE_LEVELS = {0.20, 1.35, 2.55};
    private static final int MAX_HITS = 256;
    private final Set<UUID> touched = new HashSet<>();

    @Nullable
    private UUID casterId;
    private float damage = 5f;

    public FireWallEntity(EntityType<FireWallEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void start(LivingEntity owner, Vec3 aim, float damage, int waves) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 horizontal = aim.multiply(1, 0, 1);
        if (horizontal.lengthSqr() < 1.0e-8) {
            horizontal = Vec3.directionFromRotation(0, owner.getYRot());
        }
        for (int i = 0; i < waves; i++) {
            var wave = new FireWallEntity(NarutoEntities.FIRE_WALL.get(), level);
            wave.casterId = owner.getUUID();
            wave.damage = damage;
            wave.setPos(owner.position().add(0, 0.04, 0));
            wave.setYRot((float) Math.toDegrees(Math.atan2(horizontal.z, horizontal.x)) - 90.0f);
            wave.entityData.set(TIME, -FireFront.WINDUP - i * 5);
            wave.entityData.set(PRIMARY, i == 0);
            level.addFreshEntity(wave);
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TIME, -FireFront.WINDUP);
        this.entityData.define(STOPS, new CompoundTag());
        this.entityData.define(PRIMARY, true);
    }

    public int age() {
        return this.entityData.get(TIME);
    }

    public boolean primary() {
        return this.entityData.get(PRIMARY);
    }

    public double stop(int lane) {
        CompoundTag stops = this.entityData.get(STOPS);
        return stops.contains("D" + lane) ? stops.getFloat("D" + lane) : FireFront.RANGE;
    }

    public boolean quenched(int lane) {
        return this.entityData.get(STOPS).getBoolean("W" + lane);
    }

    public Vec3 forward() {
        return Vec3.directionFromRotation(0, this.getYRot());
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(FireFront.RANGE + 2, 4, FireFront.RANGE + 2);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        int age = this.age() + 1;
        this.entityData.set(TIME, age);
        Entity owner = this.casterId == null ? null : level.getEntity(this.casterId);
        if (age <= 0) {
            if (!(owner instanceof LivingEntity caster) || !caster.isAlive() || caster.isSpectator()) {
                this.discard();
            } else {
                this.setPos(caster.position().add(0, 0.04, 0));
                if (age == 0 && this.primary()) {
                    level.playSound(null, this.blockPosition(), NarutoSounds.FIREBALL_EXHALE.get(),
                            SoundSource.PLAYERS, 1.45f, 0.61f);
                }
            }
            return;
        }
        if (age > 22) {
            this.discard();
            return;
        }
        double from = FireFront.depth(age - 1);
        double to = FireFront.depth(age);
        if (to > from) {
            this.traceLanes(level, from, to);
            this.hitFront(level, owner, from, to);
            if (this.primary() && age == 8) {
                Vec3 center = this.position().add(this.forward().scale(Math.min(to, this.stop(FireFront.LANES / 2))));
                level.playSound(null, center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.FIRE_AMBIENT,
                        SoundSource.PLAYERS, 1.05f, 0.70f);
            }
        }
    }

    private void traceLanes(ServerLevel level, double from, double to) {
        CompoundTag previous = this.entityData.get(STOPS);
        CompoundTag update = previous.copy();
        Vec3 heading = this.forward();
        Vec3 origin = this.position();
        boolean steam = false;
        boolean fire = false;
        for (int lane = 0; lane < FireFront.LANES; lane++) {
            if (previous.contains("D" + lane)) {
                continue;
            }
            Vec3 end = FireFront.point(origin, heading, lane, to, 0);
            if (!level.hasChunkAt(BlockPos.containing(end))) {
                update.putFloat("D" + lane, (float) from);
                continue;
            }
            var contact = this.clippingContact(level, origin, heading, lane, from, to);
            if (contact == null) {
                continue;
            }
            Vec3 point = contact.getLocation();
            boolean wet = level.getFluidState(contact.getBlockPos()).is(FluidTags.WATER);
            float depth = (float) Math.max(from, point.subtract(origin).dot(heading) - 0.014);
            update.putFloat("D" + lane, depth);
            update.putBoolean("W" + lane, wet);
            level.sendParticles(wet ? ParticleTypes.CLOUD : ParticleTypes.FLAME, point.x, point.y, point.z,
                    wet ? 5 : 3, 0.19, 0.22, 0.19, 0.035);
            if (wet && !steam) {
                steam = true;
                level.playSound(null, point.x, point.y, point.z, NarutoSounds.FIREBALL_STEAM.get(),
                        SoundSource.PLAYERS, 0.65f, 0.88f);
            } else if (!wet && !fire) {
                fire = true;
                level.playSound(null, point.x, point.y, point.z, NarutoSounds.FIREBALL_EXPLODE.get(),
                        SoundSource.PLAYERS, 0.44f, 0.63f);
            }
        }
        if (!update.equals(previous)) {
            this.entityData.set(STOPS, update);
        }
    }

    @Nullable
    private BlockHitResult clippingContact(ServerLevel level, Vec3 origin, Vec3 heading, int lane, double from, double to) {
        BlockHitResult closest = null;
        double limit = to;
        for (double y : PROBE_LEVELS) {
            Vec3 start = FireFront.point(origin, heading, lane, from, y);
            Vec3 end = FireFront.point(origin, heading, lane, to, y);
            BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.WATER, this));
            double depth = hit.getLocation().subtract(origin).dot(heading);
            if (hit.getType() == HitResult.Type.BLOCK && depth <= limit) {
                closest = hit;
                limit = depth;
            }
        }
        return closest;
    }

    private void hitFront(ServerLevel level, @Nullable Entity owner, double from, double to) {
        Vec3 origin = this.position();
        Vec3 heading = this.forward();
        AABB area = new AABB(origin, origin.add(heading.scale(to)).add(0, FireFront.HEIGHT, 0))
                .inflate(Math.abs(FireFront.laneOffset(0, to)) + 0.4, 0.2, Math.abs(FireFront.laneOffset(0, to)) + 0.4);
        var source = NarutoDamageTypes.getDamageSource(level, NarutoDamageTypes.FIREBALL, this, owner);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                victim -> this.canDamage(owner, victim))) {
            Vec3 relative = target.getBoundingBox().getCenter().subtract(origin);
            double depth = relative.dot(heading);
            int lane = FireFront.lane(relative, heading, depth);
            if (lane < 0 || depth < from || depth > Math.min(to, this.stop(lane))
                    || relative.y < 0 || relative.y > FireFront.HEIGHT) {
                continue;
            }
            Vec3 rayHeight = origin.add(0, relative.y, 0);
            var hit = level.clip(new ClipContext(rayHeight, origin.add(relative), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.WATER, this));
            if (hit.getType() == HitResult.Type.MISS && this.touched.size() < MAX_HITS
                    && this.touched.add(target.getUUID()) && target.hurt(source, this.damage)) {
                target.setSecondsOnFire(5);
            }
        }
    }

    private boolean canDamage(@Nullable Entity owner, LivingEntity target) {
        if (!target.isAlive() || target.isSpectator() || !target.isPickable()
                || target.getUUID().equals(this.casterId) || Faction.sameSide(owner, target)) {
            return false;
        }
        if (target instanceof Player player) {
            return !player.isCreative() && owner != null
                    && (!(owner instanceof Player caster) || caster.canHarmPlayer(player));
        }
        return owner == null || !owner.isAlliedTo(target);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("WallTime", this.age());
        tag.putBoolean("WallPrimary", this.primary());
        tag.put("WallStops", this.entityData.get(STOPS).copy());
        tag.putFloat("WallDamage", this.damage);
        if (this.casterId != null) {
            tag.putUUID("WallCaster", this.casterId);
        }
        ListTag marked = new ListTag();
        this.touched.forEach(id -> marked.add(NbtUtils.createUUID(id)));
        tag.put("WallHits", marked);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(TIME, tag.getInt("WallTime"));
        this.entityData.set(PRIMARY, tag.getBoolean("WallPrimary"));
        this.entityData.set(STOPS, tag.getCompound("WallStops"));
        this.damage = tag.getFloat("WallDamage");
        this.casterId = tag.hasUUID("WallCaster") ? tag.getUUID("WallCaster") : null;
        for (Tag hit : tag.getList("WallHits", Tag.TAG_INT_ARRAY)) {
            if (this.touched.size() >= MAX_HITS) {
                break;
            }
            this.touched.add(NbtUtils.loadUUID(hit));
        }
    }
}
