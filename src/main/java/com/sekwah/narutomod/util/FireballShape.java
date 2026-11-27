package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;

public final class FireballShape {
    public static final int MAX_CHARGE = 60;
    public static final int EXHALE_TICKS = 3;

    private FireballShape() {
    }

    public static int charge(int ticks) {
        return Mth.clamp(ticks, 0, MAX_CHARGE);
    }

    public static double radius(int charge, double age) {
        double mature = Mth.lerp(charge(charge) / 60.0, 0.76, 2.12);
        return mature * Mth.lerp(Mth.clamp(age / 5.0, 0.0, 1.0), 0.42, 1.0);
    }

    public static double blast(int charge) {
        return Mth.lerp(charge(charge) / 60.0, 3.4, 6.2);
    }

    public static float damage(int charge, double fraction) {
        if (fraction >= 1.0) {
            return 0;
        }
        return (float) (12.0 * (1.0 + charge(charge) / 60.0 * 2.0) * (1.0 - fraction * fraction));
    }
}
