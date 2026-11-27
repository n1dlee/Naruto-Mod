package com.sekwah.narutomod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WoodHouseLayoutTest {
    private static final Vec3i LEGACY_SIZE = new Vec3i(16, 11, 17);

    @Test
    void allFourSitesKeepTheCasterOutsideAndTurnTheEntryBackTowardsThem() {
        for (Direction aim : Direction.Plane.HORIZONTAL) {
            var site = WoodHouseLayout.site(BlockPos.ZERO, aim, LEGACY_SIZE);
            assertFalse(site.box().inflate(3).contains(new Vec3(0.5, 0.5, 0.5)));
            var rotatedDoor = WoodHouseLayout.rotated(new BlockPos(7, 0, 16), LEGACY_SIZE, site.rotation());
            assertEquals(aim.getOpposite(), Direction.getNearest(
                    rotatedDoor.getX() - (site.size().getX() - 1) * 0.5, 0,
                    rotatedDoor.getZ() - (site.size().getZ() - 1) * 0.5));
        }
    }

    @Test
    void nonSquareFootprintRotatesWithoutLosingOrDuplicatingCells() {
        for (Direction aim : Direction.Plane.HORIZONTAL) {
            var site = WoodHouseLayout.site(new BlockPos(100, 60, 100), aim, LEGACY_SIZE);
            var positions = new HashSet<BlockPos>();
            for (int x = 0; x < LEGACY_SIZE.getX(); x++) {
                for (int z = 0; z < LEGACY_SIZE.getZ(); z++) {
                    var pos = site.corner().offset(WoodHouseLayout.rotated(new BlockPos(x, 1, z), LEGACY_SIZE, site.rotation()));
                    assertTrue(site.box().contains(Vec3.atCenterOf(pos)));
                    assertTrue(positions.add(pos));
                }
            }
            assertEquals(16 * 17, positions.size());
        }
    }

    @Test
    void visualPhasesNeverGrowBeforeTheirWindowOrOvershootTheirSize() {
        assertEquals(0, WoodHouseLayout.phase(4, 4, 12));
        assertEquals(0.5, WoodHouseLayout.phase(10, 4, 12));
        assertEquals(1, WoodHouseLayout.phase(100, 4, 12));
        assertFalse(WoodHouseLayout.allowedSize(new Vec3i(25, 11, 17)));
    }
}
