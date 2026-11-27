package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.entity.AnimalPathRhinoEntity;
import com.sekwah.narutomod.util.AnimalPathDrive;
import com.sekwah.narutomod.util.AnimalPathDrive.Stage;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class AnimalPathRhinoRenderer extends EntityRenderer<AnimalPathRhinoEntity> {
    private static final int HIDE = 0x524B64;
    private static final int PLATE = 0x797386;
    private static final int CREASE = 0x352D46;
    private static final int ROD = 0x242030;
    private static final int HORN = 0xB4A4A3;
    private static final int RINNE = 0xC2A2EE;

    public AnimalPathRhinoRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.88f;
    }

    @Override
    public void render(AnimalPathRhinoEntity rhino, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(rhino, yaw, partial, pose, buffers, light);
        boolean low = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        Stage stage = rhino.stage();
        double age = rhino.age() + partial;
        float growth = stage == Stage.BIRTH ? Mth.clamp((float) (age / AnimalPathDrive.BIRTH), 0.10f, 1)
                : stage == Stage.FADE ? 1 - 0.89f * Mth.clamp((float) (age / AnimalPathDrive.FADE), 0f, 1f) : 1;
        float moving = stage == Stage.RAM ? 0.99f : rhino.walkAnimation.speed(partial);
        float stride = rhino.walkAnimation.position(partial);
        double brace = stage == Stage.BRACE ? Mth.clamp(age / AnimalPathDrive.BRACE, 0, 1) : stage == Stage.RAM ? 1 : 0;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, rhino.yBodyRotO, rhino.yBodyRot)));
        pose.scale(0.84f + 0.16f * growth, growth, 0.84f + 0.16f * growth);
        var body = new Mesh(pose.last().pose(), buffers.getBuffer(NarutoRenderTypes.BOULDER_BODY));
        torso(body, low);
        legs(body, stride, moving, brace);
        var head = new Head(new Vec3(0, 1.53 - 0.33 * brace, 1.01 + 0.13 * brace), brace, low);
        skull(body, head);
        var lens = new Mesh(pose.last().pose(), buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW));
        eyes(lens, head, rhino.markPulse() / 9d);
        pose.popPose();
    }

    private static void torso(Mesh mesh, boolean low) {
        mesh.blunt(new Form(new Vec3(0, 1.42, -0.28), new Vec3(0.93, 0.71, 1.13), HIDE), low);
        mesh.blunt(new Form(new Vec3(0, 1.59, 0.55), new Vec3(0.92, 0.78, 0.66), PLATE), low);
        mesh.blunt(new Form(new Vec3(0, 1.58, -0.84), new Vec3(0.84, 0.59, 0.52), HIDE), low);
        mesh.blunt(new Form(new Vec3(0, 0.97, 0.62), new Vec3(0.82, 0.32, 0.43), CREASE), true);
        if (!low) {
            mesh.blunt(new Form(new Vec3(0, 1.20, -1.38), new Vec3(0.08, 0.24, 0.25), CREASE), true);
            for (int side : new int[]{-1, 1}) {
                mesh.blunt(new Form(new Vec3(side * 0.80, 1.76, -0.09), new Vec3(0.15, 0.22, 0.38), PLATE), true);
            }
        }
    }

    private static void legs(Mesh mesh, float stride, float speed, double brace) {
        for (int i = 0; i < 4; i++) {
            int sign = i % 2 == 0 ? -1 : 1;
            boolean fore = i < 2;
            double step = Math.cos(stride * 0.72 + (i == 0 || i == 3 ? Math.PI : 0)) * Math.min(1, speed * 1.1);
            double hip = fore ? 0.56 : -0.83;
            double shove = fore ? brace * 0.19 : -brace * 0.10;
            Vec3 hoof = new Vec3(sign * 0.58, 0.24 + Math.max(0, step) * 0.19, hip + step * 0.42 + shove);
            Vec3 elbow = new Vec3(sign * 0.62, 0.70 + Math.max(0, step) * 0.09, hip + step * 0.18);
            mesh.blunt(new Form(elbow, new Vec3(0.32, 0.63, 0.34), HIDE), true);
            mesh.blunt(new Form(hoof, new Vec3(0.265, 0.23, 0.30), CREASE), true);
        }
    }

    private static void skull(Mesh mesh, Head head) {
        Vec3 at = head.center();
        mesh.blunt(new Form(at, new Vec3(0.63, 0.53, 0.64), PLATE), head.low());
        mesh.blunt(new Form(at.add(0, -0.27, 0.60), new Vec3(0.46, 0.28, 0.34), HIDE), head.low());
        mesh.horn(at.add(0, 0.03, 0.72), at.add(0, 0.46 - head.brace() * 0.19, 1.25), 0.22, HORN);
        mesh.horn(at.add(0, 0.26, 0.29), at.add(0, 0.64, 0.47), 0.14, HORN);
        for (int sign : new int[]{-1, 1}) {
            mesh.horn(at.add(sign * 0.43, 0.27, -0.29), at.add(sign * 0.56, 0.67, -0.34), 0.15, HIDE);
            for (int i = 0; i < 3; i++) {
                var pin = at.add(sign * (0.37 + i * 0.035), 0.39 - i * 0.13, 0.39 + i * 0.10);
                mesh.horn(pin, pin.add(sign * 0.08, 0.04, 0.13), 0.046, ROD);
            }
        }
    }

    private static void eyes(Mesh mesh, Head head, double pulse) {
        for (int sign : new int[]{-1, 1}) {
            Vec3 eye = head.center().add(sign * 0.46, 0.09, 0.48);
            mesh.eye(eye, sign, 0.091 + 0.024 * pulse, RINNE, 176 + (int) (79 * pulse));
            mesh.eye(eye.add(sign * 0.004, 0, 0.007), sign, 0.039, 0xF1DBFF, 206);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(AnimalPathRhinoEntity rhino) { return TextureAtlas.LOCATION_BLOCKS; }

    private record Form(Vec3 center, Vec3 half, int tone) {
    }

    private record Head(Vec3 center, double brace, boolean low) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void blunt(Form shape, boolean low) {
            int count = low ? 6 : 9;
            Vec3 c = shape.center();
            Vec3 r = shape.half();
            for (int i = 0; i < count; i++) {
                double theta = i * Math.PI * 2 / count;
                double next = (i + 1) * Math.PI * 2 / count;
                Vec3 a = new Vec3(Math.cos(theta) * r.x, 0, Math.sin(theta) * r.z);
                Vec3 b = new Vec3(Math.cos(next) * r.x, 0, Math.sin(next) * r.z);
                Vec3 high = c.add(0, r.y * 0.99, 0);
                Vec3 bottom = c.subtract(0, r.y, 0);
                Vec3 aUp = high.add(a.scale(0.61));
                Vec3 bUp = high.add(b.scale(0.61));
                Vec3 aDown = bottom.add(a.scale(0.67));
                Vec3 bDown = bottom.add(b.scale(0.67));
                quad(aUp, bUp, c.add(b), c.add(a), fade(shape.tone(), 0.87 + i % 4 * 0.063), 255);
                quad(c.add(a), c.add(b), bDown, aDown, fade(shape.tone(), 0.68 + i % 3 * 0.047), 255);
                quad(high, high, bUp, aUp, fade(shape.tone(), 1.13), 255);
                quad(bottom, aDown, bDown, bottom, fade(shape.tone(), 0.7), 255);
            }
        }

        private void horn(Vec3 a, Vec3 tip, double radius, int color) {
            Vec3 forward = tip.subtract(a).normalize();
            Vec3 right = (Math.abs(forward.y) > 0.9 ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0)).cross(forward).normalize();
            Vec3 up = forward.cross(right);
            for (int i = 0; i < 5; i++) {
                double angle = i * Math.PI * 2 / 5;
                double next = (i + 1) * Math.PI * 2 / 5;
                Vec3 p = a.add(right.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
                Vec3 q = a.add(right.scale(Math.cos(next) * radius)).add(up.scale(Math.sin(next) * radius));
                quad(p, q, tip, tip, fade(color, 0.74 + i % 3 * 0.13), 255);
            }
        }

        private void eye(Vec3 at, int sign, double size, int color, int alpha) {
            Vec3 side = new Vec3(0.76, 0, -sign * 0.59).scale(size);
            Vec3 up = new Vec3(0, size, 0);
            quad(at.add(side), at.add(up), at.subtract(side), at.subtract(up), color, alpha);
        }

        private static int fade(int rgb, double scale) {
            return Math.min(255, (int) ((rgb >>> 16 & 255) * scale)) << 16
                    | Math.min(255, (int) ((rgb >>> 8 & 255) * scale)) << 8 | Math.min(255, (int) ((rgb & 255) * scale));
        }

        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int tint, int alpha) {
            vertex(a, tint, alpha);
            vertex(b, tint, alpha);
            vertex(c, tint, alpha);
            vertex(d, tint, alpha);
        }

        private void vertex(Vec3 at, int tint, int alpha) {
            this.buffer.vertex(this.matrix, (float) at.x, (float) at.y, (float) at.z)
                    .color((tint >>> 16) & 255, (tint >>> 8) & 255, tint & 255, alpha).endVertex();
        }
    }
}
