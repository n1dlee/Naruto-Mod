package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.util.FireballShape;
import com.sekwah.narutomod.util.WaterDragonFlight;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;

final class FireballContact {
    private FireballContact() {
    }

    @Nullable
    static HitResult first(GreatFireballEntity ball, ServerLevel level, Vec3 from, Vec3 to, double radius) {
        AABB range = new AABB(from, to).inflate(radius + 1.0e-5);
        WaterDragonFlight.Contact best = null;
        LivingEntity direct = null;
        for (var shape : level.getBlockCollisions(ball, range)) {
            for (AABB box : shape.toAabbs()) {
                var hit = WaterDragonFlight.sweep(from, to, box, radius);
                if (hit != null && hit.precedes(best)) {
                    best = hit;
                }
            }
        }
        Entity owner = ball.getOwner();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, range,
                candidate -> ball.canHit(owner, candidate))) {
            var hit = WaterDragonFlight.sweep(from, to, target.getBoundingBox(), radius);
            if (hit != null && hit.precedes(best) && visible(ball, level, hit.center(), hit.point())) {
                best = hit;
                direct = target;
            }
        }
        if (best == null) {
            return null;
        }
        if (direct != null) {
            return new EntityHitResult(direct, best.point());
        }
        Vec3 out = Vec3.atLowerCornerOf(best.normal().getNormal());
        return new BlockHitResult(best.point(), best.normal(), BlockPos.containing(best.point().subtract(out.scale(0.012))), false);
    }

    static void damage(GreatFireballEntity ball, ServerLevel level, @Nullable Entity owner, float multiplier,
                       @Nullable HitResult contact) {
        Vec3 center = ball.position();
        Vec3 outward = contact instanceof BlockHitResult block ? Vec3.atLowerCornerOf(block.getDirection().getNormal())
                : Vec3.ZERO;
        Vec3 origin = center.add(outward.scale(0.04));
        double radius = FireballShape.blast(ball.charge());
        var source = NarutoDamageTypes.getDamageSource(level, NarutoDamageTypes.FIREBALL, ball, owner, center);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                victim -> ball.canHit(owner, victim))) {
            Vec3 point = WaterDragonFlight.nearestPoint(target.getBoundingBox(), origin);
            double fraction = center.distanceTo(point) / radius;
            float damage = FireballShape.damage(ball.charge(), fraction) * multiplier;
            if (damage > 0 && visible(ball, level, origin, point) && target.hurt(source, damage)) {
                target.setSecondsOnFire(Math.max(1, (int) Math.ceil(6.0 * (1 - fraction))));
            }
        }
        if (contact instanceof BlockHitResult block && owner != null
                && ForgeEventFactory.getMobGriefingEvent(level, owner)) {
            igniteFloor(level, owner, block);
        }
    }

    private static void igniteFloor(ServerLevel level, Entity owner, BlockHitResult hit) {
        BlockPos center = hit.getBlockPos().relative(hit.getDirection());
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            if (!level.isEmptyBlock(pos) || level.random.nextFloat() > 0.38f
                    || (owner instanceof Player player && !level.mayInteract(player, pos))) {
                continue;
            }
            var fire = BaseFireBlock.getState(level, pos);
            if (fire.canSurvive(level, pos)) {
                level.setBlockAndUpdate(pos, fire);
            }
        }
    }

    private static boolean visible(GreatFireballEntity ball, ServerLevel level, Vec3 from, Vec3 to) {
        return from.distanceToSqr(to) < 1.0e-8 || ball.clip(level, from, to, ClipContext.Fluid.NONE).getType() == HitResult.Type.MISS;
    }
}
