package com.sekwah.narutomod.client.renderer;

import com.sekwah.narutomod.client.renderer.SusanooShellArmor.Plate;
import com.sekwah.narutomod.client.renderer.SusanooShellRenderer.Mesh;
import com.sekwah.narutomod.client.renderer.SusanooShellRenderer.Shell;
import com.sekwah.narutomod.util.GiantForm;
import com.sekwah.narutomod.util.PerfectSusanooPose;
import com.sekwah.narutomod.util.SusanooChakraMaterial;
import com.sekwah.narutomod.util.SusanooForm;
import com.sekwah.narutomod.util.SusanooPose;
import net.minecraft.world.phys.Vec3;

import static com.sekwah.narutomod.client.renderer.SusanooShellArmor.plate;

final class PerfectSusanooRenderer {
    private PerfectSusanooRenderer() {
    }

    static void draw(Mesh mesh, Shell shell, boolean low) {
        var motion = shell.fullBody();
        var space = motion.space();
        var skin = mesh.scaled(space);
        var rig = new Rig(shell, motion.hip().scale(1 / space.scale()));
        if (!shell.ownView()) {
            leg(skin, rig, -1, space.toRig(motion.stance().left()), low);
            leg(skin, rig, 1, space.toRig(motion.stance().right()), low);
            core(skin, rig, low);
            helmet(skin, rig);
            if (SusanooForm.perfect(shell.form().stage())) {
                wings(skin, rig, motion.stance().stride(), low);
            }
            var left = PerfectSusanooPose.restArm(shell.frame(), -1, rig.shift(), -motion.armSwing() / space.scale());
            arm(skin, rig, left, -1, false, low);
        }
        if (!shell.ownView() || shell.cut() != null || shell.toss() != null) {
            var right = shell.rightArm();
            arm(skin, rig, space.toRig(right), 1, shell.toss() != null, low);
            if (shell.cut() != null) {
                SusanooShellArmor.sword(mesh, shell, right.hand());
            }
        }
    }

    private static void leg(Mesh mesh, Rig rig, int side, Vec3 planted, boolean low) {
        Vec3 hip = rig.at(side * 1.49, 8.38, -.22);
        Vec3 knee = PerfectSusanooPose.knee(rig.frame(), hip, planted);
        Vec3 ankle = planted.add(0, .82, 0);
        double light = rig.phase(0, 6);
        mesh.bone(hip, knee, 1.11, rig.tint(), light);
        mesh.bone(knee, ankle, .98, rig.tint(), light);
        var shield = new Plate(rig.tint(), light, .21);
        Vec3 across = rig.frame().right().scale(.78);
        Vec3 ahead = rig.frame().forward();
        Vec3 kneecap = knee.add(ahead.scale(.8));
        plate(mesh, kneecap.subtract(across).add(0, .85, 0), kneecap.add(across).add(0, .85, 0),
                kneecap.add(across.scale(.68)).add(0, -.57, 0), kneecap.subtract(across.scale(.68)).add(0, -.57, 0), shield);
        Vec3 toe = planted.add(ahead.scale(1.22)).add(0, .23, 0);
        Vec3 heel = planted.subtract(ahead.scale(.64)).add(0, .45, 0);
        plate(mesh, heel.subtract(across), heel.add(across), toe.add(across.scale(1.12)), toe.subtract(across.scale(1.12)), shield);
        Vec3 toeFloor = toe.add(0, -.23, 0);
        plate(mesh, toe.subtract(across.scale(1.12)), toe.add(across.scale(1.12)), toeFloor.add(across), toeFloor.subtract(across), shield);
        if (!low) {
            mesh.bone(ankle.add(ahead.scale(.65)), kneecap, .23, rig.tint(), light);
            mesh.bone(toe.subtract(across.scale(.56)), toe.add(across.scale(.56)), .10, rig.tint(), light);
        }
    }

    private static void core(Mesh mesh, Rig rig, boolean low) {
        double lock = SusanooPose.smooth((rig.age() - 4) / 8);
        mesh.bone(rig.at(0, 7.1, -.26), rig.at(0, 13.68, -.43), 1.46, rig.tint(), rig.phase(2, 7));
        for (int side = -1; side <= 1; side += 2) {
            double recoil = 1.6 * (1 - lock) * side;
            var chest = new Plate(rig.tint(), rig.phase(4, 8), .31);
            plate(mesh, rig.at(.33 * side + recoil, 13.72, 1.58), rig.at(2.96 * side + recoil, 12.89, .79),
                    rig.at(2.13 * side + recoil, 10.42, 1.15), rig.at(.31 * side + recoil, 9.73, 1.75), chest);
            plate(mesh, rig.at(2.96 * side + recoil, 12.89, .79), rig.at(2.93 * side + recoil, 12.97, -1.21),
                    rig.at(1.88 * side + recoil, 10.35, -1.12), rig.at(2.13 * side + recoil, 10.42, 1.15), chest);
            skirt(mesh, rig, side, low);
        }
        var back = new Plate(rig.tint(), rig.phase(5, 7), .26);
        plate(mesh, rig.at(-2.84, 12.92, -1.24), rig.at(2.84, 12.92, -1.24),
                rig.at(1.84, 10.35, -1.14), rig.at(-1.84, 10.35, -1.14), back);
        mesh.bone(rig.at(0, 13.38, 1.82), rig.at(0, 10.39, 1.89), .29, rig.tint(), rig.phase(7, 6));
    }

