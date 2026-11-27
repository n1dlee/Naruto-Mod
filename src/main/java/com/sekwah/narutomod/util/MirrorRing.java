package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class MirrorRing {
    public static final int SECTIONS = 8;
    public static final double RADIUS = 4.0;
    public static final int RISE_TICKS = 8;
    public static final int STAGGER_TICKS = 5;
    public static final double WIDTH = 2.0 * RADIUS * Math.tan(Math.PI / SECTIONS) + 0.04;

    private MirrorRing() {
    }

    public static Vec3 outward(int slot) {
        double angle = Math.PI * 2.0 * slot / SECTIONS;
        return new Vec3(Math.cos(angle), 0, Math.sin(angle));
    }

    public static int delay(int slot) {
        return slot * STAGGER_TICKS / (SECTIONS - 1);
    }

    public static float rise(double age) {
        float t = (float) Mth.clamp(age / RISE_TICKS, 0.0, 1.0);
        return t * t * (3.0f - 2.0f * t);
    }

    @Nullable
    public static Vec3 clipOutward(Vec3 before, Vec3 after, Vec3 pane, Vec3 normal, double padding, double height) {
        double wall = -padding;
        double start = before.subtract(pane).dot(normal);
        double end = after.subtract(pane).dot(normal);
        if (start > wall || end <= wall) {
            return null;
        }
        Vec3 contact = before.lerp(after, (wall - start) / (end - start));
        Vec3 side = new Vec3(-normal.z, 0, normal.x);
        double bearing = contact.subtract(pane).dot(side);
        if (Math.abs(bearing) > WIDTH * 0.5 + padding || contact.y < pane.y - 0.12 || contact.y > pane.y + height) {
            return null;
        }
        return contact.subtract(normal.scale(0.025));
    }
}
