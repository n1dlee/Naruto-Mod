package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The eyes themselves, drawn on the player's face.
 *
 * Every dojutsu in this mod existed only as a screen overlay on the wielder's own HUD, which
 * meant an active Sharingan was invisible to everyone else in the world - including in third
 * person to the person using it. The eye is the single most recognisable thing about these
 * techniques and nothing showed it.
 *
 * The textures are ordinary 64x64 skin layouts with every pixel transparent except the four
 * that sit over each eye. Re-rendering the head part with one of them therefore lines the eyes
 * up perfectly at any head angle for free, and it keeps working on slim skins, custom skins and
 * under helmets - none of which a hand-placed quad in front of the face would survive.
 *
 * {@link RenderType#eyes} is fullbright and additive, so the eyes stay lit in a dark room. That
 * is not artistic licence: an unlit two-pixel eye is invisible at night, which is when most of
 * these fights happen.
 */
public class PlayerEyeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final String PATH = "textures/entity/eyes/";

    private static final ResourceLocation SHARINGAN_BOTH = eyes("sharingan_both");
    private static final ResourceLocation SHARINGAN_RIGHT = eyes("sharingan_right");
    private static final ResourceLocation MANGEKYO_BOTH = eyes("mangekyo_both");
    private static final ResourceLocation MANGEKYO_RIGHT = eyes("mangekyo_right");
    private static final ResourceLocation RINNEGAN_BOTH = eyes("rinnegan_both");
    private static final ResourceLocation BYAKUGAN_BOTH = eyes("byakugan_both");
    private static final ResourceLocation SAGE_BOTH = eyes("sage_both");

    private static ResourceLocation eyes(String name) {
        return new ResourceLocation(NarutoMod.MOD_ID, PATH + name + ".png");
    }

    public PlayerEyeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled()) {
                return;
            }
            ResourceLocation texture = chooseTexture(ninjaData);
            if (texture == null) {
                return;
            }
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.eyes(texture));
            this.getParentModel().head.render(poseStack, consumer,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        });
    }

    /**
     * Which eyes are showing.
     *
     * Ordered by how much a given state overrides the face. The Rinnegan is the end of the
     * line and outranks everything; a Mangekyo outranks the plain Sharingan it grew out of;
     * Sage Mode's markings are worn over whatever eye is underneath, so it sits last and only
     * shows when no dojutsu is open.
     *
     * A transplanted Sharingan shows in one eye only, which is the whole point of a transplant
     * and is why Kakashi keeps his headband down. A born Uchiha awakens both.
     */
    private ResourceLocation chooseTexture(INinjaData ninjaData) {
        if (ninjaData.isRinneganAwakened()) {
            return RINNEGAN_BOTH;
        }
        if (ninjaData.isSharinganActive()) {
            boolean oneEye = ninjaData.isTransplantedSharingan();
            if (ninjaData.isMangekyoAwakened()) {
                return oneEye ? MANGEKYO_RIGHT : MANGEKYO_BOTH;
            }
            return oneEye ? SHARINGAN_RIGHT : SHARINGAN_BOTH;
        }
        if (ninjaData.isByakuganActive()) {
            return BYAKUGAN_BOTH;
        }
        if (ninjaData.isSageModeActive()) {
            return SAGE_BOTH;
        }
        return null;
    }
}
