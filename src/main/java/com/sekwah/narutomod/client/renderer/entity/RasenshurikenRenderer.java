package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.entity.jutsuprojectile.RasenshurikenEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Draws the Rasenshuriken in flight.
 *
 * It was registered to NoopRenderer, which is why the technique looked broken: the throw
 * always worked and the entity always flew, there was simply nothing on screen to see. Which
 * makes the jutsu with the longest cooldown in the mod read as a jutsu that does nothing.
 *
 * Two camera-facing quads: the shuriken disc itself, spinning fast, and a larger, fainter
 * wind halo counter-rotating behind it. Billboarding rather than a baked model because the
 * thing is a flat spinning disc of wind - a model would only add faces nobody ever sees.
 */
public class RasenshurikenRenderer extends EntityRenderer<RasenshurikenEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/rasenshuriken.png");
    /** Translucent+emissive so it glows against dark terrain instead of going grey. */
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucentEmissive(TEXTURE);

    private static final float DISC_RADIUS = 1.1f;
    private static final float HALO_RADIUS = 1.9f;
    /** Degrees per tick. Fast enough to blur, slow enough not to strobe. */
    private static final float SPIN_SPEED = 47f;

    public RasenshurikenRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RasenshurikenEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float age = entity.tickCount + partialTick;

        poseStack.pushPose();
        // Face the camera, so the disc always reads as a disc no matter where it is thrown.
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));

        VertexConsumer consumer = bufferSource.getBuffer(RENDER_TYPE);

        // Halo first and counter-spinning: two layers turning opposite ways is what sells
        // "wind being shredded" rather than "a texture rotating".
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(-age * SPIN_SPEED * 0.35f));
        quad(poseStack, consumer, HALO_RADIUS, packedLight, 0.55f, 0.80f, 1.0f, 0.30f);
        poseStack.popPose();

        poseStack.mulPose(Axis.ZP.rotationDegrees(age * SPIN_SPEED));
        // A slight breathing pulse keeps it alive while it crosses open ground.
        float pulse = 1.0f + Mth.sin(age * 0.9f) * 0.05f;
        poseStack.scale(pulse, pulse, pulse);
        quad(poseStack, consumer, DISC_RADIUS, packedLight, 1.0f, 1.0f, 1.0f, 0.95f);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void quad(PoseStack poseStack, VertexConsumer consumer, float radius, int packedLight,
                      float red, float green, float blue, float alpha) {
        Matrix4f pose = poseStack.last().pose();
        var normal = poseStack.last().normal();
        vertex(consumer, pose, normal, -radius, -radius, 0f, 1f, red, green, blue, alpha, packedLight);
        vertex(consumer, pose, normal, radius, -radius, 1f, 1f, red, green, blue, alpha, packedLight);
        vertex(consumer, pose, normal, radius, radius, 1f, 0f, red, green, blue, alpha, packedLight);
        vertex(consumer, pose, normal, -radius, radius, 0f, 0f, red, green, blue, alpha, packedLight);
    }

    private void vertex(VertexConsumer consumer, Matrix4f pose, org.joml.Matrix3f normal,
                        float x, float y, float u, float v,
                        float red, float green, float blue, float alpha, int packedLight) {
        consumer.vertex(pose, x, y, 0f)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, 0f, 0f, 1f)
                .endVertex();
    }

    @Override
    public int getBlockLightLevel(RasenshurikenEntity entity, net.minecraft.core.BlockPos pos) {
        return 15; // it is a ball of chakra; it lights itself
    }

    @Override
    public ResourceLocation getTextureLocation(RasenshurikenEntity entity) {
        return TEXTURE;
    }
}
