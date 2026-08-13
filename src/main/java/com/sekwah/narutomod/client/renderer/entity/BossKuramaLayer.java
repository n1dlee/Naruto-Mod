package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.client.model.entity.BijuCloakModel;
import com.sekwah.narutomod.client.model.entity.KuramaAvatarModel;
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
 * Stages 1-3 wear the shroud imported from the 1.12.2 mod - the same BijuCloakModel the
 * player's cloak uses, with its own tails and its own per-tier textures. Stage 4 swaps to
 * the Full Avatar: the giant fox, at the size of the boss's real thirteen-block hitbox.
 *
 * Two things about these models are easy to get wrong and were both wrong here first:
 *
 *  - KuramaAvatarModel is a shared singleton whose setStage() decides whether it draws the
 *    whole fox or only the two forearm claws. The player's renderer moves that flag around,
 *    so a layer that does not set it draws whatever the last caller left behind - which, at
 *    the worn stages, is two 2x2x6 boxes and nothing else. That is the "long stick".
 *  - a RenderLayer inherits a pose stack LivingEntityRenderer has already rotated and
 *    flipped by scale(-1,-1,1). Both of these models are authored with +Y downward, so they
 *    come out upright here only as long as nothing re-applies that flip.
 */
public class BossKuramaLayer extends RenderLayer<MangekyoBossEntity, HumanoidModel<MangekyoBossEntity>> {

    /**
     * The boss's fox stands at the shared final-form height, and so does the player's, and so
     * does either one's Susanoo. A model drawn taller than the hitbox you can actually hit is
     * the same class of bug as a model drawn as two floating claws, so the size is taken from
     * {@link com.sekwah.narutomod.util.GiantForm} - the number the boss's hitbox uses too -
     * and divided by the model's own measured height rather than hand-tuned here.
     */
    private static final float AVATAR_SCALE =
            com.sekwah.narutomod.util.GiantForm.HEIGHT_BLOCKS / KuramaAvatarModel.FULL_BODY_HEIGHT_BLOCKS;

    /** The stage index KuramaAvatarModel treats as "draw the entire fox". */
    private static final int AVATAR_FULL_BODY_STAGE = 3;

    /** Shroud size per stage, as a multiple of the body it hangs on. */
    private static final float[] SHROUD_SCALE = {0f, 1.10f, 1.18f, 1.28f};
    /** Tails shown per stage, feeding both the model and its texture tier. */
    private static final int[] SHROUD_TAILS = {0, 3, 6, 9};

    private static final float SHROUD_ALPHA = 0.9f;
    private static final float AVATAR_ALPHA = 0.45f;

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
        if (stage <= 0) {
            return;
        }
        if (stage >= 4) {
            renderAvatar(poseStack, bufferSource, packedLight);
        } else {
            renderShroud(poseStack, bufferSource, packedLight, stage);
        }
    }

    /** Stages 1-3: the ported shroud, thickening and gaining tails as the fight turns. */
    private void renderShroud(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int stage) {
        BijuCloakModel cloak = BijuCloakRenderer.cloakModel();
        if (cloak == null) {
            return;
        }
        int clamped = Mth.clamp(stage, 1, SHROUD_SCALE.length - 1);
        int tails = SHROUD_TAILS[clamped];
        cloak.setVisibleTails(tails);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(
                BijuCloakRenderer.cloakTextureForTails(tails, false)));

        poseStack.pushPose();
        float scale = SHROUD_SCALE[clamped];
        poseStack.scale(scale, scale, scale);
        cloak.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0f, 1.0f, 1.0f, SHROUD_ALPHA);
        poseStack.popPose();
    }

    /** Stage 4: the Full Avatar, sized to the boss's real hitbox. */
    private void renderAvatar(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        KuramaAvatarModel avatar = KuramaTailRenderer.avatarModel();
        if (avatar == null) {
            return;
        }
        // Set every frame, never assumed: the player's renderer shares this instance and
        // leaves it on whatever stage it last drew.
        avatar.setStage(AVATAR_FULL_BODY_STAGE);

        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityTranslucent(KuramaTailRenderer.avatarTexture()));

        poseStack.pushPose();
        // Origin sits 1.501 above the feet in this space, and +Y runs downward here, so this
        // drops the model's feet onto the ground the boss is standing on.
        poseStack.translate(0.0D, 1.501D, 0.0D);
        poseStack.scale(AVATAR_SCALE, AVATAR_SCALE, AVATAR_SCALE);
        avatar.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0f, 0.45f, 0.1f, AVATAR_ALPHA);
        poseStack.popPose();
    }
}
