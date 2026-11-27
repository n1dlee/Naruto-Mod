package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public final class BaikaScale {
    public static final float FULL = 2.10f;
    public static final int GROW_TICKS = 12;
    public static final int SHRINK_TICKS = 5;
    private static final Map<Player, Float> SERVER = new WeakHashMap<>();
    private static final Map<Player, Float> CLIENT = new WeakHashMap<>();

    private BaikaScale() {
    }

    public static float growth(int ticks) {
        float t = Mth.clamp(ticks / (float) GROW_TICKS, 0, 1);
        return Mth.lerp(t * t * (3 - 2 * t), 1, FULL);
    }

    public static float shrink(float from, int ticks) {
        return Mth.lerp(Mth.clamp(ticks / (float) SHRINK_TICKS, 0, 1), from, 1);
    }

    public static AABB bounds(EntityDimensions dimensions, float current, float goal, Vec3 feet) {
        float ratio = goal / current;
        return EntityDimensions.scalable(dimensions.width * ratio, dimensions.height * ratio).makeBoundingBox(feet);
    }

    public static float current(Player player) {
        return player.level() == null ? 1 : side(player).getOrDefault(player, 1f);
    }

    public static void apply(Player player, float scale) {
        float value = Mth.clamp(scale, 1, FULL);
        if (Math.abs(current(player) - value) < 0.0001f) {
            return;
        }
        var side = side(player);
        if (value > 1) {
            side.put(player, value);
        } else {
            side.remove(player);
        }
        player.refreshDimensions();
    }

    private static Map<Player, Float> side(Player player) {
        return player.level().isClientSide ? CLIENT : SERVER;
    }
}
