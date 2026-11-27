package com.sekwah.narutomod;

import com.sekwah.narutomod.util.PhoenixVolley;
import com.sekwah.narutomod.util.WaterDragonFlight;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoenixVolleyTest {
    private static final Vec3 FORWARD = new Vec3(0, 0, 1);

    @Test
    void sixPetalsFormASymmetricForwardFan() {
        for (int lane = 0; lane < PhoenixVolley.PETALS; lane++) {
            Vec3 aim = PhoenixVolley.heading(FORWARD, FORWARD, lane);
            Vec3 opposite = PhoenixVolley.heading(FORWARD, FORWARD, PhoenixVolley.PETALS - 1 - lane);
            assertEquals(1, aim.length(), 1e-8);
            assertEquals(aim.x, -opposite.x, 1e-8);
            assertEquals(aim.y, -opposite.y, 1e-8);
            assertTrue(aim.z > 0.9);
        }
    }

    @Test
    void directlyUpStillHasSpreadInsteadOfStackingEveryPetal() {
        Vec3 up = new Vec3(0, 1, 0);
        var left = PhoenixVolley.heading(up, FORWARD, 0);
        var right = PhoenixVolley.heading(up, FORWARD, PhoenixVolley.PETALS - 1);
        assertTrue(left.x * right.x < 0);
        assertTrue(left.dot(up) > 0.9);
    }

    @Test
    void wallBlocksOneSideOfTheFanButNotTheOther() {
        Vec3 from = new Vec3(0, 0.5, 0);
        var left = from.add(PhoenixVolley.heading(FORWARD, FORWARD, 0).scale(5));
        var right = from.add(PhoenixVolley.heading(FORWARD, FORWARD, 5).scale(5));
        var wall = new AABB(0.6, 0, 2.5, 2.0, 2, 2.58);
        assertNotNull(WaterDragonFlight.sweep(from, left, wall, PhoenixVolley.radius(5)));
        assertNull(WaterDragonFlight.sweep(from, right, wall, PhoenixVolley.radius(5)));
    }

    @Test
    void targetCanBeHurtOncePerVolleyNotOncePerEachPetal() {
        var first = new PhoenixVolley();
        var second = new PhoenixVolley();
        UUID target = new UUID(0, 1);
        assertFalse(first.hasHit(target));
        assertTrue(first.markHit(target));
        assertTrue(first.hasHit(target));
        assertFalse(first.markHit(target));
        assertTrue(second.markHit(target));
    }
}
