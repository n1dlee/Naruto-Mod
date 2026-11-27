package com.sekwah.narutomod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.client.renderer.GatsugaRenderer;
import com.sekwah.narutomod.client.renderer.KaitenRenderer;
import com.sekwah.narutomod.client.renderer.KamuiPhaseRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity> extends EntityRenderer<T> {
    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void narutomod$jutsuBodyRotation(T actor, PoseStack pose, float bob, float yaw, float partial, CallbackInfo callback) {
        float spin = KaitenRenderer.spin(actor, partial);
        if (spin != 0f) {
            pose.mulPose(Axis.YP.rotationDegrees(spin));
        }
        GatsugaRenderer.orient(pose, actor, partial);
    }

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void narutomod$phaseMaterial(T actor, boolean bodyVisible, boolean translucent, boolean outline,
                                       CallbackInfoReturnable<RenderType> callback) {
        if (bodyVisible && KamuiPhaseRenderer.veil(actor, Minecraft.getInstance().getFrameTime()) > 0.01f) {
            callback.setReturnValue(RenderType.entityTranslucent(this.getTextureLocation(actor)));
        }
    }

    @Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void narutomod$phaseSkin(EntityModel<?> model, PoseStack pose, VertexConsumer buffer, int light, int overlay,
                                    float red, float green, float blue, float alpha, T actor, float yaw, float partial,
                                    PoseStack outerPose, MultiBufferSource buffers, int outerLight) {
        float veil = KamuiPhaseRenderer.veil(actor, partial);
        model.renderToBuffer(pose, buffer, light, overlay, red * (1 - veil * 0.16f), green * (1 - veil * 0.11f),
                blue, alpha * (1 - veil));
    }
}
