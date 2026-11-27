package com.sekwah.narutomod.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class FlickerPath {
    private static final double SKIN = 1.0e-4;
    public static final double MIN_MOVEMENT = 0.40;

    private FlickerPath() {
    }

    public static double contact(Vec3 feet, Vec3 destination, AABB obstacle, double width, double height) {
        double side = width * 0.5 - SKIN;
        AABB occupied = new AABB(obstacle.minX - side, obstacle.minY - height + SKIN, obstacle.minZ - side,
                obstacle.maxX + side, obstacle.maxY - SKIN, obstacle.maxZ + side);
        var hit = WaterDragonFlight.sweep(feet, destination, occupied, 0);
        return hit == null ? 1.0 : hit.fraction();
    }
}
