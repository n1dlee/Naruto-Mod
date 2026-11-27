package com.sekwah.narutomod.util;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class WaterDragonFlight {
    private static final Direction.Axis[] AXES = Direction.Axis.values();

    private WaterDragonFlight() {
    }

    public static Vec3 advance(Vec3 start, Vec3 destination, double speed) {
        Vec3 offset = destination.subtract(start);
        double distance = offset.length();
        return distance <= speed ? destination : start.add(offset.scale(speed / distance));
    }

    public static Vec3 nearestPoint(AABB box, Vec3 point) {
        return new Vec3(Mth.clamp(point.x, box.minX, box.maxX),
                Mth.clamp(point.y, box.minY, box.maxY), Mth.clamp(point.z, box.minZ, box.maxZ));
    }

    @Nullable
    public static Contact sweep(Vec3 from, Vec3 to, AABB box, double halfWidth) {
        AABB expanded = box.inflate(halfWidth);
        Vec3 step = to.subtract(from);
        double entry = 0.0;
        double exit = 1.0;
        Direction normal = Direction.getNearest(-step.x, -step.y, -step.z);
        for (Direction.Axis axis : AXES) {
            double origin = from.get(axis);
            double delta = step.get(axis);
            double min = expanded.min(axis);
            double max = expanded.max(axis);
            if (Math.abs(delta) < 1.0e-9) {
                if (origin < min || origin > max) {
                    return null;
                }
                continue;
            }
            double first = ((delta > 0.0 ? min : max) - origin) / delta;
            double last = ((delta > 0.0 ? max : min) - origin) / delta;
            if (first > entry) {
                entry = first;
                normal = Direction.fromAxisAndDirection(axis,
                        delta > 0.0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
            }
            exit = Math.min(exit, last);
            if (entry > exit) {
                return null;
            }
        }
        if (exit == 0.0 && step.lengthSqr() > 0.0) {
            return null;
        }
        Vec3 center = from.lerp(to, entry);
        Vec3 point = nearestPoint(box, center);
        Vec3 separation = center.subtract(point);
        if (entry == 0.0 && separation.lengthSqr() > 1.0e-8) {
            normal = Direction.getNearest(separation.x, separation.y, separation.z);
        }
        return new Contact(entry, center, point, normal);
    }

    public record Contact(double fraction, Vec3 center, Vec3 point, Direction normal) {
        public boolean precedes(@Nullable Contact other) {
            return other == null || this.fraction < other.fraction - 1.0e-7;
        }
    }
}
