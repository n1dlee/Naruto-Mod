package com.sekwah.narutomod.util;

import net.minecraft.world.phys.Vec3;

public final class SageFocus {
    public static final int MINIMUM = 50;
    public static final int MAXIMUM = 100;
    public static final int DANGER_TICKS = 300;
    public static final int STONE_TICKS = 200;
    private static final double FOOT_DRIFT_SQR = 0.0025;
    private static final double SEAT_DRIFT_SQR = 0.0169;

    private Vec3 seat;
    private Vec3 previous;
    private int fullTicks;
    private boolean steady;
    private boolean committed;
    private Beat lastVisual;

    public enum Beat { DRAW, BALANCED, OVERFLOW, FRACTURE, PETRIFY }

    public void reset() {
        seat = null;
        previous = null;
        fullTicks = 0;
        steady = false;
        committed = false;
        lastVisual = null;
    }

    public Sample advance(int reserve, Vec3 feet, boolean onStableGround) {
        if (seat == null) {
            seat = feet;
            previous = feet;
        }
        steady = isSteadyAt(feet, onStableGround);
        previous = feet;
        if (!steady) {
            seat = feet;
            fullTicks = 0;
            return new Sample(Math.max(0, reserve - 2), Beat.FRACTURE, 0);
        }
        if (reserve < MAXIMUM) {
            fullTicks = 0;
            int charge = reserve + 1;
            return new Sample(charge, charge >= MINIMUM ? Beat.BALANCED : Beat.DRAW, charge);
        }
        fullTicks++;
        if (fullTicks >= DANGER_TICKS) {
            return new Sample(0, Beat.PETRIFY, 100);
        }
        return new Sample(MAXIMUM, Beat.OVERFLOW, fullTicks * 100 / DANGER_TICKS);
    }

    public boolean canRelease(int reserve, Vec3 feet, boolean onStableGround) {
        return steady && reserve >= MINIMUM && isSteadyAt(feet, onStableGround);
    }

    public boolean publish(Beat beat, int age) {
        boolean changed = lastVisual != beat;
        lastVisual = beat;
        return changed || age % 8 == 0;
    }

    private boolean isSteadyAt(Vec3 feet, boolean onStableGround) {
        return onStableGround && seat != null && feet.distanceToSqr(seat) <= SEAT_DRIFT_SQR
                && feet.distanceToSqr(previous) <= FOOT_DRIFT_SQR;
    }

    public boolean committed() {
        return committed;
    }

    public void commit() {
        committed = true;
    }

    public static int duration(int charge) {
        return Math.max(0, Math.min(MAXIMUM, charge)) * 12;
    }

    public record Sample(int charge, Beat beat, int intensity) {
    }
}
