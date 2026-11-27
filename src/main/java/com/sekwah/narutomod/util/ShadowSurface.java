package com.sekwah.narutomod.util;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public final class ShadowSurface {
    public static final int WINDUP = 5;
    public static final int MAX_POINTS = 37;
    public static final int MOB_TIME = 100;
    public static final int PLAYER_TIME = 22;

    private ShadowSurface() {
    }

    public static List<Vec3> path(Vec3 from, Vec3 to, double bend, BiFunction<Vec3, Vec3, Vec3> floor) {
        Vec3 aim = new Vec3(to.x - from.x, 0, to.z - from.z);
        Vec3 side = new Vec3(aim.z, 0, -aim.x).normalize();
        int steps = Math.max(1, Math.min(MAX_POINTS - 1, (int) Math.ceil(aim.length() / 0.54)));
        List<Vec3> result = new ArrayList<>(steps + 1);
        Vec3 previous = floor.apply(from, from);
        if (previous == null) {
            return List.of();
        }
        result.add(previous);
        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 flat = from.add(aim.scale(t)).add(side.scale(Math.sin(Math.PI * t) * bend));
            Vec3 next = floor.apply(previous, flat);
            if (next == null || !stepHeight(previous, next)) {
                break;
            }
            result.add(next);
            previous = next;
        }
        return List.copyOf(result);
    }

    public static boolean reaches(List<Vec3> path, Vec3 feet) {
        return path.size() >= 2 && path.get(path.size() - 1).distanceToSqr(feet) < 0.39 * 0.39;
    }

    public static boolean stepHeight(Vec3 previous, Vec3 next) {
        double rise = next.y - previous.y;
        return rise <= 1.04 && rise >= -1.12;
    }

    public static Vec3 mirror(Vec3 original, Vec3 actorStep, boolean player) {
        Vec3 planar = new Vec3(actorStep.x, 0, actorStep.z);
        double cap = player ? 0.096 : 0.19;
        Vec3 pull = planar.lengthSqr() <= cap * cap ? planar : planar.normalize().scale(cap);
        double hold = player ? 0.62 : 0.12;
        return new Vec3(original.x * hold + pull.x, original.y, original.z * hold + pull.z);
    }

}
