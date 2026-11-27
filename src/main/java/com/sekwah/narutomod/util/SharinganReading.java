package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SharinganReading {
    public static final double RANGE = 24;
    private static final double MIN_AIM = Math.cos(Math.toRadians(36));

    private SharinganReading() {
    }

    public static boolean watching(Vec3 look, Vec3 offset) {
        double distance = offset.lengthSqr();
        return distance > 1.0e-8 && distance <= RANGE * RANGE
                && look.normalize().dot(offset.normalize()) >= MIN_AIM;
    }

    public static boolean understands(int tomoe, boolean elemental, boolean awakenedNature) {
        return tomoe >= 2 && (!elemental ? tomoe >= 3 : awakenedNature);
    }

    public static Vec3 lead(Vec3 velocity, double ticks) {
        double magnitude = velocity.length();
        if (magnitude < 0.05 || magnitude > 1.8) {
            return Vec3.ZERO;
        }
        return velocity.scale(Math.min(Math.max(0, ticks), 1.55 / magnitude));
    }

    public static float turn(float age) {
        float t = 1 - Mth.clamp(age / 6, 0, 1);
        return -145 * t * t;
    }
}
