package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class GunbaiMotion {
    public static final int GUARD = 7;
    public static final double SWEEP_RANGE = 10.2;
    public static final double SIGNATURE_RANGE = 12.2;
    private static final double GUARD_WIDTH = 0.57;
    private static final double GUARD_HEIGHT = 0.70;

    private GunbaiMotion() {
    }

    public static Vec3 guardCenter(Player player, InteractionHand hand) {
        Vec3 raw = fanOrigin(player, hand, 0.43);
        Vec3 forward = player.getLookAngle();
        return raw.add(forward.scale(0.18)).add(0, 0.22, 0);
    }

    public static Vec3 fanOrigin(LivingEntity actor, InteractionHand hand, double forwardOffset) {
        Vec3 forward = actor.getLookAngle();
        if (actor instanceof Player player) {
            boolean dominant = hand == InteractionHand.MAIN_HAND;
            double side = dominant == (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT) ? 1 : -1;
            return JutsuVfx.handPosition(player, side, 1).add(forward.scale(forwardOffset));
        }
        Vec3 side = new Vec3(-forward.z, 0, forward.x).normalize();
        return actor.position().add(0, actor.getBbHeight() * 0.63, 0).add(side.scale(0.41)).add(forward.scale(forwardOffset));
    }

    @Nullable
    public static Vec3 intercept(Vec3 center, Vec3 normal, Vec3 start, Vec3 end) {
        Vec3 motion = end.subtract(start);
        double incidence = motion.dot(normal);
        double ahead = start.subtract(center).dot(normal);
        if (incidence >= -0.08 || ahead < -0.005) {
            return null;
        }
        double t = ahead / -incidence;
        if (t < 0 || t > 1) {
            return null;
        }
        Vec3 contact = start.add(motion.scale(t));
        GustFront.Frame frame = GustFront.frame(normal);
        Vec3 offset = contact.subtract(center);
        return Math.abs(offset.dot(frame.right())) <= GUARD_WIDTH && Math.abs(offset.dot(frame.up())) <= GUARD_HEIGHT
                ? contact : null;
    }

    public static Vec3 bounce(Vec3 incoming, Vec3 normal) {
        Vec3 reflected = incoming.subtract(normal.scale(2 * incoming.dot(normal)));
        return reflected.normalize().scale(Mth.clamp(incoming.length() * 0.91, 0.26, 2.9));
    }
}
