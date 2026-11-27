package com.sekwah.narutomod.util;

import net.minecraft.world.phys.Vec3;

public final class DashStride {
    public static final int LEAN_TICKS = 2;
    public static final int BRAKE_TICKS = 4;
    private static final double MIN_STEP_SQR = 0.035 * 0.035;
    private static final double MAX_STEP_SQR = 2.0 * 2.0;

    private Vec3 previous;
    private Vec3 step = Vec3.ZERO;
    private int running;
    private int braking;
    private boolean reset;

    public void update(Vec3 point, boolean enabled) {
        Vec3 delta = this.previous == null ? Vec3.ZERO : point.subtract(this.previous);
        this.previous = point;
        this.reset = delta.lengthSqr() > MAX_STEP_SQR;
        Vec3 tangent = delta.multiply(1, 0, 1);
        if (!this.reset && enabled && tangent.lengthSqr() >= MIN_STEP_SQR) {
            this.step = delta;
            this.running = Math.min(this.running + 1, LEAN_TICKS + 1);
            this.braking = 0;
        } else {
            this.step = Vec3.ZERO;
            this.braking = this.reset ? 0 : this.running > 0 ? BRAKE_TICKS : Math.max(0, this.braking - 1);
            this.running = 0;
        }
    }

    public boolean moving() {
        return this.running > 0;
    }

    public boolean starts() {
        return this.running == 1;
    }

    public boolean reset() {
        return this.reset;
    }

    public int runTicks() {
        return this.running;
    }

    public int brakeTicks() {
        return this.braking;
    }

    public Vec3 step() {
        return this.step;
    }
}
