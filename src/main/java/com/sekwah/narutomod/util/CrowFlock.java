package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CrowFlock {
    public static final int LANES = 7;
    public static final int GATHER = 5;
    public static final int FLIGHT = 11;
    public static final int FADE = 5;
    public static final double RANGE = 9;
    public static final double HULL = 0.34;

    private CrowFlock() {
    }

    public static Vec3 heading(float yaw, int lane) {
        return KamuiStep.facing(yaw + ((lane * 2f / (LANES - 1)) - 1f) * 33);
    }

    public static double travel(float age) {
        double t = Mth.clamp((age - GATHER) / FLIGHT, 0, 1);
        return RANGE * t * (1.36 - 0.36 * t);
    }

    public static boolean touches(AABB target, Vec3 origin, Vec3 heading, double prior, double next, double stop) {
        if (next <= prior || stop - 0.04 <= prior || target.getCenter().subtract(origin).dot(heading) < 0) {
            return false;
        }
        Vec3 start = origin.add(heading.scale(prior));
        Vec3 end = origin.add(heading.scale(Math.min(next, stop - 0.04)));
        AABB silhouette = target.inflate(HULL);
        return silhouette.contains(start) || silhouette.clip(start, end).isPresent();
    }

    public static int birdBudget(boolean low, double cameraDistanceSquared) {
        return low || cameraDistanceSquared >= 20 * 20 ? 7 : 17;
    }
}