    private static void skirt(Mesh mesh, Rig rig, int side, boolean low) {
        for (int i = 0; i < (low ? 2 : 3); i++) {
            double y = 9.58 - .65 * i;
            double x = 1.87 + i * .29;
            double z = 1.06 + i * .12;
            var tile = new Plate(rig.tint(), rig.phase(6 + 2 * i, 7), .18);
            plate(mesh, rig.at(.33 * side, y, z + .45), rig.at(x * side, y + .36, z - .48),
                    rig.at((x + .33) * side, y - .45, z - .43), rig.at(.44 * side, y - .57, z + .55), tile);
        }
    }

    private static void helmet(Mesh mesh, Rig rig) {
        double top = GiantForm.HEIGHT_BLOCKS;
        double rise = SusanooPose.smooth((rig.age() - 7) / 8);
        double lower = (1 - rise) * .81;
        var crown = new Plate(rig.tint(), rig.phase(7, 8), .23);
        double brow = PerfectSusanooPose.EYE_HEIGHT + .78 - lower;
        double crest = top - .52 - lower;
        plate(mesh, rig.at(-1.18, brow, .83), rig.at(1.18, brow, .83), rig.at(.76, crest, .17), rig.at(-.76, crest, .17), crown);
        plate(mesh, rig.at(-.76, crest, .17), rig.at(.76, crest, .17), rig.at(.78, crest - .28, -1.34),
                rig.at(-.78, crest - .28, -1.34), crown);
        mesh.bone(rig.at(0, brow - 1.2, -.56), rig.at(0, crest - .2, -.74), .77, rig.tint(), crown.strength());
        for (int side = -1; side <= 1; side += 2) {
            helmetSide(mesh, rig, side, brow, crest, crown);
        }
        if (rig.standing()) {
            plate(mesh, rig.at(-.71, brow - .5, 1.05), rig.at(.71, brow - .5, 1.05),
                    rig.at(.6, brow - 1.22, 1.12), rig.at(-.6, brow - 1.22, 1.12), crown);
        } else {
            mesh.bone(rig.at(0, brow + .02, .86), rig.at(0, brow - 1.04, 1.74), .34, rig.tint(), crown.strength());
        }
        mesh.bone(rig.at(-.70, brow - 1.30, .92), rig.at(.70, brow - 1.30, .92), .25, rig.tint(), crown.strength());
    }

    private static void helmetSide(Mesh mesh, Rig rig, int side, double brow, double crest, Plate crown) {
        double flare = rig.standing() ? 1.32 : rig.madara() ? 1.78 : 1.48;
        plate(mesh, rig.at(side * 1.18, brow, .83), rig.at(side * .76, crest, .17),
                rig.at(side * flare, crest + (rig.standing() ? -.22 : .28), -.91), rig.at(side * 1.45, brow - .17, -1.26), crown);
        plate(mesh, rig.at(side * 1.18, brow - .08, .80), rig.at(side * 1.45, brow - .17, -1.26),
                rig.at(side * .96, brow - 1.67, -.54), rig.at(side * .51, brow - 1.17, 1.05), crown);
        mesh.bone(rig.at(side * .24, brow - .39, 1.07), rig.at(side * .89, brow - .22, .95),
                .095, 0xFFEACF, rig.phase(11, 5));
    }

    private static void arm(Mesh mesh, Rig rig, SusanooPose.Arm arm, int side, boolean open, boolean low) {
        double power = rig.phase(3, 7);
        mesh.bone(arm.shoulder(), arm.elbow(), .88, rig.tint(), power);
        mesh.bone(arm.elbow(), arm.hand(), .74, rig.tint(), power);
        Vec3 sideAxis = rig.frame().right().scale(side);
        Vec3 forward = rig.frame().forward();
        var shield = new Plate(rig.tint(), rig.phase(6, 7), .26);
        for (int i = 0; i < (low ? 2 : 3); i++) {
            Vec3 segment = arm.shoulder().lerp(arm.elbow(), .12 + i * .19);
            Vec3 peak = segment.add(0, 1.07 - i * .16, 0);
            Vec3 outside = segment.add(sideAxis.scale((rig.madara() ? 1.91 : 1.54) - i * .22));
            plate(mesh, peak.add(forward.scale(1.18)), peak.subtract(forward.scale(1.05)),
                    outside.subtract(forward.scale(.85)), outside.add(forward.scale(.96)), shield);
        }
        hand(mesh, rig, arm, open, low);
    }

