package com.sekwah.narutomod.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WoodBurialPathTest {
    private static final Vec3 EAST = new Vec3(1, 0, 0);

    @Test
    void gapAtAnIntermediateSampleRejectsTheSpan() {
        assertNull(WoodBurialPath.route(Vec3.ZERO, EAST, (point, previous) ->
                point.x > 0.5 && point.x < 1.0 ? null : point));
    }

    @Test
    void routePreservesTheHeightsOfEachStep() {
        var route = WoodBurialPath.route(Vec3.ZERO, EAST, (point, previous) ->
                new Vec3(point.x, point.x >= 1.0 ? 1 : 0, point.z));
        assertNotNull(route);
        assertEquals(5, route.samples().size());
        assertEquals(new Vec3(WoodBurialPath.SPACING, 1, 0), route.end());
        assertEquals(0, route.samples().get(2).y);
        assertEquals(1, route.samples().get(3).y);
        assertNull(WoodBurialPath.route(Vec3.ZERO, EAST, (point, previous) -> point.add(0, -2, 0)));
    }

    @Test
    void finalWaveHasALongerTelegraph() {
        assertEquals(6, WoodBurialPath.STEPS);
        assertTrue(WoodBurialPath.finalBeat(WoodBurialPath.STEPS - 1));
        assertTrue(WoodBurialPath.beat(5) - WoodBurialPath.beat(4)
                > WoodBurialPath.beat(1) - WoodBurialPath.beat(0));
    }

    @Test
    void branchOnlyContactsTargetsCloseToItsActualSweep() {
        var tip = new Vec3(0.8, 1.2, 0);
        assertTrue(WoodBurialPath.swept(Vec3.ZERO, tip, new AABB(0.65, 0.5, -0.2, 0.98, 1.5, 0.2), false));
        assertFalse(WoodBurialPath.swept(Vec3.ZERO, tip, new AABB(0.3, 0.2, 0.85, 0.7, 1.5, 1.3), true));
        assertFalse(WoodBurialPath.swept(Vec3.ZERO, tip, new AABB(0.3, 2, -0.3, 0.7, 3, 0.3), true));
    }
}
