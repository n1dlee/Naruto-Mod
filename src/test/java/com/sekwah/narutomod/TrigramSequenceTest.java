package com.sekwah.narutomod;

import com.sekwah.narutomod.util.TrigramSequence;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrigramSequenceTest {
    @Test
    void beatsAccelerateAndDifferencesAmountToExactlySixtyFour() {
        int sum = 0;
        int gap = 9;
        int stamp = 0;
        for (int i = 0; i < TrigramSequence.PHASES; i++) {
            sum += TrigramSequence.strikes(i);
            assertEquals(TrigramSequence.total(i), sum);
            assertTrue(TrigramSequence.beat(i) - stamp < gap);
            gap = TrigramSequence.beat(i) - stamp;
            stamp = TrigramSequence.beat(i);
        }
        assertEquals(64, sum);
    }

    @Test
    void bothActorsMustStayInTheOriginalRingAndWithinMeleeReach() {
        Vec3 origin = new Vec3(0, 4, 0);
        assertTrue(TrigramSequence.within(origin, origin, origin.add(3.3, 0, 0)));
        assertFalse(TrigramSequence.within(origin, origin.add(3.4, 0, 0), origin.add(3.8, 0, 0)));
        assertFalse(TrigramSequence.within(origin, origin.add(-2.0, 0, 0), origin.add(2.0, 0, 0)));
        assertFalse(TrigramSequence.within(origin, origin, origin.add(0, 1.6, 0)));
    }

    @Test
    void blockedFinalStrikeCannotSealTheTarget() {
        assertFalse(TrigramSequence.finalBeat(2, true));
        assertFalse(TrigramSequence.finalBeat(5, false));
        assertTrue(TrigramSequence.finalBeat(5, true));
    }

    @Test
    void amplifiedPvpDamageIsBudgetedAcrossTheWholeSequence() {
        float pvp = 0;
        float pve = 0;
        for (int i = 0; i < TrigramSequence.PHASES; i++) {
            pvp += TrigramSequence.damage(i, 8, true);
            pve += TrigramSequence.damage(i, 8, false);
        }
        assertEquals(12, pvp);
        assertEquals(24, pve);
    }
}
