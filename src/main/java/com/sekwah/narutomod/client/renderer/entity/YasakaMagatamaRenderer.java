package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.entity.jutsuprojectile.YasakaMagatamaEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class YasakaMagatamaRenderer extends EntityRenderer<YasakaMagatamaEntity> {
    private static final ResourceLocation LEGACY_TEXTURE = new ResourceLocation(NarutoMod.MOD_ID, "textures/yasaka_magatama.png");

    public YasakaMagatamaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(YasakaMagatamaEntity entity) {
        return LEGACY_TEXTURE;
    }

    @Override
    public void render(YasakaMagatamaEntity seal, float yaw, float partial, PoseStack poses, MultiBufferSource buffers, int light) {
        Vec3 direction = seal.getDeltaMovement().normalize();
        poses.pushPose();
        try {
            if (direction.lengthSqr() > 0.1) {
                poses.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, 1), new Vector3f((float) direction.x, (float) direction.y, (float) direction.z)));
            }
            poses.mulPose(Axis.ZP.rotationDegrees((seal.tickCount + partial) * 12.5f));
            float scale = seal.getScale();
            poses.scale(scale, scale, scale);
            renderSeal(poses.last().pose(), buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE), seal.getTint());
        } finally {
            poses.popPose();
        }
        super.render(seal, yaw, partial, poses, buffers, light);
    }

    public static void renderSeal(Matrix4f pose, VertexConsumer sink, int tint) {
        profile(new Mesh(pose, sink), tint);
    }

    private static void profile(Mesh mesh, int tint) {
        for (int i = 0; i < 15; i++) {
            double a = i / 15.0;
            double b = (i + 1) / 15.0;
            for (int side = 0; side < 8; side++) {
                double c = side * Math.PI / 4;
                double d = (side + 1) * Math.PI / 4;
                int tone = side % 4 < 2 ? tint : tint >>> 1 & 0x7F7F7F;
                mesh.quad(point(a, c), point(a, d), point(b, d), point(b, c), tone);
            }
        }
        for (int side : new int[]{-1, 1}) {
            double z = side * 0.18;
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6;
                double b = (i + 1) * Math.PI / 6;
                Vec3 center = new Vec3(0.08, 0.30, z);
                mesh.quad(center, center.add(Math.cos(a) * 0.091, Math.sin(a) * 0.091, 0),
                        center.add(Math.cos(b) * 0.091, Math.sin(b) * 0.091, 0), center, 0x140C25);
            }
        }
    }

    private static Vec3 point(double t, double face) {
        double angle = 1.25 + t * 3.95;
        double thick = 0.23 * Math.pow(1 - t, 0.83);
        double distance = 0.30 + Math.cos(face) * thick;
        return new Vec3(Math.cos(angle) * distance, Math.sin(angle) * distance, Math.sin(face) * thick * 0.70);
    }

    private record Mesh(Matrix4f pose, VertexConsumer vertices) {
        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
            put(a, color);
            put(b, color);
            put(c, color);
            put(d, color);
        }

        private void put(Vec3 v, int color) {
            vertices.vertex(pose, (float) v.x, (float) v.y, (float) v.z)
                    .color((color >>> 16) & 255, (color >>> 8) & 255, color & 255, 233).endVertex();
        }
    }
}
