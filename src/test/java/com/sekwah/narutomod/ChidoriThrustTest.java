package com.sekwah.narutomod;

import com.sekwah.narutomod.util.ChidoriStrike;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What "in front of the fist" means.
 *
 * The Chidori stopped being a buff on your next sword swing and became an arm with a reach and
 * a direction, and the whole of that direction rests on this projection. Get it wrong in one
 * of two specific ways and nobody notices for a long time, because the technique still hits
 * things most of the time:
 *
 *  - drop the clamp and the segment becomes an infinite line, so a Chidori thrust connects
 *    with somebody standing directly BEHIND the user;
 *  - normalise against the wrong length and the reach quietly stretches or shrinks.
 *
 * Both would read in game as "it hit when it shouldn't have", which is the kind of thing that
 * gets blamed on lag rather than on arithmetic.
 */
class ChidoriThrustTest {

    /** Eyes at head height, looking due north (negative Z is north in Minecraft). */
    private static final Vec3 EYES = new Vec3(0, 1.62, 0);
    private static final Vec3 FIST = new Vec3(0, 1.62, -ChidoriStrike.REACH);

    @Test
    void somebodyStraightAheadIsOnTheLine() {
        Vec3 victim = new Vec3(0, 1.62, -2.0);
        Vec3 nearest = ChidoriStrike.nearestPointOnSegment(EYES, FIST, victim);
        assertEquals(victim.x, nearest.x, 1.0E-6);
        assertEquals(victim.z, nearest.z, 1.0E-6);
    }

    @Test
    void somebodyBehindYouIsNeverInFront() {
        // The bug the clamp exists to prevent. Without it this projects to z = +2 and the
        // thrust lands on a target the user has their back to.
        Vec3 behind = new Vec3(0, 1.62, 2.0);
        Vec3 nearest = ChidoriStrike.nearestPointOnSegment(EYES, FIST, behind);
        assertEquals(EYES, nearest, "a point behind the user must clamp to the shoulder");
        assertTrue(nearest.distanceTo(behind) >= 2.0,
                "and must stay far enough away that no hitbox check would accept it");
    }

    @Test
    void reachStopsAtTheEndOfTheArm() {
        Vec3 tooFar = new Vec3(0, 1.62, -(ChidoriStrike.REACH + 4.0));
        Vec3 nearest = ChidoriStrike.nearestPointOnSegment(EYES, FIST, tooFar);
        assertEquals(FIST, nearest, "past the fist the projection must clamp to the fist");
        assertEquals(4.0, nearest.distanceTo(tooFar), 1.0E-6,
                "and the leftover distance is what puts the target out of range");
    }

    @Test
    void somebodyOffToOneSideIsMeasuredSideways() {
        // Level with the fist but two blocks to the right: the projection lands on the line,
        // and the two blocks are what the hitbox check has to reject.
        Vec3 beside = new Vec3(2.0, 1.62, -2.0);
        Vec3 nearest = ChidoriStrike.nearestPointOnSegment(EYES, FIST, beside);
        assertEquals(2.0, nearest.distanceTo(beside), 1.0E-6);
        assertEquals(0.0, nearest.x, 1.0E-6, "the nearest point stays on the thrust itself");
    }

    @Test
    void aZeroLengthThrustDoesNotDivideByZero() {
        // Reach can only be zero if somebody edits the constant to it, but a NaN here would
        // propagate silently through every distance comparison rather than throwing.
        Vec3 nearest = ChidoriStrike.nearestPointOnSegment(EYES, EYES, new Vec3(1, 1, 1));
        assertEquals(EYES, nearest);
        assertTrue(Double.isFinite(nearest.x) && Double.isFinite(nearest.y) && Double.isFinite(nearest.z));
    }
}
