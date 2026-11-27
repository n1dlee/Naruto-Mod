package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.function.UnaryOperator;

public final class PerfectSusanooPose {
    public static final double EYE_HEIGHT = GiantForm.HEIGHT_BLOCKS * .85F;
    public static final double FOOT_WIDTH = 1.55;

    private PerfectSusanooPose() {
    }

    public static SusanooPose.Arm restArm(SusanooPose.Frame frame, int side, Vec3 shift, double swing) {
        return new SusanooPose.Arm(frame.at(side * 3.08, 12.68, -.25).add(shift),
                frame.at(side * 3.72, 10.05, .11 + swing * .54).add(shift),
                frame.at(side * 3.48, 7.65, .68 + swing).add(shift));
    }

    public static SusanooPose.Arm restArm(SusanooPose.Frame frame, int side, Vec3 shift, double swing, int stage) {
        double scale = SusanooFullBody.scale(stage);
        var space = new SusanooFullBody.Space(frame.feet(), scale);
        return space.toWorld(restArm(frame, side, shift.scale(1 / scale), swing / scale));
    }

    public static SusanooPose.Arm throwArm(SusanooPose.Frame frame, Vec3 aim, double age) {
        var rest = restArm(frame, 1, Vec3.ZERO, 0);
        Vec3 axis = aim.normalize();
        double lift = SusanooPose.smooth(age / SusanooPose.WINDUP)
                * (1 - SusanooPose.smooth((age - SusanooPose.releaseTick(SusanooPose.SHOTS - 1)) / 6));
        double burst = 0;
        for (int i = 0; i < SusanooPose.SHOTS; i++) {
            burst = Math.max(burst, 1 - Math.abs(age - SusanooPose.releaseTick(i)) / 1.5);
        }
        Vec3 loaded = frame.at(3.03, 10.55 + 1.40 * axis.y, 1.84);
        Vec3 release = frame.at(2.32, 10.55 + 1.40 * axis.y, 3.10).add(axis.scale(1.35));
        return new SusanooPose.Arm(rest.shoulder(), rest.elbow().lerp(frame.at(4.15, 11 + axis.y, .52), lift),
                rest.hand().lerp(loaded.lerp(release, SusanooPose.smooth(burst)), lift));
    }

    public static SusanooPose.Arm cutArm(SusanooPose.Frame frame, Vec3 origin, Vec3 shift, double age) {
        return cutArm(frame, origin, shift, age, SusanooForm.PERFECT.id());
    }

    public static SusanooPose.Arm cutArm(SusanooPose.Frame frame, Vec3 origin, Vec3 shift, double age, int stage) {
        var rest = restArm(frame, 1, shift, 0, stage);
        double scale = SusanooFullBody.scale(stage);
        var blade = SpectralSword.sample(origin, frame.yaw(), stage, age);
        double blend = SusanooPose.smooth(age / SpectralSword.startTick(stage))
                * (1 - SusanooPose.smooth((age - SpectralSword.endTick(stage)) / (SpectralSword.length(stage) - SpectralSword.endTick(stage))));
        Vec3 hand = rest.hand().lerp(blade.guard(), blend);
        Vec3 shoulder = rest.shoulder().add(frame.forward().scale(.62 * blend * scale));
        Vec3 elbow = shoulder.lerp(hand, .48).add(frame.right().scale(1.15 * blend * scale)).add(0, -.48 * blend * scale, 0);
        return new SusanooPose.Arm(shoulder, rest.elbow().lerp(elbow, blend), hand);
    }

    public static Vec3 hipShift(SusanooPose.Frame frame, Stance stance, double scale) {
        double limit = .55 * scale;
        double forward = Mth.clamp(stance.left().add(stance.right()).scale(.5).subtract(frame.feet()).dot(frame.forward()), -limit, limit);
        double lateral = stance.swingSide() == 0 ? 0 : -.28 * stance.swingSide() * stance.stride() * scale;
        return frame.right().scale(lateral).add(frame.forward().scale(forward * stance.stride()))
                .add(0, -.24 * stance.stride() * scale, 0);
    }

    public static Vec3 knee(SusanooPose.Frame frame, Vec3 hip, Vec3 foot) {
        Vec3 ankle = foot.add(0, .82, 0);
        double flex = Mth.clamp((7.65 - hip.distanceTo(ankle)) * .7, .16, 1.9);
        return hip.lerp(ankle, .51).add(frame.forward().scale(flex));
    }

    public static double wingOpening(double age, double stride, int side) {
        double unfold = SusanooPose.smooth((age - (side > 0 ? 11 : 9)) / 9);
        return unfold * (1 - .14 * Mth.clamp(stride, 0, 1));
    }

    public record Stance(Vec3 left, Vec3 right, double stride, int swingSide) {
        public Stance interpolate(Stance other, double partial) {
            return new Stance(left.lerp(other.left, partial), right.lerp(other.right, partial),
                    Mth.lerp(partial, stride, other.stride), other.swingSide);
        }
    }

    public static final class Gait {
        private final double scale;
        private Vec3 left;
        private Vec3 right;
        private Vec3 footFrom;
        private Vec3 footTo;
        private double progress;
        private double stride;
        private int moving;
        private int next = -1;

        public Gait() {
            this(1);
        }

        public Gait(double scale) {
            this.scale = scale;
        }

        public Stance reset(SusanooPose.Frame frame, UnaryOperator<Vec3> floor) {
            left = floor.apply(foot(frame, -1));
            right = floor.apply(foot(frame, 1));
            progress = stride = 0;
            moving = 0;
            next = -1;
            return stance();
        }

        public Stance update(SusanooPose.Frame frame, Vec3 travel, UnaryOperator<Vec3> floor) {
            double speed = travel.horizontalDistance();
            stride = Mth.lerp(.28, stride, Mth.clamp(speed * 3.8, 0, 1));
            if (moving == 0) {
                Vec3 nominal = foot(frame, next);
                if (speed > .026 || (next < 0 ? left : right).distanceToSqr(nominal) > 1.8 * scale * scale) {
                    beginStep(frame, travel, speed, floor);
                } else {
                    next = -next;
                }
            }
            if (moving != 0) {
                advance(speed);
            }
            return stance();
        }

        private void beginStep(SusanooPose.Frame frame, Vec3 travel, double speed, UnaryOperator<Vec3> floor) {
            moving = next;
            next = -next;
            progress = 0;
            footFrom = moving < 0 ? left : right;
            Vec3 lead = speed > .026 ? travel.multiply(1, 0, 1).normalize().scale(Mth.clamp(speed * 6.2, .38 * scale, 1.9 * scale)) : Vec3.ZERO;
            footTo = floor.apply(foot(frame, moving).add(lead));
        }

        private Vec3 foot(SusanooPose.Frame frame, int side) {
            return frame.at(side * FOOT_WIDTH * scale, 0, .16 * scale);
        }

        private void advance(double speed) {
            progress = Math.min(1, progress + Mth.clamp(speed / (1.85 * scale), .105, .32));
            double ease = SusanooPose.smooth(progress);
            Vec3 foot = footFrom.lerp(footTo, ease).add(0, Math.sin(Math.PI * ease) * (.27 + .91 * stride) * scale, 0);
            if (moving < 0) {
                left = foot;
            } else {
                right = foot;
            }
            if (progress >= 1) {
                moving = 0;
            }
        }

        private Stance stance() {
            return new Stance(left, right, stride, moving);
        }
    }
}
