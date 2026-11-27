package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

public final class SpiritProjectionPath {
    public static final int OPEN = 6;
    public static final int MAX_AGE = 112;
    public static final int RETURN = 7;
    public static final double RANGE = 16;
    private static final double SPEED = 0.42;

    private SpiritProjectionPath() {
    }

    public static Vec3 step(Vec3 origin, Vec3 from, Input command, Predicate<Vec3> loaded) {
        Vec3 forward = KamuiStep.facing(command.yaw());
        Vec3 left = new Vec3(forward.z, 0, -forward.x);
        Vec3 movement = forward.scale(command.forward()).add(left.scale(command.strafe())).add(0, command.vertical(), 0);
        Vec3 delta = movement.lengthSqr() <= 1 ? movement.scale(SPEED) : movement.normalize().scale(SPEED);
        Vec3 offset = from.add(delta).subtract(origin);
        Vec3 next = origin.add(offset.lengthSqr() <= RANGE * RANGE ? offset : offset.normalize().scale(RANGE));
        return loaded.test(next) ? next : from;
    }

    public static boolean keepsBody(Vec3 origin, Vec3 body, int age) {
        return age < MAX_AGE && origin.distanceToSqr(body) <= 0.39 * 0.39;
    }

    public record Input(float yaw, byte strafe, byte forward, byte vertical) {
        public Input {
            yaw = Float.isFinite(yaw) ? Mth.wrapDegrees(yaw) : 0;
            strafe = (byte) Mth.clamp(strafe, -1, 1);
            forward = (byte) Mth.clamp(forward, -1, 1);
            vertical = (byte) Mth.clamp(vertical, -1, 1);
        }
    }
}
