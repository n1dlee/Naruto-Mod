package com.sekwah.narutomod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public final class WoodCagePlan {
    public static final int WINDUP = 6;
    public static final int ROW_INTERVAL = 2;
    public static final int ROOF = 3;
    public static final int HALF_WIDTH = 2;

    private WoodCagePlan() {
    }

    public static List<Cell> row(BlockPos center, Direction gate, int row) {
        if (row == ROOF) {
            return roof(center);
        }
        if (row < 0 || row > ROOF) {
            return List.of();
        }
        var cells = new ArrayList<Cell>();
        BlockPos entry = center.relative(gate, HALF_WIDTH);
        for (int x = -HALF_WIDTH; x <= HALF_WIDTH; x++) {
            for (int z = -HALF_WIDTH; z <= HALF_WIDTH; z++) {
                boolean xEdge = Math.abs(x) == HALF_WIDTH;
                boolean zEdge = Math.abs(z) == HALF_WIDTH;
                if (!xEdge && !zEdge) {
                    continue;
                }
                BlockPos cell = center.offset(x, row, z);
                if (cell.getX() == entry.getX() && cell.getZ() == entry.getZ()) {
                    continue;
                }
                Profile profile = xEdge && zEdge || (x == 0 || z == 0) && row == 0
                        ? Profile.POST : xEdge ? Profile.RAIL_Z : Profile.RAIL_X;
                cells.add(new Cell(cell, profile, cell.below(), row == 0));
            }
        }
        return List.copyOf(cells);
    }

    private static List<Cell> roof(BlockPos center) {
        var cells = new ArrayList<Cell>();
        for (int z : new int[]{-HALF_WIDTH, 0, HALF_WIDTH}) {
            for (int x = -HALF_WIDTH; x <= HALF_WIDTH; x++) {
                BlockPos support = center.offset(x <= 0 ? -HALF_WIDTH : HALF_WIDTH, ROOF - 1, z);
                cells.add(new Cell(center.offset(x, ROOF, z), Profile.CROWN, support, false));
            }
        }
        return List.copyOf(cells);
    }

    public static int lifespan(boolean player) { return player ? 90 : 180; }

    public record Cell(BlockPos pos, Profile profile, BlockPos support, boolean ground) {
    }

    public enum Profile implements StringRepresentable {
        POST("post", Shapes.box(3 / 16d, 0, 3 / 16d, 13 / 16d, 1, 13 / 16d)),
        RAIL_X("rail_x", Shapes.box(0, 3 / 16d, 4 / 16d, 1, 13 / 16d, 12 / 16d)),
        RAIL_Z("rail_z", Shapes.box(4 / 16d, 3 / 16d, 0, 12 / 16d, 13 / 16d, 1)),
        CROWN("crown", Shapes.box(0, 3 / 16d, 4 / 16d, 1, 10 / 16d, 12 / 16d)),
        ROOT_X("root_x", Shapes.or(Shapes.box(1 / 16d, 0, 4 / 16d, 15 / 16d, 5 / 16d, 12 / 16d),
                Shapes.box(5 / 16d, 3 / 16d, 5 / 16d, 11 / 16d, 1, 11 / 16d))),
        ROOT_Z("root_z", Shapes.or(Shapes.box(4 / 16d, 0, 1 / 16d, 12 / 16d, 5 / 16d, 15 / 16d),
                Shapes.box(5 / 16d, 3 / 16d, 5 / 16d, 11 / 16d, 1, 11 / 16d)));

        private final String name;
        private final VoxelShape shape;

        Profile(String name, VoxelShape shape) {
            this.name = name;
            this.shape = shape;
        }

        public VoxelShape shape() { return this.shape; }

        @Override
        public String getSerializedName() { return this.name; }
    }
}
