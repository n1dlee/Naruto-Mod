package com.sekwah.narutomod.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class ChidoriDashPath {
    private static final double HAND_WIDTH = 0.38;
    private static final double MOVEMENT_EPSILON = 1.0e-5;

    private ChidoriDashPath() {
    }

    public static Vec3 groundHeading(Vec3 direction, float yaw) {
        Vec3 horizontal = direction.multiply(1, 0, 1);
        return horizontal.lengthSqr() < MOVEMENT_EPSILON
                ? Vec3.directionFromRotation(0, yaw) : horizontal.normalize();
    }

    public static boolean obstructed(Vec3 attempted, Vec3 actual) {
        return attempted.subtract(actual).horizontalDistanceSqr() > MOVEMENT_EPSILON;
    }

    @Nullable
    public static Contact contact(Vec3 from, Vec3 reached, AABB target) {
        if (from.subtract(reached).horizontalDistanceSqr() < MOVEMENT_EPSILON) {
            return null;
        }
        AABB reach = target.inflate(HAND_WIDTH, 0.30, HAND_WIDTH);
        Vec3 center = ChidoriStrike.firstContact(reach, from, reached).orElse(null);
        if (center == null) {
            return null;
        }
        return new Contact(center, WaterDragonFlight.nearestPoint(target, center), from.distanceToSqr(center));
    }

    public record Contact(Vec3 center, Vec3 point, double distanceSqr) {
    }
}
