package com.sekwah.narutomod.util;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class LaserCircusPath {
    public static final int GATHER = 8;
    public static final int BRANCHES = 4;
    public static final int SAMPLES = 13;
    public static final double RANGE = 17;
    public static final double CONTACT = 0.13;

    private LaserCircusPath() {
    }

    public static Vec3 endpoint(Vec3 from, Vec3 facing, int lane) {
        double side = lane - (BRANCHES - 1) * 0.5;
        Vec3 lateral = side(facing);
        return from.add(facing.scale(RANGE)).add(lateral.scale(side * 1.08)).add(0, side % 1 * 0.73, 0);
    }

    public static List<Vec3> route(Vec3 from, Vec3 to, int lane) {
        Vec3 axis = ProjectileAxis.direction(to.subtract(from), new Vec3(0, 0, 1));
        Vec3 side = side(axis);
        Vec3 upper = axis.cross(side);
        double distance = Math.min(1, from.distanceTo(to) / 7);
        double lateral = (lane - 1.5) * distance * 0.78;
        var path = new ArrayList<Vec3>(SAMPLES);
        for (int i = 0; i < SAMPLES; i++) {
            double t = i / (double) (SAMPLES - 1);
            double bend = Math.sin(Math.PI * t) * (1.15 - 0.52 * t);
            Vec3 part = from.lerp(to, t).add(side.scale(bend * lateral))
                    .add(upper.scale(Math.sin(Math.PI * 2 * t) * distance * (0.29 + lane * 0.11)));
            path.add(i == 0 ? from : i == SAMPLES - 1 ? to : part);
        }
        return List.copyOf(path);
    }

    private static Vec3 side(Vec3 axis) {
        return (Math.abs(axis.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).cross(axis).normalize();
    }
}
