package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.client.model.jutsu.FireballJutsuModel;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.entity.jutsuprojectile.GreatFireballEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.GreatFireballEntity.Phase;
import com.sekwah.narutomod.util.FireballShape;
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

public final class GreatFireballRenderer extends EntityRenderer<GreatFireballEntity> {
    static final ResourceLocation SKIN = new ResourceLocation("narutomod", "textures/entity/jutsu/projectiles/fireball.png");
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private final FireballJutsuModel shell;

    public GreatFireballRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shell = new FireballJutsuModel(context.bakeLayer(FireballJutsuModel.LAYER_LOCATION));
    }

    @Override
    public void render(GreatFireballEntity entity, float yaw, float partialTick, PoseStack stack,
                       MultiBufferSource buffers, int light) {
        var phase = entity.phase();
        float age = entity.phaseAge() + partialTick;
        if (phase == Phase.FLIGHT || phase == Phase.EXHALE) {
            this.renderBody(entity, yaw, partialTick, stack, buffers, age);
        } else {
            renderContact(entity, stack.last().pose(), buffers, age);
        }
        super.render(entity, yaw, partialTick, stack, buffers, light);
    }

    private void renderBody(GreatFireballEntity entity, float yaw, float partialTick, PoseStack stack,
                            MultiBufferSource buffers, float age) {
        boolean exhale = entity.phase() == Phase.EXHALE;
        float radius = (float) FireballShape.radius(entity.charge(), exhale ? 0 : age);
        radius *= exhale ? Mth.clamp(0.38f + age * 0.22f, 0f, 1f) : 1.0f;
        stack.pushPose();
        stack.mulPose(Axis.YP.rotationDegrees(-yaw));
        stack.mulPose(Axis.XP.rotationDegrees(Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot())));
        VertexConsumer glow = buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW);
        drawPlume(stack.last().pose(), glow, radius, age, exhale);
        stack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 10.5f));
        float scale = radius / 0.77f;
        stack.scale(scale, scale, scale);
        var fire = buffers.getBuffer(RenderType.entityTranslucentEmissive(SKIN));
        this.shell.renderToBuffer(stack, fire, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                1.0f, 0.64f, 0.35f, 0.89f);
        stack.scale(0.61f, 0.61f, 0.61f);
        this.shell.renderToBuffer(stack, fire, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                1.0f, 0.96f, 0.68f, 0.83f);
        stack.popPose();
    }

    private static void drawPlume(Matrix4f matrix, VertexConsumer glow, float radius, float age, boolean exhale) {
        for (int i = 0; i < 8; i++) {
            double angle = i * Mth.TWO_PI / 8 + age * 0.17;
            Vec3 center = new Vec3(Math.cos(angle) * radius, Math.sin(angle) * radius, 0);
            Vec3 spread = center.cross(new Vec3(0, 0, 1)).normalize().scale(radius * 0.24);
            Vec3 end = center.scale(exhale ? 0.45 : 0.31).add(0, 0, (exhale ? 1.50 : -1.67) * radius);
            streak(matrix, glow, center, end, spread, exhale ? 0.79f : 0.66f, false);
        }
    }

    private static void renderContact(GreatFireballEntity entity, Matrix4f matrix,
                                      MultiBufferSource buffers, float age) {
        float fade = Mth.clamp(1.0f - age / 9.0f, 0, 1);
        boolean steam = entity.phase() == Phase.STEAM || entity.phase() == Phase.FADE;
        VertexConsumer buffer = buffers.getBuffer(steam ? NarutoRenderTypes.WATER_SURFACE : NarutoRenderTypes.CHAKRA_GLOW);
        Vec3 axis = steam ? UP : Vec3.atLowerCornerOf(entity.face().getNormal());
        Vec3 side = axis.cross(Math.abs(axis.y) > 0.85 ? new Vec3(1, 0, 0) : UP).normalize();
        Vec3 across = axis.cross(side);
        float intensity = entity.phase() == Phase.FADE ? 0.32f : 1.0f;
        double size = 0.64 + entity.charge() / 70.0;
        for (int i = 0; i < 12; i++) {
            double a = i * Mth.TWO_PI / 12;
            Vec3 radial = side.scale(Math.cos(a)).add(across.scale(Math.sin(a)));
            Vec3 start = radial.scale((0.16 + age * 0.06) * size);
            Vec3 end = radial.scale((0.43 + age * (steam ? 0.10 : 0.18)) * size)
                    .add(axis.scale(age * (steam ? 0.15 : 0.095)))
                    .add(0, steam ? 0.06 : 0.11 * age - 0.018 * age * age, 0);
            Vec3 width = axis.cross(radial).scale(size * (steam ? 0.19 : 0.12));
            streak(matrix, buffer, start, end, width, fade * intensity, steam);
        }
    }

    private static void streak(Matrix4f matrix, VertexConsumer buffer, Vec3 from, Vec3 to,
                               Vec3 width, float fade, boolean steam) {
        float r = steam ? 0.78f : 1.0f;
        float g = steam ? 0.86f : 0.69f;
        float b = steam ? 0.92f : 0.21f;
        float alpha = steam ? fade * 0.39f : fade * 0.80f;
        vertex(matrix, buffer, from.subtract(width), r, g, b, alpha);
        vertex(matrix, buffer, from.add(width), r, g, b, alpha * 0.83f);
        vertex(matrix, buffer, to.add(width.scale(0.25)), r, g * 0.73f, b, alpha * 0.19f);
        vertex(matrix, buffer, to.subtract(width.scale(0.25)), r, g * 0.73f, b, alpha * 0.05f);
    }

    private static void vertex(Matrix4f matrix, VertexConsumer buffer, Vec3 point,
                               float red, float green, float blue, float alpha) {
        buffer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z).color(red, green, blue, alpha).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(GreatFireballEntity entity) {
        return SKIN;
    }
}
