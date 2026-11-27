package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.util.SageFocus;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class SageStoneLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final RenderType STONE = RenderType.entityTranslucent(new ResourceLocation("minecraft", "textures/block/andesite.png"));

    public SageStoneLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer actor,
                       float swing, float stride, float partial, float age, float yaw, float pitch) {
        int ticks = actor.getCapability(NinjaCapabilityHandler.NINJA_DATA).map(data -> data.getSageStoneTicks()).orElse(0);
        if (ticks <= 0 || actor.isInvisible() || actor.isSpectator()) {
            return;
        }
        float envelope = Mth.clamp((SageFocus.STONE_TICKS - ticks + 2 + partial) / 9, 0, 1)
                * Mth.clamp((ticks - partial) / 14, 0, 1);
        float opacity = envelope * (NarutoConfig.reducedBodyRiteEffects ? 0.50f : 0.88f);
        pose.pushPose();
        try {
            pose.translate(0, 0.68, 0);
            pose.scale(1.021f, 1.022f, 1.027f);
            pose.translate(0, -0.68, 0);
            getParentModel().renderToBuffer(pose, buffers.getBuffer(STONE), light, OverlayTexture.NO_OVERLAY,
                    0.83f, 0.87f, 0.87f, opacity);
        } finally {
            pose.popPose();
        }
    }
}
