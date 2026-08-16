package com.sekwah.narutomod;

import com.sekwah.narutomod.entity.TailedBeastVariant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tailed beast power ladder.
 *
 * These are the numbers the mod's owner specified directly - each tail doubles the beast, and
 * Kurama is 256 times the One Tail - so they are exactly the kind of thing that must fail
 * loudly if someone later "tidies up" a constant. The original table was hand-written and had
 * silently drifted to a 1.8x spread across a range meant to be 128x; nothing caught it because
 * nothing was checking.
 */
class PowerLadderTest {

    @Test
    void eachTailDoublesTheBeast() {
        for (int tails = 2; tails <= 9; tails++) {
            assertEquals(TailedBeastVariant.powerMultiplier(tails - 1) * 2f,
                    TailedBeastVariant.powerMultiplier(tails), 1.0E-3f,
                    "tail " + tails + " must be twice tail " + (tails - 1));
        }
    }

    @Test
    void kuramaIsTwoHundredAndFiftySixTimesShukaku() {
        assertEquals(256f, TailedBeastVariant.powerMultiplier(TailedBeastVariant.KURAMA_TAILS)
                / TailedBeastVariant.powerMultiplier(1), 1.0E-3f);
    }

    @Test
    void theLadderIsSplitEvenlyBetweenHealthAndDamage() {
        // statMultiplier is applied to BOTH axes, so its square has to be the full ladder.
        // If this drifts, one axis quietly carries more of the doubling than the other.
        for (int tails = 1; tails <= 9; tails++) {
            float stat = TailedBeastVariant.statMultiplier(tails);
            assertEquals(TailedBeastVariant.powerMultiplier(tails), stat * stat, 1.0E-2f,
                    "stat multiplier squared must equal the power multiplier at tail " + tails);
        }
    }

    @Test
    void everyBeastIsStrongerThanTheOneBelowIt() {
        TailedBeastVariant[] beasts = TailedBeastVariant.values();
        for (int i = 1; i < beasts.length; i++) {
            float previous = beasts[i - 1].getHealth() * beasts[i - 1].getDamage();
            float current = beasts[i].getHealth() * beasts[i].getDamage();
            assertTrue(current > previous,
                    beasts[i].getName() + " must out-rank " + beasts[i - 1].getName()
                            + " (" + current + " vs " + previous + ")");
        }
    }

    @Test
    void toughnessBiasNeverMovesABeastOffItsRung() {
        // The bias trades health against damage; their product must be untouched by it.
        for (TailedBeastVariant beast : TailedBeastVariant.values()) {
            float product = beast.getHealth() * beast.getDamage();
            float unbiased = TailedBeastVariant.healthForTails(beast.getTails())
                    * TailedBeastVariant.damageForTails(beast.getTails());
            assertEquals(unbiased, product, unbiased * 0.001f,
                    beast.getName() + " has been moved off its rung by its bias");
        }
    }
}
