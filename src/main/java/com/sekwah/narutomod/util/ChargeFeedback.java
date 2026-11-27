package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;

public final class ChargeFeedback {
    public static final int RAMP_TICKS = 8;
    public static final int FADE_TICKS = 5;
    private static final float EPSILON = 0.0001f;
    private float total;
    private boolean completed;

    public Tick accept(float before, float after, float capacity, float stationaryRate) {
        float gain = Math.max(0.0f, after - before);
        if (gain <= EPSILON) {
            return new Tick(0, false);
        }
        this.total += gain;
        int strength = Mth.clamp(Math.round(gain * 4 / Math.max(stationaryRate, EPSILON)), 1, 4);
        boolean full = !this.completed && before < capacity - EPSILON && after >= capacity - EPSILON;
        this.completed |= full;
        return new Tick(strength, full);
    }

    public boolean earned(float capacity) {
        return this.total >= Math.max(0.5f, capacity * 0.025f);
    }

    public void clear() {
        this.total = 0;
        this.completed = false;
    }

    public record Tick(int strength, boolean completed) {
    }
}
