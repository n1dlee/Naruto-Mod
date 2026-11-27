package com.sekwah.narutomod;

import com.sekwah.narutomod.util.DashStride;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DashStrideTest {
    @Test
    void movingDiagonallyCreatesMeasuredSegmentRatherThanProjectedIntent() {
        DashStride dash = new DashStride();
        dash.update(Vec3.ZERO, true);
        Vec3 point = new Vec3(0.3, 0.12, 0.4);
        dash.update(point, true);
        assertTrue(dash.starts());
        assertEquals(point, dash.step());
        dash.update(point.add(0.3, 0, 0.4), true);
        assertFalse(dash.starts());
        assertEquals(2, dash.runTicks());
    }

    @Test
    void blockedMovementEndsTrailAndGivesFourTickBrake() {
        DashStride dash = new DashStride();
        dash.update(Vec3.ZERO, true);
        Vec3 contact = new Vec3(0.4, 0, 0);
        dash.update(contact, true);
        dash.update(contact, true);
        assertEquals(Vec3.ZERO, dash.step());
        assertFalse(dash.moving());
        assertEquals(DashStride.BRAKE_TICKS, dash.brakeTicks());
        for (int tick = 0; tick < DashStride.BRAKE_TICKS; tick++) {
            dash.update(contact, true);
        }
        assertEquals(0, dash.brakeTicks());
    }

    @Test
    void teleportsNeverConnectToExistingPath() {
        DashStride dash = new DashStride();
        dash.update(Vec3.ZERO, true);
        dash.update(new Vec3(0, 0, 0.5), true);
        dash.update(new Vec3(0, 0, 12), true);
        assertTrue(dash.reset());
        assertFalse(dash.moving());
        assertEquals(Vec3.ZERO, dash.step());
        assertEquals(0, dash.brakeTicks());
    }

    @Test
    void verticalMotionAndDisabledToggleDoNotReadAsDash() {
        DashStride dash = new DashStride();
        dash.update(Vec3.ZERO, true);
        dash.update(new Vec3(0, 0.4, 0), true);
        assertFalse(dash.moving());
        dash.update(new Vec3(0.4, 0.4, 0), false);
        assertFalse(dash.moving());
    }
}
