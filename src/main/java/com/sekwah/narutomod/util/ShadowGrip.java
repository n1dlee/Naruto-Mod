package com.sekwah.narutomod.util;

public final class ShadowGrip {
    public static final int LIFT = 6;
    public static final int INTERVAL = 11;
    public static final int PULSES = 3;
    public static final int DROP = 7;

    private ShadowGrip() {
    }

    public static int pulseAt(int age) {
        int elapsed = age - LIFT;
        if (elapsed < 0 || elapsed % INTERVAL != 0) {
            return -1;
        }
        int index = elapsed / INTERVAL;
        return index < PULSES ? index : -1;
    }

    public static int duration() {
        return LIFT + (PULSES - 1) * INTERVAL + 5;
    }

    public static boolean matches(int expectedCast, int actualCast, boolean sameTarget) {
        return sameTarget && expectedCast == actualCast;
    }
}
