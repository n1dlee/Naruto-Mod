package com.sekwah.narutomod.client.renderer;

import com.sekwah.narutomod.client.renderer.SusanooShellRenderer.Mesh;
import com.sekwah.narutomod.client.renderer.SusanooShellRenderer.Shell;
import com.sekwah.narutomod.util.SpectralSword;
import com.sekwah.narutomod.util.SusanooForm;
import com.sekwah.narutomod.util.SusanooPose;
import net.minecraft.world.phys.Vec3;

final class SusanooShellArmor {
    private SusanooShellArmor() {
    }

    static void skull(Mesh mesh, Shell shell) {
        var frame = shell.frame();
        var style = shell.form().style();
        var shape = SusanooForm.fromId(shell.form().stage());
        boolean armored = shape == SusanooForm.ARMORED || shape == SusanooForm.STANDING;
        double rise = SusanooPose.smooth((shell.age() - 4) / 6);
        double y = (3.14 + .45 * rise) * SusanooPose.torsoScale(shell.form().stage());
        double edge = rise * shell.edge();
        int tint = style.tint();
        mesh.bone(frame.at(0, y - .71, -.31), frame.at(0, y, -.35), .23, tint, edge);
        var face = new Plate(tint, edge, .05);
        Vec3[] crown = headRing(frame, y + .72, .43, .31, -.51);
        Vec3[] brow = headRing(frame, y + .37, armored ? .72 : .60, .55, -.50);
        Vec3[] cheeks = headRing(frame, y - .04, .51, .46, -.29);
        band(mesh, crown, brow, face);
        band(mesh, brow, cheeks, new Plate(tint, edge * .77, .015));
        plate(mesh, crown[0], crown[1], crown[2], crown[3], face);
        faceDetails(mesh, new Head(frame, y, edge, armored, style.madara() || armored && !style.itachi()), tint);
    }

    private static void faceDetails(Mesh mesh, Head head, int tint) {
        var f = head.frame();
        double y = head.y();
        double edge = head.edge();
        double z = head.armored() ? .71 : .55;
        for (int side = -1; side <= 1; side += 2) {
            mesh.bone(f.at(.44 * side, y + .07, z), f.at(.49 * side, y - .26, z - .04), .12, tint, edge);
            mesh.bone(f.at(.13 * side, y + .17, z + .035), f.at(.40 * side, y + .22, z + .025),
                    .033, 0xFFE6F3, edge);
            if (head.crest()) {
                var ridge = new Plate(tint, edge, .055);
                plate(mesh, f.at(side * .31, y + .68, .28), f.at(side * .67, y + .62, .05),
                        f.at(side * .54, y + (head.armored() ? 1.09 : .79), -.44),
                        f.at(side * .32, y + .72, -.53), ridge);
            }
        }
        if (head.armored() && !head.crest()) {
            var visor = new Plate(tint, edge, .06);
            plate(mesh, f.at(-.64, y + .48, .53), f.at(.64, y + .48, .53),
                    f.at(.33, y + .29, .74), f.at(-.33, y + .29, .74), visor);
        }
        mesh.bone(f.at(-.43, y - .27, z), f.at(.43, y - .27, z), .148, tint, edge);
        mesh.bone(f.at(0, y + .31, z), f.at(0, y - .025, z + .17), head.armored() ? .17 : .115, tint, edge);
    }

    static void torso(Mesh mesh, Shell shell, boolean low) {
        var f = shell.frame();
        double chest = reveal(shell.age(), 4, 7);
        double waist = reveal(shell.age(), 9, 8);
        int tint = shell.form().style().tint();
        if (chest > 0) {
            breastplate(mesh, f, new Plate(tint, shell.edge() * chest, .15), chest);
        }
        if (waist > 0) {
            tassets(mesh, f, new Plate(tint, shell.edge() * waist, .06), waist, low);
        }
    }

