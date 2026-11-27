package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class KaitenDome {
    public static final int WINDUP = 4;
    public static final int MAX_ACTIVE = 24;
    public static final int COLLAPSE = 5;
    public static final double RADIUS = 2.47;
    public static final double HEIGHT = 2.67;
    private static final double EPSILON = 1.0e-7;

    private KaitenDome() {
    }

    public static Vec3 surface(double angle, double latitude) {
        double radius = RADIUS * Math.cos(latitude);
        return new Vec3(radius * Math.cos(angle), HEIGHT * Math.sin(latitude), radius * Math.sin(angle));
    }

    public static Vec3 scaled(Vec3 position) {
        return new Vec3(position.x / RADIUS, position.y / HEIGHT, position.z / RADIUS);
    }

    public static Vec3 normal(Vec3 surface) {
        return scaled(scaled(surface)).normalize();
    }

    public static @Nullable Vec3 entry(Vec3 center, Vec3 from, Vec3 to) {
        Vec3 a = scaled(from.subtract(center));
        Vec3 motion = scaled(to.subtract(from));
        double c = a.lengthSqr() - 1;
        double b = a.dot(motion);
        double d = motion.lengthSqr();
        double discriminant = b * b - d * c;
        if (c < -EPSILON || d < EPSILON || b >= 0 || discriminant <= EPSILON) {
            return null;
        }
        double t = (-b - Math.sqrt(discriminant)) / d;
        if (t < -EPSILON || t > 1 + EPSILON) {
            return null;
        }
        Vec3 hit = from.lerp(to, Mth.clamp(t, 0, 1));
        return hit.y >= center.y - EPSILON ? hit : null;
    }

    public static Vec3 reflect(Vec3 velocity, Vec3 normal) {
        double dot = velocity.dot(normal);
        return velocity.subtract(normal.scale(2 * dot)).scale(0.87);
    }

    public static boolean active(long age) {
        return age >= WINDUP && age < WINDUP + MAX_ACTIVE;
    }

    public enum Step { GATHER, ACTIVE, CONTACT, COLLAPSE }
}
