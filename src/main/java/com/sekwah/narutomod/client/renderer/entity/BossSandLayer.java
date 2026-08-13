package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.model.entity.BijuCloakModel;
import com.sekwah.narutomod.client.model.entity.OneTailModel;
import com.sekwah.narutomod.entity.MangekyoBossEntity;
import com.sekwah.narutomod.entity.TailedBeastVariant;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Shukaku coming out of Gaara, on the same stage ladder Naruto's fox uses.
 *
 * Stages 1-3 wear the One Tail's own version of the imported chakra shroud - the same
 * BijuCloakModel the player's cloak uses, with the sand skin. Stage 4 is the One Tail itself,
 * drawn with the model already ported for the tailed beasts rather than a second copy of the
 * same geometry, at the shared final-form height so it stands as a peer to a Complete Body
 * Susanoo and to Naruto's Kurama.
 *
 * As a RenderLayer this inherits a pose stack LivingEntityRenderer has already rotated and
 * flipped by scale(-1,-1,1); none of that may be applied a second time.
 */
public class BossSandLayer extends RenderLayer<MangekyoBossEntity, HumanoidModel<MangekyoBossEntity>> {

    private static final ResourceLocation SAND_CLOAK =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/bijucloak_sand.png");

    /** Shroud size per stage, as a multiple of the body it hangs on. */
    private static final float[] SHROUD_SCALE = {0f, 1.10f, 1.18f, 1.28f};
    /** The One Tail has one tail. The shroud shows it from the moment the sand answers. */
    private static final int SHROUD_TAILS = 1;

    private static final float SHROUD_ALPHA = 0.9f;
    private static final float SHUKAKU_ALPHA = 0.75f;

    private final OneTailModel shukaku;
    private final float shukakuScale;
    private final float shukakuFeetOffset;

    public BossSandLayer(RenderLayerParent<MangekyoBossEntity, HumanoidModel<MangekyoBossEntity>> parent,
                         EntityRendererProvider.Context context) {
        super(parent);
        this.shukaku = new OneTailModel(context.bakeLayer(OneTailModel.LAYER_LOCATION));
        TailedBeastVariant oneTail = TailedBeastVariant.SHUKAKU;
        // Derived from the measured model height, not hand-tuned: the same rule every other
        // final form follows, so this cannot drift away from the Susanoo or the fox.
        float modelHeight = oneTail.getHeight() / oneTail.getRenderScale();
        this.shukakuScale = com.sekwah.narutomod.util.GiantForm.HEIGHT_BLOCKS / modelHeight;
        this.shukakuFeetOffset = oneTail.getFeetOffset();
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       MangekyoBossEntity boss, float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!boss.getVariant().hasSandCloak()) {
            return;
        }
        int stage = boss.getSusanooStage();
        if (stage <= 0) {
            return;
        }
        if (stage >= 4) {
            renderShukaku(poseStack, bufferSource, packedLight);
        } else {
            renderShroud(poseStack, bufferSource, packedLight, stage);
        }
    }

    /** Stages 1-3: sand armour over him, thickening as more of it leaves the gourd. */
    private void renderShroud(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int stage) {
        BijuCloakModel cloak = BijuCloakRenderer.cloakModel();
        if (cloak == null) {
            return;
        }
        // Set every frame, never assumed: the player's cloak renderer shares this instance
        // and leaves it on whatever tail count it last drew.
        cloak.setVisibleTails(SHROUD_TAILS);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(SAND_CLOAK));
        poseStack.pushPose();
        float scale = SHROUD_SCALE[Mth.clamp(stage, 1, SHROUD_SCALE.length - 1)];
        poseStack.scale(scale, scale, scale);
        cloak.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0f, 1.0f, 1.0f, SHROUD_ALPHA);
        poseStack.popPose();
    }

    /** Stage 4: the One Tail, at the size everything else's final form stands. */
    private void renderShukaku(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityTranslucent(TailedBeastVariant.SHUKAKU.getTexture()));

        poseStack.pushPose();
        // The origin here sits 1.501 world-blocks above the boss's feet and +Y runs downward,
        // so moving by T lowers it by T. Shukaku's own feet hang feetOffset*scale below its
        // origin, so putting them on the ground means landing the origin at that height:
        // T = 1.501 - feetOffset*scale. Getting this backwards buries the beast.
        poseStack.translate(0.0D, 1.501D - this.shukakuFeetOffset * this.shukakuScale, 0.0D);
        poseStack.scale(this.shukakuScale, this.shukakuScale, this.shukakuScale);
        this.shukaku.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0f, 1.0f, 1.0f, SHUKAKU_ALPHA);
        poseStack.popPose();
    }
}
