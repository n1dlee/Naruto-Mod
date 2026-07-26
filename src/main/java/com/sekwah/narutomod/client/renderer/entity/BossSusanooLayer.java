package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.model.entity.SusanooModel;
import com.sekwah.narutomod.entity.MangekyoBossEntity;
import com.sekwah.narutomod.entity.MangekyoBossVariant;
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
 * Wraps a boss in their own Susanoo once the fight has worn them down. Reuses the same
 * SusanooModel the players get, tinted with that wielder's canon colour from
 * {@link MangekyoBossVariant} — Itachi red-orange, Madara blue, and so on.
 */
public class BossSusanooLayer extends RenderLayer<MangekyoBossEntity, HumanoidModel<MangekyoBossEntity>> {

    private static final ResourceLocation SUSANOO_TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/susanoo.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucent(SUSANOO_TEXTURE);
    /** Bosses stay in the hovering-ribcage range — they never grow the Complete Body giant. */
    private static final float[] STAGE_SCALE = {0f, 1.8f, 2.4f, 3.2f};
    private static final float ALPHA = 0.55f;

    private final SusanooModel susanooModel;

    public BossSusanooLayer(RenderLayerParent<MangekyoBossEntity, HumanoidModel<MangekyoBossEntity>> parent,
                            EntityRendererProvider.Context context) {
        super(parent);
        this.susanooModel = new SusanooModel(context.bakeLayer(SusanooModel.LAYER_LOCATION));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       MangekyoBossEntity boss, float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        int stage = boss.getSusanooStage();
        if (stage <= 0) {
            return;
        }
        MangekyoBossVariant variant = boss.getVariant();
        VertexConsumer consumer = bufferSource.getBuffer(RENDER_TYPE);

        poseStack.pushPose();
        // The parent renderer has already rotated into the boss's body space, so undo that
        // and re-apply the same yaw convention SusanooRenderer uses for players.
        float bodyYaw = Mth.rotLerp(partialTick, boss.yBodyRotO, boss.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyYaw));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));

        float scale = STAGE_SCALE[Mth.clamp(stage, 1, STAGE_SCALE.length - 1)];
        // Same model-space flip the player-side renderer needs (negative Y is up).
        poseStack.scale(-scale, -scale, scale);

        this.susanooModel.setStage(stage);
        this.susanooModel.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                variant.susanooRed(), variant.susanooGreen(), variant.susanooBlue(), ALPHA);

        poseStack.popPose();
    }
}
