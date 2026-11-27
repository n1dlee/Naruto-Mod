package com.sekwah.narutomod;

import com.sekwah.narutomod.util.BoulderRoll;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoulderRollTest {
    @Test
    void fastShellHitsAThinTargetBetweenServerSamples() {
        var target = new AABB(-0.25, 0, 0.52, 0.25, 1, 0.66);
        assertNotNull(BoulderRoll.contact(new Vec3(0, 0.56, 0), new Vec3(0, 0.56, 1.02), target, 0.56));
    }

    @Test
    void expandedBoxCornerAndTargetsBeyondTheClippedMovementDoNotCount() {
        var diagonal = new AABB(0.53, 1.09, 0.50, 0.65, 1.15, 0.67);
        var behind = new AABB(-0.25, 0, 1.33, 0.25, 1, 1.9);
        Vec3 start = new Vec3(0, 0.56, 0);
        Vec3 stopped = new Vec3(0, 0.56, 0.59);
        assertNull(BoulderRoll.contact(start, stopped, diagonal, 0.56));
        assertNull(BoulderRoll.contact(start, stopped, behind, 0.56));
    }

    @Test
    void spinUsesActualDistanceAndTheActiveShellRadius() {
        double radius = BoulderRoll.radius(false);
        assertEquals(2 * Math.PI, BoulderRoll.rotation(2 * Math.PI * radius, radius), 1.0e-8);
        assertTrue(BoulderRoll.rotation(1, BoulderRoll.radius(true)) < BoulderRoll.rotation(1, radius));
        assertTrue(BoulderRoll.step(0) < BoulderRoll.step(4));
        assertTrue(BoulderRoll.step(4) > BoulderRoll.step(BoulderRoll.segments() - 1));
    }
}
