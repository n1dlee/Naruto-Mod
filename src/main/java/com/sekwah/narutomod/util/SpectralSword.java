package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SpectralSword {
    public static final int CUT_START = 6;
    public static final int CUT_END = 11;
    public static final int LENGTH = 18;

    private SpectralSword() {
    }

    public static float formation(float ticks) {
        float value = Mth.clamp(ticks / 12f, 0, 1);
        return value * value * (3 - 2 * value);
    }

    public static boolean active(int tick) {
        return active(2, tick);
    }

    public static boolean active(int stage, int tick) {
        return tick >= startTick(stage) && tick <= endTick(stage);
    }

    public static int startTick(int stage) {
        return stage == 4 ? 10 : CUT_START;
    }

    public static int endTick(int stage) {
        return stage == 4 ? 16 : CUT_END;
    }

    public static int length(int stage) {
        return stage == 4 ? 26 : LENGTH;
    }

    public static Blade sample(Vec3 feet, float yaw, int stage, double tick) {
        if (stage == 4) {
            return perfect(SusanooPose.frame(feet, yaw), tick);
        }
        double t = Mth.clamp((tick - CUT_START) / (CUT_END - CUT_START), 0, 1);
        Vec3 axis = KamuiStep.facing(yaw);
        Vec3 side = new Vec3(-axis.z, 0, axis.x);
        boolean standing = SusanooForm.fromId(stage) == SusanooForm.STANDING;
        Vec3 guard = feet.add(side.scale(.65)).add(axis.scale(1.1)).add(0, standing ? 3.8 - 1.3 * t : 2.2 - t, 0);
        Vec3 span = axis.yRot((float) Math.toRadians(Mth.lerp(t, -69, 64)));
        Vec3 tip = guard.add(span.scale(3.8 + SusanooForm.fromId(stage).combatTier() * .65))
                .add(0, standing ? .55 - 4.8 * t : 1.6 - 2.1 * t, 0);
        return new Blade(guard, tip);
    }

    private static Blade perfect(SusanooPose.Frame frame, double tick) {
        double t = Mth.clamp((tick - startTick(4)) / (endTick(4) - startTick(4)), 0, 1);
        double drop = SusanooPose.smooth(t / .68);
        Vec3 guard = frame.at(Mth.lerp(t, 3.55, .86), Mth.lerp(drop, 11.4, 4.55), Mth.lerp(drop, 1.8, 4.12));
        Vec3 span = frame.forward().yRot((float) Math.toRadians(Mth.lerp(t, -61, 68)));
        Vec3 tip = guard.add(span.scale(16.5)).add(0, Mth.lerp(drop, 3.1, -9.0), 0);
        return new Blade(guard, tip);
    }

    public record Blade(Vec3 guard, Vec3 tip) {
        public Vec3 contact(AABB box, double width) {
            AABB fat = box.inflate(width);
            return fat.contains(guard) ? guard : fat.clip(guard, tip).orElse(null);
        }
    }
}
