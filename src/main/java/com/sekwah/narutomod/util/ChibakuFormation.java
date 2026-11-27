package com.sekwah.narutomod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ChibakuFormation {
    public static final int SETTLE = 16;
    public static final int MAX_SOURCES = 22;
    public static final Profile PLAYER = new Profile(new Rise(20, 0.43), 74, 14, 8.5, 11, 15);
    public static final Profile NPC = new Profile(new Rise(90, 0.50), 160, 128, 24, 48, 45);
    private static final double GOLDEN = Math.PI * (3 - Math.sqrt(5));

    private ChibakuFormation() {
    }

    public static Vec3 radial(int slot, int count, double spin) {
        double y = 1 - 2.0 * (slot + 0.5) / count;
        double across = Math.sqrt(1 - y * y);
        double theta = GOLDEN * slot + spin;
        return new Vec3(Math.cos(theta) * across, y, Math.sin(theta) * across);
    }

    public static double grow(double age, int total) {
        double t = Mth.clamp(age / total, 0, 1);
        return 0.24 + 2.16 * t * t * (3 - 2 * t);
    }

    public static double flight(double age, int slot) {
        double t = Mth.clamp((age - (slot % 11) * 2) / (21 + (slot % 4) * 3), 0, 1);
        return t * t * (3 - 2 * t);
    }

    public static Vec3 drift(Vec3 from, Vec3 to, double amount, int slot) {
        if (amount <= 0) {
            return from;
        }
        if (amount >= 1) {
            return to;
        }
        double angle = slot * GOLDEN + amount * Math.PI * 1.35;
        double swing = Math.sin(Math.PI * amount) * (0.74 + slot % 5 * 0.13);
        return from.lerp(to, amount).add(Math.cos(angle) * swing, swing * 1.4, Math.sin(angle) * swing);
    }

    public static double strength(double distance, double radius) {
        return Mth.clamp(1 - distance / radius, 0, 1);
    }

    public record Source(BlockPos origin, int tint) {
    }

    public record Rise(int ticks, double speed) {
    }

    public record Profile(Rise rise, int gatherTicks, double pullRange, double impactRange, double shockRange, float impactDamage) {
    }

    public enum Phase { RISE, GATHER, FALL, IMPACT, DISSOLVE, LEGACY }
}
