package com.sekwah.narutomod;

import com.sekwah.narutomod.util.FireFront;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FireFrontTest {
    @Test
    void chargeNeverMovesFrontUntilItIsReleased() {
        assertEquals(12, FireFront.WINDUP);
        assertEquals(0.0, FireFront.depth(-FireFront.WINDUP));
        assertEquals(0.0, FireFront.depth(0));
        assertTrue(FireFront.depth(1) > 0.0);
    }

    @Test
    void forwardSpeedIsCommonToAllSegmentsAndRangeIsBounded() {
        Vec3 origin = new Vec3(12, 64, -7);
        Vec3 heading = new Vec3(1, 0, 0);
        for (int lane = 0; lane < FireFront.LANES; lane++) {
            assertEquals(14.0, FireFront.point(origin, heading, lane, FireFront.depth(50), 0)
                    .subtract(origin).dot(heading));
        }
    }

    @Test
    void eachWorldSegmentMapsBackToTheSameLane() {
        Vec3 direction = new Vec3(-0.6, 0, 0.8);
        for (int lane = 0; lane < FireFront.LANES; lane++) {
            Vec3 point = FireFront.point(Vec3.ZERO, direction, lane, 8, 1.3);
            assertEquals(lane, FireFront.lane(point, direction, 8));
        }
    }

    @Test
    void somebodyBehindOrBeyondTheFlanksIsOutsideTheFront() {
        Vec3 dir = new Vec3(0, 0, 1);
        assertEquals(-1, FireFront.lane(new Vec3(0, 0, -0.1), dir, -0.1));
        assertEquals(-1, FireFront.lane(new Vec3(7, 0, 3), dir, 3));
        assertEquals(FireFront.LANES / 2, FireFront.lane(new Vec3(0, 0, 3), dir, 3));
    }
}
