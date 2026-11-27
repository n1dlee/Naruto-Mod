package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.entity.jutsuprojectile.GianBeamEntity;
import com.sekwah.narutomod.util.GianLine;
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

public class GianBeamRenderer extends EntityRenderer<GianBeamEntity> {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 X = new Vec3(1, 0, 0);
    private static final Tint SILVER = new Tint(234, 251, 255);
    private static final Tint SKY = new Tint(69, 188, 255);
    private static final Tint AMBER = new Tint(255, 174, 53);
    private static final Tint PORCELAIN = new Tint(200, 191, 158);
    private static final Tint INK = new Tint(22, 17, 23);
    private static final Tint RED = new Tint(168, 40, 58);

    public GianBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GianBeamEntity beam, float yaw, float partial, PoseStack poses, MultiBufferSource buffers, int light) {
        super.render(beam, yaw, partial, poses, buffers, light);
        var game = Minecraft.getInstance();
        boolean low = game.options.graphicsMode().get() == GraphicsStatus.FAST
                || beam.distanceToSqr(game.getCameraEntity() == null ? beam : game.getCameraEntity()) > 44 * 44;
        Matrix4f matrix = poses.last().pose();
        Frame frame = Frame.along(beam.getLookAngle());
        if (beam.mask()) {
            mask(new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE)), frame,
                    Mth.clamp((beam.age() + partial) / 3, 0.25f, 1) * fade(beam.age(), partial));
        }
        var mesh = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW));
        Tint rim = beam.mask() ? AMBER : SKY;
        if (beam.age() < GianLine.PREPARE) {
            charge(mesh, frame, rim, beam.age() + partial, low);
        } else if (beam.length() > 0.01) {
            var look = new BeamLook(beam.length(), fade(beam.age(), partial), beam.getId(), rim, low);
            bolt(mesh, frame, look);
        }
    }

    private static float fade(int age, float partial) {
        return Mth.clamp(1 - (age + partial - GianLine.PREPARE - GianLine.SHOCK) / GianLine.FADE, 0, 1);
    }

    private static void charge(Mesh mesh, Frame frame, Tint rim, float ticks, boolean low) {
        double radius = 0.075 + ticks * 0.019;
        int rays = low ? 4 : 8;
        for (int i = 0; i < rays; i++) {
            double angle = i * Math.PI * 2 / rays + ticks * 0.42;
            Vec3 outer = frame.offset(0, Math.cos(angle) * radius, Math.sin(angle) * radius);
            Vec3 skew = frame.offset(-0.15, Math.cos(angle + 0.3) * radius, Math.sin(angle + 0.3) * radius);
            mesh.ribbon(skew, outer.scale(0.46), frame.side(), new Stroke(rim, 0.021, 179));
        }
        mesh.ribbon(frame.axis().scale(-0.05), frame.axis().scale(0.25), frame.up(), new Stroke(SILVER, radius * 0.33, 200));
    }

    private static void bolt(Mesh mesh, Frame frame, BeamLook look) {
        int segments = Mth.clamp((int) Math.ceil(look.length() * (look.low() ? 1.1 : 2.0)), 1, 42);
        Vec3 previous = Vec3.ZERO;
        for (int i = 1; i <= segments; i++) {
            double along = look.length() * i / segments;
            double bend = i == segments ? 0 : 0.11;
            double salt = i * 2.1 + look.seed() * 0.07;
            Vec3 point = frame.offset(along, bend * Math.sin(salt), bend * Math.cos(salt * 1.19));
            float crest = (1f - 0.38f * i / segments) * look.fade();
            double width = (0.08 + 0.07 * crest) * (i == segments ? 0.75 : 1);
            mesh.ribbon(previous, point, frame.side(), new Stroke(look.rim(), width + 0.06, (int) (160 * crest)));
            mesh.ribbon(previous, point, frame.up(), new Stroke(look.rim(), width + 0.06, (int) (132 * crest)));
            mesh.ribbon(previous, point, frame.side(), new Stroke(SILVER, width * 0.39, (int) (232 * crest)));
            if (!look.low() && (i == segments / 3 || i == segments * 2 / 3) && along + 0.37 < look.length()) {
                fringe(mesh, frame, point, look.rim(), crest, i % 2 == 0 ? 1 : -1);
            }
            previous = point;
        }
    }

    private static void fringe(Mesh mesh, Frame frame, Vec3 point, Tint tint, float crest, int sign) {
        Vec3 elbow = point.add(frame.offset(0.11, sign * 0.22, 0.08));
        Vec3 tip = elbow.add(frame.offset(0.26, sign * 0.13, -0.13));
        mesh.ribbon(point, elbow, frame.up(), new Stroke(tint, 0.028, (int) (125 * crest)));
        mesh.ribbon(elbow, tip, frame.side(), new Stroke(tint, 0.015, (int) (78 * crest)));
    }

    private static void mask(Mesh mesh, Frame frame, float fade) {
        int alpha = (int) (242 * fade);
        Vec3 left = frame.offset(-0.1, -0.37, 0.14);
        Vec3 right = frame.offset(-0.1, 0.37, 0.14);
        Vec3 skull = frame.offset(-0.1, 0, 0.49);
        Vec3 jaw = frame.offset(-0.06, 0, -0.36);
        Vec3 back = frame.offset(-0.41, 0, 0.03);
        Vec3 nose = frame.offset(-0.016, 0, 0.10);
        mesh.quad(left, skull, right, back, INK, alpha);
        mesh.quad(left, jaw, right, back, INK, alpha);
        mesh.quad(left, skull, nose, jaw, PORCELAIN, alpha);
        mesh.quad(skull, right, jaw, nose, PORCELAIN, alpha);
        for (int sign = -1; sign <= 1; sign += 2) {
            Vec3 socket = frame.offset(-0.006, sign * 0.15, 0.19);
            Vec3 temple = frame.offset(-0.065, sign * 0.28, 0.23);
            mesh.ribbon(socket, temple, frame.up(), new Stroke(RED, 0.04, alpha));
        }
        mesh.ribbon(frame.offset(0.006, -0.12, 0), frame.offset(0.006, 0.12, 0),
                frame.up(), new Stroke(INK, 0.057, alpha));
    }

    @Override
    public ResourceLocation getTextureLocation(GianBeamEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    private record Frame(Vec3 axis, Vec3 side, Vec3 up) {
        private static Frame along(Vec3 direction) {
            Vec3 side = direction.cross(Math.abs(direction.y) > 0.94 ? X : UP).normalize();
            return new Frame(direction, side, side.cross(direction).normalize());
        }

        private Vec3 offset(double along, double width, double height) {
            return axis.scale(along).add(side.scale(width)).add(up.scale(height));
        }
    }

    private record BeamLook(float length, float fade, int seed, Tint rim, boolean low) {
    }

    private record Stroke(Tint tint, double width, int alpha) {
    }

    private record Tint(int red, int green, int blue) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void ribbon(Vec3 from, Vec3 to, Vec3 side, Stroke stroke) {
            Vec3 width = side.scale(stroke.width());
            quad(from.add(width), from.subtract(width), to.subtract(width), to.add(width), stroke.tint(), stroke.alpha());
        }

        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Tint color, int alpha) {
            vertex(a, color, alpha);
            vertex(b, color, alpha);
            vertex(c, color, alpha);
            vertex(d, color, alpha);
        }

        private void vertex(Vec3 point, Tint color, int alpha) {
            buffer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color(color.red(), color.green(), color.blue(), alpha).endVertex();
        }
    }
}
