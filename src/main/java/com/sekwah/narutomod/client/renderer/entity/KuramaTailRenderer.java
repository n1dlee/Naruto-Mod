package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.client.model.entity.KuramaAvatarModel;
import com.sekwah.narutomod.client.model.entity.KuramaFoxModel;
import com.sekwah.narutomod.client.model.entity.KuramaTailModel;
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
 * Renders 1/4/9 Kurama chakra tails behind the player based on
 * {@code INinjaData.getKuramaTailCount()}, plus the growing fox exoskeleton (tails 4-8).
 *
 * Tails 1-8 render on Post (player still visible). Tail 9 (Full Avatar) is rendered
 * directly from RenderEvents' Pre handler instead — see {@link #renderFullAvatar} —
 * because canceling Pre to hide the player appears to also suppress Post from firing,
 * which would otherwise leave nothing rendered once the player disappears.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KuramaTailRenderer {

    private static final ResourceLocation TAIL_TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/kurama_tail.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucent(TAIL_TEXTURE);

    private static final ResourceLocation AVATAR_TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/kurama_avatar.png");
    private static final RenderType AVATAR_RENDER_TYPE = RenderType.entityTranslucent(AVATAR_TEXTURE);

    /** The imported fox's own skin, in its Kurama Chakra Mode colouring. */
    private static final ResourceLocation FOX_TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/biju/ninetailskcm.png");
    private static final RenderType FOX_RENDER_TYPE = RenderType.entityTranslucent(FOX_TEXTURE);

    private static KuramaTailModel model;
    private static KuramaAvatarModel avatarModel;
    private static KuramaFoxModel foxModel;

    /**
     * Kurama's Full Avatar stands at exactly the height every other final form does - see
     * {@link com.sekwah.narutomod.util.GiantForm}. It is canonically Susanoo's Complete Body's
     * peer, and it has to be able to fight one.
     *
     * The divisor is the fox's SKULL height, not its overall extent: the tails arc well above
     * the head, and matching those to the hitbox would shrink the body that the hitbox is
     * actually drawn around. At this scale the head fills the eighteen blocks and the tails
     * reach about thirty, which is the silhouette the form is supposed to have.
     */
    private static final float FULL_AVATAR_SCALE =
            com.sekwah.narutomod.util.GiantForm.HEIGHT_BLOCKS / KuramaFoxModel.BODY_HEIGHT_BLOCKS;

    public static void setModel(KuramaTailModel bakedModel) {
        model = bakedModel;
    }

    public static void setAvatarModel(KuramaAvatarModel bakedModel) {
        avatarModel = bakedModel;
    }

    public static void setFoxModel(KuramaFoxModel bakedModel) {
        foxModel = bakedModel;
    }

    // Exposed for BossKuramaLayer so the Naruto boss manifests exactly the fox the player
    // does, instead of a second, quietly divergent copy of the same geometry.

    static KuramaAvatarModel avatarModel() {
        return avatarModel;
    }

    static KuramaFoxModel foxModel() {
        return foxModel;
    }

    static ResourceLocation avatarTexture() {
        return AVATAR_TEXTURE;
    }

    /** The boss's fox uses the plain orange skin; the player's is the chakra-mode one. */
    static ResourceLocation foxTexture() {
        return BOSS_FOX_TEXTURE;
    }

    private static final ResourceLocation BOSS_FOX_TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/biju/ninetails.png");

    static float fullAvatarScale() {
        return FULL_AVATAR_SCALE;
    }

    /**
     * Tails 1-8 are NOT drawn here any more.
     *
     * There were two tail implementations running at once: these procedural ones, and the
     * nine tails baked into the imported BijuCloakModel that BijuCloakRenderer wears on the
     * player. Both fired on RenderPlayerEvent.Post, so a cloaked player grew two overlapping
     * sets of tails at slightly different angles - the "two in one" look.
     *
     * The worn shroud won: it is the authentic 1.12.2 silhouette, it has ears and body
     * markings to match, and it retextures per tier. This class now owns only the Full
     * Avatar at nine tails, which is a different thing entirely - a giant fox rendered in
     * place of the hidden player, from the Pre handler (see renderFullAvatar).
     */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        // Intentionally empty - kept as the documented home of the tail-rendering decision.
    }

    /**
     * Draws the full Nine-Tails avatar (tails + complete fox looming behind) in place of
     * the (now hidden) player, called directly from RenderEvents.playerRenderEvent(Pre)
     * while that event is being canceled.
     */
    public static void renderFullAvatar(RenderPlayerEvent.Pre event, INinjaData ninjaData) {
        if (model == null) {
            return;
        }
        render(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(),
                event.getEntity(), event.getPartialTick(), ninjaData.getKuramaTailCount(), ninjaData.getTransformPower());
    }

    private static void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                Player player, float partialTick, int tailCount, float power) {
        VertexConsumer consumer = bufferSource.getBuffer(RENDER_TYPE);
        float ageInTicks = player.tickCount + partialTick;
        float tailScale = 1.3f + power * 0.6f;
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);

        poseStack.pushPose();
        poseStack.translate(0.0, 0.9, 0.15);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));

        /*
         * The procedural tails stop at eight.
         *
         * At nine the Full Avatar below IS the fox, tails and all, and drawing these on top
         * of it put nine orange spokes through the model at a completely different scale.
         * With the camera also sitting too high to see the avatar, all that was left on
         * screen was the spokes - the "orange sticks and no Kurama at all" report. The class
         * comment above has claimed these were gone since the shroud was imported; this is
         * the loop that kept drawing them anyway.
         */
        if (tailCount < 9) {
            // Tails render in their own scaled sub-scope so tailScale doesn't leak into (and
            // compound with) the exoskeleton/avatar block below.
            poseStack.pushPose();
            poseStack.scale(tailScale, tailScale, tailScale);
            float arcSpread = tailCount == 1 ? 0f : 140f / (tailCount - 1);
            float startAngle = tailCount == 1 ? 0f : -70f;

            for (int i = 0; i < tailCount; i++) {
                float angle = startAngle + i * arcSpread;
                float wavePhase = (ageInTicks + i * 6) * 0.15f;
                float wave = (float) Math.sin(wavePhase) * 0.12f;

                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(angle));
                model.animate(wave);
                model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                        1.0f, 0.5f, 0.1f, 0.9f);
                poseStack.popPose();
            }
            poseStack.popPose();
        }

        // Tails 4+: growing fox exoskeleton (worn claws -> full looming avatar at tail 9),
        // rendered in its own independent scope so its translate/scale amounts are plain
        // world units, not compounded by tailScale (this used to nest inside the tail-scaled
        // block, which threw the Full Avatar's position and size off by an extra tailScale
        // multiplier on top of its own).
        if (tailCount >= 9 && foxModel != null) {
            // Full Avatar: the imported fox itself, not a stand-in. Everything below draws
            // in the standard vanilla entity convention (model +Y runs downward), so it takes
            // the vanilla flip scale(-S,-S,S). The procedural tails above are authored for
            // the unflipped renderer and must NOT get that flip.
            poseStack.pushPose();
            // The -0.9 cancels the translate(0, 0.9, 0.15) applied above, which is meant for
            // the tails and not for a body that has to stand on the ground.
            poseStack.translate(0.0, -0.9, 0.6);
            float bob = (float) Math.sin(ageInTicks * 0.05) * 0.15f;
            poseStack.translate(0.0, bob, 0.0);
            // No power-surge term: the boss's fox has no surge to match, and a form whose
            // size depends on the scroll wheel cannot be the same size as its opposite
            // number. The surge still drives damage and duration.
            poseStack.scale(-FULL_AVATAR_SCALE, -FULL_AVATAR_SCALE, FULL_AVATAR_SCALE);
            // This model's soles sit at +FEET_OFFSET, not at zero. Undoing that after the
            // scale (so the shift is in model units) is what puts the fox on the ground
            // instead of buried to the ribs.
            poseStack.translate(0.0, -KuramaFoxModel.FEET_OFFSET, 0.0);

            // Shared baked instance: pose it every frame, never assume the last caller left
            // it where this one wants it.
            foxModel.waveTails(ageInTicks);
            foxModel.renderToBuffer(poseStack, bufferSource.getBuffer(FOX_RENDER_TYPE), packedLight,
                    OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 0.9f);
            poseStack.popPose();
        } else if (tailCount >= 4 && avatarModel != null) {
            // Worn stages (tails 4-8): claw plating fitted on the player's own forearms. The
            // hand-built KuramaAvatarModel still owns these - it was only ever wrong as a
            // whole fox.
            VertexConsumer avatarConsumer = bufferSource.getBuffer(AVATAR_RENDER_TYPE);
            avatarModel.setStage(tailCount >= 8 ? 2 : 1);

            poseStack.pushPose();
            float wornScale = 0.9f + power * 0.3f;
            poseStack.scale(-wornScale, -wornScale, wornScale);
            avatarModel.renderToBuffer(poseStack, avatarConsumer, packedLight, OverlayTexture.NO_OVERLAY,
                    1.0f, 0.45f, 0.1f, 0.8f);
            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
