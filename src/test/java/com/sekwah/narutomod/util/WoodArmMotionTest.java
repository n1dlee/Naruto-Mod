package com.sekwah.narutomod.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WoodArmMotionTest {
    @Test
    void handAcceleratesButDoesNotOvershootItsRange() {
        assertTrue(WoodArmMotion.step(0, 8) < WoodArmMotion.step(3, 8));
        assertEquals(0.06, WoodArmMotion.step(6, 0.06), 1.0e-9);
        assertEquals(0, WoodArmMotion.step(6, -0.01), 1.0e-9);
    }

    @Test
    void aThinWallWinsAnExactOrEarlierHit() {
        Vec3 from = Vec3.ZERO;
        Vec3 to = new Vec3(4, 0, 0);
        var wall = WaterDragonFlight.sweep(from, to, new AABB(2, -2, -2, 2.03, 2, 2), WoodArmMotion.RADIUS);
        var behind = WaterDragonFlight.sweep(from, to, new AABB(2.04, -0.4, -0.4, 2.7, 0.4, 0.4), WoodArmMotion.RADIUS);
        var before = WaterDragonFlight.sweep(from, to, new AABB(1, -0.4, -0.4, 1.6, 0.4, 0.4), WoodArmMotion.RADIUS);
        assertTrue(WoodArmMotion.hits(before, wall));
        assertFalse(WoodArmMotion.hits(behind, wall));
        assertFalse(WoodArmMotion.hits(wall, wall));
        assertFalse(WoodArmMotion.hits(null, null));
    }

    @Test
    void pullStaysHorizontalAndWeakensInPvpOrAtCloseRange() {
        var caster = new Vec3(0, -8, 0);
        var target = new Vec3(5, 4, 0);
        assertEquals(0, WoodArmMotion.pull(target, caster, false).distanceTo(new Vec3(-0.43, 0, 0)), 1.0e-9);
        assertEquals(0, WoodArmMotion.pull(target, caster, true).distanceTo(new Vec3(-0.26, 0, 0)), 1.0e-9);
        assertEquals(0, WoodArmMotion.pull(new Vec3(1.5, 4, 0), caster, false).lengthSqr(), 1.0e-9);
    }
}
