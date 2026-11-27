package com.sekwah.narutomod.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NarakaRestorationTest {
    @Test
    void healOnlyReturnsMissingHealthUpToItsCap() {
        assertEquals(0, NarakaRestoration.heal(0, 2));
        assertEquals(0, NarakaRestoration.heal(-3, 2));
        assertEquals(2.5f, NarakaRestoration.heal(2.5f, 2));
        assertEquals(12, NarakaRestoration.heal(20, 9));
    }

    @Test
    void anEchoCannotRecreateALivingOriginalOrCrossAWorldBoundary() {
        assertTrue(NarakaRestoration.usableEcho(99, 100, true, false));
        assertFalse(NarakaRestoration.usableEcho(100, 100, true, false));
        assertFalse(NarakaRestoration.usableEcho(99, 100, false, false));
        assertFalse(NarakaRestoration.usableEcho(99, 100, true, true));
    }

    @Test
    void theJawClosesOnTheActualReconstructionBeat() {
        assertEquals(0, NarakaRestoration.jaw(0), 1.0e-9);
        assertTrue(NarakaRestoration.jaw(0.5) > 0.93);
        assertEquals(0, NarakaRestoration.jaw(1), 1.0e-9);
    }
}
