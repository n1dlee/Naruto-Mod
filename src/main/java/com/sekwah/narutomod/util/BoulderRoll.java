package com.sekwah.narutomod.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

public final class BoulderRoll {
    public static final int WINDUP = 5;
    public static final int RECOVERY = 6;
    private static final double[] STEPS = {0.42, 0.67, 0.88, 1.04, 1.10, 1.05, 0.96, 0.77, 0.57, 0.34};
    private static final Map<Player, Float> SERVER = new WeakHashMap<>();
    private static final Map<Player, Float> CLIENT = new WeakHashMap<>();

    private BoulderRoll() {
    }

    public static float radius(boolean large) { return large ? 0.83f : 0.56f; }

    public static int segments() { return STEPS.length; }

    public static double step(int index) { return STEPS[index]; }

    public static double rotation(double distance, double radius) { return distance / radius; }

    public static @Nullable Vec3 contact(Vec3 from, Vec3 to, AABB box, double radius) {
        var swept = WaterDragonFlight.sweep(from, to, box, radius);
        if (swept == null) {
            return null;
        }
        int samples = Math.max(1, (int) Math.ceil(from.distanceTo(to) / 0.07));
        for (int i = 0; i <= samples; i++) {
            double t = swept.fraction() + i * (1 - swept.fraction()) / samples;
            Vec3 center = from.lerp(to, t);
            Vec3 contact = WaterDragonFlight.nearestPoint(box, center);
            if (contact.distanceToSqr(center) <= radius * radius + 0.0001) {
                return contact;
            }
        }
        return null;
    }

    public static float body(Player player) {
        return player.level() == null ? 0 : side(player).getOrDefault(player, 0f);
    }

    public static void apply(Player player, float radius) {
        if (body(player) == radius) {
            return;
        }
        if (radius > 0) {
            side(player).put(player, radius);
        } else {
            side(player).remove(player);
        }
        player.refreshDimensions();
    }

    private static Map<Player, Float> side(Player player) {
        return player.level().isClientSide ? CLIENT : SERVER;
    }

    public enum Stage { TUCK, ROLL, BUMP, SETTLE, HOLD }
}
