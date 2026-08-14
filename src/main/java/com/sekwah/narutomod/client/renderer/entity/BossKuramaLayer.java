package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.client.model.entity.BijuCloakModel;
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
 *  - KuramaFoxModel is a shared baked instance the player's renderer also draws. Its tail
 *    pose is state, so this layer sets it every frame instead of inheriting whatever the
 *    last caller left. The same trap once left the avatar showing only its forearm claws.
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
            com.sekwah.narutomod.util.GiantForm.HEIGHT_BLOCKS
                    / com.sekwah.narutomod.client.model.entity.KuramaFoxModel.BODY_HEIGHT_BLOCKS;

    /** Shroud size per stage, as a multiple of the body it hangs on. */
    private static final float[] SHROUD_SCALE = {0f, 1.10f, 1.18f, 1.28f};
    /** Tails shown per stage, feeding both the model and its texture tier. */
    private static final int[] SHROUD_TAILS = {0, 3, 6, 9};

    private static final float SHROUD_ALPHA = 0.9f;
    /** Matches the player's fox: solid enough to read as a body rather than orange haze. */
    private static final float AVATAR_ALPHA = 0.8f;

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
            renderAvatar(poseStack, bufferSource, packedLight, ageInTicks);
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
    private void renderAvatar(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                              float ageInTicks) {
        com.sekwah.narutomod.client.model.entity.KuramaFoxModel fox = KuramaTailRenderer.foxModel();
        if (fox == null) {
            return;
        }
        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityTranslucent(KuramaTailRenderer.foxTexture()));

        poseStack.pushPose();
        // Origin sits 1.501 above the feet in this space, and +Y runs downward here, so this
        // puts the origin on the ground the boss is standing on.
        poseStack.translate(0.0D, 1.501D, 0.0D);
        poseStack.scale(AVATAR_SCALE, AVATAR_SCALE, AVATAR_SCALE);
        // The imported model's soles are at +FEET_OFFSET rather than at zero, and +Y is down
        // here, so the fox has to come back up by that much or it stands in a hole.
        poseStack.translate(0.0D,
                -com.sekwah.narutomod.client.model.entity.KuramaFoxModel.FEET_OFFSET, 0.0D);

        // Shared baked instance - the player's renderer poses it too, so this sets the tails
        // every frame rather than inheriting whatever the last caller left.
        fox.waveTails(ageInTicks);
        fox.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0f, 1.0f, 1.0f, AVATAR_ALPHA);
        poseStack.popPose();
    }
}
