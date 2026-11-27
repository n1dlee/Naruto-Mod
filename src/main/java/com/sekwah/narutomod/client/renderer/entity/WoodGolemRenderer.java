package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.model.entity.WoodGolemModel;
import com.sekwah.narutomod.entity.WoodGolemEntity;
import com.sekwah.narutomod.util.WoodGolemMotion;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class WoodGolemRenderer extends HumanoidMobRenderer<WoodGolemEntity, WoodGolemModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/wood_golem.png");

    public WoodGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new WoodGolemModel(context.bakeLayer(WoodGolemModel.LAYER)), 0.92f);
    }

    @Override
    protected void scale(WoodGolemEntity golem, PoseStack pose, float partial) {
        float growth = (float) WoodGolemMotion.ease(golem.growth(partial));
        float fall = (float) WoodGolemMotion.ease(golem.dismantleProgress(partial));
        float body = WoodGolemModel.SCALE * (0.72f + 0.28f * growth) * (1 - 0.42f * fall);
        float height = WoodGolemModel.SCALE * (0.11f + 0.89f * growth) * (1 - 0.91f * fall);
        pose.scale(body, height, body);
    }

    @Override
    public ResourceLocation getTextureLocation(WoodGolemEntity golem) { return TEXTURE; }
}
