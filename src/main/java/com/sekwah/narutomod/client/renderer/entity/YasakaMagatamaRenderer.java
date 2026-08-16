package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.entity.jutsuprojectile.YasakaMagatamaEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * The magatama seal: a spinning billboard, exactly as the 1.12.2 renderer drew it.
 *
 * There is no model to bake - the original has no model class either. The whole effect is one
 * textured quad turned to face the camera and spun about its own axis as it travels, which at
 * the speed these move reads as a seal tumbling through the air.
 */
public class YasakaMagatamaRenderer extends EntityRenderer<YasakaMagatamaEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/yasaka_magatama.png");

    /** Turns per second in flight. Fast enough to blur, slow enough to read as a shape. */
    private static final float SPIN_DEGREES_PER_TICK = 17.0f;

    public YasakaMagatamaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(YasakaMagatamaEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(YasakaMagatamaEntity magatama, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float size = magatama.getScale();
        int tint = magatama.getTint();
        float red = ((tint >> 16) & 0xFF) / 255f;
        float green = ((tint >> 8) & 0xFF) / 255f;
        float blue = (tint & 0xFF) / 255f;

        poseStack.pushPose();
        // Face the camera, then spin in the plane of the billboard so the seal tumbles rather
        // than sliding along flat.
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotationDegrees((magatama.tickCount + partialTick) * SPIN_DEGREES_PER_TICK));
        poseStack.scale(size, size, size);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        // Fullbright: the seal is made of chakra and should not be dimmed by the cave it is
        // flying through.
        int light = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;

        quad(matrix, consumer, red, green, blue, light);
        poseStack.popPose();

        super.render(magatama, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void quad(Matrix4f matrix, VertexConsumer consumer,
                             float red, float green, float blue, int light) {
        put(matrix, consumer, -0.5f, -0.5f, 0f, 1f, red, green, blue, light);
        put(matrix, consumer, 0.5f, -0.5f, 1f, 1f, red, green, blue, light);
        put(matrix, consumer, 0.5f, 0.5f, 1f, 0f, red, green, blue, light);
        put(matrix, consumer, -0.5f, 0.5f, 0f, 0f, red, green, blue, light);
    }

    private static void put(Matrix4f matrix, VertexConsumer consumer, float x, float y,
                            float u, float v, float red, float green, float blue, int light) {
        consumer.vertex(matrix, x, y, 0f)
                .color(red, green, blue, 0.95f)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0f, 0f, 1f)
                .endVertex();
    }
}
