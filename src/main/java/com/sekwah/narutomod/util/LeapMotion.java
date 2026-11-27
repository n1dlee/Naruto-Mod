package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class LeapMotion {
    public static final int WINDUP = 3;
    private static final float[] SPEED = {2.0f, 2.5f, 3.2f, 4.0f, 5.5f};

    private LeapMotion() {
    }

    public static Vec3 impulse(int rank, Vec3 look, float yaw, float strafe, float forward) {
        int tier = Mth.clamp(rank, 0, SPEED.length - 1);
        double up = 0.8 + tier * 0.1;
        if (Math.abs(strafe) + Math.abs(forward) < 0.01) {
            Vec3 aim = look.normalize();
            return new Vec3(aim.x * SPEED[tier], aim.y * 0.6 + up, aim.z * SPEED[tier]);
        }
        Vec3 local = new Vec3(strafe, 0, forward).normalize();
        double angle = Math.toRadians(yaw);
        double x = local.x * Math.cos(angle) - local.z * Math.sin(angle);
        double z = local.z * Math.cos(angle) + local.x * Math.sin(angle);
        double horizontal = SPEED[tier] * (forward > 0 ? 1.0 : 0.75);
        return new Vec3(x * horizontal, up * (forward > 0 ? 1.0 : 0.65), z * horizontal);
    }

    public static boolean airborne(double baseY, double currentY, boolean supported) {
        return !supported && currentY - baseY > 0.055;
    }

    public static float impact(double fallHeight, double speedDown) {
        return (float) Mth.clamp(Math.max(fallHeight / 7.0, speedDown / 1.2), 0.12, 1.0);
    }
}
