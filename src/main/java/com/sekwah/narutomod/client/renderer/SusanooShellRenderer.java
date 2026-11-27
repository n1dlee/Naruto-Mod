package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.client.renderer.entity.YasakaMagatamaRenderer;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.entity.MangekyoBossEntity;
import com.sekwah.narutomod.entity.MangekyoBossVariant;
import com.sekwah.narutomod.events.FinalChakraEffects;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket.Phase;
import com.sekwah.narutomod.util.PerfectSusanooPose;
import com.sekwah.narutomod.util.SusanooChakraMaterial;
import com.sekwah.narutomod.util.SusanooFullBody;
import com.sekwah.narutomod.util.SpectralSword;
import com.sekwah.narutomod.util.SusanooForm;
import com.sekwah.narutomod.util.SusanooPose;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;

final class SusanooShellRenderer {
    private static final Vec3 UP = new Vec3(0, 1, 0);

    private SusanooShellRenderer() {
    }

    static void draw(Minecraft mc, PoseStack pose, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        if (mc.level == null || mc.player == null) {
            return;
        }
        var shells = new ArrayList<Shell>();
        for (var candidate : mc.level.entitiesForRendering()) {
            if (candidate instanceof LivingEntity actor && actor.isAlive() && !actor.isSpectator()
                    && !actor.isInvisibleTo(mc.player) && actor.distanceToSqr(camera) < 104 * 104) {
                boolean ownView = actor == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
                var shell = snapshot(actor, partial, ownView);
                if (shell != null && (shell.fullBody != null || actor.distanceToSqr(camera) < 72 * 72)) {
                    shells.add(shell);
                }
            }
        }
        if (shells.isEmpty()) {
            return;
        }
        pose.pushPose();
        try {
            pose.translate(-camera.x, -camera.y, -camera.z);
            boolean low = NarutoConfig.reducedBodyRiteEffects || mc.options.graphicsMode().get() == GraphicsStatus.FAST;
            var dark = new Mesh(pose.last().pose(), buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE), camera, false);
            for (Shell shell : shells) {
                boolean reduced = low || shell.frame.feet().distanceToSqr(camera) > 36 * 36;
                body(dark, shell, reduced);
                SusanooChakraVeil.draw(dark, shell, reduced);
                seals(pose, dark.sink, shell);
            }
            buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
            var light = new Mesh(pose.last().pose(), buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW), camera, true);
            for (Shell shell : shells) {
                body(light, shell, low || shell.frame.feet().distanceToSqr(camera) > 36 * 36);
                if (mc.getEntityRenderDispatcher().shouldRenderHitBoxes()) {
                    anchors(light, shell);
                }
            }
            buffers.endBatch(NarutoRenderTypes.CHAKRA_GLOW);
        } finally {
            pose.popPose();
        }
    }

    private static Shell snapshot(LivingEntity actor, float partial, boolean ownView) {
        if (actor instanceof Player player) {
            var data = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).orElse(null);
            if (data != null && !data.getInvisible() && data.isSusanooActive() && supported(data.getSusanooStage())) {
                var style = new Style(FinalChakraEffects.tint(data), MangekyoBossVariant.byFormId(data.getMangekyoForm()));
                float integrity = data.getSusanooDurability() / Math.max(1, data.getSusanooMaxDurability());
                return shell(actor, new Form(data.getSusanooStage(), data.getSusanooStageAge(), integrity, style), partial, ownView);
            }
        } else if (actor instanceof MangekyoBossEntity boss && boss.getVariant().hasSusanoo() && supported(boss.getSusanooStage())) {
            var variant = boss.getVariant();
            var style = new Style(FinalChakraEffects.rgb(variant.susanooRed(), variant.susanooGreen(), variant.susanooBlue()), variant);
            return shell(actor, new Form(boss.getSusanooStage(), boss.getSusanooStageAge(), boss.getSusanooVisualIntegrity(), style),
                    partial, ownView);
        }
        return null;
    }

    private static boolean supported(int stage) {
        return SusanooForm.fromId(stage) != SusanooForm.NONE;
    }

    private static Shell shell(LivingEntity actor, Form form, float partial, boolean ownView) {
        var toss = actionForForm(actor, form.stage(), Phase.YASAKA_GATHER, partial);
        var cut = actionForForm(actor, form.stage(), Phase.SWORD, partial);
        float bodyYaw = Mth.rotLerp(partial, actor.yBodyRotO, actor.yBodyRot);
        float yaw = toss != null ? toss.yaw() : cut != null ? cut.yaw() : bodyYaw;
        var frame = SusanooPose.frame(actor.getPosition(partial), yaw);
        float stride = actor.walkAnimation.speed(partial);
        double sway = Math.sin(actor.walkAnimation.position(partial) * .72) * Math.min(.34, stride);
        FullBodyMotion fullBody = null;
        if (SusanooFullBody.hasLegs(form.stage())) {
            double scale = SusanooFullBody.scale(form.stage());
            var stance = PerfectSusanooWalk.sample(actor, actor.yBodyRot, form.age(), partial, scale);
            fullBody = new FullBodyMotion(stance, new SusanooFullBody.Space(frame.feet(), scale),
                    PerfectSusanooPose.hipShift(frame, stance, scale), sway * 2.6 * scale);
        }
        return new Shell(frame, form, toss, cut, ownView, form.age() + partial, sway, fullBody, actor.tickCount + partial);
    }

    private static FinalChakraRenderer.Action actionForForm(LivingEntity actor, int stage, Phase phase, float partial) {
        var action = FinalChakraRenderer.action(actor, phase, partial);
        return action != null && action.stage() == stage ? action : null;
    }

    private static void body(Mesh mesh, Shell shell, boolean low) {
        if (shell.fullBody != null) {
            PerfectSusanooRenderer.draw(mesh, shell, low);
            return;
        }
        int stage = shell.form.stage;
        var shape = SusanooForm.fromId(stage);
        boolean armored = shape == SusanooForm.ARMORED;
        int tint = shell.form.style.tint();
        var right = shell.rightArm();
        if (!shell.ownView) {
            if (shape != SusanooForm.AVATAR) ribs(mesh, shell, low);
            if (stage >= 2 && shell.age > 4) {
                if (shape == SusanooForm.AVATAR) {
                    SusanooIntermediateRenderer.avatar(mesh, shell, low);
                } else {
                    SusanooShellArmor.skull(mesh, shell);
                }
                var left = SusanooPose.rest(shell.frame, stage, -1, -shell.sway);
                arm(mesh, left, -.15, shell.edge(), tint);
                SusanooIntermediateRenderer.arm(mesh, shell, left, -1, low);
                if (armored) {
                    SusanooShellArmor.torso(mesh, shell, low);
                    SusanooShellArmor.pauldron(mesh, shell, left, -1, low);
                }
            }
        }
        if (stage >= 2 && shell.age > 4 && (!shell.ownView || shell.toss != null || shell.cut != null)) {
            arm(mesh, right, shell.toss == null ? -.12 : .82, shell.edge(), tint);
            SusanooIntermediateRenderer.arm(mesh, shell, right, 1, low);
            if (shell.cut != null) {
                SusanooShellArmor.sword(mesh, shell, right.hand());
            }
            if (armored && !shell.ownView) {
                SusanooShellArmor.pauldron(mesh, shell, right, 1, low);
            }
        }
    }

    private static void ribs(Mesh mesh, Shell shell, boolean low) {
        int tint = shell.form.style.tint();
        SusanooPose.Frame frame = shell.frame;
        double spread = shell.form.stage == 1 ? .84 : 1;
        double skeletonScale = SusanooPose.torsoScale(shell.form.stage);
        double spineHeight = .91 + 2.08 * SusanooPose.smooth(shell.age / 7);
        mesh.bone(frame.at(0, .85, -.63), frame.at(0, spineHeight * skeletonScale, -.63), .16, tint, shell.edge());
        for (int rib = 0; rib < 5; rib++) {
            double reveal = SusanooPose.smooth((shell.age - rib * 1.35) / 5);
            double y = (1.02 + rib * .38) * skeletonScale;
            double width = (.84 + rib * .06) * spread;
            if (reveal < .04) {
                continue;
            }
            for (int side : new int[]{-1, 1}) {
                int segments = low ? 4 : 7;
                Vec3 previous = frame.at(side * .2, y, -.67);
                for (int i = 1; i <= segments; i++) {
                    double angle = i * 2.54 * reveal / segments;
                    double x = side * (.2 + width * Math.sin(angle));
                    Vec3 next = frame.at(x, y + .12 * Math.sin(angle) - .16 * (1 - Math.cos(angle)),
                            -.67 + (1 - Math.cos(angle)) * (.54 + rib * .034));
                    mesh.bone(previous, next, .115 - .045 * i / segments, tint, shell.edge());
                    if (mesh.glow && !low && shell.form.integrity < .65 && i == 3 && rib % 2 == 0) {
                        mesh.bone(next, next.add(0, .18, 0), .045, 0xE4CFF8, .8);
                    }
                    previous = next;
                }
            }
        }
    }

    private static void arm(Mesh mesh, SusanooPose.Arm arm, double opening, double edge, int tint) {
        mesh.bone(arm.shoulder(), arm.elbow(), .19, tint, edge);
        mesh.bone(arm.elbow(), arm.hand(), .15, tint, edge);
        Vec3 grip = arm.hand().subtract(arm.elbow()).normalize();
        Vec3 side = grip.cross(UP).normalize();
        if (side.lengthSqr() < .01) {
            side = new Vec3(1, 0, 0);
        }
        Vec3 palm = arm.hand().subtract(grip.scale(.22));
        mesh.bone(palm.subtract(side.scale(.15)), palm.add(side.scale(.15)), .115, tint, edge);
        for (int i = -1; i <= 1; i++) {
            Vec3 base = palm.add(side.scale(i * .105));
            Vec3 joint = base.add(grip.scale(.19)).add(0, .08 + opening * .07, 0);
            mesh.bone(base, joint, .058, tint, edge);
            mesh.bone(joint, joint.add(grip.scale(.12)).add(0, opening * .13, 0), .042, tint, edge);
        }
    }

    private static void seals(PoseStack pose, VertexConsumer sink, Shell shell) {
        if (shell.toss == null || shell.form.stage < 2) {
            return;
        }
        double age = shell.toss.age();
        Vec3 aim = shell.toss.aim().normalize();
        Vec3 palm = shell.rightArm().hand();
        for (int i = 0; i < SusanooPose.SHOTS; i++) {
            if (age >= SusanooPose.releaseTick(i) || age < 1 + i) {
                continue;
            }
            double angle = i * Math.PI * 2 / 3 + age * .24;
            double spread = .35 * SusanooPose.smooth((SusanooPose.releaseTick(i) - age) / 3);
            Vec3 center = palm.add(shell.frame.right().scale(Math.cos(angle) * spread))
                    .add(0, Math.sin(angle) * spread, 0);
            pose.pushPose();
            try {
                pose.translate(center.x, center.y, center.z);
                pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, 1),
                        new Vector3f((float) aim.x, (float) aim.y, (float) aim.z)));
                pose.mulPose(Axis.ZP.rotationDegrees((float) (i * 120 + age * 19)));
                float size = (float) (.43 + .58 * SusanooPose.smooth((age - i) / 4));
                if (SusanooForm.perfect(shell.form.stage)) {
                    size *= 1.6f;
                }
                pose.scale(size, size, size);
                YasakaMagatamaRenderer.renderSeal(pose.last().pose(), sink, shell.form.style.tint());
            } finally {
                pose.popPose();
            }
        }
    }

    private static void anchors(Mesh mesh, Shell shell) {
        Vec3 root = shell.frame.feet();
        mesh.bone(root, root.add(0, .48, 0), .045, 0x36DDB5, 1);
        if (shell.form.stage >= 2) {
            Vec3 socket = shell.rightArm().hand();
            mesh.bone(socket.add(0, -.18, 0), socket.add(0, .18, 0), .074, 0xFFB235, 1);
        }
        if (shell.fullBody != null) {
            for (Vec3 foot : new Vec3[]{shell.fullBody.stance.left(), shell.fullBody.stance.right()}) {
                mesh.bone(foot, foot.add(0, .64, 0), .12, 0x36DDB5, 1);
            }
            if (shell.cut != null) {
                var cut = shell.cut;
                var blade = SpectralSword.sample(cut.origin(), cut.yaw(), cut.stage(), cut.age());
                int color = SpectralSword.active(cut.stage(), (int) cut.age()) ? 0xFF6155 : 0x549FFF;
                mesh.bone(blade.guard(), blade.tip(), .06, color, .9);
            }
        }
    }

    record Style(int tint, MangekyoBossVariant wearer) {
        boolean madara() { return wearer == MangekyoBossVariant.MADARA; }
        boolean itachi() { return wearer == MangekyoBossVariant.ITACHI; }
    }

    record Form(int stage, int age, float integrity, Style style) {
    }

    record FullBodyMotion(PerfectSusanooPose.Stance stance, SusanooFullBody.Space space, Vec3 hip, double armSwing) {
    }

    record Shell(SusanooPose.Frame frame, Form form, FinalChakraRenderer.Action toss, FinalChakraRenderer.Action cut,
                 boolean ownView, double age, double sway, FullBodyMotion fullBody, double flow) {
        SusanooPose.Arm rightArm() {
            if (fullBody != null) {
                if (toss != null) {
                    return SusanooPose.throwArm(frame, form.stage, toss.aim(), toss.age());
                }
                if (cut != null) {
                    return PerfectSusanooPose.cutArm(frame, cut.origin(), fullBody.hip, cut.age(), form.stage);
                }
                return PerfectSusanooPose.restArm(frame, 1, fullBody.hip, fullBody.armSwing, form.stage);
            }
            if (toss != null) {
                return SusanooPose.throwArm(frame, form.stage, toss.aim(), toss.age());
            }
            return cut != null ? SusanooPose.cutArm(frame, cut.origin(), form.stage, cut.age())
                    : SusanooPose.rest(frame, form.stage, 1, sway);
        }

        double edge() {
            return (.35 + .65 * SusanooPose.smooth(age / 11)) * (.56 + .44 * form.integrity);
        }
    }

    record Mesh(Matrix4f matrix, VertexConsumer sink, Vec3 camera, boolean glow, SusanooFullBody.Space space) {
        Mesh(Matrix4f matrix, VertexConsumer sink, Vec3 camera, boolean glow) {
            this(matrix, sink, camera, glow, new SusanooFullBody.Space(Vec3.ZERO, 1));
        }

        Mesh scaled(SusanooFullBody.Space transform) {
            return transform.scale() == 1 ? this : new Mesh(matrix, sink, transform.toRig(camera), glow, transform);
        }

        double rim(Vec3 normal, Vec3 surface) {
            return SusanooChakraMaterial.rim(normal, camera.subtract(surface));
        }

        void bone(Vec3 a, Vec3 b, double width, int tint, double strength) {
            Vec3 axis = b.subtract(a).normalize();
            if (axis.lengthSqr() < .01) {
                return;
            }
            Vec3 side = axis.cross(Math.abs(axis.y) < .92 ? UP : new Vec3(0, 0, 1)).normalize();
            Vec3 up = side.cross(axis).normalize();
            Vec3[] rim = {side, up, side.scale(-1), up.scale(-1)};
            if (glow) {
                Vec3 high = up.scale(width);
                quad(a.add(high), b.add(high.scale(.82)), b.add(high.scale(.82)).add(side.scale(width * .18)),
                        a.add(high).add(side.scale(width * .18)), shade(tint, 1), (int) (107 * strength));
                return;
            }
            for (int i = 0; i < rim.length; i++) {
                Vec3 u = rim[i].scale(width);
                Vec3 v = rim[(i + 1) % rim.length].scale(width);
                quad(a.add(u), b.add(u.scale(.82)), b.add(v.scale(.82)), a.add(v),
                        shade(tint, i == 0 ? .72 : .39), (int) (185 * strength));
            }
        }

        static int shade(int tint, double value) {
            double light = value * .91;
            int red = 12 + (int) ((tint >>> 16 & 255) * light);
            int green = 10 + (int) ((tint >>> 8 & 255) * light);
            int blue = 16 + (int) ((tint & 255) * light);
            return (red << 16) | (green << 8) | blue;
        }

        void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int rgb, int alpha) {
            vertex(a, rgb, alpha);
            vertex(b, rgb, alpha);
            vertex(c, rgb, alpha);
            vertex(d, rgb, alpha);
        }

        record Fade(int base, int tip) {
        }

        void gradient(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int rgb, Fade fade) {
            vertex(a, rgb, fade.base());
            vertex(b, rgb, fade.base());
            vertex(c, rgb, fade.tip());
            vertex(d, rgb, fade.tip());
        }

        private void vertex(Vec3 v, int rgb, int alpha) {
            Vec3 point = space.toWorld(v);
            sink.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color(rgb >>> 16 & 255, rgb >>> 8 & 255, rgb & 255, Mth.clamp(alpha, 0, 255)).endVertex();
        }
    }
}
