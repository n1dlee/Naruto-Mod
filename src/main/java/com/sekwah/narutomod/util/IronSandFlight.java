package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class IronSandFlight {
    public static final int GATHER = 7;
    public static final int FLIGHT = 14;
    public static final int SCATTER = 12;
    public static final double RADIUS = 0.36;

    private IronSandFlight() {
    }

    public static Vec3 step(Vec3 position, Vec3 axis, int age) {
        return position.add(axis.scale(1.27 + Mth.clamp(age, 0, 6) * 0.102));
    }

    public static double density(double age) {
        double t = Mth.clamp(age / GATHER, 0, 1);
        return t * t * (3 - 2 * t);
    }

    public static Vec3 scatter(Vec3 position, Vec3 drift, double ticks) {
        return position.add(drift.scale(ticks)).add(0, -0.0042 * ticks * ticks, 0);
    }
}
