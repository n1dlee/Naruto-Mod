package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.util.PhoenixVolley;
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

public class FireballJutsuRenderer extends EntityRenderer<FireballJutsuEntity> {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 EAST = new Vec3(1, 0, 0);
    private static final Tone RIM = new Tone(255, 54, 13);
    private static final Tone GOLD = new Tone(255, 159, 33);
    private static final Tone CORE = new Tone(255, 221, 127);
    private static final Tone MIST = new Tone(178, 178, 163);

    public FireballJutsuRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FireballJutsuEntity petal, float yaw, float partial, PoseStack poses, MultiBufferSource buffers, int light) {
        super.render(petal, yaw, partial, poses, buffers, light);
        var game = Minecraft.getInstance();
        boolean low = game.options.graphicsMode().get() == GraphicsStatus.FAST
                || petal.distanceToSqr(game.getCameraEntity() == null ? petal : game.getCameraEntity()) > 34 * 34;
        boolean steam = petal.phase() == FireballJutsuEntity.Phase.STEAM;
        var pass = new Mesh(poses.last().pose(), buffers.getBuffer(steam ? NarutoRenderTypes.WATER_SURFACE
                : NarutoRenderTypes.CHAKRA_GLOW));
        float age = petal.age() + partial;
        Vec3 heading = petal.getLookAngle();
        switch (petal.phase()) {
            case GATHER -> flame(pass, Frame.around(heading), new Heat(age, (0.3 + age / 6.0) * 0.19, 0.55f), low);
            case FLY -> flame(pass, Frame.around(heading), new Heat(age, PhoenixVolley.radius(petal.age()), 1), low);
            case FADE -> flame(pass, Frame.around(heading), new Heat(age, 0.16, fade(age)), low);
            case STEAM -> burst(pass, Frame.around(UP), age, MIST, low);
            case BLOCK, VICTIM -> burst(pass, Frame.around(Vec3.atLowerCornerOf(petal.face().getNormal())), age, GOLD, low);
        }
    }

    private static void flame(Mesh mesh, Frame frame, Heat heat, boolean low) {
        int sides = low ? 4 : 7;
        Vec3 nose = frame.axis().scale(heat.radius());
        for (int i = 0; i < sides; i++) {
            Vec3 a = frame.radial(i, sides, heat.age() * 0.19).scale(heat.radius());
            Vec3 b = frame.radial(i + 1, sides, heat.age() * 0.19).scale(heat.radius());
            double wobble = 0.77 + Math.cos(i * 4.7 + heat.age() * 0.8) * 0.22;
            Vec3 tail = frame.axis().scale(-heat.radius() * (low ? 2.8 : 4.7) * wobble);
            tail = tail.add(a.scale(low ? 0 : 0.35));
            mesh.quad(nose, nose, a, b, i % 3 == 0 ? CORE : GOLD, (int) (194 * heat.fade()));
            mesh.quad(a, b, tail, tail, RIM, (int) (166 * heat.fade()));
            if (!low && i % 2 == 0) {
                Vec3 spark = a.scale(0.61);
                Vec3 distal = tail.scale(0.51);
                mesh.quad(a, spark, distal, distal, GOLD, (int) (136 * heat.fade()));
            }
        }
    }

    private static void burst(Mesh mesh, Frame frame, float age, Tone color, boolean low) {
        int count = low ? 5 : 10;
        double extent = 0.1 + age * 0.13;
        for (int i = 0; i < count; i++) {
            Vec3 outward = frame.radial(i, count, 0);
            Vec3 center = outward.scale(extent * (i % 2 == 0 ? 1 : 0.7)).add(frame.axis().scale(extent * 0.65));
            Vec3 inner = center.scale(0.42);
            Vec3 band = frame.radial(i + 2, count, 0).scale(0.032);
            mesh.quad(inner, center.add(band), center.add(outward.scale(0.04)), center.subtract(band),
                    color, (int) (192 * fade(age)));
        }
    }

    private static float fade(float age) {
        return Mth.clamp(1 - age / PhoenixVolley.FADE_TICKS, 0, 1);
    }

    @Override
    public ResourceLocation getTextureLocation(FireballJutsuEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    private record Frame(Vec3 axis, Vec3 right, Vec3 up) {
        private static Frame around(Vec3 direction) {
            Vec3 right = direction.cross(Math.abs(direction.y) > 0.93 ? EAST : UP).normalize();
            return new Frame(direction, right, direction.cross(right).normalize());
        }

        private Vec3 radial(int index, int count, double phase) {
            double angle = index * Math.PI * 2 / count + phase;
            return right.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
        }
    }

    private record Heat(float age, double radius, float fade) {
    }

    private record Tone(int red, int green, int blue) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Tone tone, int alpha) {
            vertex(a, tone, alpha);
            vertex(b, tone, alpha);
            vertex(c, tone, alpha);
            vertex(d, tone, alpha);
        }

        private void vertex(Vec3 point, Tone tone, int alpha) {
            buffer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color(tone.red(), tone.green(), tone.blue(), alpha).endVertex();
        }
    }
}
