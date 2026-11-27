package com.sekwah.narutomod;

import com.sekwah.narutomod.util.AmaterasuFlames;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmaterasuFlamesTest {
    @Test
    void ownerIsTheOnlyOneWhoCanRecallTheirFlame() {
        UUID owner = new UUID(0, 1);
        assertTrue(AmaterasuFlames.ownedBy(owner, new UUID(0, 1)));
        assertFalse(AmaterasuFlames.ownedBy(owner, new UUID(0, 2)));
        assertFalse(AmaterasuFlames.ownedBy(null, owner));
    }

    @Test
    void expiryIsWorldTimeAndDoesNotResetOnLoad() {
        assertEquals(460, AmaterasuFlames.expiry(260, true));
        assertEquals(484, AmaterasuFlames.expiry(260, false));
        assertTrue(AmaterasuFlames.PULSE > 10);
    }

    @Test
    void wallPatchIsLocalAndMostlyOnTheNearSide() {
        AABB west = AmaterasuFlames.patch(new Vec3(4, 1, 2), Direction.WEST);
        assertTrue(west.contains(3.6, 1.0, 2));
        assertFalse(west.contains(4.24, 1.0, 2));
        assertFalse(west.contains(3.6, 1.0, 3));
        AABB floor = AmaterasuFlames.patch(Vec3.ZERO, Direction.UP);
        assertTrue(floor.contains(0, 0.43, 0));
        assertFalse(floor.contains(0, -0.24, 0));
    }

    @Test
    void powerHasALimitAndGroundOnlyCreatesAFewCarriers() {
        assertEquals(1, AmaterasuFlames.power(-5));
        assertEquals(AmaterasuFlames.power(5), AmaterasuFlames.power(1000));
        assertEquals(6, AmaterasuFlames.MAX_CARRIERS);
    }
}
