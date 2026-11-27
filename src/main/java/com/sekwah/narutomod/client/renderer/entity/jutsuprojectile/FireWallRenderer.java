package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.entity.jutsuprojectile.FireWallEntity;
import com.sekwah.narutomod.util.FireFront;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class FireWallRenderer extends EntityRenderer<FireWallEntity> {
    private static final Vec3 FORWARD = new Vec3(0, 0, 1);

    public FireWallRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FireWallEntity entity, float yaw, float partialTick, PoseStack stack,
                       MultiBufferSource buffers, int light) {
        float age = entity.age() + partialTick;
        stack.pushPose();
        stack.mulPose(Axis.YP.rotationDegrees(-yaw));
        Matrix4f matrix = stack.last().pose();
        if (age < 0) {
            if (entity.primary()) {
                charge(matrix, buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW), age);
            }
        } else {
            double depth = FireFront.depth(age);
            float fade = 1f - Mth.clamp((age - 14f) / 8f, 0f, 1f);
            smoke(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE), entity, depth, fade);
            flames(matrix, buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW), entity, age, depth, fade);
        }
        stack.popPose();
        super.render(entity, yaw, partialTick, stack, buffers, light);
    }

    private static void charge(Matrix4f matrix, VertexConsumer buffer, float age) {
        float fill = Mth.clamp(1.0f + age / FireFront.WINDUP, 0, 1);
        double size = 0.23 + fill * 0.56;
        for (int i = 0; i < 8; i++) {
            double theta = i * Mth.TWO_PI / 8 + age * 0.38;
            double end = theta + 0.56;
            Vec3 a = new Vec3(Math.cos(theta) * size, 1.43 + Math.sin(theta) * size * 0.55, 0.32);
            Vec3 b = new Vec3(Math.cos(end) * size, 1.43 + Math.sin(end) * size * 0.55, 0.32);
            Vec3 core = new Vec3(0, 1.43, 0.62);
            vertex(matrix, buffer, a, 1, 0.31f, 0.05f, fill * 0.64f);
            vertex(matrix, buffer, b, 1, 0.52f, 0.13f, fill * 0.58f);
            vertex(matrix, buffer, core, 1, 0.95f, 0.52f, fill * 0.63f);
            vertex(matrix, buffer, core, 1, 0.95f, 0.52f, fill * 0.63f);
        }
    }

    private static void flames(Matrix4f matrix, VertexConsumer buffer, FireWallEntity entity, float age,
                               double depth, float fade) {
        for (int lane = 0; lane < FireFront.LANES; lane++) {
            if (entity.stop(lane) + 0.02 < depth) {
                continue;
            }
            Vec3 foot = FireFront.point(Vec3.ZERO, FORWARD, lane, depth, 0.06);
            double halfWidth = Math.max(0.14, FireFront.laneOffset(6, depth) * 0.5);
            Vec3 a = foot.add(-halfWidth, 0, 0);
            Vec3 b = foot.add(halfWidth, 0, 0);
            Vec3 tipA = crest(a, lane, age);
            Vec3 tipB = crest(b, lane + 1, age);
            sheet(matrix, buffer, new Sheet(a, b, tipA, tipB), fade);
            sheet(matrix, buffer, new Sheet(a.add(0, 0.18, -0.31), b.add(0, 0.18, -0.31),
                    a.lerp(tipA, 0.62).add(0, 0, -0.24), b.lerp(tipB, 0.62).add(0, 0, -0.24)), fade * 0.64f);
        }
        for (int lane = 0; lane < FireFront.LANES; lane++) {
            double cap = entity.stop(lane);
            if (depth > cap && depth - cap < 3.3 && !entity.quenched(lane)) {
                Vec3 foot = FireFront.point(Vec3.ZERO, FORWARD, lane, cap, 0.18);
                sheet(matrix, buffer, new Sheet(foot.add(-0.22, 0, 0), foot.add(0.22, 0, 0),
                        foot.add(-0.08, 2.8, -0.45), foot.add(0.13, 3.2, -0.32)),
                        fade * (float) (1.0 - (depth - cap) / 3.3));
            }
        }
    }

    private static Vec3 crest(Vec3 foot, int lane, float age) {
        double rise = FireFront.HEIGHT * (0.64 + 0.32 * Math.abs(Mth.sin(age * 1.1f + lane * 2.5f)));
        return foot.add(Mth.sin(lane + age * 0.87f) * 0.16, rise, -0.22 - rise * 0.21);
    }

    private static void sheet(Matrix4f matrix, VertexConsumer buffer, Sheet sheet, float alpha) {
        vertex(matrix, buffer, sheet.a(), 1, 0.89f, 0.39f, alpha * 0.88f);
        vertex(matrix, buffer, sheet.b(), 1, 0.71f, 0.22f, alpha * 0.84f);
        vertex(matrix, buffer, sheet.topB(), 1, 0.29f, 0.05f, alpha * 0.08f);
        vertex(matrix, buffer, sheet.topA(), 1, 0.36f, 0.08f, 0);
    }

    private record Sheet(Vec3 a, Vec3 b, Vec3 topA, Vec3 topB) {
    }

    private static void smoke(Matrix4f matrix, VertexConsumer buffer, FireWallEntity entity, double depth, float fade) {
        for (int lane = 0; lane < FireFront.LANES - 1; lane++) {
            double stop = Math.min(entity.stop(lane), entity.stop(lane + 1));
            if ((depth > stop + 3.0) || depth < 0.12) {
                continue;
            }
            boolean steam = entity.quenched(lane) || entity.quenched(lane + 1);
            double origin = Math.min(depth, stop);
            Vec3 a = FireFront.point(Vec3.ZERO, FORWARD, lane, origin, steam ? 0.10 : 1.30);
            Vec3 b = FireFront.point(Vec3.ZERO, FORWARD, lane + 1, origin, steam ? 0.10 : 1.30);
            float shade = steam ? 0.64f : 0.09f;
            vertex(matrix, buffer, a, shade, shade * 0.94f, shade, fade * 0.48f);
            vertex(matrix, buffer, b, shade, shade * 0.94f, shade, fade * 0.42f);
            vertex(matrix, buffer, b.add(0.25, 1.96, -1.28), shade, shade, shade, 0);
            vertex(matrix, buffer, a.add(-0.25, 2.13, -1.28), shade, shade, shade, 0);
        }
    }

    private static void vertex(Matrix4f matrix, VertexConsumer buffer, Vec3 point,
                               float red, float green, float blue, float alpha) {
        buffer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z).color(red, green, blue, alpha).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(FireWallEntity entity) {
        return GreatFireballRenderer.SKIN;
    }
}
