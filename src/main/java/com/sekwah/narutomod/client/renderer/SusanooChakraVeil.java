package com.sekwah.narutomod.client.renderer;

import com.sekwah.narutomod.client.renderer.SusanooShellRenderer.Mesh;
import com.sekwah.narutomod.client.renderer.SusanooShellRenderer.Shell;
import com.sekwah.narutomod.util.SusanooForm;
import com.sekwah.narutomod.util.SusanooFullBody;
import com.sekwah.narutomod.util.SusanooPose;
import net.minecraft.world.phys.Vec3;

final class SusanooChakraVeil {
    private SusanooChakraVeil() {
    }

    static void draw(Mesh mesh, Shell shell, boolean low) {
        var form = SusanooForm.fromId(shell.form().stage());
        if (shell.ownView() || form.ordinal() < SusanooForm.SINEW.ordinal()) return;
        boolean fullBody = SusanooFullBody.hasLegs(form.id());
        double fullScale = SusanooFullBody.scale(form.id());
        double scale = fullBody ? 2.8 * fullScale : form.torsoScale();
        double lift = fullBody ? 6.15 * fullScale : 0;
        int tint = shell.form().style().tint();
        double strength = shell.edge() * SusanooPose.smooth((shell.age() - 2) / 9);
        var field = new Field(shell.frame(), scale, lift, shell.flow(), tint, strength);
        if (form == SusanooForm.SINEW || form == SusanooForm.AVATAR) {
            skin(mesh, field, form == SusanooForm.AVATAR, low);
            fibers(mesh, field, low);
        }
        flames(mesh, field, low, fullBody);
    }

    private static void skin(Mesh mesh, Field f, boolean closed, boolean low) {
        int loops = low ? 6 : 12;
        double[] wide = {.52, .85, 1.27, 1.36, .61};
        double[] deep = {.35, .55, .68, .51, .30};
        double[] height = {.62, 1.25, 1.91, 2.79, 3.02};
        int face = Mesh.shade(f.tint(), closed ? .70 : .53);
        for (int ring = 0; ring < height.length - 1; ring++) {
            for (int i = 0; i < loops; i++) {
                double a = 2 * Math.PI * i / loops;
                double b = 2 * Math.PI * (i + 1) / loops;
                double edge = .54 + .46 * Math.abs(Math.sin((a + b) * .5));
                int opacity = (int) ((closed ? 105 : 62) * f.strength() * edge);
                mesh.gradient(f.at(wide[ring] * Math.cos(a), height[ring], deep[ring] * Math.sin(a)),
                        f.at(wide[ring] * Math.cos(b), height[ring], deep[ring] * Math.sin(b)),
                        f.at(wide[ring + 1] * Math.cos(b), height[ring + 1], deep[ring + 1] * Math.sin(b)),
                        f.at(wide[ring + 1] * Math.cos(a), height[ring + 1], deep[ring + 1] * Math.sin(a)),
                        face, new Mesh.Fade(opacity, ring == 3 ? opacity / 4 : opacity));
            }
        }
    }

    private static void fibers(Mesh mesh, Field f, boolean low) {
        int count = low ? 2 : 5;
        int tint = Mesh.shade(f.tint(), .90);
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < count; i++) {
                double shift = i * .12;
                Vec3 root = f.at(side * (.27 + shift), 1.25 + shift, .51);
                Vec3 bow = f.at(side * (.57 + shift), 2.02 + shift * .68, .66);
                Vec3 tip = f.at(side * (.83 + shift * .72), 2.79 + shift * .23, .48);
                Vec3 width = f.frame().right().scale(.042 * f.scale());
                int opacity = (int) ((112 + 27 * Math.sin(f.clock() * .23 + i)) * f.strength());
                mesh.gradient(root.subtract(width), root.add(width), bow.add(width), bow.subtract(width), tint,
                        new Mesh.Fade(opacity / 5, opacity));
                mesh.gradient(bow.subtract(width), bow.add(width), tip.add(width.scale(.22)), tip.subtract(width.scale(.22)), tint,
                        new Mesh.Fade(opacity, 0));
            }
        }
    }

    private static void flames(Mesh mesh, Field f, boolean low, boolean perfect) {
        int tint = Mesh.shade(f.tint(), 1);
        int count = low ? 6 : 12;
        for (int i = 0; i < count; i++) {
            double angle = (i + .3) * Math.PI * 2 / count;
            double wind = Math.sin(f.clock() * .20 + i * 1.78);
            double wave = Math.cos(f.clock() * .28 - i * 2.03);
            double x = Math.cos(angle);
            double z = Math.sin(angle) * (perfect ? .85 : .70);
            double y = (i % 4) * .61 + .86;
            Vec3 base = f.at(x * 1.29, y, z);
            Vec3 middle = f.at(x * (1.50 + .12 * wind), y + .52, z - .19 + wave * .08);
            Vec3 end = f.at(x * (1.57 + .25 * wave), y + 1.08 + .22 * wind, z - .39 + wave * .16);
            Vec3 across = f.frame().right().scale(-Math.sin(angle)).add(f.frame().forward().scale(Math.cos(angle)))
                    .scale(f.scale() * (.12 + .04 * wave));
            int alpha = (int) ((55 + 18 * wind) * f.strength());
            mesh.gradient(base.subtract(across), base.add(across), middle.add(across.scale(.76)), middle.subtract(across.scale(.76)),
                    tint, new Mesh.Fade(alpha / 4, alpha));
            mesh.gradient(middle.subtract(across.scale(.76)), middle.add(across.scale(.76)), end, end, tint,
                    new Mesh.Fade(alpha, 0));
        }
    }

    private record Field(SusanooPose.Frame frame, double scale, double lift, double clock, int tint, double strength) {
        private Vec3 at(double x, double y, double z) {
            return frame.at(x * scale, lift + y * scale, z * scale);
        }
    }
}
