package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class AdamantineChain {
    public static final int BRANCHES = 3;
    public static final int WINDUP = 8;
    public static final int RETRACT = 5;
    public static final int SEGMENTS = 7;
    public static final double REACH = 10;
    private static final double CONE = Math.cos(Math.toRadians(34));

    private AdamantineChain() {
    }

    public static boolean inCone(Vec3 start, Vec3 target, Vec3 aim) {
        Vec3 delta = target.subtract(start);
        return delta.lengthSqr() <= REACH * REACH && delta.normalize().dot(aim) >= CONE;
    }

    public static Vec3 point(Vec3 from, Vec3 to, double progress, boolean taut) {
        double arc = Math.sin(Math.PI * progress);
        double lift = taut ? -0.12 : 0.34;
        return from.lerp(to, progress).add(0, arc * lift, 0);
    }

    public static float extension(float age) {
        return Mth.clamp((age - 1) / (WINDUP - 1), 0, 1);
    }

    public static int duration(boolean pvp, boolean beast) {
        return pvp ? 22 : beast ? 62 : 46;
    }

    public enum Phase { SEEK, SEALED, RETRACT }
}
