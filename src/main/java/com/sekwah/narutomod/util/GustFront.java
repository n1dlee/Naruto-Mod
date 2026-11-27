package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GustFront {
    public static final int PREPARE = 7;
    public static final int FLIGHT = 7;
    public static final int FADE = 5;
    public static final int LANES = 11;
    public static final double RANGE = 12;
    public static final double MOB_RANGE = 8;
    private static final Vec3 UP = new Vec3(0, 1, 0);

    private GustFront() {
    }

    public static Frame frame(Vec3 direction) {
        Vec3 forward = direction.normalize();
        Vec3 right = forward.cross(Math.abs(forward.y) > 0.95 ? new Vec3(1, 0, 0) : UP).normalize();
        return new Frame(forward, right, right.cross(forward).normalize());
    }

    public static double distance(float age, double range) {
        return distance(age, range, Shape.WIND);
    }

    public static double distance(float age, double range, Shape shape) {
        return Mth.clamp(age * range / shape.flight(), 0, range);
    }

    public static int lane(Frame frame, Vec3 offset) {
        return lane(frame, offset, Shape.WIND);
    }

    public static int lane(Frame frame, Vec3 offset, Shape shape) {
        double forward = offset.dot(frame.forward());
        if (forward <= 0) {
            return -1;
        }
        double side = offset.dot(frame.right());
        double angle = Math.atan2(side, forward);
        double vertical = offset.dot(frame.up());
        double top = shape.arch(forward);
        if (Math.abs(angle) > shape.halfAngle + 1.0e-7 || vertical > top || vertical < -top * 0.77) {
            return -1;
        }
        return Mth.clamp((int) Math.round((angle / shape.halfAngle + 1) * (LANES - 1) * 0.5), 0, LANES - 1);
    }

    public static boolean crossed(double range, double start, double end, double obstruction) {
        return range >= start - 0.18 && range <= end && range < obstruction - 0.025;
    }

    public static double arch(double forward) {
        return Shape.WIND.arch(forward);
    }

    public static double strength(double range, double rank, boolean mob) {
        double f = 1 - 0.56 * Mth.clamp(range / (mob ? MOB_RANGE : RANGE), 0, 1);
        return f * (mob ? 0.68 : Mth.clamp(0.78 + rank * 0.22, 0.82, 1.58));
    }

    public static double palmRange(boolean focused) {
        return focused ? 8 : 4;
    }

    public static Vec3 palmProbe(AABB target, Vec3 origin, Vec3 aim) {
        double center = target.getCenter().subtract(origin).dot(aim);
        double extent = (target.getXsize() * Math.abs(aim.x) + target.getYsize() * Math.abs(aim.y)
                + target.getZsize() * Math.abs(aim.z)) * 0.5;
        return origin.add(aim.scale(Math.max(0.08, center - extent)));
    }

    public record Frame(Vec3 forward, Vec3 right, Vec3 up) {
        public Vec3 lane(int index) {
            return lane(index, Shape.WIND);
        }

        public Vec3 lane(int index, Shape shape) {
            double angle = ((index * 2.0 / (LANES - 1)) - 1) * shape.halfAngle;
            return forward.scale(Math.cos(angle)).add(right.scale(Math.sin(angle)));
        }
    }

    public enum Shape {
        WIND(PREPARE, FLIGHT, 35, 0.31, 0.11),
        PALM(4, 5, 19, 0.24, 0.053);

        private final int prepare;
        private final int flight;
        private final double halfAngle;
        private final double height;
        private final double growth;

        Shape(int prepare, int flight, double halfAngle, double height, double growth) {
            this.prepare = prepare;
            this.flight = flight;
            this.halfAngle = Math.toRadians(halfAngle);
            this.height = height;
            this.growth = growth;
        }

        public int prepare() { return this.prepare; }

        public int flight() { return this.flight; }

        public double arch(double forward) { return this.height + forward * this.growth; }
    }
}
