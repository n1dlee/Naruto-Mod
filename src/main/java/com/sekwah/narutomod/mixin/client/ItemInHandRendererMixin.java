package com.sekwah.narutomod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutomod.client.renderer.ChakraFlowRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Shadow @Final private ItemRenderer itemRenderer;

    @Inject(method = "renderItem", at = @At("TAIL"))
    private void narutomod$renderChakraEdge(LivingEntity entity, ItemStack stack, ItemDisplayContext display,
                                          boolean left, PoseStack poses, MultiBufferSource buffers,
                                          int light, CallbackInfo ci) {
        ChakraFlowRenderer.draw(entity, stack, new ChakraFlowRenderer.HandRender(display, left, itemRenderer, poses, buffers));
    }
}
