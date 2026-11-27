package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ShinraWave {
    public static final int CHARGE = 7;
    public static final int EXPAND = 8;
    public static final int AFTERSHOCK = 10;
    public static final double RANGE = 11.5;
    private static final double SHELL = 0.52;

    private ShinraWave() {
    }

    public static double radius(double age) {
        double t = Mth.clamp(age / EXPAND, 0, 1);
        return RANGE * (t * 0.72 + t * t * 0.28);
    }

    public static boolean touches(double distance, double previousRadius, double radius) {
        return distance >= Math.max(0, previousRadius - SHELL) && distance <= radius;
    }

    public static double falloff(double distance) {
        return Math.pow(Mth.clamp(1 - distance / RANGE, 0, 1), 0.79);
    }

    public static Vec3 impulse(Vec3 from, Vec3 target, Vec3 fallback, boolean pvp, double resistance) {
        Vec3 offset = target.subtract(from);
        double reach = falloff(offset.length());
        Vec3 direction = ProjectileAxis.direction(offset, fallback);
        double transfer = reach * Mth.clamp(1 - resistance, 0, 1);
        return direction.scale((pvp ? 0.92 : 1.94) * transfer).add(0, (pvp ? 0.14 : 0.22) * transfer, 0);
    }

    public enum Mode { CHARGE, PULSE, CANCEL }
}
