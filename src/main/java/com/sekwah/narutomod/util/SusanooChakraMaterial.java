package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SusanooChakraMaterial {
    private SusanooChakraMaterial() {
    }

    public static double rim(Vec3 normal, Vec3 view) {
        if (normal.lengthSqr() < 1e-8 || view.lengthSqr() < 1e-8) return 0;
        double incidence = Mth.clamp(Math.abs(normal.dot(view)) / Math.sqrt(normal.lengthSqr() * view.lengthSqr()), 0, 1);
        double fresnel = 1 - incidence;
        return fresnel * fresnel * (.32 + .68 * fresnel);
    }

    public static double vein(double ticks, double rootToTip, int seed) {
        double wave = .5 + .5 * Math.cos(rootToTip * 12.5 - ticks * .29 + seed * .67);
        return .2 + .8 * wave * wave * wave;
    }
}
