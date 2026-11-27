package com.sekwah.narutomod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class SurfaceSupport {
    public static final double GRIP_REACH = 0.12;
    private static final double[][] PROBES = {{0.34, 0}, {0.60, 0}, {0.83, 0}, {0.50, -0.24}, {0.50, 0.24}};

    private SurfaceSupport() {
    }

    @Nullable
    public static BlockHitResult find(Player player, Direction face, double gap) {
        AABB box = player.getBoundingBox();
        Vec3 center = box.getCenter();
        Vec3 normal = SurfaceStride.normal(face);
        double reach = (face.getAxis().isVertical() ? box.getYsize() : box.getXsize()) * 0.5 + gap;
        Vec3 lateral = face.getAxis() == Direction.Axis.X ? new Vec3(0, 0, 1) : new Vec3(1, 0, 0);
        for (double[] probe : PROBES) {
            Vec3 start = face.getAxis().isVertical()
                    ? center.add(probe[1], 0, box.getZsize() * (probe[0] - 0.5))
                    : new Vec3(center.x, box.minY + probe[0] * box.getYsize(), center.z).add(lateral.scale(probe[1]));
            Vec3 end = start.add(normal.scale(reach));
            BlockHitResult hit = player.level().clip(
                    new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK && hit.getDirection() == face.getOpposite()) {
                return hit;
            }
        }
        return null;
    }

    @Nullable
    public static Vec3 waterAtFeet(Player player, Vec3 point) {
        BlockPos pos = BlockPos.containing(point.x, player.getY() - 0.14, point.z);
        var state = player.level().getFluidState(pos);
        if (!state.is(FluidTags.WATER) || player.level().getBlockState(pos).blocksMotion()) {
            return null;
        }
        double y = pos.getY() + state.getHeight(player.level(), pos);
        return Math.abs(y - player.getY()) <= 0.18 ? new Vec3(point.x, y + 0.016, point.z) : null;
    }

    public static Vec3 pivot(Player player, Direction face, @Nullable BlockHitResult support) {
        Vec3 normal = SurfaceStride.normal(face);
        double reach = support != null ? support.getLocation().subtract(player.position()).dot(normal)
                : (face.getAxis().isVertical() ? player.getBbHeight() : player.getBbWidth() * 0.5);
        return normal.scale(Math.max(0.0, reach - 0.012));
    }
}
