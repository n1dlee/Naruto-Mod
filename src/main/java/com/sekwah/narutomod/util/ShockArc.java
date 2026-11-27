package com.sekwah.narutomod.util;

import net.minecraft.world.phys.Vec3;

public final class ShockArc {
    public static final int PREPARE = 3;
    public static final int FADE = 3;
    public static final double RANGE = 6;
    public static final double RADIUS = 0.115;

    private ShockArc() {
    }

    public static Vec3 endpoint(Vec3 from, Vec3 heading) {
        return from.add(heading.normalize().scale(RANGE));
    }

    public static int stunTicks(boolean accepted, boolean player) {
        return accepted ? (player ? 30 : 40) : 0;
    }
}
