package com.sekwah.narutomod.util;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChibakuFormationTest {
    @Test
    void shellPointsAreSeparatedAndStayAtUnitRadiusAsItTurns() {
        int count = ChibakuFormation.MAX_SOURCES;
        for (int i = 0; i < count; i++) {
            Vec3 point = ChibakuFormation.radial(i, count, 1.13);
            assertEquals(1, point.length(), 1.0e-9);
            for (int j = i + 1; j < count; j++) {
                assertTrue(point.distanceTo(ChibakuFormation.radial(j, count, 1.13)) > 0.35);
            }
        }
    }

    @Test
    void debrisBeginsAtTheObservedSurfaceAndSettlesOnTheShell() {
        var from = new Vec3(11, 60, 44);
        var to = new Vec3(10, 71, 46);
        assertEquals(from, ChibakuFormation.drift(from, to, -1, 5));
        assertEquals(to, ChibakuFormation.drift(from, to, 1, 5));
        assertTrue(ChibakuFormation.drift(from, to, 0.5, 5).y > from.lerp(to, 0.5).y);
        assertTrue(ChibakuFormation.flight(3, 0) > ChibakuFormation.flight(3, 6));
        assertEquals(1, ChibakuFormation.flight(200, 14));
    }

    @Test
    void pullAndImpactHaveBoundedProfiles() {
        var player = ChibakuFormation.PLAYER;
        assertEquals(0, ChibakuFormation.strength(player.shockRange() + 1, player.shockRange()));
        assertEquals(1, ChibakuFormation.strength(0, player.pullRange()));
        assertTrue(player.impactRange() < player.shockRange());
        assertTrue(ChibakuFormation.grow(20, player.gatherTicks()) < ChibakuFormation.grow(35, player.gatherTicks()));
        assertEquals(2.4, ChibakuFormation.grow(900, player.gatherTicks()), 1.0e-8);
    }
}
