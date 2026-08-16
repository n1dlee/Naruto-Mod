package com.sekwah.narutomod.mixin.client;

import com.sekwah.narutomod.anims.PlayerAnimHandler;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public class PlayerModelMixin<T extends LivingEntity> extends HumanoidModel<T> {

    public PlayerModelMixin(ModelPart p_170677_) {
        super(p_170677_, RenderType::entityTranslucent);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At(value = "HEAD"))
    public void setupAnimPre(T player, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        PlayerAnimHandler.preSprintingAnim(player, (PlayerModel) (Object) this);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/model/HumanoidModel;setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"))
    public void setupAnim(T player, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        // Asked, not guessed. PlayerRendererMixin sets this flag around the hand render, so
        // a still third-person player facing due south no longer looks identical to a
        // first-person hand and lose its poses for the frame.
        if (com.sekwah.narutomod.anims.FirstPersonAnimHandler.isRenderingFirstPersonHand()) {
            return;
        }
        PlayerAnimHandler.sprintingAnim(player, (PlayerModel) (Object) this, limbSwing, limbSwingAmount, ageInTicks);
        this.hat.copyFrom(this.head);
    }
}
