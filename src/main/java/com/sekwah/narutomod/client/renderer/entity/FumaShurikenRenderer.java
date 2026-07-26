package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.entity.projectile.FumaShurikenEntity;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class FumaShurikenRenderer extends ArrowRenderer<FumaShurikenEntity> {

    private final ItemRenderer itemRenderer;
    private final ItemStack renderingItem;

    public static final ResourceLocation RES_ARROW = new ResourceLocation("narutomod", "textures/entity/projectiles/kunai.png");

    public FumaShurikenRenderer(EntityRendererProvider.Context manager) {
        super(manager);
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
        this.renderingItem = new ItemStack(NarutoItems.FUMA_SHURIKEN.get());
    }

    @Override
    public void render(FumaShurikenEntity entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn) {
        matrixStackIn.pushPose();
        float rotateSpeed = -65;
        matrixStackIn.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot()) - 90.0F));
        matrixStackIn.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot())));
        matrixStackIn.mulPose(Axis.XP.rotationDegrees(entityIn.getRotOffset()));
        matrixStackIn.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entityIn.getPrevRotateTicks() * rotateSpeed, entityIn.getRotateTicks() * rotateSpeed)));
        matrixStackIn.scale(0.85f, 0.85f, 0.85f);
        BakedModel ibakedmodel = itemRenderer.getModel(this.renderingItem, entityIn.level(), null, entityIn.getId());
        itemRenderer.render(this.renderingItem, ItemDisplayContext.FIXED, false, matrixStackIn, bufferIn, packedLightIn, OverlayTexture.NO_OVERLAY, ibakedmodel);
        matrixStackIn.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(FumaShurikenEntity entity) {
        return RES_ARROW;
    }
}
