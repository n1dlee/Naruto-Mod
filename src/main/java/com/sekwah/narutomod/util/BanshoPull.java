package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class BanshoPull {
    public static final int FOCUS = 5;
    public static final int DURATION = 10;
    public static final double RANGE = 17;
    public static final double STOP = 1.85;

    private BanshoPull() {
    }

    public static Vec3 velocity(Vec3 feet, Vec3 destination, int tick, boolean pvp, double resistance, boolean grounded) {
        Vec3 offset = destination.subtract(feet);
        double distance = offset.length();
        if (distance <= STOP) {
            return Vec3.ZERO;
        }
        double strength = (0.40 + 0.60 * Mth.clamp(tick / 4.0, 0, 1)) * Mth.clamp(1 - resistance, 0, 1);
        Vec3 ask = offset.scale(Math.min((distance - STOP) * 0.42, (pvp ? 0.64 : 1.08) * strength) / distance);
        return new Vec3(ask.x, Mth.clamp(ask.y, grounded ? 0 : -0.31, pvp ? 0.19 : 0.30), ask.z);
    }

    public enum Stage { FOCUS, DRAG, RELEASE, BREAK }
}
