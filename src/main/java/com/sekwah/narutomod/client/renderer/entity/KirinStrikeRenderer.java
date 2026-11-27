package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.model.entity.DragonHeadModel;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.entity.jutsuprojectile.KirinStrikeEntity;
import com.sekwah.narutomod.util.KirinTimeline;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class KirinStrikeRenderer extends EntityRenderer<KirinStrikeEntity> {
    private static final ResourceLocation SKIN = new ResourceLocation(NarutoMod.MOD_ID, "textures/dragon_lightning.png");
    private final DragonHeadModel head;

    public KirinStrikeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.head = new DragonHeadModel(context.bakeLayer(DragonHeadModel.LAYER_LOCATION));
    }

    @Override
    public void render(KirinStrikeEntity entity, float yaw, float partialTick, PoseStack stack,
                       MultiBufferSource buffers, int light) {
        float age = entity.age() + partialTick;
        boolean low = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        Matrix4f matrix = stack.last().pose();
        if (age < KirinTimeline.WARNING) {
            cloud(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE), entity.cloudHeight(), age);
        }
        VertexConsumer glow = buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW);
        float fade = 1.0f - Mth.clamp((age - KirinTimeline.CONTACT) / KirinTimeline.RESIDUE, 0, 1);
        ring(matrix, glow, entity.radius(), age, fade, low ? 16 : 28);
        if (age >= KirinTimeline.WARNING) {
            double headHeight = KirinTimeline.headHeight(age, entity.cloudHeight());
            column(matrix, glow, entity.cloudHeight(), headHeight, age, fade, low ? 3 : 6);
            if (age < KirinTimeline.CONTACT + 1.0f) {
                this.drawHead(stack, buffers, headHeight, fade);
            }
        }
        super.render(entity, yaw, partialTick, stack, buffers, light);
    }

    private void drawHead(PoseStack stack, MultiBufferSource buffers, double y, float alpha) {
        stack.pushPose();
        stack.translate(0, y + 0.93, 0);
        stack.mulPose(Axis.XP.rotationDegrees(90.0f));
        stack.mulPose(Axis.YP.rotationDegrees(180.0f));
        stack.scale(-0.87f, -0.87f, 0.87f);
        this.head.setJawOpening(0.51f);
        this.head.renderToBuffer(stack, buffers.getBuffer(RenderType.entityTranslucentEmissive(SKIN)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0.78f, 0.90f, 1.0f, alpha);
        this.head.setJawOpening(0);
        stack.popPose();
    }

    private static void cloud(Matrix4f matrix, VertexConsumer buffer, double y, float age) {
        for (int w = 0; w < 3; w++) {
            double radius = 2.3 + w * 1.15 + age * 0.055;
            double level = y + w * 0.47;
            Vec3 center = new Vec3(0, level + 0.4, 0);
            for (int i = 0; i < 18; i++) {
                double start = i * Mth.TWO_PI / 18 + age * 0.03;
                double stop = start + Mth.TWO_PI / 18;
                Vec3 a = polar(radius * (0.91 + 0.09 * Math.cos(start * 3)), level, start);
                Vec3 b = polar(radius * (0.91 + 0.09 * Math.cos(stop * 3)), level, stop);
                vertex(matrix, buffer, center, 0.04f, 0.075f, 0.17f, 0.50f);
                vertex(matrix, buffer, center, 0.04f, 0.075f, 0.17f, 0.50f);
                vertex(matrix, buffer, a, 0.19f, 0.30f, 0.52f, 0);
                vertex(matrix, buffer, b, 0.19f, 0.30f, 0.52f, 0);
            }
        }
    }

    private static void ring(Matrix4f matrix, VertexConsumer buffer, float radius, float age, float fade, int segments) {
        float strength = age < KirinTimeline.WARNING ? 0.45f + 0.49f * age / KirinTimeline.WARNING : fade;
        double turn = age * 0.061;
        for (int part = 0; part < segments; part++) {
            double a = turn + part * Mth.TWO_PI / segments;
            double b = a + Mth.TWO_PI / segments * 0.62;
            Vec3 start = polar(radius - 0.07, 0.024, a);
            Vec3 end = polar(radius - 0.07, 0.024, b);
            Vec3 wing = polar(0.082, 0, a);
            strip(matrix, buffer, start, end, wing, strength * (part % 3 == 0 ? 0.85f : 0.47f), false);
        }
    }

    private static void column(Matrix4f matrix, VertexConsumer buffer, float cloud, double headY,
                               float age, float fade, int forks) {
        int segments = 9;
        Vec3 previous = new Vec3(0, cloud, 0);
        for (int i = 1; i <= segments; i++) {
            float t = i / (float) segments;
            double slack = Math.sin(t * Math.PI) * 1.18;
            Vec3 next = new Vec3(slack * Mth.sin(i * 8.9f + age * 2.1f),
                    Mth.lerp(t, cloud, headY), slack * Mth.cos(i * 7.6f - age * 2.4f));
            lightning(matrix, buffer, previous, next, fade, (float) (0.27 + (1 - t) * 0.46));
            if (i <= forks) {
                Vec3 branch = next.add(polar(1.7 + i * 0.48, -1.9, i * 2.41 + age * 0.24));
                Vec3 elbow = next.lerp(branch, 0.42).add(0.43, 0.72, -0.18);
                lightning(matrix, buffer, next, elbow, fade * 0.63f, 0.16f);
                lightning(matrix, buffer, elbow, branch, fade * 0.49f, 0.08f);
            }
            previous = next;
        }
    }

    private static void lightning(Matrix4f matrix, VertexConsumer buffer, Vec3 start, Vec3 end, float alpha, float radius) {
        Vec3 x = new Vec3(radius, 0, 0);
        Vec3 z = new Vec3(0, 0, radius);
        strip(matrix, buffer, start, end, x, alpha * 0.45f, false);
        strip(matrix, buffer, start, end, z, alpha * 0.45f, false);
        strip(matrix, buffer, start, end, x.scale(0.40), alpha, true);
        strip(matrix, buffer, start, end, z.scale(0.40), alpha, true);
    }

    private static void strip(Matrix4f matrix, VertexConsumer buffer, Vec3 a, Vec3 b, Vec3 width,
                              float alpha, boolean white) {
        float r = white ? 0.91f : 0.19f;
        float g = white ? 0.97f : 0.62f;
        vertex(matrix, buffer, a.add(width), r, g, 1, alpha);
        vertex(matrix, buffer, a.subtract(width), r, g, 1, alpha);
        vertex(matrix, buffer, b.subtract(width.scale(0.72)), r, g, 1, alpha * 0.72f);
        vertex(matrix, buffer, b.add(width.scale(0.72)), r, g, 1, alpha * 0.72f);
    }

    private static Vec3 polar(double radius, double y, double angle) {
        return new Vec3(radius * Math.cos(angle), y, radius * Math.sin(angle));
    }

    private static void vertex(Matrix4f matrix, VertexConsumer buffer, Vec3 at, float red, float green,
                               float blue, float alpha) {
        buffer.vertex(matrix, (float) at.x, (float) at.y, (float) at.z).color(red, green, blue, alpha).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(KirinStrikeEntity entity) {
        return SKIN;
    }
}
