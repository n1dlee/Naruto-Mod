package com.sekwah.narutomod.util;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class IceSpearVolley {
    public static final int COUNT = 6;
    public static final int LIFE = 15;
    public static final int BREAK = 8;
    public static final double RADIUS = 0.105;
    public static final double SPEED = 2.06;
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 X = new Vec3(1, 0, 0);
    private static final int HIT_CAP = 2;
    private final Map<UUID, Integer> contacts = new HashMap<>();

    public static Vec3 side(Vec3 axis) {
        return (Math.abs(axis.y) > 0.9 ? X : UP).cross(axis).normalize();
    }

    public static Lane lane(Vec3 facing, int index) {
        Vec3 axis = ProjectileAxis.direction(facing, new Vec3(0, 0, 1));
        Vec3 right = side(axis);
        Vec3 up = axis.cross(right);
        double slot = index - (COUNT - 1) * 0.5;
        Vec3 start = axis.scale(0.73).add(right.scale(slot * 0.244))
                .add(up.scale((index % 2 == 0 ? 0.085 : -0.058) - 0.13));
        Vec3 aim = axis.add(right.scale(slot * 0.027)).add(up.scale(index % 2 == 0 ? 0.013 : -0.013)).normalize();
        int release = 5 + (int) Math.floor(Math.abs(slot));
        return new Lane(start, aim, release);
    }

    public boolean claim(UUID target) {
        int n = this.contacts.getOrDefault(target, 0);
        if (n >= HIT_CAP) {
            return false;
        }
        this.contacts.put(target, n + 1);
        return true;
    }

    public record Lane(Vec3 offset, Vec3 heading, int release) {
    }
}
