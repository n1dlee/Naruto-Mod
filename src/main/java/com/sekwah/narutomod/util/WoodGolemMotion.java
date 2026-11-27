package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class WoodGolemMotion {
    public static final int GROWTH = 18;
    public static final int WINDUP = 11;
    public static final int SWING = 6;
    public static final int RECOVERY = 12;
    public static final double FIST = 0.40;

    private WoodGolemMotion() {
    }

    public static double ease(double phase) {
        double t = Mth.clamp(phase, 0, 1);
        return t * t * (3 - 2 * t);
    }

    public static boolean active(int age) { return age >= WINDUP && age < WINDUP + SWING; }

    public static Vec3 arm(double age) {
        double cock = ease(age / WINDUP);
        double swipe = ease((age - WINDUP) / SWING);
        double drop = ease((age - WINDUP + 3) / 5);
        double rest = ease((age - WINDUP - SWING) / RECOVERY);
        double angle = (0.29 + 1.04 * cock - 2.19 * swipe) * (1 - rest);
        double reach = (0.16 + 1.13 * cock + 0.31 * swipe) * (1 - rest);
        double down = (0.56 - 0.47 * cock + 1.43 * drop + 0.20 * swipe) * (1 - rest) + rest * 1.45;
        return new Vec3(Math.sin(angle) * reach, -down, Math.cos(angle) * reach);
    }

    public static Vec3 fist(Vec3 feet, float yaw, double attackAge) {
        Vec3 arm = arm(attackAge);
        Vec3 forward = KamuiStep.facing(yaw);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        return shoulder(feet, yaw).add(0, arm.y, 0).add(right.scale(arm.x)).add(forward.scale(arm.z));
    }

    public static Vec3 shoulder(Vec3 feet, float yaw) {
        Vec3 forward = KamuiStep.facing(yaw);
        return feet.add(-forward.z * 0.74, 3.31, forward.x * 0.74);
    }

    public static boolean validLease(boolean exists, boolean sameWorld, boolean alive, long tick, long expires) {
        return exists && sameWorld && alive && tick <= expires;
    }
}
