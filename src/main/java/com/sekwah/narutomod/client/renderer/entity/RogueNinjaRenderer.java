package com.sekwah.narutomod.client.renderer.entity;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.entity.RogueNinjaEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/** Draws a rogue ninja with the village skin picked by its variant byte. */
public class RogueNinjaRenderer extends HumanoidMobRenderer<RogueNinjaEntity, HumanoidModel<RogueNinjaEntity>> {

    private static final ResourceLocation[] TEXTURES;

    static {
        TEXTURES = new ResourceLocation[RogueNinjaEntity.VARIANT_TEXTURES.length];
        for (int i = 0; i < TEXTURES.length; i++) {
            TEXTURES[i] = new ResourceLocation(NarutoMod.MOD_ID,
                    "textures/entity/rogue/" + RogueNinjaEntity.VARIANT_TEXTURES[i] + ".png");
        }
    }

    public RogueNinjaRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    private static final ResourceLocation UCHIHA_TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/rogue/uchiha_rogue.png");

    @Override
    public ResourceLocation getTextureLocation(RogueNinjaEntity entity) {
        // The Uchiha rogue subclasses the ordinary one and so lands here too. Its red eyes
        // are the whole tell that this is the kill worth making, so it gets its own skin
        // rather than a village one.
        if (entity instanceof com.sekwah.narutomod.entity.UchihaRogueEntity) {
            return UCHIHA_TEXTURE;
        }
        return TEXTURES[Math.floorMod(entity.getVariant(), TEXTURES.length)];
    }
}
