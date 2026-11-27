package com.sekwah.narutomod.util;

import com.sekwah.narutomod.entity.TailedBeastVariant;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class BijudamaProfile {
    public static final int WINDUP = 40;
    private static final float MAX_DAMAGE = 76;
    private static final float MAX_RADIUS = 13;

    private BijudamaProfile() {
    }

    public static float radius(TailedBeastVariant variant, int rage) {
        return limitRadius(4 + variant.getTails() * 0.45f + rage * 0.8f);
    }

    public static float limitRadius(float input) {
        return Float.isFinite(input) ? Mth.clamp(input, 2, MAX_RADIUS) : 5;
    }

    public static float damage(int tails, float radius, double distance) {
        if (distance > radius || distance < 0) {
            return 0;
        }
        double raw = 26.0 * TailedBeastVariant.powerMultiplier(tails) * (radius / 5.0);
        double peak = raw * MAX_DAMAGE / (raw + MAX_DAMAGE);
        return (float) (peak * (0.26 + 0.74 * Math.max(0, 1 - distance / radius)));
    }

    public static float core(float radius, float progress) {
        float t = Mth.clamp(progress, 0, 1);
        return radius * 0.21f * (0.10f + 0.90f * t * t);
    }

    public static Vec3 mouth(TailedBeastVariant variant, Vec3 feet, float width, float height, float yaw) {
        Mouth mount = switch (variant) {
            case SHUKAKU -> new Mouth(0.78, 0.57);
            case MATATABI -> new Mouth(0.67, 0.75);
            case ISOBU -> new Mouth(0.42, 0.72);
            case SON_GOKU -> new Mouth(0.76, 0.53);
            case KOKUO -> new Mouth(0.79, 0.77);
            case SAIKEN -> new Mouth(0.79, 0.46);
            case CHOMEI -> new Mouth(0.53, 0.62);
            case GYUKI -> new Mouth(0.80, 0.53);
        };
        return feet.add(0, height * mount.height(), 0).add(KamuiStep.facing(yaw).scale(width * mount.forward() + 0.56));
    }

    private record Mouth(double height, double forward) {
    }
}
