package com.sekwah.narutomod.util;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class SurfaceStride {
    private static final double STEP_LENGTH = 0.46;
    private static final double DEAD_ZONE = 0.008;
    private static final double MAX_DISPLACEMENT_SQR = 1.0;
    @Nullable
    private Direction face;
    private Vec3 previousPosition = Vec3.ZERO;
    private Vec3 forward = Vec3.ZERO;
    private double previousSteps;
    private double steps;
    private float previousPace;
    private float pace;

    public int advance(Vec3 position, @Nullable Direction surface) {
        Vec3 movement = position.subtract(this.previousPosition);
        this.previousPosition = position;
        this.previousSteps = this.steps;
        this.previousPace = this.pace;
        if (surface == null || movement.lengthSqr() > MAX_DISPLACEMENT_SQR) {
            this.clear(surface);
            return 0;
        }
        if (surface != this.face) {
            this.face = surface;
            this.forward = Vec3.ZERO;
            return 0;
        }
        Vec3 tangent = project(movement, normal(surface));
        double distance = tangent.length();
        if (distance < DEAD_ZONE) {
            this.pace = Mth.approach(this.pace, 0.0f, 0.4f);
            return 0;
        }
        this.forward = tangent.scale(1.0 / distance);
        this.pace = Mth.approach(this.pace, (float) Math.min(distance / 0.18, 1.0), 0.5f);
        this.steps += distance / STEP_LENGTH;
        int step = Mth.floor(this.steps);
        return step > Mth.floor(this.previousSteps) ? (step % 2 == 0 ? 1 : -1) : 0;
    }

    public Vec3 forward() {
        return this.forward;
    }

    public Sample sample(float partialTick) {
        return new Sample((float) (Mth.lerp(partialTick, this.previousSteps, this.steps) % 2.0) * Mth.PI,
                Mth.lerp(partialTick, this.previousPace, this.pace));
    }

    private void clear(@Nullable Direction face) {
        this.face = face;
        this.previousSteps = this.steps = 0;
        this.previousPace = this.pace = 0;
        this.forward = Vec3.ZERO;
    }

    public static Vec3 project(Vec3 motion, Vec3 normal) {
        return motion.subtract(normal.scale(motion.dot(normal)));
    }

    public static Vec3 normal(Direction surface) {
        return Vec3.atLowerCornerOf(surface.getNormal());
    }

    public record Sample(float phase, float pace) {
    }
}
