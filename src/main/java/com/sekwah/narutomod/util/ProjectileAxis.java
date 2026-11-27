package com.sekwah.narutomod.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class ProjectileAxis {
    private static final Vec3 FORWARD = new Vec3(0, 0, 1);
    private static final double MIN_LENGTH_SQUARED = 1.0e-12;

    private ProjectileAxis() {
    }

    public static Vec3 direction(Vec3 preferred, Vec3 fallback) {
        if (usable(preferred)) {
            return normalize(preferred);
        }
        return usable(fallback) ? normalize(fallback) : FORWARD;
    }

    private static boolean usable(Vec3 value) {
        double length = value.lengthSqr();
        return Double.isFinite(length) && length >= MIN_LENGTH_SQUARED;
    }

    private static Vec3 normalize(Vec3 value) {
        return value.scale(1.0 / Math.sqrt(value.lengthSqr()));
    }

    public static Vector3f encode(Vec3 direction) {
        return new Vector3f((float) direction.x, (float) direction.y, (float) direction.z);
    }

    public static Vec3 decode(Vector3f packed) {
        return new Vec3(packed.x(), packed.y(), packed.z());
    }

    public static Angles angles(Vec3 direction) {
        Vec3 vector = ProjectileAxis.direction(direction, FORWARD);
        float yaw = (float) Math.toDegrees(Math.atan2(-vector.x, vector.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(vector.y, vector.horizontalDistance()));
        return new Angles(yaw, pitch);
    }

    public record Angles(float yaw, float pitch) {
    }
}
