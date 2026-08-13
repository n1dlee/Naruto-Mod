package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.model.entity.BijudamaModel;
import com.sekwah.narutomod.entity.jutsuprojectile.TailedBeastBombEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/**
 * The Bijudama in flight: the imported orb, tinted with the throwing beast's chakra colour
 * and sized to the blast it is going to make, so what you see is what will hit you.
 *
 * The model is a ball of spikes radiating from the origin, about five model units across, so
 * the scale here turns that into the bomb's actual radius. It spins, because a sphere of
 * identical spokes is otherwise indistinguishable from a still image.
 */
public class TailedBeastBombRenderer extends EntityRenderer<TailedBeastBombEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/biju/bijudama.png");

    /** Diameter of the imported orb in blocks, at scale 1. */
    private static final float MODEL_DIAMETER = 0.3125f;
    /** Drawn a good deal smaller than the blast: the sphere is the core, not the reach. */
    private static final float VISUAL_FRACTION = 0.42f;

    private final BijudamaModel model;

    public TailedBeastBombRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new BijudamaModel(context.bakeLayer(BijudamaModel.LAYER_LOCATION));
    }

    @Override
    public void render(TailedBeastBombEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float diameter = entity.getPower() * 2.0f * VISUAL_FRACTION;
        float scale = diameter / MODEL_DIAMETER;
        float spin = (entity.tickCount + partialTick) * 9.0F;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.mulPose(Axis.XP.rotationDegrees(spin * 0.6F));
        poseStack.scale(-scale, -scale, scale);

        Vector3f colour = entity.getVariant().getChakraColour();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE));
        this.model.renderToBuffer(poseStack, consumer, 15728880, OverlayTexture.NO_OVERLAY,
                colour.x(), colour.y(), colour.z(), 0.85f);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TailedBeastBombEntity entity) {
        return TEXTURE;
    }
}
