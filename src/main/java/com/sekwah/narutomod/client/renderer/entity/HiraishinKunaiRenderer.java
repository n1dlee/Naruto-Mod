package com.sekwah.narutomod.client.renderer.entity;

import com.sekwah.narutomod.entity.projectile.HiraishinKunaiEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Same shape as a thrown kunai, but wearing the seal's gold wrapping. */
public class HiraishinKunaiRenderer extends ArrowRenderer<HiraishinKunaiEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("narutomod", "textures/entity/projectiles/hiraishin_kunai.png");

    public HiraishinKunaiRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(HiraishinKunaiEntity entity) {
        return TEXTURE;
    }
}
