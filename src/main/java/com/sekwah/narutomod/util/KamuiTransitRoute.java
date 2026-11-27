package com.sekwah.narutomod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Predicate;

public final class KamuiTransitRoute {
    public static final int WINDUP = 10;
    public static final int EXIT_WINDUP = 8;
    public static final int MOB_STAY = 160;
    public static final int PLATFORM_Y = 64;

    private KamuiTransitRoute() {
    }

    public static BlockPos island(UUID identity) {
        long high = identity.getMostSignificantBits();
        long low = identity.getLeastSignificantBits();
        return new BlockPos((Math.floorMod(high ^ Long.rotateLeft(low, 15), 251) - 125) * 96, PLATFORM_Y,
                (Math.floorMod(low ^ Long.rotateLeft(high, 21), 263) - 131) * 96);
    }

    public static boolean maintains(Vec3 started, Vec3 current, double radius) {
        return started.distanceToSqr(current) <= radius * radius;
    }

    @Nullable
    public static Vec3 landing(Vec3 wanted, Predicate<Vec3> accepts) {
        if (accepts.test(wanted)) {
            return wanted;
        }
        for (int distance = 0; distance <= 4; distance++) {
            for (int dy : new int[] {0, 1, -1, 2, -2, 3, -3, 4}) {
                for (int i = 0; i < (distance == 0 ? 1 : 8); i++) {
                    double angle = i * Math.PI / 4;
                    Vec3 alternative = wanted.add(Math.cos(angle) * distance, dy, Math.sin(angle) * distance);
                    if (!alternative.equals(wanted) && accepts.test(alternative)) {
                        return alternative;
                    }
                }
            }
        }
        return null;
    }
}
