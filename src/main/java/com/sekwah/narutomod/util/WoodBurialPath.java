package com.sekwah.narutomod.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class WoodBurialPath {
    public static final int STEPS = 6;
    public static final double SPACING = 1.62;
    private static final int[] BEATS = {7, 12, 17, 22, 27, 34};

    private WoodBurialPath() {
    }

    public static int beat(int index) { return BEATS[index]; }

    public static boolean finalBeat(int index) { return index == STEPS - 1; }

    public static @Nullable Route route(Vec3 start, Vec3 axis, Ground ground) {
        var samples = new ArrayList<Vec3>(5);
        samples.add(start);
        Vec3 previous = start;
        for (int i = 1; i <= 4; i++) {
            Vec3 at = start.add(axis.scale(i * SPACING / 4));
            Vec3 next = ground.sample(new Vec3(at.x, previous.y, at.z), previous);
            if (next == null || Math.abs(next.y - previous.y) > 1.001
                    || next.subtract(previous).horizontalDistanceSqr() > 0.64 * 0.64) {
                return null;
            }
            previous = next;
            samples.add(next);
        }
        return new Route(List.copyOf(samples));
    }

    public static boolean swept(Vec3 root, Vec3 tip, AABB target, boolean heavy) {
        double reach = heavy ? 0.46 : 0.35;
        return BoulderRoll.contact(root, tip, target, reach) != null;
    }

    public record Route(List<Vec3> samples) {
        public Vec3 end() { return this.samples.get(this.samples.size() - 1); }
    }

    @FunctionalInterface
    public interface Ground {
        @Nullable Vec3 sample(Vec3 requested, Vec3 previous);
    }
}
