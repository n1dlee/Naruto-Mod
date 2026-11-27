package com.sekwah.narutomod.util;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimalPathDriveTest {
    @Test
    void summonSitesUseAVisibleFanAndAFartherFallback() {
        Vec3 start = new Vec3(0, 65, 0);
        Vec3 front = new Vec3(0, 0, 1);
        Vec3 left = SummoningCircle.candidate(start, front, 4, 1);
        Vec3 right = SummoningCircle.candidate(start, front, 4, 2);
        assertEquals(-left.x, right.x, 1.0e-8);
        assertEquals(left.z, right.z, 1.0e-8);
        assertTrue(SummoningCircle.candidate(start, front, 4, 3).z > 4);
        var footprint = SummoningCircle.box(right, AnimalPathDrive.WIDTH, AnimalPathDrive.HEIGHT);
        assertEquals(AnimalPathDrive.WIDTH, footprint.getXsize(), 1.0e-7);
        assertEquals(AnimalPathDrive.HEIGHT, footprint.getYsize(), 1.0e-7);
    }

    @Test
    void theWindupRejectsWallsAndHighOrDistantEnemies() {
        assertTrue(AnimalPathDrive.canBrace(new Vec3(4, 0.7, 0), true));
        assertTrue(AnimalPathDrive.canBrace(new Vec3(1.1, 0, 0), true));
        assertFalse(AnimalPathDrive.canBrace(new Vec3(0.3, 0, 0), true));
        assertFalse(AnimalPathDrive.canBrace(new Vec3(4, 0.7, 0), false));
        assertFalse(AnimalPathDrive.canBrace(new Vec3(4, 2, 0), true));
        assertFalse(AnimalPathDrive.canBrace(new Vec3(10, 0, 0), true));
        assertTrue(AnimalPathDrive.tempo(3) > AnimalPathDrive.tempo(0));
        assertTrue(AnimalPathDrive.tempo(7) < AnimalPathDrive.tempo(3));
    }

    @Test
    void ownerDepartureAndExpiryBreakTheSummonBond() {
        assertTrue(AnimalPathDrive.liveBond(true, true, 4, 20));
        assertFalse(AnimalPathDrive.liveBond(true, false, 4, 20));
        assertFalse(AnimalPathDrive.liveBond(true, true, 20, 20));
        assertFalse(AnimalPathDrive.liveBond(false, true, 4, 20));
    }
}