    private static void breastplate(Mesh mesh, SusanooPose.Frame f, Plate surface, double reveal) {
        for (int side = -1; side <= 1; side += 2) {
            double shift = (1 - reveal) * side * .78;
            plate(mesh, f.at(side * .10 + shift, 3.68, .89), f.at(side * 1.20 + shift, 3.82, .42),
                    f.at(side * .94 + shift, 2.60, .68), f.at(side * .14 + shift, 2.37, .92), surface);
            plate(mesh, f.at(side * 1.20 + shift, 3.82, .42), f.at(side * .95 + shift, 3.72, -.88),
                    f.at(side * .75 + shift, 2.67, -.75), f.at(side * .94 + shift, 2.60, .68), surface);
        }
        plate(mesh, f.at(-.94, 3.66, -.83), f.at(.94, 3.66, -.83), f.at(.82, 2.68, -.75),
                f.at(-.82, 2.68, -.75), surface);
        mesh.bone(f.at(0, 3.69, 1.02), f.at(0, 2.54, 1.08), .144, surface.tint(), surface.strength());
    }

    private static void tassets(Mesh mesh, SusanooPose.Frame f, Plate surface, double reveal, boolean low) {
        for (int i = 0; i < (low ? 2 : 3); i++) {
            double y = 2.37 - (1 - reveal) * .34 - i * .41;
            double x = .89 + i * .11;
            double z = .71 + i * .065;
            for (int side = -1; side <= 1; side += 2) {
                plate(mesh, f.at(side * .13, y, z + .19), f.at(side * x, y + .16, z - .34),
                        f.at(side * (x + .13), y - .23, z - .37), f.at(side * .14, y - .32, z + .24), surface);
            }
        }
    }

    static void pauldron(Mesh mesh, Shell shell, SusanooPose.Arm arm, int side, boolean low) {
        var f = shell.frame();
        var style = shell.form().style();
        Vec3 outside = f.right().scale(side);
        int count = low ? 2 : 3;
        for (int i = 0; i < count; i++) {
            double close = reveal(shell.age(), 5 + i * 2, 7);
            if (close <= 0) {
                continue;
            }
            Vec3 at = arm.shoulder().lerp(arm.elbow(), .10 + i * .20).add(outside.scale(.45 * (1 - close)));
            double forward = .67 - i * .07;
            double width = (style.madara() ? .83 : style.itachi() ? .71 : .64) - i * .11;
            Vec3 top = at.add(0, (style.itachi() ? .48 : .60) - i * .09, 0);
            Vec3 wing = at.add(outside.scale(width)).add(0, .30 - i * .14, 0);
            var surface = new Plate(style.tint(), shell.edge() * close, .12);
            plate(mesh, top.add(f.forward().scale(forward)), top.subtract(f.forward().scale(forward)),
                    wing.subtract(f.forward().scale(forward * .64)), wing.add(f.forward().scale(forward)), surface);
            plate(mesh, wing.add(f.forward().scale(forward)), wing.subtract(f.forward().scale(forward * .64)),
                    wing.add(0, -.31, 0).subtract(f.forward().scale(forward * .46)),
                    wing.add(0, -.38, 0).add(f.forward().scale(forward * .77)), surface);
        }
    }

