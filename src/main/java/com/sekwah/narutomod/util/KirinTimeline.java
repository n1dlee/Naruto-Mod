package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;

public final class KirinTimeline {
    public static final int WARNING = 24;
    public static final int DESCENT = 3;
    public static final int CONTACT = WARNING + DESCENT;
    public static final int RESIDUE = 10;

    private KirinTimeline() {
    }

    public static double cloudHeight(double groundY, int buildLimit) {
        return Math.max(0.0, Math.min(34.0, buildLimit - groundY - 2.0));
    }

    public static double headHeight(double age, double cloudHeight) {
        double progress = Mth.clamp((age - WARNING) / DESCENT, 0.0, 1.0);
        return cloudHeight * (1.0 - progress * progress);
    }

    public static boolean checkSkyAt(int age) {
        return age == WARNING || age == CONTACT;
    }
}
