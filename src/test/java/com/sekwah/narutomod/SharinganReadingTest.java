package com.sekwah.narutomod;

import com.sekwah.narutomod.util.DojutsuVisual;
import com.sekwah.narutomod.util.SharinganReading;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharinganReadingTest {
    @Test
    void sightAloneCannotCopyBehindTheCasterOrOutOfRange() {
        Vec3 facing = new Vec3(0, 0, 1);
        assertTrue(SharinganReading.watching(facing, new Vec3(3, 0, 10)));
        assertFalse(SharinganReading.watching(facing, new Vec3(0, 0, -12)));
        assertFalse(SharinganReading.watching(facing, new Vec3(0, 0, 25)));
        assertFalse(SharinganReading.watching(facing, new Vec3(8, 0, 8)));
    }

    @Test
    void tomoeAndNativeNatureAreCheckedIndependently() {
        assertFalse(SharinganReading.understands(1, true, true));
        assertFalse(SharinganReading.understands(2, false, true));
        assertFalse(SharinganReading.understands(3, true, false));
        assertTrue(SharinganReading.understands(2, true, true));
        assertTrue(SharinganReading.understands(3, false, true));
    }

    @Test
    void predictionDoesNotDrawAnOldTeleportOrAnUnmovingTarget() {
        assertEquals(Vec3.ZERO, SharinganReading.lead(new Vec3(4, 0, 0), 5));
        assertEquals(Vec3.ZERO, SharinganReading.lead(new Vec3(0.02, 0, 0), 5));
        assertEquals(1.55, SharinganReading.lead(new Vec3(0.8, 0, 0), 10).length(), 1e-6);
    }

    @Test
    void publicIrisDoesNotSkipTomoeAndStartupSpinSettlesOnce() {
        int one = DojutsuVisual.resolve(new DojutsuVisual.State(false, false, 1, false, false, false, false, false));
        int two = DojutsuVisual.resolve(new DojutsuVisual.State(false, false, 2, false, false, false, false, false));
        assertEquals(DojutsuVisual.Eye.TOMOE_ONE, DojutsuVisual.eye(one, true));
        assertEquals(DojutsuVisual.Eye.TOMOE_TWO, DojutsuVisual.eye(two, false));
        assertTrue(SharinganReading.turn(0) < SharinganReading.turn(4));
        assertEquals(0, SharinganReading.turn(50), 1e-6);
    }
}
