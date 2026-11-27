package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.network.s2c.WaterDragonImpactPacket.Kind;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.WaterDragonFlight;
import com.sekwah.narutomod.util.WaterDragonFlight.Contact;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

final class WaterDragonCollision {
    private static final float SPLASH_FACTOR = 8.0f / 18.0f;
    private WaterDragonCollision() {
    }

    @Nullable
    static Impact sweep(ChakraDragonEntity dragon, ServerLevel level, Vec3 from, Vec3 to,
                        double headRadius, @Nullable LivingEntity caster) {
        AABB bounds = new AABB(from, to).inflate(headRadius + 1.0e-5);
        Contact best = null;
        for (var shape : level.getBlockCollisions(dragon, bounds)) {
            for (AABB box : shape.toAabbs()) {
                Contact contact = WaterDragonFlight.sweep(from, to, box, headRadius);
                if (contact != null && contact.precedes(best)) {
                    best = contact;
                }
            }
        }

        LivingEntity direct = null;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, bounds,
                victim -> canHit(caster, dragon.ownerId(), victim))) {
            Contact contact = WaterDragonFlight.sweep(from, to, candidate.getBoundingBox(), headRadius);
            if (contact != null && contact.precedes(best)
                    && hasSplashSight(dragon, level, contact.center(), contact.point())) {
                best = contact;
                direct = candidate;
            }
        }
        if (best == null) {
            return null;
        }
        Vec3 outward = direct == null ? Vec3.atLowerCornerOf(best.normal().getNormal()) : flightAxis(dragon, from, to);
        return new Impact(best.center(), best.point(), outward, direct, direct == null ? Kind.BLOCK : Kind.ENTITY);
    }

    static Vec3 flightAxis(ChakraDragonEntity dragon, Vec3 from, Vec3 to) {
        Vec3 travel = to.subtract(from);
        return travel.lengthSqr() > 1.0e-8 ? travel.normalize() : dragon.getLookAngle();
    }

    private static boolean hasSplashSight(ChakraDragonEntity dragon, ServerLevel level, Vec3 start, Vec3 end) {
        return start.distanceToSqr(end) < 1.0e-8 || level.clip(
                new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon)
        ).getType() == HitResult.Type.MISS;
    }

    static void damage(ChakraDragonEntity dragon, ServerLevel level, @Nullable LivingEntity caster,
                       Impact contact, float damage, double radius) {
        Vec3 point = contact.point();
        LivingEntity direct = contact.direct();
        DamageSource source = NarutoDamageTypes.getDamageSource(level, NarutoDamageTypes.WATER_BULLET,
                dragon, caster, point);
        if (direct != null) {
            hit(direct, source, point, damage);
        }
        Vec3 origin = contact.kind() == Kind.BLOCK ? point.add(contact.axis().scale(0.025)) : point;
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(radius),
                target -> target != direct && canHit(caster, dragon.ownerId(), target))) {
            Vec3 nearest = WaterDragonFlight.nearestPoint(victim.getBoundingBox(), origin);
            if (point.distanceToSqr(nearest) <= radius * radius && hasSplashSight(dragon, level, origin, nearest)) {
                hit(victim, source, point, damage * SPLASH_FACTOR);
            }
        }
    }

    private static boolean canHit(@Nullable LivingEntity caster, @Nullable UUID ownerId, LivingEntity victim) {
        if (!victim.isAlive() || victim.isSpectator() || victim.getUUID().equals(ownerId)
                || Faction.sameSide(caster, victim)
                || (ownerId != null && Faction.ownerOf(victim).filter(ownerId::equals).isPresent())) {
            return false;
        }
        if (victim instanceof Player player) {
            return !player.isCreative() && (caster != null || ownerId == null)
                    && (caster == null || !caster.isAlliedTo(player))
                    && (!(caster instanceof Player source) || source.canHarmPlayer(player));
        }
        return caster == null || !caster.isAlliedTo(victim);
    }

    private static void hit(LivingEntity target, DamageSource source, Vec3 point, float damage) {
        if (target.hurt(source, damage)) {
            Vec3 impulse = target.position().subtract(point).multiply(1, 0, 1).normalize().scale(0.9);
            target.push(impulse.x, 0.45, impulse.z);
            target.hurtMarked = true;
            target.clearFire();
        }
    }

    record Impact(Vec3 center, Vec3 point, Vec3 axis, @Nullable LivingEntity direct, Kind kind) {
    }
}
