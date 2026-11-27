package com.sekwah.narutomod.client.renderer;

import com.sekwah.narutomod.client.renderer.SusanooShellRenderer.Mesh;
import com.sekwah.narutomod.client.renderer.SusanooShellRenderer.Shell;
import com.sekwah.narutomod.util.SusanooForm;
import com.sekwah.narutomod.util.SusanooPose;
import net.minecraft.world.phys.Vec3;

final class SusanooIntermediateRenderer {
    private static final Vec3 UP = new Vec3(0, 1, 0);

    private SusanooIntermediateRenderer() {
    }

    static void avatar(Mesh mesh, Shell shell, boolean low) {
        var f = shell.frame();
        double rise = SusanooPose.smooth((shell.age() - 4) / 7);
        double y = 4.14 + .28 * rise;
        var ink = skin(shell, low);
        volume(mesh, f.at(0, 3.23, -.24), f.at(0, y + .22, -.24), .52, ink);
        volume(mesh, f.at(0, y - .52, .19), f.at(0, y + .79, -.14), .64, ink);
        if (mesh.glow()) {
            mesh.bone(f.at(-.52, y + .23, .29), f.at(.52, y + .23, .29), .045,
                    Mesh.shade(ink.tint(), 1), ink.opacity());
            return;
        }
        int dark = Mesh.shade(ink.tint(), .36);
        int face = Mesh.shade(ink.tint(), .77);
        int alpha = (int) (185 * ink.opacity());
        mesh.quad(f.at(-.41, y + .45, .50), f.at(.41, y + .45, .50), f.at(.56, y + .06, .61), f.at(-.56, y + .06, .61), dark, alpha);
        mesh.quad(f.at(-.56, y + .06, .61), f.at(.56, y + .06, .61), f.at(.29, y - .48, .68), f.at(-.29, y - .48, .68), face, alpha);
        canopy(mesh, f, ink);
    }

    private static void canopy(Mesh mesh, SusanooPose.Frame f, Ink ink) {
        for (int side = -1; side <= 1; side += 2) {
            volume(mesh, f.at(.39 * side, 2.83, .55), f.at(1.61 * side, 3.45, -.04), .45, ink);
            Vec3 top = f.at(1.20 * side, 2.38, .56);
            Vec3 fall = f.at(1.54 * side, .47, .31);
            mesh.gradient(f.at(.31 * side, 2.23, .86), top, fall, f.at(.42 * side, .28, .77),
                    Mesh.shade(ink.tint(), .64), new Mesh.Fade((int) (133 * ink.opacity()), 0));
        }
    }

    static void arm(Mesh mesh, Shell shell, SusanooPose.Arm arm, int side, boolean low) {
        var form = SusanooForm.fromId(shell.form().stage());
        if (form != SusanooForm.AVATAR && form != SusanooForm.SINEW) return;
        var ink = skin(shell, low);
        if (form == SusanooForm.AVATAR) {
            volume(mesh, arm.shoulder(), arm.elbow(), .52, ink);
            volume(mesh, arm.elbow(), arm.hand(), .46, ink);
        } else {
            sinew(mesh, shell.frame(), arm, side, ink);
        }
    }

    private static void sinew(Mesh mesh, SusanooPose.Frame f, SusanooPose.Arm arm, int side, Ink ink) {
        Vec3 shoulder = arm.shoulder();
        Vec3 elbow = arm.elbow();
        Vec3 outside = f.right().scale(side);
        volume(mesh, shoulder.add(outside.scale(.18)).add(0, .25, 0), shoulder.lerp(elbow, .64), .53, ink);
        volume(mesh, shoulder.add(f.forward().scale(.29)), elbow.add(f.forward().scale(.22)), .28, ink);
        volume(mesh, elbow.add(outside.scale(.23)), arm.hand(), .34, ink);
        var medium = new Ink(ink.tint(), ink.opacity() * .70, ink.sides());
        volume(mesh, f.at(side * .28, 2.47, .68), shoulder.add(f.forward().scale(.48)), .27, medium);
        if (mesh.glow()) {
            mesh.bone(shoulder.add(f.forward().scale(.44)), elbow.add(f.forward().scale(.30)), .048, ink.tint(), ink.opacity());
        }
    }

    private static Ink skin(Shell shell, boolean low) {
        return new Ink(shell.form().style().tint(), shell.edge() * SusanooPose.smooth((shell.age() - 3) / 7), low ? 5 : 8);
    }

    private static void volume(Mesh mesh, Vec3 a, Vec3 b, double width, Ink ink) {
        Vec3 axis = b.subtract(a).normalize();
        Vec3 u = axis.cross(Math.abs(axis.y) < .87 ? UP : new Vec3(0, 0, 1)).normalize();
        Vec3 v = axis.cross(u).normalize();
        for (int i = 0; i < ink.sides(); i++) {
            Vec3 l = heading(u, v, i * Math.PI * 2 / ink.sides());
            Vec3 r = heading(u, v, (i + 1) * Math.PI * 2 / ink.sides());
            Vec3 middle = a.lerp(b, .43);
            double rim = mesh.rim(l.add(r), middle);
            int alpha = (int) ((mesh.glow() ? 41 * rim : 117 + 42 * rim) * ink.opacity());
            if (alpha < 3) continue;
            int tint = Mesh.shade(ink.tint(), mesh.glow() ? 1 : .45 + .25 * rim);
            Vec3 lm = middle.add(l.scale(width));
            Vec3 rm = middle.add(r.scale(width));
            mesh.gradient(a.add(l.scale(width * .53)), a.add(r.scale(width * .53)), rm, lm, tint, new Mesh.Fade(alpha / 2, alpha));
            mesh.gradient(lm, rm, b.add(r.scale(width * .45)), b.add(l.scale(width * .45)), tint, new Mesh.Fade(alpha, alpha / 3));
        }
    }

    private static Vec3 heading(Vec3 u, Vec3 v, double a) {
        return u.scale(Math.cos(a)).add(v.scale(Math.sin(a)));
    }

    private record Ink(int tint, double opacity, int sides) {
    }
}
