package com.sekwah.narutomod.util;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class AmaterasuFlames {
    public static final int FOCUS = 4;
    public static final int BURN = 200;
    public static final int FADE = 8;
    public static final int PULSE = 12;
    public static final int MAX_CARRIERS = 6;
    public static final double RANGE = 32;

    private AmaterasuFlames() {
    }

    public static boolean ownedBy(UUID owner, UUID viewer) {
        return owner != null && owner.equals(viewer);
    }

    public static long expiry(long now, boolean carrier) {
        return now + (carrier ? BURN : BURN + 24);
    }

    public static float power(int rank) {
        return 1 + Math.max(0, Math.min(4, rank - 1)) * 0.33f;
    }

    public static AABB patch(Vec3 origin, Direction face) {
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 pad = new Vec3(0.59 * (1 - Math.abs(normal.x)) + 0.12,
                0.59 * (1 - Math.abs(normal.y)) + 0.12,
                0.59 * (1 - Math.abs(normal.z)) + 0.12);
        return new AABB(origin.subtract(pad), origin.add(pad)).expandTowards(normal.scale(0.47));
    }
}
