package com.sekwah.narutomod;

import com.sekwah.narutomod.util.ShockArc;
import com.sekwah.narutomod.util.WaterDragonFlight;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShockArcTest {
    private static final Vec3 START = new Vec3(-5, 1.5, 9);
    private static final Vec3 END = ShockArc.endpoint(START, new Vec3(4, 0, 0));

    @Test
    void normalizedCastDoesNotHaveExtendedRangeOrSideChain() {
        assertEquals(ShockArc.RANGE, START.distanceTo(END), 1e-7);
        assertNull(WaterDragonFlight.sweep(START, END, new AABB(1.24, 1.2, 8.8, 1.65, 1.9, 9.2), ShockArc.RADIUS));
        assertNull(WaterDragonFlight.sweep(START, END, new AABB(-1.5, 1.2, 9.24, -1.0, 1.9, 9.8), ShockArc.RADIUS));
    }

    @Test
    void firstTargetIsChosenByItsNearFaceAndWallCanBeatBothTargets() {
        var near = WaterDragonFlight.sweep(START, END, new AABB(-3.8, 1, 8.8, -3.3, 2, 9.2), ShockArc.RADIUS);
        var far = WaterDragonFlight.sweep(START, END, new AABB(-2, 1, 8.8, -1.3, 2, 9.2), ShockArc.RADIUS);
        var wall = WaterDragonFlight.sweep(START, END, new AABB(-4, 1, 8.8, -3.91, 2, 9.2), ShockArc.RADIUS);
        assertNotNull(near);
        assertNotNull(far);
        assertNotNull(wall);
        assertTrue(near.precedes(far));
        assertFalse(near.precedes(wall));
        assertFalse(far.precedes(wall));
    }

    @Test
    void cancelledOrImmuneHitDoesNotApplyControl() {
        assertEquals(0, ShockArc.stunTicks(false, true));
        assertEquals(0, ShockArc.stunTicks(false, false));
        assertEquals(30, ShockArc.stunTicks(true, true));
        assertEquals(40, ShockArc.stunTicks(true, false));
    }
}
