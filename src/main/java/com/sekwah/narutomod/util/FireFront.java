package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class FireFront {
    public static final int LANES = 11;
    public static final int WINDUP = 12;
    public static final double RANGE = 14.0;
    public static final double HEIGHT = 3.1;
    private static final double SPREAD = 0.69;

    private FireFront() {
    }

    public static double depth(double age) {
        return Mth.clamp(age * 0.94, 0.0, RANGE);
    }

    public static double laneOffset(int lane, double depth) {
        return ((2.0 * lane) / (LANES - 1) - 1.0) * depth * SPREAD;
    }

    public static Vec3 point(Vec3 origin, Vec3 forward, int lane, double depth, double height) {
        Vec3 side = new Vec3(forward.z, 0, -forward.x);
        return origin.add(forward.scale(depth)).add(side.scale(laneOffset(lane, depth))).add(0, height, 0);
    }

    public static int lane(Vec3 relative, Vec3 forward, double depth) {
        double lateral = relative.dot(new Vec3(forward.z, 0, -forward.x));
        double width = depth * SPREAD;
        if (depth < 0.0 || Math.abs(lateral) > width + 0.27) {
            return -1;
        }
        double normalized = width > 1.0e-5 ? Mth.clamp(lateral / width, -1.0, 1.0) : 0.0;
        return (int) Math.round((normalized + 1.0) * (LANES - 1) * 0.5);
    }
}
