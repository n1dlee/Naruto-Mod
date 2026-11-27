package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PhoenixVolley {
    public static final int PETALS = 6;
    public static final int WINDUP = 4;
    public static final int INTERVAL = 3;
    public static final int MAX_FLIGHT = 30;
    public static final int FADE_TICKS = 5;
    private static final Vec3 UP = new Vec3(0, 1, 0);

    private final Set<UUID> victims = new HashSet<>();

    public boolean hasHit(UUID entity) {
        return this.victims.contains(entity);
    }

    public boolean markHit(UUID entity) {
        return this.victims.add(entity);
    }

    public static Vec3 heading(Vec3 direction, Vec3 horizontal, int index) {
        int lane = Mth.clamp(index, 0, PETALS - 1);
        Vec3 forward = direction.normalize();
        Vec3 side = (Math.abs(forward.y) > 0.96 ? horizontal : forward).cross(UP).normalize();
        Vec3 lift = side.cross(forward).normalize();
        double offset = (lane - (PETALS - 1) / 2.0) * 0.115;
        double height = (lane % 2 == 0 ? 1 : -1) * (0.035 + Math.abs(offset) * 0.04);
        return forward.add(side.scale(offset)).add(lift.scale(height)).normalize();
    }

    public static double radius(int age) {
        return 0.14 + 0.09 * Mth.clamp(age / 5.0, 0, 1);
    }

    public static double speed(int age) {
        return 0.98 + 0.55 * Mth.clamp(age / 5.0, 0, 1);
    }
}
