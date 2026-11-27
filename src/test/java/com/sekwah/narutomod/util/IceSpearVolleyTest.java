package com.sekwah.narutomod.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IceSpearVolleyTest {
    @Test
    void fixedFanRetainsItsPatternForVerticalAndDiagonalAim() {
        for (Vec3 direction : new Vec3[]{new Vec3(0, 1, 0), new Vec3(0, -1, 0), new Vec3(2, 3, 7).normalize()}) {
            var starts = new HashSet<Vec3>();
            var bearings = new HashSet<Vec3>();
            for (int slot = 0; slot < IceSpearVolley.COUNT; slot++) {
                var lane = IceSpearVolley.lane(direction, slot);
                assertTrue(starts.add(lane.offset()));
                assertTrue(bearings.add(lane.heading()));
                assertEquals(1, lane.heading().length(), 1.0e-9);
                assertTrue(lane.heading().dot(direction) > 0.997);
                assertEquals(0.73, lane.offset().dot(direction), 1.0e-9);
                assertEquals(lane.release(), IceSpearVolley.lane(direction, 5 - slot).release());
            }
        }
    }

    @Test
    void volleySharesOneCapPerTargetWithoutMutatingOtherTargets() {
        var volley = new IceSpearVolley();
        var first = new UUID(1, 1);
        var second = new UUID(1, 2);
        assertTrue(volley.claim(first));
        assertTrue(volley.claim(first));
        assertFalse(volley.claim(first));
        assertTrue(volley.claim(second));
        assertTrue(new IceSpearVolley().claim(first));
    }

    @Test
    void aNarrowMovingTargetCanBeComparedAgainstASingleThinBlock() {
        var axis = IceSpearVolley.lane(new Vec3(0, 0, 1), 3).heading();
        var from = Vec3.ZERO;
        var to = axis.scale(IceSpearVolley.SPEED);
        var wall = WaterDragonFlight.sweep(from, to, new AABB(-1, -1, 0.91, 1, 1, 0.94), IceSpearVolley.RADIUS);
        var entity = WaterDragonFlight.sweep(from, to, new AABB(-0.07, -0.34, 1.22, 0.07, 0.34, 1.46), IceSpearVolley.RADIUS);
        assertNotNull(wall);
        assertNotNull(entity);
        assertTrue(wall.precedes(entity));
    }
}
