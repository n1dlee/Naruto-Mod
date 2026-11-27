package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.model.entity.IceMirrorModel;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.entity.jutsuprojectile.IceMirrorEntity;
import com.sekwah.narutomod.util.MirrorRing;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class IceMirrorRenderer extends EntityRenderer<IceMirrorEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/ice_mirror.png");
    private static final float HALF = (float) MirrorRing.WIDTH * 0.5f;
    private static final float HEIGHT = (float) IceMirrorEntity.HEIGHT;
    private static final float FACE = 0.061f;
    private final IceMirrorModel model;

    public IceMirrorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new IceMirrorModel(context.bakeLayer(IceMirrorModel.LAYER_LOCATION));
    }

    @Override
    public void render(IceMirrorEntity entity, float entityYaw, float partialTick,
                       PoseStack stack, MultiBufferSource buffers, int packedLight) {
        float rise = MirrorRing.rise(entity.riseTicks() + partialTick);
        float fracture = 1f - Mth.clamp(entity.getMirrorHealth() / IceMirrorEntity.MAX_HEALTH, 0f, 1f);
        float breakup = Mth.clamp((entity.shatterTicks() - 1 + partialTick) / 5f, 0f, 1f);
        stack.pushPose();
        stack.mulPose(Axis.YP.rotationDegrees(180.0f - entity.getFacing()));
        VertexConsumer glow = buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW);
        float time = entity.tickCount + partialTick;
        drawDetail(stack.last().pose(), glow, entity, rise, fracture, breakup, time);
        if (rise > 0.01f) {
            stack.scale(-((float) MirrorRing.WIDTH / 0.75f), -rise * HEIGHT / IceMirrorModel.HEIGHT_BLOCKS, 1.0f);
            VertexConsumer glass = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
            this.model.renderToBuffer(stack, glass, packedLight, OverlayTexture.NO_OVERLAY,
                    0.82f, 0.97f, 1.0f, (0.67f - 0.26f * fracture) * (1f - breakup));
        }
        stack.popPose();
        super.render(entity, entityYaw, partialTick, stack, buffers, packedLight);
    }

    private static void drawDetail(Matrix4f matrix, VertexConsumer glow, IceMirrorEntity entity,
                                   float rise, float fracture, float breakup, float time) {
        float y = Math.max(0.04f, rise * HEIGHT);
        float fade = 1f - breakup;
        line(matrix, glow, at(-HALF, 0.04f), at(HALF, 0.04f), 0.018f, 0.42f * fade);
        line(matrix, glow, at(-HALF, y), at(HALF, y), 0.025f, (0.73f - 0.27f * rise) * fade);
        if (rise > 0.06f) {
            line(matrix, glow, at(-HALF, 0.04f), at(-HALF, y), 0.024f, 0.44f * fade);
            line(matrix, glow, at(HALF, 0.04f), at(HALF, y), 0.024f, 0.44f * fade);
            float sweep = Mth.lerp((time * 0.051f) % 1.0f, -HALF + 0.14f, HALF - 0.14f);
            line(matrix, glow, at(sweep - 0.14f, 0.07f), at(sweep + 0.14f, y - 0.05f),
                    0.042f, (0.20f + entity.reflectionTicks() * 0.06f) * fade);
        }
        if (fracture > 0.03f) {
            cracks(matrix, glow, y, fracture, fade, breakup);
        }
        if (entity.reflectionTicks() > 0) {
            shade(matrix, glow, y, entity.reflectionTicks() / 7.0f);
        }
    }

    private static void cracks(Matrix4f matrix, VertexConsumer buffer, float height, float damage, float fade, float breakup) {
        for (int i = 0; i < 5; i++) {
            float sign = i % 2 == 0 ? 1 : -1;
            Vec3 center = at(0.05f, height * 0.52f);
            Vec3 join = at(sign * (0.18f + i * 0.11f), height * (0.22f + 0.15f * i));
            Vec3 tip = at(sign * (0.63f + 0.67f * damage + breakup * 0.35f),
                    height * (0.13f + 0.17f * i));
            line(matrix, buffer, center, join, 0.016f, damage * fade);
            line(matrix, buffer, join, tip, 0.012f, damage * 0.77f * fade);
        }
    }

    private static void shade(Matrix4f matrix, VertexConsumer buffer, float height, float alpha) {
        line(matrix, buffer, at(0, height * 0.87f), at(0, height * 0.73f), 0.11f, alpha * 0.62f);
        line(matrix, buffer, at(0, height * 0.70f), at(0, height * 0.39f), 0.20f, alpha * 0.46f);
        line(matrix, buffer, at(-0.08f, height * 0.40f), at(-0.25f, height * 0.15f), 0.065f, alpha * 0.38f);
        line(matrix, buffer, at(0.08f, height * 0.40f), at(0.24f, height * 0.15f), 0.065f, alpha * 0.38f);
    }

    private static Vec3 at(float x, float y) {
        return new Vec3(x, y, FACE);
    }

    private static void line(Matrix4f matrix, VertexConsumer buffer, Vec3 a, Vec3 b, float width, float alpha) {
        Vec3 side = new Vec3(-(b.y - a.y), b.x - a.x, 0).normalize().scale(width);
        for (int sign = -1; sign <= 1; sign += 2) {
            vertex(matrix, buffer, a.add(side), sign, alpha);
            vertex(matrix, buffer, a.subtract(side), sign, alpha);
            vertex(matrix, buffer, b.subtract(side), sign, alpha * 0.69f);
            vertex(matrix, buffer, b.add(side), sign, alpha * 0.69f);
        }
    }

    private static void vertex(Matrix4f matrix, VertexConsumer buffer, Vec3 point, int sign, float alpha) {
        buffer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z * sign)
                .color(0.71f, 0.93f, 1f, alpha).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(IceMirrorEntity entity) {
        return TEXTURE;
    }
}
