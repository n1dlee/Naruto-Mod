package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class WaterShotShape {
    public static final int PREPARE = 4;
    public static final int MAX_FLIGHT = 29;
    public static final int SPLASH_TICKS = 6;
    public static final int SHOTS = 3;
    public static final int SHOT_INTERVAL = 15;

    private WaterShotShape() {
    }

    public static double radius(boolean heavy) {
        return heavy ? 0.28 : 0.17;
    }

    public static double velocity(int age, boolean heavy) {
        return Math.min(heavy ? 1.35 : 1.64, 0.72 + 0.16 * Math.max(0, age));
    }

    public static double splashRadius(boolean heavy) {
        return heavy ? 2.55 : 1.9;
    }

    public static float splashDamage(double distance, boolean heavy) {
        double t = Mth.clamp(distance / splashRadius(heavy), 0, 1);
        return (float) (4 * (1 - t) * (1 - t));
    }

    public static Vec3 reflect(Vec3 heading, Vec3 normal) {
        return heading.subtract(normal.scale(2 * heading.dot(normal)));
    }
}
