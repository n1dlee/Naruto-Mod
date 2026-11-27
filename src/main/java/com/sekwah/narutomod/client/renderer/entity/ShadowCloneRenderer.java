package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutomod.entity.ClayCloneEntity;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.util.ClayCloneState;
import com.sekwah.narutomod.util.ClayCloneState.Phase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
    private final PlayerModel<ShadowCloneEntity> wideModel;
    private final PlayerModel<ShadowCloneEntity> slimModel;

    public ShadowCloneRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
        this.wideModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new ClayCloneGlaze(this));
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowCloneEntity entity) {
        // A boss's clone carries its wielder's variant, because the player lookup below can
        // never resolve a boss - it is not a Player - and every one of them came out as Steve.
        byte bossVariant = entity.getBossVariant();
        if (bossVariant >= 0) {
            return com.sekwah.narutomod.entity.MangekyoBossVariant.byId(bossVariant).texture();
        }
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
        if ((entity.isPlayerFormation() || entity instanceof ClayCloneEntity) && mc.getConnection() != null) {
            var profile = mc.getConnection().getPlayerInfo(ownerUUID.get());
            if (profile != null) {
                return profile.getSkinLocation();
            }
        }

        // Owner is outside client range — use Steve as fallback
        return entity.isPlayerFormation() ? DefaultPlayerSkin.getDefaultSkin(ownerUUID.get()) : STEVE_SKIN;
    }

    @Override
    public void render(ShadowCloneEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity instanceof ClayCloneEntity clay && (clay.phase() == Phase.BURST || clay.phase() == Phase.FIZZLE)) {
            ClayCloneGlaze.flakes(clay, poseStack, bufferSource, partialTick);
            if (clay.phase() == Phase.BURST || clay.stageAge() >= 6) {
                return;
            }
        }
        poseStack.pushPose();
        var previous = this.model;
        try {
            if (entity instanceof ClayCloneEntity clay) {
                applyClayPose(clay, poseStack, partialTick);
            } else if (entity.isPlayerFormation()) {
                this.model = playerModel(entity);
                applyPlayerPose(entity, poseStack, partialTick);
            }
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        } finally {
            this.model = previous;
            poseStack.popPose();
        }
    }

    private PlayerModel<ShadowCloneEntity> playerModel(ShadowCloneEntity entity) {
        var connection = Minecraft.getInstance().getConnection();
        var uuid = entity.getOwnerUUID();
        if (uuid.isEmpty()) {
            return wideModel;
        }
        var player = connection == null ? null : connection.getPlayerInfo(uuid.get());
        String profile = player == null ? DefaultPlayerSkin.getSkinModelName(uuid.get()) : player.getModelName();
        return "slim".equals(profile) ? slimModel : wideModel;
    }

    private static void applyPlayerPose(ShadowCloneEntity entity, PoseStack poses, float partial) {
        if (entity.isDeadOrDying()) {
            float remaining = 1 - Mth.clamp((entity.deathTime + partial) / 6, 0, 0.94f);
            poses.scale(remaining, remaining * remaining, remaining);
        } else {
            float growth = Mth.clamp((entity.level().getGameTime() - entity.formationBorn() + partial) / 5, 0, 1);
            poses.scale(0.82f + 0.18f * growth, 0.35f + 0.65f * growth, 0.82f + 0.18f * growth);
        }
    }

    private static void applyClayPose(ClayCloneEntity clay, PoseStack pose, float partial) {
        float age = clay.stageAge() + partial;
        if (clay.phase() == Phase.FORM) {
            float growth = Mth.clamp(age / ClayCloneState.FORM, 0, 1);
            pose.scale(0.8f + growth * 0.2f, 0.23f + growth * 0.77f, 0.8f + growth * 0.2f);
        } else if (clay.phase() == Phase.FIZZLE) {
            pose.scale(1, Mth.clamp(1 - age / 7f, 0.15f, 1), 1);
        }
    }
}
