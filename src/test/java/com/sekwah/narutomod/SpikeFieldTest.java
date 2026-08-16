package com.sekwah.narutomod;

import com.sekwah.narutomod.util.SpikeField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The spike field's sizing rules.
 *
 * Both numbers here are things the owner asked for in so many words - a twenty-block ceiling,
 * and roughly seven times as many spikes as the old flat count - and both are the sort of
 * thing that gets quietly re-tuned later. The density rule in particular is easy to break by
 * "simplifying" the area formula back into a linear one, which is the bug it replaced.
 */
class SpikeFieldTest {

    @Test
    void radiusGrowsWithMasteryAndStopsAtTwenty() {
        assertEquals(SpikeField.MIN_RADIUS, SpikeField.radiusFor(0), 1.0E-6);
        assertTrue(SpikeField.radiusFor(8) > SpikeField.radiusFor(4));
        assertEquals(SpikeField.MAX_RADIUS, SpikeField.radiusFor(20), 1.0E-6);
        // Past the level cap it must not keep climbing.
        assertEquals(SpikeField.MAX_RADIUS, SpikeField.radiusFor(99), 1.0E-6);
    }

    @Test
    void countNeverExceedsTheProtectiveCeiling() {
        for (int level = 0; level <= 40; level++) {
            int count = SpikeField.countFor(level);
            assertTrue(count <= SpikeField.MAX_SPIKES,
                    "level " + level + " asked for " + count + " spikes");
            assertTrue(count >= 8, "a field must never be fewer than a handful of spikes");
        }
    }

    @Test
    void densityHoldsAsTheFieldGrows() {
        // The whole point of deriving the count from AREA: a bigger disc must not be a
        // sparser one.
        //
        // Levels one to five only, because the performance ceiling takes over at six - a
        // ten-block disc wants 173 spikes and is given 150. Past that point density is
        // SUPPOSED to fall; it is the cost of not writing six hundred blocks in a tick. This
        // test covers the range where the formula is actually in charge, which is the range
        // where a regression to a linear count would hide.
        double previousDensity = -1;
        for (int level = 1; level <= 5; level++) {
            double radius = SpikeField.radiusFor(level);
            double density = SpikeField.countFor(level) / (Math.PI * radius * radius);
            if (previousDensity >= 0) {
                assertEquals(previousDensity, density, 0.05,
                        "density collapsed between levels - is the count linear again?");
            }
            previousDensity = density;
        }
    }
}
