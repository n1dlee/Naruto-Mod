package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.entity.jutsuprojectile.GustFrontEntity;
import com.sekwah.narutomod.util.GustFront;
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

public class GustFrontRenderer extends EntityRenderer<GustFrontEntity> {
    private static final Tint RIM = new Tint(209, 241, 226);
    private static final Tint FILM = new Tint(154, 193, 183);
    private static final Tint FOAM = new Tint(233, 252, 243);
    private static final Tint FAN_EDGE = new Tint(219, 232, 243);
    private static final Tint FAN_SHADOW = new Tint(102, 128, 169);
    private static final Tint PALM_EDGE = new Tint(227, 249, 255);
    private static final Tint PALM_BODY = new Tint(129, 191, 230);

    public GustFrontRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GustFrontEntity entity, float yaw, float partial, PoseStack poses, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partial, poses, buffers, light);
        Minecraft game = Minecraft.getInstance();
        boolean low = game.options.graphicsMode().get() == GraphicsStatus.FAST
                || entity.distanceToSqr(game.getCameraEntity() == null ? entity : game.getCameraEntity()) > 37 * 37;
        GustFront.Frame frame = GustFront.frame(entity.getLookAngle());
        var mesh = new Mesh(poses.last().pose(), buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        var shape = entity.shape();
        float age = entity.age() + partial;
        if (age < shape.prepare()) {
            if (entity.isPalm()) {
                palmGather(mesh, frame, age / shape.prepare(), low);
            } else if (entity.isFan()) {
                fanTrace(mesh, frame, age);
            } else {
                this.inhale(mesh, frame, age, low);
            }
            return;
        }
        float travel = age - shape.prepare() - (entity.isPalm() ? 1 : 0);
        double distance = GustFront.distance(travel, entity.range(), shape);
        float fade = 1 - Mth.clamp((travel - shape.flight()) / GustFront.FADE, 0, 1);
        this.edge(mesh, frame, new Crest(distance, fade, entity.bounds(), low, entity.isFan(), shape));
    }

    private static void palmGather(Mesh mesh, GustFront.Frame frame, float t, boolean low) {
        int count = low ? 5 : 9;
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count + t * 1.36;
            Vec3 unit = frame.right().scale(Math.cos(angle)).add(frame.up().scale(Math.sin(angle) * 0.74));
            Vec3 near = unit.scale(0.19 - t * 0.11);
            Vec3 outer = unit.scale(0.30 - t * 0.08).add(frame.forward().scale(0.19));
            mesh.ribbon(near, outer, frame.up().scale(0.006), PALM_EDGE, Math.round(39 + 86 * t));
        }
    }

    private static void fanTrace(Mesh mesh, GustFront.Frame frame, float age) {
        float t = Mth.clamp(age / GustFront.PREPARE, 0, 1);
        Vec3 root = frame.forward().scale(0.16);
        Vec3 from = root.add(frame.right().scale(-0.49 * t)).add(frame.up().scale(-0.32 + 0.25 * t));
        Vec3 to = root.add(frame.right().scale(0.57 * t)).add(frame.up().scale(0.38));
        mesh.ribbon(from, to, frame.forward().scale(0.026), FAN_EDGE, Math.round(116 * t));
        mesh.ribbon(from.add(frame.forward().scale(-0.12)), to, frame.up().scale(0.049), FAN_SHADOW, Math.round(95 * t));
    }

    private void inhale(Mesh mesh, GustFront.Frame frame, float age, boolean low) {
        int count = low ? 5 : 10;
        for (int i = 0; i < count; i++) {
            double angle = i * Math.PI * 2 / count + age * 0.63;
            double radius = Mth.clamp((GustFront.PREPARE - age) / GustFront.PREPARE, 0, 1) * 0.28 + 0.075;
            Vec3 edge = frame.right().scale(radius * Math.cos(angle)).add(frame.up().scale(radius * Math.sin(angle)));
            Vec3 tail = edge.scale(1.55).add(frame.forward().scale(0.24));
            mesh.ribbon(tail, edge.scale(0.38), frame.up().scale(0.023), FILM, 117);
        }
    }

    private void edge(Mesh mesh, GustFront.Frame frame, Crest crest) {
        for (int lane = 0; lane < GustFront.LANES - 1; lane++) {
            double a = Math.min(crest.distance(), crest.bounds()[lane]);
            double b = Math.min(crest.distance(), crest.bounds()[lane + 1]);
            if (Math.min(a, b) < 0.075 || Math.abs(a - b) > 0.74 || crest.distance() - Math.max(a, b) > 0.9) {
                continue;
            }
            Vec3 left = frame.lane(lane, crest.shape()).scale(a);
            Vec3 right = frame.lane(lane + 1, crest.shape()).scale(b);
            double arch = crest.shape().arch(Math.min(left.dot(frame.forward()), right.dot(frame.forward())));
            var section = new Section(left, right, frame.up(), frame.forward(), arch);
            if (crest.shape() == GustFront.Shape.PALM) {
                section.palm(mesh, crest.fade(), crest.low(), lane);
            } else if (crest.fan()) {
                section.cut(mesh, crest.fade(), crest.low(), lane);
            } else {
                section.draw(mesh, crest.fade(), crest.low());
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(GustFrontEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    private record Crest(double distance, float fade, double[] bounds, boolean low, boolean fan, GustFront.Shape shape) {
    }

    private record Section(Vec3 left, Vec3 right, Vec3 up, Vec3 forward, double arch) {
        private void palm(Mesh mesh, float fade, boolean low, int lane) {
            Vec3 upper = up.scale(arch);
            Vec3 lower = up.scale(-arch * 0.77);
            Vec3 aft = forward.scale(-0.22 - arch * 0.47);
            float highlight = fade * (float) (1 - Math.abs(lane + 0.5 - (GustFront.LANES - 1) * 0.5) * 0.12);
            mesh.quad(left.add(upper), right.add(upper), right.add(lower), left.add(lower), PALM_BODY, (int) (53 * highlight));
            mesh.ribbon(left, right, up.scale(0.014), PALM_EDGE, (int) (228 * highlight));
            mesh.ribbon(left.add(upper), right.add(upper), up.scale(0.009), PALM_EDGE, (int) (124 * highlight));
            mesh.ribbon(left.add(lower), right.add(lower), up.scale(0.009), PALM_EDGE, (int) (91 * highlight));
            if (!low) {
                mesh.quad(left.add(upper), right.add(upper), right.add(aft).add(upper.scale(0.32)),
                        left.add(aft).add(upper.scale(0.32)), PALM_EDGE, (int) (47 * highlight));
                mesh.ribbon(left.add(aft), right.add(aft), up.scale(0.010), PALM_BODY, (int) (108 * highlight));
            }
        }

        private void draw(Mesh mesh, float fade, boolean low) {
            Vec3 top = up.scale(arch);
            Vec3 trail = forward.scale(-0.44 - arch * 0.33);
            Vec3 lower = up.scale(-arch * 0.77);
            mesh.quad(left.add(top), right.add(top), right.add(lower), left.add(lower), FILM, (int) (57 * fade));
            mesh.ribbon(left, right, up.scale(0.045), RIM, (int) (142 * fade));
            mesh.ribbon(left.add(top.scale(0.78)), right.add(top.scale(0.78)), up.scale(0.04), FOAM, (int) (96 * fade));
            if (!low) {
                mesh.quad(left.add(top), right.add(top), right.add(trail).add(top.scale(0.54)),
                        left.add(trail).add(top.scale(0.54)), RIM, (int) (52 * fade));
                mesh.ribbon(left.add(trail).add(lower), right.add(trail).add(lower), up.scale(0.022),
                        RIM, (int) (102 * fade));
            }
        }

        private void cut(Mesh mesh, float fade, boolean low, int lane) {
            Vec3 a = left.add(up.scale((lane - 5) * (0.05 + arch * 0.06)));
            Vec3 b = right.add(up.scale((lane - 4) * (0.05 + arch * 0.06)));
            Vec3 back = forward.scale(-0.54 - arch * 0.37);
            mesh.quad(a, b, b.add(back).add(up.scale(-0.13)), a.add(back).add(up.scale(0.16)), FAN_SHADOW, (int) (89 * fade));
            mesh.ribbon(a, b, up.scale(0.042), FAN_EDGE, (int) (202 * fade));
            if (!low) {
                mesh.ribbon(a.add(back).add(up.scale(0.22)), b.add(back.scale(0.53)).add(up.scale(0.22)),
                        up.scale(0.014), FAN_EDGE, (int) (112 * fade));
            }
        }
    }

    private record Tint(int r, int g, int b) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void ribbon(Vec3 from, Vec3 to, Vec3 width, Tint tint, int alpha) {
            quad(from.add(width), to.add(width), to.subtract(width), from.subtract(width), tint, alpha);
        }

        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Tint tint, int alpha) {
            vertex(a, tint, alpha);
            vertex(b, tint, alpha);
            vertex(c, tint, alpha);
            vertex(d, tint, alpha);
        }

        private void vertex(Vec3 point, Tint tint, int alpha) {
            buffer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color(tint.r(), tint.g(), tint.b(), alpha).endVertex();
        }
    }
}
