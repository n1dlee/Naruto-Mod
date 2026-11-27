package com.sekwah.narutomod.util;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class GenjutsuGaze {
    public static final int FOCUS_TICKS = 5;
    public static final int MAX_TICKS = 50;
    public static final double RANGE = 10;
    private static final double TARGET_ANGLE = Math.cos(Math.toRadians(33));
    private static final double CAST_DOT = Math.cos(Math.toRadians(10));

    private GenjutsuGaze() {
    }

    public static boolean eyeContact(Vec3 eyes, Vec3 look, Vec3 otherEyes, Vec3 otherLook) {
        return eyeContact(eyes, look, otherEyes, otherLook, RANGE);
    }

    public static boolean eyeContact(Vec3 eyes, Vec3 look, Vec3 otherEyes, Vec3 otherLook, double range) {
        Vec3 between = otherEyes.subtract(eyes);
        double distance = between.lengthSqr();
        if (distance < 0.04 || distance > range * range) {
            return false;
        }
        Vec3 aim = between.scale(1.0 / Math.sqrt(distance));
        return aim.dot(look) >= CAST_DOT && aim.dot(otherLook) <= -TARGET_ANGLE;
    }

    public static boolean eyesMeet(LivingEntity actor, LivingEntity target, double range) {
        if (target.isRemoved() || !canSee(actor) || !canSee(target) || target.level() != actor.level()
                || actor.isAlliedTo(target) || Faction.sameSide(actor, target)) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || actor.isInvisibleTo(player)
                || actor instanceof Player caster && !caster.canHarmPlayer(player))) {
            return false;
        }
        if (actor instanceof Player player && target.isInvisibleTo(player)) {
            return false;
        }
        return eyeContact(actor.getEyePosition(), direction(actor), target.getEyePosition(), direction(target), range)
                && actor.hasLineOfSight(target) && target.hasLineOfSight(actor);
    }

    private static boolean canSee(LivingEntity entity) {
        return entity.isAlive() && !entity.isSpectator() && !entity.isSleeping() && !entity.hasEffect(MobEffects.BLINDNESS);
    }

    private static Vec3 direction(LivingEntity actor) {
        return actor instanceof Player ? actor.getLookAngle() : Vec3.directionFromRotation(actor.getXRot(), actor.getYHeadRot());
    }

    public static int duration(int activeTomoe) {
        return switch (activeTomoe) {
            case 0 -> MAX_TICKS;
            case 1 -> 30;
            case 2 -> 17;
            default -> 0;
        };
    }

    public static float distortion(float age, int duration, double accessibility) {
        if (age < 0 || duration <= 0 || age >= duration) {
            return 0;
        }
        double fade = Math.min(1, Math.min((age + 1) / 4, (duration - age) / 7));
        return (float) (fade * Math.max(0, Math.min(1, accessibility)));
    }
}
