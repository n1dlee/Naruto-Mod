package com.sekwah.narutomod;

import com.sekwah.narutomod.capabilities.DoubleJumpData;
import com.sekwah.narutomod.util.DoubleJumpMotion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DoubleJumpMotionTest {
    @Test
    void serverConsumesAtMostOneImpulseUntilRearmed() {
        DoubleJumpData data = new DoubleJumpData(false);
        assertFalse(data.consumeServer());
        data.canDoubleJumpServer = true;
        assertTrue(data.consumeServer());
        assertFalse(data.consumeServer());
    }

    @Test
    void blockedInputStatesAreNotAnAirWindow() {
        assertTrue(DoubleJumpMotion.validWindow(false, false, false, false));
        assertFalse(DoubleJumpMotion.validWindow(true, false, false, false));
        assertFalse(DoubleJumpMotion.validWindow(false, true, false, false));
        assertFalse(DoubleJumpMotion.validWindow(false, false, true, false));
        assertFalse(DoubleJumpMotion.validWindow(false, false, false, true));
    }

    @Test
    void fallingDoesNotCancelTheNewUpwardImpulse() {
        assertEquals(DoubleJumpMotion.lift(0, 2), DoubleJumpMotion.lift(-1.4, 2));
        assertTrue(DoubleJumpMotion.lift(0, 2) > 0.5);
    }

    @Test
    void aSecondImpulseIsBoundedEvenDuringAnotherAscent() {
        assertEquals(1.29, DoubleJumpMotion.lift(3, 9), 1.0e-8);
        assertEquals(0.42, DoubleJumpMotion.lift(0, -4), 1.0e-8);
    }
}
