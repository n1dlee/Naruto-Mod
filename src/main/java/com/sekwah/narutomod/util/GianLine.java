package com.sekwah.narutomod.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class GianLine {
    public static final double RANGE = 20;
    public static final double RADIUS = 0.19;
    public static final int PREPARE = 7;
    public static final int SHOCK = 3;
    public static final int FADE = 5;

    private GianLine() {
    }

    @Nullable
    public static WaterDragonFlight.Contact contact(Vec3 from, Vec3 to, AABB shape) {
        return WaterDragonFlight.sweep(from, to, shape, RADIUS);
    }

    public static boolean precedesWall(WaterDragonFlight.Contact entity, @Nullable WaterDragonFlight.Contact wall) {
        return entity.precedes(wall);
    }

    public static float nextDamage(float damage, boolean accepted) {
        return accepted ? damage * 0.65f : damage;
    }
}
