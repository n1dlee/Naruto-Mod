package com.sekwah.narutomod.util;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClayFlightTest {
    @Test
    void birdArcsWithoutUnexplainedSidewaysDrift() {
        Vec3 initial = ClayFlight.birdLaunch(new Vec3(1, 0, 0));
        assertTrue(initial.y > 0);
        Vec3 velocity = initial;
        for (int i = 0; i < 13; i++) {
            velocity = ClayFlight.nextFlight(velocity);
        }
        assertTrue(velocity.y < -0.25);
        assertTrue(velocity.x > 0);
        assertEquals(0, velocity.z, 1.0e-9);
    }

    @Test
    void contactOffsetsAwayFromTheSurfaceByTheCollisionRadius() {
        Vec3 point = new Vec3(3, 10, 8);
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 settled = ClayFlight.settle(point, up);
        assertEquals(point.x, settled.x);
        assertEquals(point.z, settled.z);
        assertTrue(settled.y - point.y > ClayFlight.RADIUS);
        assertEquals(point, ClayFlight.settle(point, Vec3.ZERO));
    }

    @Test
    void remoteOnlyWorksInTheArmedWindowAndFlashStaysBounded() {
        assertFalse(ClayFlight.armed(ClayFlight.ARM - 1));
        assertTrue(ClayFlight.armed(ClayFlight.ARM));
        assertTrue(ClayFlight.armed(ClayFlight.FUSE - 1));
        assertFalse(ClayFlight.armed(ClayFlight.FUSE));
        for (int t = 0; t < ClayFlight.FUSE; t++) {
            assertTrue(ClayFlight.flash(t, 0.5f) > 0.25 && ClayFlight.flash(t, 0.5f) <= 1);
        }
    }
}
