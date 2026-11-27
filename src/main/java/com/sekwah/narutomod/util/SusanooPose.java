package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SusanooPose {
    public static final int WINDUP = 7;
    public static final int SPACING = 2;
    public static final int SHOTS = 3;
    public static final int THROW_LENGTH = 17;

    private SusanooPose() {
    }

    public static int releaseTick(int shot) {
        return WINDUP + shot * SPACING;
    }

    public static float scale(int stage) {
        return 1.2f + SusanooForm.fromId(stage).combatTier() * 0.9f;
    }

    public static Frame frame(Vec3 feet, float yaw) {
        Vec3 forward = KamuiStep.facing(yaw);
        return new Frame(feet, forward, new Vec3(-forward.z, 0, forward.x));
    }

    public static double torsoScale(int stage) {
        return SusanooForm.fromId(stage).torsoScale();
    }

    public static Arm rest(Frame frame, int stage, int side, double movement) {
        if (SusanooFullBody.hasLegs(stage)) {
            return PerfectSusanooPose.restArm(frame, side, Vec3.ZERO, movement, stage);
        }
        double scale = torsoScale(stage);
        return new Arm(frame.at(side * 1.04 * scale, 2.88 * scale, -.23),
                frame.at(side * 1.43 * scale, 2.02 * scale, movement),
                frame.at(side * 1.39 * scale, 1.15 * scale, .32 + movement));
    }

    public static Arm throwArm(Frame frame, int stage, Vec3 aim, double age) {
        if (SusanooFullBody.hasLegs(stage)) {
            var space = new SusanooFullBody.Space(frame.feet, SusanooFullBody.scale(stage));
            return space.toWorld(PerfectSusanooPose.throwArm(frame, aim, age));
        }
        Arm rest = rest(frame, stage, 1, 0);
        double scale = torsoScale(stage);
        double shape = smooth(age / WINDUP) * (1 - smooth((age - releaseTick(SHOTS - 1)) / 6));
        Vec3 axis = aim.normalize();
        Vec3 elbow = frame.at(1.54 * scale, 2.45 * scale + .56 * axis.y, .24);
        double wristY = 2.68 * scale + .92 * axis.y;
        Vec3 held = frame.at(1.26 * scale, wristY, .38);
        Vec3 sent = frame.at(.91 * scale, wristY, .70).add(axis.scale(.67));
        double thrust = 0;
        for (int i = 0; i < SHOTS; i++) {
            thrust = Math.max(thrust, 1 - Math.abs(age - releaseTick(i)) / 1.5);
        }
        return new Arm(rest.shoulder, rest.elbow.lerp(elbow, shape),
                rest.hand.lerp(held.lerp(sent, smooth(thrust)), shape));
    }

    public static Arm cutArm(Frame frame, Vec3 bladeOrigin, int stage, double age) {
        Arm rest = rest(frame, stage, 1, 0);
        var blade = SpectralSword.sample(bladeOrigin, frame.yaw(), stage, age);
        double weight = smooth(age / SpectralSword.CUT_START)
                * (1 - smooth((age - SpectralSword.CUT_END) / (SpectralSword.LENGTH - SpectralSword.CUT_END)));
        Vec3 hand = rest.hand.lerp(blade.guard(), weight);
        Vec3 bent = rest.shoulder.lerp(hand, .53).add(frame.right.scale(.66)).add(frame.forward.scale(.18)).add(0, .09, 0);
        return new Arm(rest.shoulder, rest.elbow.lerp(bent, weight), hand);
    }

    public static Vec3 releasePoint(Frame frame, int stage, Vec3 aim, int shot) {
        if (SusanooForm.fromId(stage).combatTier() >= 2) {
            return throwArm(frame, stage, aim, releaseTick(shot)).hand();
        }
        return frame.at(.67, 1.9, 1.35);
    }

    public static double smooth(double value) {
        double clamped = Mth.clamp(value, 0, 1);
        return clamped * clamped * (3 - 2 * clamped);
    }

    public record Arm(Vec3 shoulder, Vec3 elbow, Vec3 hand) {
    }

    public record Frame(Vec3 feet, Vec3 forward, Vec3 right) {
        public Vec3 at(double x, double y, double z) {
            return feet.add(right.scale(x)).add(0, y, 0).add(forward.scale(z));
        }

        public float yaw() {
            return (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
        }
    }
}
