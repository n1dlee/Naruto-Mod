package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class WindVortex {
    public static final double RADIUS = 4.5;
    public static final int EXPANSION_TICKS = 4;
    public static final int LIFETIME = 12;
    private static final int MAX_MARKS = 256;
    private final Set<UUID> touched = new HashSet<>();

    public static double radius(double age) {
        double remaining = 1.0 - Mth.clamp(age / EXPANSION_TICKS, 0.0, 1.0);
        return RADIUS * (1.0 - remaining * remaining * remaining);
    }

    public boolean mark(UUID target) {
        return this.touched.size() < MAX_MARKS && this.touched.add(target);
    }

    public Set<UUID> marks() {
        return Set.copyOf(this.touched);
    }

    public static Vec3 velocity(Vec3 direction, int age) {
        return direction.scale(Mth.lerp(Mth.clamp(age / 4.0, 0, 1), 0.9, 1.65));
    }
}
