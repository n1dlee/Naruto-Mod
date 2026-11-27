package com.sekwah.narutomod;

import com.sekwah.narutomod.util.WoodCagePlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WoodCagePlanTest {
    @Test
    void everyOrientationLeavesAFullHeightGateAndHollowInterior() {
        BlockPos center = new BlockPos(12, 62, -17);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos gate = center.relative(facing, 2);
            for (int y = 0; y < WoodCagePlan.ROOF; y++) {
                int layer = y;
                var row = WoodCagePlan.row(center, facing, layer);
                assertEquals(15, row.size());
                assertTrue(row.stream().noneMatch(cell -> cell.pos().equals(gate.above(layer))));
                assertTrue(row.stream().noneMatch(cell -> cell.pos().equals(center.above(layer))));
            }
        }
    }

    @Test
    void roofSupportsAreAtMatchingHeightOutsideThePrisonInterior() {
        var roof = WoodCagePlan.row(BlockPos.ZERO, Direction.NORTH, WoodCagePlan.ROOF);
        assertEquals(15, roof.size());
        assertTrue(roof.stream().allMatch(cell -> !cell.ground() && cell.support().getY() == 2 && Math.abs(cell.support().getX()) == 2));
        assertTrue(WoodCagePlan.row(BlockPos.ZERO, Direction.NORTH, 0).stream()
                .allMatch(cell -> cell.ground() && cell.support().equals(cell.pos().below())));
    }

    @Test
    void modelsHaveOpenBaysRatherThanFullOpaqueBlocks() {
        var rail = WoodCagePlan.Profile.RAIL_X.shape().bounds();
        assertEquals(1, rail.getXsize(), 1.0e-7);
        assertEquals(0.5, rail.getZsize(), 1.0e-7);
        assertTrue(rail.maxY < 1);
        assertFalse(WoodCagePlan.Profile.POST.shape().bounds().contains(0.02, 0.45, 0.02));
        assertEquals(WoodCagePlan.lifespan(false) / 2, WoodCagePlan.lifespan(true));
    }
}
