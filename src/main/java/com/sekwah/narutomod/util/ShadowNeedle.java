package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ShadowNeedle {
    public static final int RISE_AT = 7;
    public static final int BRANCHES = 3;
    public static final double STRIKE_HEIGHT = 1.22;

    private ShadowNeedle() {
    }

    public static boolean reaches(Vec3 ground, Vec3 feet, double width) {
        Vec3 offset = feet.subtract(ground);
        double reach = 0.35 + Math.min(width * 0.25, 0.24);
        return Math.abs(offset.y) < 0.37 && offset.horizontalDistanceSqr() <= reach * reach;
    }

    public static double height(float age, boolean accepted) {
        double amount = age < 1.4 ? age / 1.4 : Math.pow(Math.max(0, (7 - age) / 5.6), 1.65);
        return (accepted ? STRIKE_HEIGHT : 0.23) * Mth.clamp(amount, 0, 1);
    }
}
