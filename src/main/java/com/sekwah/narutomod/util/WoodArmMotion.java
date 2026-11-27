package com.sekwah.narutomod.util;

import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class WoodArmMotion {
    public static final int WINDUP = 5;
    public static final double RANGE = 14;
    public static final double RADIUS = 0.25;
    private static final double[] STEPS = {0.92, 1.52, 1.98, 2.4, 2.62};

    private WoodArmMotion() {
    }

    public static double step(int age, double remaining) {
        return Math.min(Math.max(0, remaining), STEPS[Math.min(age, STEPS.length - 1)]);
    }

    public static boolean hits(@Nullable WaterDragonFlight.Contact actor, @Nullable WaterDragonFlight.Contact block) {
        return actor != null && actor.precedes(block);
    }

    public static Vec3 pull(Vec3 target, Vec3 caster, boolean pvp) {
        Vec3 direction = caster.subtract(target).multiply(1, 0, 1);
        double remaining = Math.max(0, direction.horizontalDistance() - 1.55);
        return direction.normalize().scale(Math.min(remaining * 0.31, pvp ? 0.26 : 0.43));
    }
}
