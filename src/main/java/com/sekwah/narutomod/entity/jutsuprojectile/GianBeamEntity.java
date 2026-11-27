package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.GianLine;
import com.sekwah.narutomod.util.WaterDragonFlight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;

public class GianBeamEntity extends Projectile {
    private static final EntityDataAccessor<Integer> AGE =
            SynchedEntityData.defineId(GianBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LENGTH =
            SynchedEntityData.defineId(GianBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> MASK =
            SynchedEntityData.defineId(GianBeamEntity.class, EntityDataSerializers.BOOLEAN);
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private float damage;

    public GianBeamEntity(EntityType<GianBeamEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static boolean launch(LivingEntity owner, Vec3 aim, float damage, boolean mask) {
        if (!(owner.level() instanceof ServerLevel level) || !owner.isAlive() || owner.isSpectator()) {
            return false;
        }
        var beam = new GianBeamEntity(NarutoEntities.GIAN_BEAM.get(), level);
        beam.setOwner(owner);
        beam.damage = damage;
        beam.entityData.set(MASK, mask);
        Vec3 heading = aim.subtract(beam.anchor(owner)).normalize();
        if (heading.lengthSqr() == 0) {
            return false;
        }
        beam.setRot((float) Math.toDegrees(Math.atan2(heading.z, heading.x)) - 90,
                (float) -Math.toDegrees(Math.atan2(heading.y, heading.horizontalDistance())));
        beam.setPos(beam.muzzle(owner));
        boolean added = level.addFreshEntity(beam);
        if (added) {
            level.playSound(null, beam.getX(), beam.getY(), beam.getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                    SoundSource.HOSTILE, 0.42f, 1.6f);
        }
        return added;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(AGE, 0);
        this.entityData.define(LENGTH, 0f);
        this.entityData.define(MASK, false);
    }

    public int age() {
        return this.entityData.get(AGE);
    }

    public float length() {
        return this.entityData.get(LENGTH);
    }

    public boolean mask() {
        return this.entityData.get(MASK);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().expandTowards(this.getLookAngle().scale(this.length())).inflate(1.5);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        int age = this.age() + 1;
        this.entityData.set(AGE, age);
        if (age < GianLine.PREPARE) {
            if (this.getOwner() instanceof LivingEntity owner && owner.isAlive() && !owner.isSpectator()) {
                this.setPos(this.muzzle(owner));
            } else {
                this.discard();
            }
        } else if (age == GianLine.PREPARE) {
            if (this.getOwner() instanceof LivingEntity owner && owner.isAlive() && !owner.isSpectator()) {
                this.setPos(this.muzzle(owner));
                this.discharge(level, owner);
            } else {
                this.discard();
            }
        } else if (age > GianLine.PREPARE + GianLine.SHOCK + GianLine.FADE) {
            this.discard();
        }
    }

    private Vec3 anchor(LivingEntity owner) {
        Vec3 eye = owner.getEyePosition().add(0, -0.16, 0);
        if (!this.mask()) {
            return eye;
        }
        Vec3 side = Vec3.directionFromRotation(0, owner.getYRot()).cross(UP);
        return eye.add(side.scale(0.67)).add(0, -0.13, 0);
    }

    private Vec3 muzzle(LivingEntity owner) {
        Vec3 eye = owner.getEyePosition().add(0, -0.16, 0);
        Vec3 intended = this.anchor(owner).add(this.getLookAngle().scale(this.mask() ? 0.53 : 0.38));
        var wall = ProjectileVolume.clip(this, eye, intended, ClipContext.Fluid.NONE);
        return wall.getType() == HitResult.Type.MISS ? intended
                : wall.getLocation().add(eye.subtract(intended).normalize().scale(0.04));
    }

    private void discharge(ServerLevel level, LivingEntity owner) {
        Vec3 from = this.position();
        Vec3 aim = this.getLookAngle();
        Vec3 requested = loadedTip(level, from, aim);
        var wall = this.firstWall(level, from, requested);
        Vec3 until = wall == null ? requested : wall.center();
        this.entityData.set(LENGTH, (float) from.distanceTo(until));
        this.strike(level, owner, new Trace(from, requested, until, wall));
        if (wall != null) {
            Vec3 p = wall.point();
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 9, 0.12, 0.12, 0.12, 0.07);
        }
        level.playSound(null, until.x, until.y, until.z, NarutoSounds.GIAN_CRACK.get(),
                SoundSource.HOSTILE, this.mask() ? 1.3f : 0.95f, this.mask() ? 0.82f : 1.05f);
    }

    private static Vec3 loadedTip(ServerLevel level, Vec3 from, Vec3 aim) {
        Vec3 tip = from;
        for (double d = 0.5; d <= GianLine.RANGE; d += 0.5) {
            Vec3 point = from.add(aim.scale(d));
            BlockPos pos = BlockPos.containing(point);
            if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos) || level.isOutsideBuildHeight(pos)) {
                break;
            }
            tip = point;
        }
        return tip;
    }

    @Nullable
    private WaterDragonFlight.Contact firstWall(ServerLevel level, Vec3 from, Vec3 to) {
        WaterDragonFlight.Contact nearest = null;
        AABB scan = new AABB(from, to).inflate(GianLine.RADIUS + 0.0001);
        for (var shape : level.getBlockCollisions(this, scan)) {
            for (var bounds : shape.toAabbs()) {
                var hit = GianLine.contact(from, to, bounds);
                if (hit != null && hit.precedes(nearest)) {
                    nearest = hit;
                }
            }
        }
        return nearest;
    }

    private void strike(ServerLevel level, LivingEntity owner, Trace trace) {
        if (trace.from().distanceToSqr(trace.until()) < 1.0e-8) {
            return;
        }
        var targets = new ArrayList<Target>();
        AABB scan = new AABB(trace.from(), trace.until()).inflate(GianLine.RADIUS);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, scan, candidate -> eligible(owner, candidate))) {
            var hit = GianLine.contact(trace.from(), trace.requested(), entity.getBoundingBox());
            if (hit != null && GianLine.precedesWall(hit, trace.wall()) && ProjectileVolume.visible(this, hit.center(), hit.point())) {
                targets.add(new Target(entity, hit));
            }
        }
        targets.sort(Comparator.comparingDouble(target -> target.hit().fraction()));
        float left = this.damage;
        var source = NarutoDamageTypes.getDamageSource(level, NarutoDamageTypes.CHIDORI, this, owner, trace.from());
        for (Target contact : targets) {
            if (left < 1) {
                break;
            }
            boolean accepted = contact.entity().hurt(source, left);
            if (accepted) {
                Vec3 p = contact.hit().point();
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 7, 0.12, 0.16, 0.12, 0.06);
            }
            left = GianLine.nextDamage(left, accepted);
        }
    }

    private static boolean eligible(LivingEntity owner, LivingEntity target) {
        if (target == owner || !target.isAlive() || !target.isPickable() || target.isSpectator()
                || owner.isAlliedTo(target) || Faction.sameSide(owner, target)) {
            return false;
        }
        return !(target instanceof Player player)
                || (!player.isCreative() && (!(owner instanceof Player caster) || caster.canHarmPlayer(player)));
    }

    private record Trace(Vec3 from, Vec3 requested, Vec3 until, @Nullable WaterDragonFlight.Contact wall) {
    }

    private record Target(LivingEntity entity, WaterDragonFlight.Contact hit) {
    }
}
