package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.util.WaterDragonFlight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.function.Predicate;

final class ProjectileVolume {
    private ProjectileVolume() {
    }

    @Nullable
    static Impact first(Projectile shot, ServerLevel level, Vec3 from, Vec3 to, double radius,
                        Predicate<LivingEntity> hittable) {
        AABB scan = new AABB(from, to).inflate(radius + 0.0001);
        WaterDragonFlight.Contact best = null;
        LivingEntity direct = null;
        for (var shape : level.getBlockCollisions(shot, scan)) {
            for (var box : shape.toAabbs()) {
                var hit = WaterDragonFlight.sweep(from, to, box, radius);
                if (hit != null && hit.precedes(best)) {
                    best = hit;
                }
            }
        }
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, scan, hittable)) {
            var hit = WaterDragonFlight.sweep(from, to, target.getBoundingBox(), radius);
            if (hit != null && hit.precedes(best) && visible(shot, hit.center(), hit.point())) {
                best = hit;
                direct = target;
            }
        }
        if (best == null) {
            return null;
        }
        if (direct != null) {
            return new Impact(new EntityHitResult(direct, best.point()), best.normal(), direct);
        }
        Vec3 normal = Vec3.atLowerCornerOf(best.normal().getNormal());
        BlockPos block = BlockPos.containing(best.point().subtract(normal.scale(0.012)));
        return new Impact(new BlockHitResult(best.point(), best.normal(), block, false), best.normal(), null);
    }

    static boolean visible(Projectile shot, Vec3 from, Vec3 to) {
        return from.distanceToSqr(to) < 1.0e-8 || clip(shot, from, to, ClipContext.Fluid.NONE).getType() == HitResult.Type.MISS;
    }

    static BlockHitResult clip(Projectile shot, Vec3 from, Vec3 to, ClipContext.Fluid fluid) {
        return shot.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, fluid, shot));
    }

    record Impact(HitResult result, Direction face, @Nullable LivingEntity target) {
    }
}
