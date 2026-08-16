package com.sekwah.narutomod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutomod.anims.FirstPersonAnimHandler;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives the first-person hand the jutsu poses the third-person body already had.
 *
 * PlayerRenderer#renderHand runs a fixed sequence: it calls setupAnim with all-zero arguments,
 * writes {@code arm.xRot = 0}, draws the arm, writes {@code sleeve.xRot = 0}, draws the sleeve.
 * Anything applied earlier than the zeroing is discarded, so the pose has to land in the gap
 * between each zero and its matching draw - which is exactly where these two injections sit,
 * immediately before ModelPart#render, once for the arm and once for the sleeve.
 *
 * The two public entry points are marked because renderHand is shared between them and cannot
 * otherwise tell which hand it is drawing.
 *
 * All four injections are {@code require = 0}. If a future Minecraft or Forge build reshuffles
 * this method the poses quietly stop applying, which is a cosmetic regression; the alternative
 * is refusing to launch the game over one arm angle.
 */
@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {

    @Inject(method = "renderRightHand", at = @At("HEAD"), require = 0)
    private void narutomod$markRightHand(PoseStack poseStack, MultiBufferSource bufferSource, int light,
                                         AbstractClientPlayer player, CallbackInfo ci) {
        FirstPersonAnimHandler.setRenderingHand(true);
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"), require = 0)
    private void narutomod$markLeftHand(PoseStack poseStack, MultiBufferSource bufferSource, int light,
                                        AbstractClientPlayer player, CallbackInfo ci) {
        FirstPersonAnimHandler.setRenderingHand(false);
    }

    @Inject(
            method = "renderHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",
                    ordinal = 0),
            require = 0)
    private void narutomod$poseArm(PoseStack poseStack, MultiBufferSource bufferSource, int light,
                                   AbstractClientPlayer player, ModelPart arm, ModelPart sleeve,
                                   CallbackInfo ci) {
        FirstPersonAnimHandler.poseHand(player, arm);
    }

    /**
     * The sleeve is a separate part with its own xRot zeroed after the arm is drawn, so posing
     * the arm alone would slide the skin's overlay layer off the limb underneath it.
     */
    @Inject(
            method = "renderHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",
                    ordinal = 1),
            require = 0)
    private void narutomod$poseSleeve(PoseStack poseStack, MultiBufferSource bufferSource, int light,
                                      AbstractClientPlayer player, ModelPart arm, ModelPart sleeve,
                                      CallbackInfo ci) {
        FirstPersonAnimHandler.poseHand(player, sleeve);
    }
}
