package com.sekwah.narutomod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class SubstitutionRoute {
    public static final int MAX_MARKER_DISTANCE = 70;
    public static final double MARKER_LIFT = 2.0;

    private SubstitutionRoute() {
    }

    public static boolean usesMarker(Vec3 current, @Nullable Vec3 marker, ResourceLocation here,
                                     @Nullable ResourceLocation dimension) {
        return marker != null && here.equals(dimension)
                && current.distanceToSqr(marker.subtract(0, MARKER_LIFT, 0)) <= MAX_MARKER_DISTANCE * MAX_MARKER_DISTANCE;
    }

    @Nullable
    public static Vec3 safeAt(ServerLevel level, Player player, Vec3 requested, double search) {
        BlockPos column = BlockPos.containing(requested);
        if (!level.hasChunkAt(column) || !level.getWorldBorder().isWithinBounds(column)) {
            return null;
        }
        var hit = level.clip(new ClipContext(requested.add(0, search, 0), requested.add(0, -search, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) {
            return null;
        }
        Vec3 feet = new Vec3(requested.x, hit.getLocation().y + 0.015, requested.z);
        var bounds = player.getBoundingBox().move(feet.subtract(player.position()));
        var floor = level.getBlockState(hit.getBlockPos());
        if (!level.getWorldBorder().isWithinBounds(bounds) || !level.noCollision(player, bounds)
                || level.containsAnyLiquid(bounds) || floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.CACTUS)
                || floor.is(Blocks.CAMPFIRE) || floor.is(Blocks.SOUL_CAMPFIRE)
                || level.getBlockState(BlockPos.containing(feet)).is(Blocks.FIRE)
                || level.getBlockState(BlockPos.containing(feet)).is(Blocks.SOUL_FIRE)) {
            return null;
        }
        return feet;
    }
}
