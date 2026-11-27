package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class NagashiWave {
    public static final int SPOKES = 16;
    public static final int EXPANSION_TICKS = 5;
    public static final int RESIDUAL_TICKS = 6;
    public static final double FLOOR_TOLERANCE = 1.15;

    private NagashiWave() {
    }

    public static double radius(double age, double max) {
        double progress = Mth.clamp(age / EXPANSION_TICKS, 0, 1);
        return max * progress * (2 - progress);
    }

    public static Vec3 direction(int spoke) {
        double angle = Math.PI * 2 * spoke / SPOKES;
        return new Vec3(Math.cos(angle), 0, Math.sin(angle));
    }

    public static boolean reaches(Vec3 center, double targetFeet, AABB target, double radius) {
        if (Math.abs(targetFeet - center.y) > FLOOR_TOLERANCE) {
            return false;
        }
        Vec3 nearest = WaterDragonFlight.nearestPoint(target, center);
        return nearest.subtract(center).horizontalDistanceSqr() <= radius * radius;
    }
}
