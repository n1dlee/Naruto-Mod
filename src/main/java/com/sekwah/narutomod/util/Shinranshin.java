package com.sekwah.narutomod.util;

import java.util.Objects;
import java.util.UUID;

public final class Shinranshin {
    public static final int FOCUS = 6;
    public static final int MOB_TICKS = 38;
    public static final int PVP_TICKS = 20;
    public static final int FADE = 5;
    public static final double RANGE = 8;
    public static final double BREAK_RANGE = 9.75;

    private Shinranshin() {
    }

    public static boolean maintains(long now, long expires, double distanceSquared, boolean lineOpen) {
        return now < expires && lineOpen && distanceSquared <= BREAK_RANGE * BREAK_RANGE;
    }

    public static boolean ownsTarget(UUID assigned, UUID current, boolean lostAssignment) {
        return !lostAssignment && Objects.equals(assigned, current);
    }

    public enum Stage { WINDUP, GRIP, BREAK }
}
