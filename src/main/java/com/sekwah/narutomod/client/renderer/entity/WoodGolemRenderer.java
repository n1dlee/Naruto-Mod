package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.entity.WoodGolemEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * The golem is a hand-painted humanoid skin blown up to giant size.
 *
 * The 1.12.2 mod's own ModelWoodGolem was a ModelBiped subclass, so its texture is laid
 * out as an ordinary 64x64 player skin (at 8x resolution) - which means the vanilla
 * humanoid model already fits it exactly, and converting the Java model would have bought
 * nothing but work.
 */
public class WoodGolemRenderer extends HumanoidMobRenderer<WoodGolemEntity, HumanoidModel<WoodGolemEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/wood_golem.png");

    /** Roughly two and a half players tall, matching the 4.5-block hitbox. */
    private static final float SCALE = 2.4f;

    public WoodGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.9F);
    }

    @Override
    protected void scale(WoodGolemEntity golem, PoseStack poseStack, float partialTicks) {
        // Rises out of the ground over its first second and a half rather than popping in.
        float grown = SCALE * (0.15f + 0.85f * golem.getGrowthProgress());
        poseStack.scale(grown, grown, grown);
    }

    @Override
    public ResourceLocation getTextureLocation(WoodGolemEntity entity) {
        return TEXTURE;
    }
}
