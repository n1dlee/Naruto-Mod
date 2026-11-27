package com.sekwah.narutomod.mixin.client;

import com.sekwah.narutomod.anims.PlayerAnimHandler;
import com.sekwah.narutomod.anims.PoseBlender;
import com.sekwah.narutomod.client.renderer.MedicalPalmRenderer;
import com.sekwah.narutomod.client.renderer.ScalpelRenderer;
import com.sekwah.narutomod.client.renderer.BaikaRenderer;
import com.sekwah.narutomod.client.renderer.BoulderRollRenderer;
import com.sekwah.narutomod.client.renderer.KikaichuRenderer;
import com.sekwah.narutomod.client.renderer.TrigramPalmsRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
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
        BaikaRenderer.pose((PlayerModel<?>) (Object) this, player, Mth.frac(ageInTicks));
        BoulderRollRenderer.pose(this, player, Mth.frac(ageInTicks));
        KikaichuRenderer.sleevePose(this, player, Mth.frac(ageInTicks));
        TrigramPalmsRenderer.pose(this, player, Mth.frac(ageInTicks));
        float warm = MedicalPalmRenderer.weight(player, Mth.frac(ageInTicks));
        if (warm > PoseBlender.EPSILON) {
            boolean right = player.getMainArm() == HumanoidArm.RIGHT;
            PoseBlender.rotate(right ? this.rightArm : this.leftArm, warm, -1.04f, right ? -0.32f : 0.32f, right ? 0.12f : -0.12f);
            PoseBlender.rotate(right ? this.leftArm : this.rightArm, warm * 0.77f, -0.76f, right ? 0.40f : -0.40f, right ? -0.16f : 0.16f);
        }
        float edge = ScalpelRenderer.weight(player, Mth.frac(ageInTicks));
        if (edge > PoseBlender.EPSILON) {
            boolean right = player.getMainArm() == HumanoidArm.RIGHT;
            PoseBlender.rotate(right ? this.rightArm : this.leftArm, edge, -0.86f, right ? -0.35f : 0.35f, right ? 0.19f : -0.19f);
        }
        this.hat.copyFrom(this.head);
    }
}
