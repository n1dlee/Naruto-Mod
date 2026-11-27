package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;

public final class DoubleJumpMotion {
    private DoubleJumpMotion() {
    }

    public static double lift(double vertical, int rank) {
        return Mth.clamp(vertical, 0.0, 0.55) + 0.42 + Mth.clamp(rank, 0, 4) * 0.08;
    }

    public static boolean validWindow(boolean grounded, boolean climbing, boolean inWater, boolean specialMovement) {
        return !grounded && !climbing && !inWater && !specialMovement;
    }
}
