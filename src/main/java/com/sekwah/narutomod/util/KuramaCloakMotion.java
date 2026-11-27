package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class KuramaCloakMotion {
    private KuramaCloakMotion() {
    }

    public enum Strike {
        PAW(7, 14), TAIL(10, 20);

        private final int windup;
        private final int duration;

        Strike(int windup, int duration) {
            this.windup = windup;
            this.duration = duration;
        }

        public int windup() {
            return windup;
        }

        public int duration() {
            return duration;
        }
    }

    public static int desiredTails(float power, int rank) {
        int maximum = rank >= 4 ? 9 : 4;
        return 1 + (int) Math.floor(Mth.clamp(power, 0, 1) * (maximum - 1));
    }

    public static int stageTicks(int next) {
        return next == 9 ? 16 : 12;
    }

    public static float unfold(float ticks, int stage) {
        float t = Mth.clamp(ticks / stageTicks(stage), 0, 1);
        return t * t * (3 - 2 * t);
    }

    public record Blow(Strike kind, Vec3 feet, float yaw, boolean giant) {
        public Vec3 axis() {
            return KamuiStep.facing(yaw);
        }

        public Vec3 origin() {
            return feet.add(axis().scale(kind == Strike.PAW ? (giant ? 1.8 : 0.30) : 0))
                    .add(0, giant ? (kind == Strike.PAW ? 1.65 : 2.5) : 0.92, 0);
        }

        public double reach() {
            return kind == Strike.PAW ? (giant ? 9.0 : 3.2) : (giant ? 26.0 : 7.0);
        }

        public double pawHalfWidth() {
            return giant ? 2.3 : 0.85;
        }

        public double halfAngle() {
            return Math.toRadians(giant ? 80 : 55);
        }

        public boolean covers(AABB target) {
            Vec3 origin = origin();
            double vertical = giant ? 4.5 : 1.7;
            if (target.maxY < origin.y - vertical || target.minY > origin.y + vertical) {
                return false;
            }
            Vec3 center = target.getCenter().subtract(origin).multiply(1, 0, 1);
            double fore = center.dot(axis());
            double halfBody = Math.min(2.2, Math.max(target.getXsize(), target.getZsize()) * 0.5);
            if (kind == Strike.PAW) {
                double side = Math.sqrt(Math.max(0, center.lengthSqr() - fore * fore));
                return fore >= -halfBody && fore <= reach() + halfBody && side <= pawHalfWidth() + halfBody;
            }
            return center.length() <= reach() + halfBody
                    && (center.lengthSqr() < 0.25 || fore >= Math.cos(halfAngle()) * center.length() - halfBody);
        }

        public Vec3 endpoint(double progress) {
            if (kind == Strike.PAW) {
                return origin().add(axis().scale(reach() * progress));
            }
            double angle = (2 * progress - 1) * halfAngle();
            return origin().add(axis().yRot((float) angle).scale(reach() * 0.95));
        }
    }
}
