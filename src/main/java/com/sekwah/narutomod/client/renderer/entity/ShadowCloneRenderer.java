package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * Renders ShadowClone with the owning player's skin texture.
 * Falls back to default Steve skin when owner is not in render range.
 */
public class ShadowCloneRenderer extends HumanoidMobRenderer<ShadowCloneEntity, HumanoidModel<ShadowCloneEntity>> {

    private static final ResourceLocation STEVE_SKIN =
            new ResourceLocation("textures/entity/player/wide/steve.png");

    public ShadowCloneRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowCloneEntity entity) {
        Optional<UUID> ownerUUID = entity.getOwnerUUID();
        if (ownerUUID.isEmpty()) {
            return STEVE_SKIN;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Player owner = mc.level.getPlayerByUUID(ownerUUID.get());
            // AbstractClientPlayer has getSkinTextureLocation() available client-side
            if (owner instanceof AbstractClientPlayer clientPlayer) {
                return clientPlayer.getSkinTextureLocation();
            }
        }

        // Owner is outside client range — use Steve as fallback
        return STEVE_SKIN;
    }

    @Override
    public void render(ShadowCloneEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