    static void sword(Mesh mesh, Shell shell, Vec3 hand) {
        var cut = shell.cut();
        double age = cut.age();
        boolean giant = cut.stage() == 4;
        double solid = SusanooPose.smooth(age / (giant ? 8 : 5))
                * SusanooPose.smooth((SpectralSword.length(cut.stage()) - age) / (giant ? 7 : 5));
        if (solid <= .025) {
            return;
        }
        var blade = SpectralSword.sample(cut.origin(), cut.yaw(), cut.stage(), age);
        Vec3 span = blade.tip().subtract(blade.guard());
        Vec3 axis = span.normalize();
        Vec3 edge = new Vec3(axis.z, 0, -axis.x).normalize().scale(giant ? .71 : cut.stage() == 3 ? .27 : .19);
        Vec3 heel = hand.add(span.scale(.08 * solid));
        Vec3 tip = hand.add(span.scale(solid));
        Vec3 nose = tip.subtract(axis.scale((giant ? 2.2 : .7) * solid));
        int tint = shell.form().style().tint();
        mesh.bone(hand.subtract(axis.scale(giant ? 1.28 : .45)), heel, giant ? .26 : .084, tint, solid);
        mesh.bone(heel.add(edge.scale(1.42)), heel.subtract(edge.scale(1.42)), giant ? .23 : .09, tint, solid);
        if (mesh.glow()) {
            mesh.bone(heel.add(edge), nose.add(edge.scale(.63)), giant ? .10 : .045, 0xFBE2FF, solid);
            mesh.bone(nose.add(edge.scale(.63)), tip, giant ? .10 : .043, 0xFBE2FF, solid);
        } else {
            var steel = new Plate(tint, solid, .09);
            plate(mesh, heel.add(edge), nose.add(edge.scale(.63)), nose.subtract(edge.scale(.63)),
                    heel.subtract(edge), steel);
            mesh.quad(nose.add(edge.scale(.63)), tip, tip, nose.subtract(edge.scale(.63)), Mesh.shade(tint, .55),
                    (int) (216 * solid));
        }
    }

    private static double reveal(double age, double start, double length) {
        return SusanooPose.smooth((age - start) / length);
    }

    private static Vec3[] headRing(SusanooPose.Frame f, double y, double x, double front, double back) {
        return new Vec3[]{f.at(-x, y, front), f.at(x, y, front), f.at(x, y, back), f.at(-x, y, back)};
    }

    private static void band(Mesh mesh, Vec3[] top, Vec3[] bottom, Plate surface) {
        for (int i = 0; i < top.length; i++) {
            int next = (i + 1) % top.length;
            plate(mesh, top[i], top[next], bottom[next], bottom[i], surface);
        }
    }

    static void plate(Mesh mesh, Vec3 a, Vec3 b, Vec3 c, Vec3 d, Plate surface) {
        Vec3 middle = a.add(b).add(c).add(d).scale(.25);
        Vec3 normal = b.subtract(a).cross(d.subtract(a)).normalize();
        Vec3 lift = normal.scale(surface.bevel());
        double viewRim = mesh.rim(normal, middle);
        Vec3 ia = a.lerp(middle, .21).add(lift);
        Vec3 ib = b.lerp(middle, .21).add(lift);
        Vec3 ic = c.lerp(middle, .21).add(lift);
        Vec3 id = d.lerp(middle, .21).add(lift);
        if (mesh.glow()) {
            int alpha = (int) ((27 + viewRim * 88) * surface.strength());
            mesh.gradient(a, b, b.lerp(ib, .44), a.lerp(ia, .44), Mesh.shade(surface.tint(), 1), new Mesh.Fade(alpha, 0));
            if (viewRim > .42) {
                mesh.gradient(c, d, d.lerp(id, .29), c.lerp(ic, .29), Mesh.shade(surface.tint(), .92), new Mesh.Fade(alpha / 2, 0));
            }
        } else {
            int rim = Mesh.shade(surface.tint(), .64);
            int alpha = (int) ((134 + viewRim * 43) * surface.strength());
            mesh.quad(ia, ib, ic, id, Mesh.shade(surface.tint(), .30), (int) (alpha * .87));
            mesh.quad(a, b, ib, ia, rim, alpha);
            mesh.quad(b, c, ic, ib, rim, alpha);
            mesh.quad(c, d, id, ic, rim, alpha);
            mesh.quad(d, a, ia, id, rim, alpha);
        }
    }

    record Plate(int tint, double strength, double bevel) {
    }

    private record Head(SusanooPose.Frame frame, double y, double edge, boolean armored, boolean crest) {
    }
}
