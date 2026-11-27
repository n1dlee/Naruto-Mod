package com.sekwah.narutomod.util;

import net.minecraft.world.phys.Vec3;

public final class TrigramSequence {
    public static final double RING = 3.55;
    public static final double START_RANGE = 3.35;
    public static final double REACH = 3.55;
    public static final int PHASES = 6;
    private static final int[] BEATS = {8, 15, 21, 26, 30, 33};

    private TrigramSequence() {
    }

    public static int beat(int phase) { return BEATS[phase]; }

    public static int total(int phase) { return 2 << phase; }

    public static int strikes(int phase) { return phase == 0 ? 2 : total(phase) - total(phase - 1); }

    public static boolean finalBeat(int phase, boolean accepted) {
        return phase == PHASES - 1 && accepted;
    }

    public static float damage(int phase, float rank, boolean pvp) {
        float max = pvp ? 12 : 24;
        return Math.min(max, 17.2f * rank) * strikes(phase) / 64;
    }

    public static boolean within(Vec3 anchor, Vec3 caster, Vec3 target) {
        return Math.abs(target.y - anchor.y) <= 1.50 && Math.abs(caster.y - anchor.y) <= 1.05
                && target.subtract(anchor).horizontalDistanceSqr() <= RING * RING
                && caster.subtract(anchor).horizontalDistanceSqr() <= RING * RING
                && caster.distanceToSqr(target) <= REACH * REACH;
    }

    public enum Cue { STANCE, BURST, SEALED, BREAK }
}
