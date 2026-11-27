package com.sekwah.narutomod.entity.jutsuprojectile;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class DragonTrail {
    private static final int HISTORY_SIZE = 12;
    private final ArrayDeque<Vec3> history = new ArrayDeque<>(HISTORY_SIZE);

    public void push(Vec3 point) {
        if (this.history.peekFirst() != null && this.history.peekFirst().distanceToSqr(point) < 1.0e-8) {
            return;
        }
        if (this.history.size() == HISTORY_SIZE) {
            this.history.removeLast();
        }
        this.history.addFirst(point);
    }

    public void clear() {
        this.history.clear();
    }

    public AABB bounds(AABB head, double padding) {
        AABB bounds = head;
        for (Vec3 point : this.history) {
            bounds = bounds.minmax(new AABB(point, point).inflate(padding));
        }
        return bounds;
    }

    public List<Vec3> sample(Vec3 head, double length, int count) {
        List<Vec3> result = new ArrayList<>(count);
        result.add(head);
        double spacing = length / (count - 1);
        double remaining = spacing;
        Vec3 from = head;
        for (Vec3 point : this.history) {
            double distance = from.distanceTo(point);
            while (distance >= remaining && result.size() < count) {
                from = from.lerp(point, remaining / distance);
                result.add(from);
                distance -= remaining;
                remaining = spacing;
            }
            if (result.size() == count) {
                return result;
            }
            remaining -= distance;
            from = point;
        }
        if (from.distanceToSqr(result.get(result.size() - 1)) > 1.0e-8) {
            result.add(from);
        }
        return result;
    }
}
