package com.sekwah.narutomod.util;

import net.minecraft.world.phys.Vec3;

public final class AnimalPathDrive {
    public static final int BIRTH = 11;
    public static final int BRACE = 8;
    public static final int RAM = 8;
    public static final int REST = 21;
    public static final int FADE = 9;
    public static final int LIFE = 950;
    public static final float WIDTH = 2.25f;
    public static final float HEIGHT = 2.38f;
    public static final double MIN_CHARGE = 0.78;
    public static final double MAX_CHARGE = 8.1;

    private AnimalPathDrive() {
    }

    public static double tempo(int tick) {
        return tick < 2 ? 0.45 + tick * 0.14 : tick < 6 ? 0.79 : 0.58;
    }

    public static boolean canBrace(Vec3 offset, boolean visible) {
        double length = offset.horizontalDistanceSqr();
        return visible && Math.abs(offset.y) <= 1.8 && length >= MIN_CHARGE * MIN_CHARGE && length <= MAX_CHARGE * MAX_CHARGE;
    }

    public static boolean liveBond(boolean ownerPresent, boolean sameWorld, long now, long end) {
        return ownerPresent && sameWorld && now < end;
    }

    public enum Stage { BIRTH, PROWL, BRACE, RAM, REST, FADE }
}
