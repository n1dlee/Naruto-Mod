package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ByakuganSight {
    public static final int REVEAL = 5;
    public static final int VISIBLE_LIMIT = 24;
    public static final int LOW_LIMIT = 12;
    private static final int[] NATIVE_RANGE = {20, 100, 300, 500, 600};
    private static final double BLIND_COSINE = Math.cos(Math.toRadians(7));

    private ByakuganSight() {
    }

    public static int range(int level, boolean natural, boolean active) {
        int rank = Mth.clamp(level, 0, NATIVE_RANGE.length - 1);
        if (natural) {
            return active ? NATIVE_RANGE[rank] : 0;
        }
        return level <= 0 ? 0 : active ? Math.max(20, NATIVE_RANGE[rank] / 2) : 20;
    }

    public static boolean outsideBlindSpur(Vec3 forward, Vec3 offset) {
        if (offset.horizontalDistanceSqr() < 1.0e-7) {
            return true;
        }
        double dot = (forward.x * offset.x + forward.z * offset.z)
                / Math.sqrt(offset.horizontalDistanceSqr() * forward.horizontalDistanceSqr());
        return !Double.isFinite(dot) || dot > -BLIND_COSINE;
    }

    public static float reveal(float age) {
        float t = Mth.clamp(age / REVEAL, 0, 1);
        return t * t * (3 - 2 * t);
    }

    public static double strength(int level) {
        return level >= 3 ? 6 : level >= 1 ? 3 : 0;
    }

    public static float shielding(int level) {
        return level >= 4 ? 0.6f : level >= 2 ? 0.8f : 1;
    }
}
