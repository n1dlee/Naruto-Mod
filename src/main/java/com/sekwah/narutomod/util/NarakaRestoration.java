package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;

public final class NarakaRestoration {
    public static final int HEAL_TICKS = 12;
    public static final int REBUILD_TICKS = 18;
    public static final int MEMORY = 110;
    public static final double REACH = 8.5;

    private NarakaRestoration() {
    }

    public static float heal(float missing, float rank) {
        return Math.min(Math.max(0, missing), Mth.clamp(8 * rank, 4, 12));
    }

    public static boolean usableEcho(long now, long expires, boolean sameWorld, boolean originalAlive) {
        return now < expires && sameWorld && !originalAlive;
    }

    public static double jaw(double progress) {
        double t = Mth.clamp(progress, 0, 1);
        return Math.sin(Math.PI * t) * (0.90 + 0.10 * t);
    }

    public enum Stage { HEALING, HEALED, OPEN, REBUILT, ABORT }
}
