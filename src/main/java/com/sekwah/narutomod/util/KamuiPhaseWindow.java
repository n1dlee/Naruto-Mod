package com.sekwah.narutomod.util;

import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public final class KamuiPhaseWindow {
    public static final int OPEN = 3;
    public static final int HOLD = 25;
    public static final int RETURN = 5;
    public static final double TETHER = 11;

    private KamuiPhaseWindow() {
    }

    public enum Stage {
        OPENING, PHASED, RESOLVING, SEALED;

        public boolean collisionOff() { return this == PHASED || this == RESOLVING; }
        public boolean protects() { return this == PHASED; }
    }

    public static Stage advance(int age, boolean requested, boolean withinTether) {
        if (!requested || !withinTether || age >= OPEN + HOLD) {
            return Stage.RESOLVING;
        }
        return age < OPEN ? Stage.OPENING : Stage.PHASED;
    }

    @Nullable
    public static Vec3 settle(Vec3 now, Vec3 lastSafe, Vec3 origin, Predicate<Vec3> free) {
        if (free.test(now)) {
            return now;
        }
        if (!lastSafe.equals(now) && free.test(lastSafe)) {
            return lastSafe;
        }
        if (!origin.equals(lastSafe) && !origin.equals(now) && free.test(origin)) {
            return origin;
        }
        for (int lift : new int[] {1, 0, 2, -1, 3, 4, 6}) {
            for (int step = 0; step <= 3; step++) {
                int samples = step == 0 ? 1 : 8;
                for (int i = 0; i < samples; i++) {
                    double angle = i * Math.PI * 2 / samples;
                    Vec3 candidate = lastSafe.add(Math.cos(angle) * step, lift, Math.sin(angle) * step);
                    if (free.test(candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }
}
