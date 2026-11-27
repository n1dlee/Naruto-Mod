package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class ScalpelEdge {
    public static final int FORM = 4;
    public static final int RECOVER = 9;
    public static final float UPKEEP = 0.5f;
    public static final float CUT_COST = 4f;
    private static final double REACH = 2.35;

    private ScalpelEdge() {
    }

    public static @Nullable Vec3 contact(Vec3 hand, Vec3 direction, AABB target) {
        Vec3 tip = hand.add(direction.scale(REACH));
        AABB contactBox = target.inflate(0.13);
        if (contactBox.contains(hand)) {
            return WaterDragonFlight.nearestPoint(target, hand.add(direction.scale(0.09)));
        }
        return contactBox.clip(hand, tip).map(point -> WaterDragonFlight.nearestPoint(target, point)).orElse(null);
    }

    public static boolean ready(long age, long now, long recoverUntil) {
        return age >= FORM && now >= recoverUntil;
    }

    public static boolean sameStrike(long recordedTick, long now, boolean sameTarget) {
        return recordedTick == now && sameTarget;
    }

    public static float bonus(float rank, boolean pvp) {
        return Mth.clamp(rank * 2.5f, 0.9f, pvp ? 3 : 4.5f);
    }

    public enum Phase { FORM, READY, CUT, RELEASE }
}
