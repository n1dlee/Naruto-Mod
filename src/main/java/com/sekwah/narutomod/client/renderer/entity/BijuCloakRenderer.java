package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.client.model.entity.BijuCloakModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Draws the biju chakra shroud directly on the player — the fox-eared, tailed cloak from
 * the 1.12.2 mod, worn rather than looming behind like the giant avatar.
 *
 * One model covers every stage; only the skin changes, exactly as the original did:
 *   tails 1-3  -> the thin red version
 *   tails 4-8  -> the denser red version
 *   9 tails    -> Kurama's full shroud
 *   KCM        -> the gold chakra mode, which is the whole point of KCM having no shell
 *
 * KCM takes priority over the cloak tiers so toggling it always reads as the gold form.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BijuCloakRenderer {

    private static final ResourceLocation CLOAK_L1 =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/bijucloakl1.png");
    private static final ResourceLocation CLOAK_L2 =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/bijucloakl2.png");
    private static final ResourceLocation CLOAK_KURAMA =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/bijucloak_kurama.png");
    private static final ResourceLocation CLOAK_KCM =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/bijucloak_kcm2.png");

    /** Slightly larger than the player so it reads as a shroud around them, not a skin. */
    private static final float CLOAK_SCALE = 1.08f;

    private static BijuCloakModel model;

    public static void setModel(BijuCloakModel bakedModel) {
        model = bakedModel;
    }

    // Exposed for BossKuramaLayer: the Naruto boss wears the same imported 1.12.2 shroud the
    // player does, rather than a scaled-down copy of the Full Avatar standing in for it.

    static BijuCloakModel cloakModel() {
        return model;
    }

    /** Shroud texture for a tail count, matching the player's own tier thresholds. */
    static ResourceLocation cloakTextureForTails(int tails, boolean kcm) {
        if (kcm) {
            return CLOAK_KCM;
        }
        if (tails >= 8) {
            return CLOAK_KURAMA;
        }
        return tails >= 4 ? CLOAK_L2 : CLOAK_L1;
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        if (model == null) {
            return;
        }
        Player player = event.getEntity();
        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player && mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            boolean kcm = ninjaData.isKcmActive();
            boolean cloak = ninjaData.isKuramaCloakActive();
            if (!kcm && !cloak) {
                return;
            }
            // The Full Avatar at 9 tails already renders a giant fox and hides the player,
            // so drawing the worn shroud on top of that would just clip through it.
            int tails = ninjaData.getKuramaTailCount();
            if (cloak && !kcm && tails >= 9) {
                return;
            }

            // Kurama Chakra Mode is the refined form - the marked cloak and nothing else.
            // Everything else shows exactly as many tails as the wielder has drawn out.
            model.setVisibleTails(kcm ? 0 : tails);
            // Inherit the wearer's pose. Without this the shroud renders in its rest stance
            // while the player underneath runs, sneaks and casts - the KCM form standing to
            // attention inside a sprinting body.
            model.animateFrom(event.getRenderer().getModel());

            ResourceLocation texture;
            if (kcm) {
                texture = CLOAK_KCM;
            } else if (tails >= 8) {
                texture = CLOAK_KURAMA;
            } else if (tails >= 4) {
                texture = CLOAK_L2;
            } else {
                texture = CLOAK_L1;
            }

            PoseStack poseStack = event.getPoseStack();
            VertexConsumer consumer = event.getMultiBufferSource()
                    .getBuffer(RenderType.entityTranslucent(texture));

            poseStack.pushPose();
            // Same convention the other ported bodies use: lift by the standard biped
            // 1.5 blocks, then flip because these models are authored with +Y downward.
            poseStack.translate(0.0D, 1.501D * CLOAK_SCALE, 0.0D);
            float bodyYaw = Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
            poseStack.scale(-CLOAK_SCALE, -CLOAK_SCALE, CLOAK_SCALE);

            model.renderToBuffer(poseStack, consumer, event.getPackedLight(), OverlayTexture.NO_OVERLAY,
                    1.0f, 1.0f, 1.0f, 0.9f);
            poseStack.popPose();
        });
    }
}
