package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class GatsugaMotion {
    public static final int WINDUP = 5;
    public static final int STEPS = 12;
    public static final int FADE = 6;
    public static final double PARTNER_RANGE = 5.5;

    private GatsugaMotion() {
    }

    public static Vec3 path(Lane lane, float progress) {
        double t = Mth.clamp(progress, 0, 1);
        double bow = 0.87 * Math.sin(Math.PI * t) * (lane.wolf() ? -1 : 1);
        return lane.start().lerp(lane.end(), t).add(lane.side().scale(bow));
    }

    public static Vec3 step(Vec3 feet, Vec3 target) {
        Vec3 shift = target.subtract(feet).multiply(1, 0, 1);
        return shift.lengthSqr() > 1.05 * 1.05 ? shift.normalize().scale(1.05) : shift;
    }

    public static boolean obstructed(Vec3 intended, Vec3 moved) {
        return intended.subtract(moved).horizontalDistanceSqr() > 0.125 * 0.125;
    }

    public record Lane(Vec3 start, Vec3 end, Vec3 side, boolean wolf) {
    }

    public static final class HitLedger {
        private static final int CAPACITY = 16;
        private final Set<UUID> hit = new HashSet<>();

        public boolean mark(UUID target) {
            return this.hit.size() < CAPACITY && this.hit.add(target);
        }
    }

    public enum Stage { BRACE, TRAVEL, BLOCK, CONTACT, STOP }
}
