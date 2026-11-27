package com.sekwah.narutomod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class DoryuhekiLayout {
    public static final int HEIGHT = 3;
    public static final int SOLID = 4;
    public static final int LIFETIME = 300;
    private static final int STAGE_TICKS = 2;

    private DoryuhekiLayout() {
    }

    public static List<Column> columns(BlockPos feet, Direction forward) {
        BlockPos center = feet.relative(forward, 2);
        Direction cross = forward.getClockWise();
        return List.of(new Column(center, 0), new Column(center.relative(cross), 2),
                new Column(center.relative(cross.getOpposite()), 4));
    }

    public static long riseAt(long cast, Column column, int row) {
        return cast + 5 + column.delay() + row * 9;
    }

    public static long retractAt(long cast, Column column, int row) {
        return cast + LIFETIME + column.delay() + (HEIGHT - row - 1) * 9;
    }

    public static int height(long now, long riseAt, long retractAt) {
        if (now >= retractAt) {
            return (int) (SOLID - 1 - Math.min(SOLID - 1, (now - retractAt) / STAGE_TICKS));
        }
        if (now < riseAt) {
            return 0;
        }
        return (int) (1 + Math.min(SOLID - 1, (now - riseAt) / STAGE_TICKS));
    }

    public static AABB bounds(BlockPos pos, int stage) {
        double height = Mth.clamp(stage, 0, SOLID) / (double) SOLID;
        return new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + height, pos.getZ() + 1);
    }

    public record Column(BlockPos base, int delay) {
    }
}
