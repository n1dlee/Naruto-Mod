package com.sekwah.narutomod.util;

public final class MedicalPalm {
    public static final double REACH = 3.35;
    public static final int GATHER = 6;
    public static final int INTERVAL = 10;
    public static final float COST = 6;

    private MedicalPalm() {
    }

    public static boolean beat(int age) {
        return age >= GATHER && (age - GATHER) % INTERVAL == 0;
    }

    public static boolean accepted(float before, float after) {
        return Float.isFinite(before) && Float.isFinite(after) && after > before + 1.0e-4f;
    }

    public static boolean inFrontOfCover(double contact, double obstruction) {
        return contact >= 0 && contact <= REACH && contact < obstruction - 0.012;
    }

    public enum Phase { GATHER, HOLD, HEAL, RELEASE }
}
