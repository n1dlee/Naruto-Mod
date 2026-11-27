package com.sekwah.narutomod.util;

import net.minecraft.world.phys.Vec3;

public final class KotoInfluence {
    public static final double RANGE = 16;
    public static final int FOCUS_TICKS = 7;
    public static final int MOB_TICKS = 100;
    public static final int PLAYER_TICKS = 42;
    public static final int RESISTED_TICKS = 18;
    public static final double ATTACK_PENALTY = -0.24;
    private static final double MIN_AIM = Math.cos(Math.toRadians(11));

    private KotoInfluence() {
    }

    public static boolean aimed(Vec3 origin, Vec3 direction, Vec3 target) {
        Vec3 offset = target.subtract(origin);
        double length = offset.lengthSqr();
        return length >= 0.04 && length <= RANGE * RANGE && offset.dot(direction) >= MIN_AIM * Math.sqrt(length);
    }

    public static int playerDuration(boolean perceptiveEye) {
        return perceptiveEye ? RESISTED_TICKS : PLAYER_TICKS;
    }

    public static boolean maintains(long now, long expires, boolean sameWorld, double distanceSquared) {
        return now < expires && sameWorld && distanceSquared <= 24 * 24;
    }

    public static float afterimage(float age, double strength) {
        if (age < 0 || age >= 12) {
            return 0;
        }
        return (float) (Math.sin(Math.PI * (age + 0.4) / 12.4) * Math.max(0, Math.min(1, strength)));
    }
}
