package com.sekwah.narutomod.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IronSandFlightTest {
    @Test
    void accelerationKeepsTheFrozenAimForDiagonalAndVerticalShots() {
        for (Vec3 axis : new Vec3[]{new Vec3(3, 2, -1).normalize(), new Vec3(0, -1, 0), new Vec3(0, 1, 0)}) {
            var start = new Vec3(-0.5, 8.1, 10);
            Vec3 center = start;
            double total = 0;
            for (int age = 1; age <= IronSandFlight.FLIGHT; age++) {
                Vec3 next = IronSandFlight.step(center, axis, age);
                total += next.distanceTo(center);
                assertEquals(0, next.subtract(start).cross(axis).length(), 1.0e-9);
                center = next;
            }
            assertEquals(total, center.subtract(start).dot(axis), 1.0e-8);
        }
    }

    @Test
    void aThinPlateContactsBeforeTheTargetEvenAtMaximumSpeed() {
        var from = Vec3.ZERO;
        var to = IronSandFlight.step(from, new Vec3(0, 0, 1), 8);
        var block = WaterDragonFlight.sweep(from, to, new AABB(-1, -1, 0.95, 1, 1, 1), IronSandFlight.RADIUS);
        var target = WaterDragonFlight.sweep(from, to, new AABB(-0.4, -0.4, 1.22, 0.4, 0.4, 1.9), IronSandFlight.RADIUS);
        assertNotNull(block);
        assertNotNull(target);
        assertTrue(block.precedes(target));
    }

    @Test
    void gatheringClampsAndDebrisSagsRatherThanSeekingTheCaster() {
        assertEquals(0, IronSandFlight.density(-2));
        assertEquals(0.5, IronSandFlight.density(IronSandFlight.GATHER * 0.5));
        assertEquals(1, IronSandFlight.density(20));
        var at = IronSandFlight.scatter(new Vec3(0, 1, 0), new Vec3(0.1, 0, 0), 10);
        assertEquals(1, at.x, 1.0e-9);
        assertEquals(0.58, at.y, 1.0e-9);
    }
}
