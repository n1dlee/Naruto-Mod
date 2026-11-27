package com.sekwah.narutomod.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WoodGolemMotionTest {
    @Test
    void poseReachesAStandingMobAfterButNeverBeforeTheWindup() {
        var mob = new AABB(-0.4, 0, 1.47, 0.4, 1.96, 2.23);
        boolean contact = false;
        for (int age = WoodGolemMotion.WINDUP; age < WoodGolemMotion.WINDUP + WoodGolemMotion.SWING; age++) {
            Vec3 p = WoodGolemMotion.fist(Vec3.ZERO, 0, age - 1);
            Vec3 q = WoodGolemMotion.fist(Vec3.ZERO, 0, age);
            contact |= WaterDragonFlight.sweep(p, q, mob, WoodGolemMotion.FIST) != null;
        }
        assertFalse(WoodGolemMotion.active(WoodGolemMotion.WINDUP - 1));
        assertTrue(contact);
        assertFalse(WoodGolemMotion.active(WoodGolemMotion.WINDUP + WoodGolemMotion.SWING));
    }

    @Test
    void swipeRotatesWithBodyWithoutChangingHeightOrReach() {
        var feet = new Vec3(3, 2, -10);
        int age = WoodGolemMotion.WINDUP + 3;
        var south = WoodGolemMotion.fist(feet, 0, age).subtract(feet);
        var west = WoodGolemMotion.fist(feet, 90, age).subtract(feet);
        assertEquals(south.y, west.y, 1.0e-8);
        assertEquals(south.length(), west.length(), 1.0e-7);
        assertEquals(-south.z, west.x, 1.0e-7);
        assertEquals(south.x, west.z, 1.0e-7);
    }

    @Test
    void expiredMissingOrCrossWorldLeaseNeverStaysActive() {
        assertTrue(WoodGolemMotion.validLease(true, true, true, 13, 13));
        assertFalse(WoodGolemMotion.validLease(true, true, true, 14, 13));
        assertFalse(WoodGolemMotion.validLease(false, true, true, 12, 13));
        assertFalse(WoodGolemMotion.validLease(true, false, true, 12, 13));
        assertFalse(WoodGolemMotion.validLease(true, true, false, 12, 13));
    }
}