    private static void hand(Mesh mesh, Rig rig, SusanooPose.Arm arm, boolean open, boolean low) {
        double power = rig.phase(3, 7);
        Vec3 direction = arm.hand().subtract(arm.elbow()).normalize();
        Vec3 across = direction.cross(rig.frame().forward()).normalize();
        if (across.lengthSqr() < .01) {
            across = rig.frame().right();
        }
        Vec3 palm = arm.hand().subtract(direction.scale(.27));
        mesh.bone(palm.subtract(across.scale(.49)), palm.add(across.scale(.49)), .36, rig.tint(), power);
        for (int i = 0; i < (low ? 2 : 4); i++) {
            Vec3 knuckle = palm.add(across.scale((i - (low ? .5 : 1.5)) * (low ? .49 : .255)));
            Vec3 finger = knuckle.add(direction.scale(open ? .75 : .49)).add(0, open ? .29 : -.16, 0);
            mesh.bone(knuckle, finger, .135, rig.tint(), power);
        }
    }

    private static void wings(Mesh mesh, Rig rig, double stride, boolean low) {
        for (int side = -1; side <= 1; side += 2) {
            double spread = PerfectSusanooPose.wingOpening(rig.age(), stride, side);
            if (spread > .02) {
                wing(mesh, rig, side, spread, low);
            }
        }
    }

    private static void wing(Mesh mesh, Rig rig, int side, double spread, boolean low) {
        var surface = new Plate(rig.tint(), rig.phase(9, 8), .24);
        Vec3 root = rig.at(1.83 * side, 12.6, -1.39);
        Vec3 joint = rig.at((2.52 + 3.1 * spread) * side, 13.2 + 1.25 * spread, -2.95 + 1.28 * spread);
        Vec3 top = rig.at((3.31 + 7.52 * spread) * side, 14.0 + 3.31 * spread, -4.1 + 2.56 * spread);
        Vec3 fork = joint.subtract(rig.frame().forward().scale(.44)).add(0, -2.13, 0);
        plate(mesh, root, joint, top, fork, surface);
        mesh.bone(root, joint, .49, rig.tint(), surface.strength());
        mesh.bone(joint, top, .37, rig.tint(), surface.strength());
        int count = low ? 3 : 6;
        for (int i = 0; i < count; i++) {
            double t = (i + .2) / count;
            Vec3 spine = joint.lerp(top, t);
            Vec3 rim = joint.lerp(top, Math.min(1, t + .72 / count));
            Vec3 end = spine.add(rig.frame().right().scale(side * spread * (2.3 - 1.9 * t)))
                    .subtract(rig.frame().forward().scale(1.1 + .32 * t)).add(0, -5.75 + 3.1 * t, 0);
            plate(mesh, spine, rim, end, end.add(rig.frame().right().scale(-side * (.52 + spread * .36))), surface);
            if (mesh.glow()) {
                wingVein(mesh, rig, spine.lerp(rim, .44), end, side * i, t);
            }
        }
    }

    private static void wingVein(Mesh mesh, Rig rig, Vec3 from, Vec3 tip, int seed, double position) {
        double motion = SusanooChakraMaterial.vein(rig.shell().flow(), position, seed);
        double weight = rig.phase(11, 7) * motion;
        Vec3 ridge = from.lerp(tip, .56);
        Vec3 stripe = rig.frame().right().scale(.115 * (.8 + motion));
        int tint = Mesh.shade(rig.tint(), 1);
        mesh.gradient(from, from.add(stripe), ridge.add(stripe), ridge, tint, new Mesh.Fade(12, (int) (118 * weight)));
        mesh.gradient(ridge, ridge.add(stripe), tip, tip, tint, new Mesh.Fade((int) (118 * weight), 0));
    }

    private record Rig(Shell shell, Vec3 shift) {
        private SusanooPose.Frame frame() { return shell.frame(); }
        private int tint() { return shell.form().style().tint(); }
        private boolean madara() { return shell.form().style().madara(); }
        private boolean standing() { return shell.form().stage() == SusanooForm.STANDING.id(); }
        private double age() { return shell.age(); }

        private Vec3 at(double x, double y, double z) {
            return frame().at(x, y, z).add(shift);
        }

        private double phase(double start, double duration) {
            return shell.edge() * SusanooPose.smooth((age() - start) / duration);
        }
    }
}
