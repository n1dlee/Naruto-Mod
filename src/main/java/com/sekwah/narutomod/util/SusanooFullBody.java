package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SusanooFullBody {
    public static final double STANDING_SCALE = .5;
    public static final double STANDING_EYE_HEIGHT = PerfectSusanooPose.EYE_HEIGHT * STANDING_SCALE;
    public static final double STANDING_CAMERA_BACK = 8;
    public static final double PERFECT_CAMERA_BACK = 17;

    private SusanooFullBody() {
    }

    public static boolean standing(int id) {
        return id == SusanooForm.STANDING.id();
    }

    public static boolean hasLegs(int id) {
        return standing(id) || SusanooForm.perfect(id);
    }

    public static double scale(int id) {
        return standing(id) ? STANDING_SCALE : 1;
    }

    public static double rise(double vanillaY, double feetY, double weight) {
        return Math.max(0, feetY + STANDING_EYE_HEIGHT - vanillaY) * Mth.clamp(weight, 0, 1);
    }

    public static double seatShift(double normalEye) {
        return STANDING_EYE_HEIGHT - normalEye;
    }

    public record Space(Vec3 root, double scale) {
        public Vec3 toWorld(Vec3 rigPoint) {
            return scale == 1 ? rigPoint : root.add(rigPoint.subtract(root).scale(scale));
        }

        public Vec3 toRig(Vec3 worldPoint) {
            return scale == 1 ? worldPoint : root.add(worldPoint.subtract(root).scale(1 / scale));
        }

        public SusanooPose.Arm toRig(SusanooPose.Arm arm) {
            return scale == 1 ? arm : new SusanooPose.Arm(toRig(arm.shoulder()), toRig(arm.elbow()), toRig(arm.hand()));
        }

        public SusanooPose.Arm toWorld(SusanooPose.Arm arm) {
            return scale == 1 ? arm : new SusanooPose.Arm(toWorld(arm.shoulder()), toWorld(arm.elbow()), toWorld(arm.hand()));
        }
    }
}
