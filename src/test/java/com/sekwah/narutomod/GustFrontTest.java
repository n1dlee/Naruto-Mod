package com.sekwah.narutomod;

import com.sekwah.narutomod.util.GustFront;
import com.sekwah.narutomod.util.WaterDragonFlight;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GustFrontTest {
    private static final GustFront.Frame FRAME = GustFront.frame(new Vec3(0.2, 0.18, 1));

    @Test
    void everyRenderedLaneMapsBackToItsServerLane() {
        for (int i = 0; i < GustFront.LANES; i++) {
            assertEquals(i, GustFront.lane(FRAME, FRAME.lane(i).scale(8)));
        }
    }

    @Test
    void positionsBehindAboveAndBeyondTheCrescentAreRejected() {
        Vec3 center = FRAME.forward().scale(8);
        assertEquals(-1, GustFront.lane(FRAME, center.reverse()));
        assertEquals(-1, GustFront.lane(FRAME, center.add(FRAME.up().scale(GustFront.arch(8) + 0.25))));
        assertEquals(-1, GustFront.lane(FRAME, center.add(FRAME.right().scale(8))));
    }

    @Test
    void laneStoppedByCoverNeverReachesTheFarTarget() {
        assertTrue(GustFront.crossed(3.1, 2.5, 4.5, 3.5));
        assertFalse(GustFront.crossed(4, 2.5, 4.5, 3.5));
        assertFalse(GustFront.crossed(2.15, 2.5, 4.5, 12));
        assertFalse(GustFront.crossed(5.5, 2.5, 4.5, 12));
    }

    @Test
    void forwardSpeedAndImpulseReachAreCappedForBothProfiles() {
        assertEquals(0, GustFront.distance(-1, GustFront.RANGE));
        assertEquals(GustFront.RANGE, GustFront.distance(100, GustFront.RANGE));
        assertEquals(GustFront.MOB_RANGE, GustFront.distance(100, GustFront.MOB_RANGE));
        assertTrue(GustFront.strength(0, 10, false) < 2);
        assertTrue(GustFront.strength(9, 1, false) < GustFront.strength(1, 1, false));
    }

    @Test
    void palmIsNarrowerButItsRenderedLanesStillMatchTheServer() {
        var palm = GustFront.Shape.PALM;
        for (int i = 0; i < GustFront.LANES; i++) {
            assertEquals(i, GustFront.lane(FRAME, FRAME.lane(i, palm).scale(7), palm));
        }
        Vec3 offAxis = FRAME.forward().scale(6).add(FRAME.right().scale(3));
        assertTrue(GustFront.lane(FRAME, offAxis) >= 0);
        assertEquals(-1, GustFront.lane(FRAME, offAxis, palm));
    }

    @Test
    void focusedReachCannotAppearOrDamageDuringWindupOrPastTheRange() {
        var palm = GustFront.Shape.PALM;
        assertEquals(4, palm.prepare());
        assertEquals(0, GustFront.distance(-1, GustFront.palmRange(true), palm));
        assertEquals(4, GustFront.distance(10, GustFront.palmRange(false), palm));
        assertEquals(8, GustFront.distance(10, GustFront.palmRange(true), palm));
        assertFalse(GustFront.crossed(5, 3.2, 6.4, 4.3));
    }

    @Test
    void palmProbesTheFaceOfTheHitboxRatherThanTheCenterOrFeet() {
        Vec3 origin = new Vec3(0, 1.36, 0);
        AABB target = new AABB(-0.33, 0, 3, 0.33, 1.8, 3.66);
        Vec3 probe = GustFront.palmProbe(target, origin, new Vec3(0, 0, 1));
        Vec3 contact = WaterDragonFlight.nearestPoint(target, probe);
        assertEquals(3.0, contact.z, 1.0e-7);
        assertEquals(origin.y, contact.y);
        assertTrue(GustFront.crossed(contact.distanceTo(origin), 1.6, 3.2, 3.4));
        assertFalse(GustFront.crossed(contact.distanceTo(origin), 1.6, 3.2, 2.98));
    }
}
