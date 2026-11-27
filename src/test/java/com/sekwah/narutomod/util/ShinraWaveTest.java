package com.sekwah.narutomod.util;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShinraWaveTest {
    @Test
    void frontMovesOutwardAndDoesNotOvershootItsRadius() {
        double previous = ShinraWave.radius(0);
        assertEquals(0, previous);
        for (int t = 1; t <= ShinraWave.EXPAND; t++) {
            double current = ShinraWave.radius(t);
            assertTrue(current > previous);
            previous = current;
        }
        assertEquals(ShinraWave.RANGE, previous);
        assertEquals(previous, ShinraWave.radius(20));
    }

    @Test
    void damageWindowFollowsTheActualShellNotTheWholeFilledSphere() {
        double last = ShinraWave.radius(4);
        double front = ShinraWave.radius(5);
        assertTrue(ShinraWave.touches((last + front) * 0.5, last, front));
        assertFalse(ShinraWave.touches(last - 0.7, last, front));
        assertFalse(ShinraWave.touches(front + 0.01, last, front));
    }

    @Test
    void pushIsRadialAndRespectsPvpAndFullResistance() {
        Vec3 direction = new Vec3(1, 0, 0);
        Vec3 far = new Vec3(7, 0, 0);
        Vec3 mob = ShinraWave.impulse(Vec3.ZERO, direction, direction, false, 0);
        Vec3 player = ShinraWave.impulse(Vec3.ZERO, direction, direction, true, 0);
        assertTrue(mob.x > player.x && player.x > 0);
        assertTrue(ShinraWave.impulse(Vec3.ZERO, far, direction, false, 0).x < mob.x);
        assertEquals(0, ShinraWave.impulse(Vec3.ZERO, direction, direction, false, 1).lengthSqr(), 1.0e-9);
        assertEquals(0, mob.z, 1.0e-9);
        assertTrue(ShinraWave.impulse(Vec3.ZERO, new Vec3(0, 5, 0), direction, false, 0).y > 0);
    }
}
