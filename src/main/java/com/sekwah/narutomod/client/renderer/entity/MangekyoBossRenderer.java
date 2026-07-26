package com.sekwah.narutomod.client.renderer.entity;

import com.sekwah.narutomod.entity.MangekyoBossEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws a Mangekyo boss with that wielder's own skin, picked off the variant byte.
 * Reuses the vanilla humanoid rig the same way ShadowCloneRenderer does — these are
 * human ninja, so no custom model geometry is needed.
 */
public class MangekyoBossRenderer extends HumanoidMobRenderer<MangekyoBossEntity, HumanoidModel<MangekyoBossEntity>> {

    public MangekyoBossRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new BossSusanooLayer(this, context));
    }

    @Override
    public ResourceLocation getTextureLocation(MangekyoBossEntity entity) {
        return entity.getVariant().texture();
    }
}
