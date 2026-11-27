package com.sekwah.narutomod;

import com.sekwah.narutomod.util.ChargeFeedback;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChargeFeedbackTest {
    @Test
    void startingFullCreatesNoEmissionSoundOrXp() {
        ChargeFeedback charge = new ChargeFeedback();
        assertEquals(new ChargeFeedback.Tick(0, false), charge.accept(100, 100, 100, 5));
        assertFalse(charge.earned(100));
    }

    @Test
    void actualGainDeterminesEmissionAndTheMovingRateIsWeaker() {
        ChargeFeedback charge = new ChargeFeedback();
        assertEquals(4, charge.accept(20, 25, 100, 5).strength());
        assertEquals(1, charge.accept(25, 26, 100, 5).strength());
        assertEquals(0, charge.accept(26, 24, 100, 5).strength());
    }

    @Test
    void fullSignalIsSingleUseUntilChannelIsCleared() {
        ChargeFeedback charge = new ChargeFeedback();
        assertTrue(charge.accept(91, 100, 100, 9).completed());
        assertFalse(charge.accept(98, 100, 100, 9).completed());
        charge.clear();
        assertTrue(charge.accept(95, 100, 100, 9).completed());
    }

    @Test
    void smallTopUpCannotFarmPracticeXp() {
        ChargeFeedback charge = new ChargeFeedback();
        charge.accept(99, 100, 100, 5);
        assertFalse(charge.earned(100));
        charge.clear();
        charge.accept(95, 100, 100, 5);
        assertTrue(charge.earned(100));
        charge.clear();
        assertFalse(charge.earned(100));
    }
}
