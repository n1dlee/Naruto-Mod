package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class KikaichuFlight {
    public static final int GATHER = 5;
    public static final int FEED = 7;
    public static final int MAX_LEG_TICKS = 22;
    public static final double RANGE = 9;
    public static final double RETURN_DISTANCE = 12;
    public static final double RADIUS = 0.26;
    public static final float COST = 22;
    public static final float MAX_DRAIN = 13;

    private KikaichuFlight() {
    }

    public static Vec3 advance(Vec3 from, Vec3 to, boolean returning) {
        return WaterDragonFlight.advance(from, to, returning ? 0.78 : 0.70);
    }

    public static boolean reached(Vec3 next, Vec3 destination) {
        return next.distanceToSqr(destination) <= 0.11 * 0.11;
    }

    public static float credit(float before, float after) {
        if (!Float.isFinite(before) || !Float.isFinite(after)) {
            return 0;
        }
        return Mth.clamp(before - Math.max(0, after), 0, MAX_DRAIN) * 0.58f;
    }

    public static final class Cargo {
        private boolean loaded;
        private boolean settled;
        private float amount;

        public void collect(float before, float after) {
            if (!this.loaded && !this.settled) {
                this.amount = credit(before, after);
                this.loaded = true;
            }
        }

        public float deliver(boolean actuallyReturned) {
            if (this.settled) {
                return 0;
            }
            this.settled = true;
            return actuallyReturned ? this.amount : 0;
        }

        public boolean glowing() {
            return this.amount > 0 && !this.settled;
        }
    }

    public enum Stage { GATHER, SEEK, FEED, RETURN, DISSOLVE, HOME }
}
