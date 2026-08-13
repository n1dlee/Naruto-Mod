package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.client.model.entity.KuramaAvatarModel;
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

    private static KuramaTailModel model;
    private static KuramaAvatarModel avatarModel;

    /**
     * Kurama's Full Avatar is canonically comparable to or bigger than Susanoo's Complete
     * Body (roughly "Hokage Rock"-sized once Naruto has Six Paths chakra) — far larger than
     * the old flat 1.0-1.3x range, which made the endgame form barely bigger than the player
     * it was supposed to be hiding. This only applies at tail 9 (exoStage 3); the worn
     * exoskeleton stages (tails 4-8) stay close to human scale since the player model is
     * still visible underneath them. Tuned against KuramaAvatarModel's new ground-anchored
     * full-body geometry (~55 units / ~3.4 blocks tall unscaled, legs to ear-tip).
     */
    private static final float FULL_AVATAR_BASE_SCALE = 14.0f;

    public static void setModel(KuramaTailModel bakedModel) {
        model = bakedModel;
    }

    public static void setAvatarModel(KuramaAvatarModel bakedModel) {
        avatarModel = bakedModel;
    }

    // Exposed for BossKuramaLayer so the Naruto boss manifests exactly the fox the player
    // does, instead of a second, quietly divergent copy of the same geometry.

    static KuramaAvatarModel avatarModel() {
        return avatarModel;
    }

    static ResourceLocation avatarTexture() {
        return AVATAR_TEXTURE;
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

        // Tails 4+: growing fox exoskeleton (worn claws -> full looming avatar at tail 9),
        // rendered in its own independent scope so its translate/scale amounts are plain
        // world units, not compounded by tailScale (this used to nest inside the tail-scaled
        // block, which threw the Full Avatar's position and size off by an extra tailScale
        // multiplier on top of its own).
        if (tailCount >= 4 && avatarModel != null) {
            VertexConsumer avatarConsumer = bufferSource.getBuffer(AVATAR_RENDER_TYPE);
            int exoStage = tailCount >= 9 ? 3 : (tailCount >= 8 ? 2 : 1);
            avatarModel.setStage(exoStage);

            // NOTE: KuramaAvatarModel (unlike KuramaTailModel) is authored in the standard
            // vanilla entity-model convention (negative Y = up), so both branches below use
            // the vanilla flip scale(-S,-S,S). The tails above are authored for the unflipped
            // renderer and must NOT get the flip.
            poseStack.pushPose();
            if (exoStage < 3) {
                // Worn stages: stay fitted on the player's own body, no extra offset/bob
                float wornScale = 0.9f + power * 0.3f;
                poseStack.scale(-wornScale, -wornScale, wornScale);
            } else {
                // Full Avatar: a genuine giant standing on the ground, not a slightly-bigger
                // shell floating behind the player — this is the form that hides the player
                // model entirely, so it needs to actually read as "the thing you're now
                // piloting from inside." The -0.9 cancels out the translate(0, 0.9, 0.15)
                // applied above (which is meant for the tails, not this ground-anchored
                // body) so the model's own legs (Y=0 to Y=-22) land at the player's real feet.
                poseStack.translate(0.0, -0.9, 0.6);
                float bob = (float) Math.sin(ageInTicks * 0.05) * 0.15f;
                poseStack.translate(0.0, bob, 0.0);
                float avatarScale = FULL_AVATAR_BASE_SCALE + power * 3.0f;
                poseStack.scale(-avatarScale, -avatarScale, avatarScale);
            }
            avatarModel.renderToBuffer(poseStack, avatarConsumer, packedLight, OverlayTexture.NO_OVERLAY,
                    1.0f, 0.45f, 0.1f, 0.35f);
            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
