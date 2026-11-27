package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;

public final class ClayCloneState {
    public static final int FORM = 9;
    public static final int CHASE = 76;
    public static final int ARM = 18;
    public static final int SHARDS = 12;
    public static final double TRIGGER_DISTANCE = 2.9;
    public static final double BREAK_DISTANCE = 5.2;

    private ClayCloneState() {
    }

    public static Phase pursue(int ticks, double distanceSquared, boolean visible) {
        if (ticks >= CHASE) {
            return Phase.FIZZLE;
        }
        return distanceSquared <= TRIGGER_DISTANCE * TRIGGER_DISTANCE && visible ? Phase.ARMED : Phase.PURSUE;
    }

    public static Phase armed(int ticks, double distanceSquared, boolean visible) {
        if (!visible || distanceSquared > BREAK_DISTANCE * BREAK_DISTANCE) {
            return Phase.FIZZLE;
        }
        return ticks >= ARM ? Phase.BURST : Phase.ARMED;
    }

    public static double seam(double age) {
        double pulse = 0.27 + Math.pow(0.5 + 0.5 * Math.sin(age * (age < 9 ? 0.77 : 1.48)), 3) * 0.73;
        return pulse * Mth.clamp((age + 2) / 8, 0.15, 1);
    }

    public enum Phase { FORM, PURSUE, ARMED, BURST, FIZZLE }
}
