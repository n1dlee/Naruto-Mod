package com.sekwah.narutomod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public final class KamuiStep {
    private static final double MAX_DRIFT_SQR = 0.81;
    private static final double MIN_TRAVEL_SQR = 4.0;

    private KamuiStep() {
    }

    public static Vec3 facing(float yaw) {
        double radians = Math.toRadians(yaw);
        return new Vec3(-Math.sin(radians), 0, Math.cos(radians));
    }

    @Nullable
    public static Vec3 choose(Vec3 from, float yaw, Predicate<Vec3> accepts) {
        Vec3 forward = facing(yaw);
        for (double length = 6; length >= 3; length -= 1.5) {
            Vec3 base = from.add(forward.scale(length));
            for (int lift = 0; lift <= 2; lift++) {
                Vec3 candidate = base.add(0, lift, 0);
                if (accepts.test(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    public static boolean canRelease(Vec3 origin, Vec3 current, Vec3 destination, Predicate<Vec3> accepts) {
        return origin.distanceToSqr(current) <= MAX_DRIFT_SQR
                && current.distanceToSqr(destination) >= MIN_TRAVEL_SQR && accepts.test(destination);
    }

    public static AABB arrivalBounds(LivingEntity actor, Vec3 feet) {
        return actor.getDimensions(net.minecraft.world.entity.Pose.STANDING).makeBoundingBox(feet);
    }

    public static boolean safe(ServerLevel level, LivingEntity actor, Vec3 feet) {
        AABB body = arrivalBounds(actor, feet);
        BlockPos min = BlockPos.containing(body.minX, body.minY, body.minZ);
        BlockPos max = BlockPos.containing(body.maxX, body.maxY, body.maxZ);
        var border = level.getWorldBorder();
        if (!loaded(level, min, max) || !insideBorder(body, border.getMinX(), border.getMinZ(), border.getMaxX(), border.getMaxZ())
                || !level.noCollision(actor, body) || level.containsAnyLiquid(body)
                || !level.getEntitiesOfClass(LivingEntity.class, body,
                target -> target != actor && target.isAlive() && !target.isSpectator()).isEmpty()) {
            return false;
        }
        return !hazard(level, body);
    }

    public static boolean insideBorder(AABB body, double minX, double minZ, double maxX, double maxZ) {
        return body.minX >= minX && body.minZ >= minZ && body.maxX <= maxX && body.maxZ <= maxZ;
    }

    private static boolean loaded(ServerLevel level, BlockPos min, BlockPos max) {
        return min.getY() > level.getMinBuildHeight() && max.getY() < level.getMaxBuildHeight()
                && level.hasChunkAt(min) && level.hasChunkAt(max)
                && level.hasChunkAt(new BlockPos(min.getX(), min.getY(), max.getZ()))
                && level.hasChunkAt(new BlockPos(max.getX(), min.getY(), min.getZ()));
    }

    private static boolean hazard(ServerLevel level, AABB body) {
        BlockPos min = BlockPos.containing(body.minX + 0.001, body.minY - 0.11, body.minZ + 0.001);
        BlockPos max = BlockPos.containing(body.maxX - 0.001, body.maxY - 0.001, body.maxZ - 0.001);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            var state = level.getBlockState(pos);
            if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.MAGMA_BLOCK)
                    || state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.POWDER_SNOW)
                    || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE) || !state.getFluidState().isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
