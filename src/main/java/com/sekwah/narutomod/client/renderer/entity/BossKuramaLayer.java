package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.entity.MangekyoBossEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * The fox around the Naruto boss, growing with his transformation stage.
 *
 * Stages 1-3 are a shroud that hangs on the body: orange chakra thickening as the fight
 * turns, drawn at roughly human scale so the wielder stays visible inside it. Stage 4 is the
 * Full Avatar - the same giant model the player's nine-tailed form uses, at the same scale,
 * standing where the boss stands.
 *
 * Same inherited-pose rule as BossSusanooLayer: a RenderLayer runs inside a stack that
 * LivingEntityRenderer has already rotated into the body's facing, flipped by scale(-1,-1,1)
 * and translated down 1.501. Re-applying any of that is what once buried the Susanoo
 * underground, so this works in the space it is handed.
 */
public class BossKuramaLayer extends RenderLayer<MangekyoBossEntity, HumanoidModel<MangekyoBossEntity>> {

    /**
     * KuramaAvatarModel's unscaled height in blocks, legs to ear-tip (see the note on
     * KuramaTailRenderer#FULL_AVATAR_BASE_SCALE). Every scale below is derived from this so
     * the fox matches the boss's real size instead of being eyeballed.
     */
    private static final float MODEL_HEIGHT_BLOCKS = 3.4f;

    /**
     * The player's avatar is drawn at 14x - roughly Hokage Rock scale - because the player's
     * hitbox never grows and the giant is pure spectacle. The boss's hitbox DOES grow, to the
     * same 13 blocks Complete Body uses, so its fox has to be that tall and no taller: a
     * 48-block model over a 13-block hitbox would mean swinging at a leg and hitting nothing,
     * which is the exact mismatch that kept bosses off stage 4 in the first place.
     */
    private static final float AVATAR_TARGET_HEIGHT = 13.0f;
    private static final float AVATAR_SCALE = AVATAR_TARGET_HEIGHT / MODEL_HEIGHT_BLOCKS;

    /**
     * Shroud height in blocks per stage. A worn cloak has to stay close to the body it hangs
     * on - the boss is under two blocks tall, so these are scaled against that, not against
     * the avatar.
     */
    private static final float[] SHROUD_HEIGHT = {0f, 2.3f, 2.7f, 3.2f};
    private static final float SHROUD_ALPHA = 0.72f;
    private static final float AVATAR_ALPHA = 0.85f;

    public BossKuramaLayer(RenderLayerParent<MangekyoBossEntity, HumanoidModel<MangekyoBossEntity>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       MangekyoBossEntity boss, float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!boss.getVariant().hasKuramaCloak()) {
            return;
        }
        int stage = boss.getSusanooStage();
        if (stage <= 0 || KuramaTailRenderer.avatarModel() == null) {
            return;
        }

        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityTranslucent(KuramaTailRenderer.avatarTexture()));

        poseStack.pushPose();
        if (stage >= 4) {
            poseStack.translate(0.0D, 1.501D, 0.0D);
            poseStack.scale(AVATAR_SCALE, AVATAR_SCALE, AVATAR_SCALE);
            KuramaTailRenderer.avatarModel().renderToBuffer(poseStack, consumer, packedLight,
                    OverlayTexture.NO_OVERLAY, 1.0f, 0.55f, 0.12f, AVATAR_ALPHA);
        } else {
            float scale = SHROUD_HEIGHT[Mth.clamp(stage, 1, SHROUD_HEIGHT.length - 1)]
                    / MODEL_HEIGHT_BLOCKS;
            poseStack.translate(0.0D, 1.501D, 0.0D);
            poseStack.scale(scale, scale, scale);
            // Deepens from cloak orange toward the red-gold of Kurama Chakra Mode as the
            // stages climb, so the escalation is legible at a glance.
            float red = 1.0f;
            float green = 0.40f + stage * 0.10f;
            float blue = 0.05f + stage * 0.05f;
            KuramaTailRenderer.avatarModel().renderToBuffer(poseStack, consumer, packedLight,
                    OverlayTexture.NO_OVERLAY, red, green, blue, SHROUD_ALPHA);
        }
        poseStack.popPose();
    }
}
