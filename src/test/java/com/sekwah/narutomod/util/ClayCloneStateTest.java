package com.sekwah.narutomod.util;

import com.sekwah.narutomod.util.ClayCloneState.Phase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClayCloneStateTest {
    @Test
    void movingTowardsTheTargetDoesNotArmWithoutRangeAndVisibility() {
        assertEquals(Phase.PURSUE, ClayCloneState.pursue(12, 4, false));
        assertEquals(Phase.PURSUE, ClayCloneState.pursue(12, 9, true));
        assertEquals(Phase.ARMED, ClayCloneState.pursue(12, 7, true));
        assertEquals(Phase.FIZZLE, ClayCloneState.pursue(ClayCloneState.CHASE, 4, true));
    }

    @Test
    void expiredFuseCanOnlyDetonateWhileTheTargetIsStillValid() {
        assertEquals(Phase.ARMED, ClayCloneState.armed(ClayCloneState.ARM - 1, 4, true));
        assertEquals(Phase.BURST, ClayCloneState.armed(ClayCloneState.ARM, 4, true));
        assertEquals(Phase.FIZZLE, ClayCloneState.armed(ClayCloneState.ARM, 4, false));
        assertEquals(Phase.FIZZLE, ClayCloneState.armed(ClayCloneState.ARM, 30, true));
    }

    @Test
    void seamBrightensWithoutExceedingTheAllowedOpacity() {
        double[] peak = {0, 0};
        for (int t = 0; t < ClayCloneState.ARM * 8; t++) {
            double age = t / 8d;
            double value = ClayCloneState.seam(age);
            assertTrue(value >= 0 && value <= 1);
            int window = age < 2 ? 0 : 1;
            peak[window] = Math.max(peak[window], value);
        }
        assertTrue(peak[1] > peak[0] * 1.5);
    }
}
