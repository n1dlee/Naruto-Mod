package com.sekwah.narutomod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public final class EarthSpikeShape {
    public static final int SEGMENTS = 3;
    public static final int LIFETIME = 160;
    public static final int WAIT_LIMIT = 8;
    private static final int ROW_INTERVAL = 3;
    private static final int[] LOWER_INSET = {1, 3, 5};
    private static final int[] UPPER_INSET = {3, 5, 7};

    private EarthSpikeShape() {
    }

    public static long riseAt(long now, double distance, int row) {
        return now + 4 + (int) (Math.max(0, distance) * 0.65) + row * ROW_INTERVAL;
    }

    public static long endAt(long rise, int row) {
        return rise + LIFETIME - row * (ROW_INTERVAL + 2);
    }

    public static AABB slab(BlockPos block, int row, boolean upper) {
        double inset = (upper ? UPPER_INSET[row] : LOWER_INSET[row]) / 16.0;
        double half = upper ? 0.5 : 0;
        return new AABB(block.getX() + inset, block.getY() + half, block.getZ() + inset,
                block.getX() + 1 - inset, block.getY() + half + 0.5, block.getZ() + 1 - inset);
    }

    public static boolean touches(BlockPos block, int row, AABB target) {
        return slab(block, row, false).intersects(target) || slab(block, row, true).intersects(target);
    }
}
