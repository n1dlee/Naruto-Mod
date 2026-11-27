package com.sekwah.narutomod;

import com.sekwah.narutomod.util.KirinTimeline;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class KirinTimelineTest {
    @Test
    void warningHoldsHeadInCloudUntilTheThreeTickDescent() {
        assertEquals(34, KirinTimeline.headHeight(0, 34));
        assertEquals(34, KirinTimeline.headHeight(KirinTimeline.WARNING, 34));
        assertTrue(KirinTimeline.headHeight(KirinTimeline.WARNING + 1, 34) < 34);
        assertEquals(0, KirinTimeline.headHeight(KirinTimeline.CONTACT, 34));
        assertEquals(0, KirinTimeline.headHeight(KirinTimeline.CONTACT + 8, 34));
    }

    @Test
    void skyChecksRunAtReleaseAndAtContactNotOnlyAtMarking() {
        var checks = IntStream.rangeClosed(0, KirinTimeline.CONTACT + KirinTimeline.RESIDUE)
                .filter(KirinTimeline::checkSkyAt).toArray();
        assertArrayEquals(new int[] {KirinTimeline.WARNING, KirinTimeline.CONTACT}, checks);
        assertEquals(3, KirinTimeline.CONTACT - KirinTimeline.WARNING);
    }

    @Test
    void cloudNeverExceedsBuildHeightAndKeepsNormalScaleAtGround() {
        assertEquals(34, KirinTimeline.cloudHeight(72, 320));
        assertEquals(6.5, KirinTimeline.cloudHeight(311.5, 320));
        assertEquals(0, KirinTimeline.cloudHeight(319, 320));
    }
}
