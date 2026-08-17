package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.client.model.entity.SusanooClothedModel;
import com.sekwah.narutomod.client.model.entity.SusanooModel;
import com.sekwah.narutomod.client.model.entity.SusanooSkeletonModel;
import com.sekwah.narutomod.client.model.entity.SusanooWingedModel;
import net.minecraft.client.model.Model;
import com.sekwah.narutomod.entity.MangekyoBossVariant;
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
 * Renders the Susanoo ribcage/avatar around the player based on
 * {@code INinjaData.isSusanooActive()} / {@code getSusanooStage()}.
 *
 * Stages 1-3 render on Post (player still visible, avatar looms around them). Stage 4
 * (Complete Body) is rendered directly from RenderEvents' Pre handler instead — see
 * {@link #renderFullBody} — because canceling Pre to hide the player appears to also
 * suppress Post from firing, which would otherwise leave nothing rendered at stage 4.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SusanooRenderer {

    private static final ResourceLocation SUSANOO_TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/susanoo.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucent(SUSANOO_TEXTURE);

    /**
     * Canon-shaped progression: stages 1-3 are a ribcage/torso hovering AROUND the caster
     * (slightly larger than the player, growing modestly per stage), and only Complete Body
     * (stage 4) becomes the towering giant. Tuned against SusanooModel's geometry: the hover
     * torso spans ~34 units (~2.1 blocks unscaled), so stage 1 at 1.8x wraps a player-sized
     * ribcage; stage 4 at 13x with the torso lifted onto legs stands ~45 blocks tall.
     */
    private static final float[] STAGE_BASE_SCALE = {0f, 1.8f, 2.4f, 3.2f, 13.0f};

    /** Colour of an un-formed Mangekyo's Susanoo — the original violet. */
    private static final float DEFAULT_RED = 0.55f;
    private static final float DEFAULT_GREEN = 0.30f;
    private static final float DEFAULT_BLUE = 0.85f;
    private static final float ALPHA = 0.6f;

    private static SusanooModel model;

    /**
     * Phase 18: the detailed bodies ported from the 1.12.2 mod's Susanoo entities, used in
     * place of the blocky fallback once they are baked. Stages map onto the canon
     * progression: skeleton for the early ribcage, clothed for the armoured torso,
     * winged for Complete Body.
     */
    private static SusanooSkeletonModel skeletonModel;
    private static SusanooClothedModel clothedModel;
    private static SusanooWingedModel wingedModel;

    private static final ResourceLocation SKELETON_TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/susanooskeleton.png");
    private static final ResourceLocation CLOTHED_TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/susanoo_clothed.png");
    private static final ResourceLocation WINGED_TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/susanoo_winged.png");

    /**
     * Measured extents of the ported bodies, in model units (16 units = 1 block):
     * total height, and how far the geometry reaches "down" the model's +Y axis. The
     * three were authored at wildly different scales — the skeleton is a 5-block giant
     * while the clothed/winged bodies are player-sized — so a single multiplier cannot
     * serve all three. Instead each stage declares the height it wants in blocks and the
     * scale is derived from that.
     */
    private static final float SKELETON_HEIGHT_U = 86.5f;
    private static final float SKELETON_BOTTOM_U = 50.5f;
    private static final float CLOTHED_HEIGHT_U = 33.5f;
    private static final float CLOTHED_BOTTOM_U = 24.1f;
    private static final float WINGED_HEIGHT_U = 37.0f;
    private static final float WINGED_BOTTOM_U = 24.0f;

    /**
     * How tall the Susanoo should actually stand, in blocks, per stage.
     *
     * Stage 4 comes from {@link com.sekwah.narutomod.util.GiantForm} rather than a number of
     * its own, because the boss's Complete Body, the boss's Kurama and the player's Kurama all
     * have to land on the same height - they are meant to fight each other.
     */
    private static final float[] STAGE_TARGET_HEIGHT =
            {0f, 3.2f, 4.6f, 6.5f, com.sekwah.narutomod.util.GiantForm.HEIGHT_BLOCKS};

    public static void setModel(SusanooModel bakedModel) {
        model = bakedModel;
    }

    public static void setDetailedModels(SusanooSkeletonModel skeleton, SusanooClothedModel clothed,
                                         SusanooWingedModel winged) {
        skeletonModel = skeleton;
        clothedModel = clothed;
        wingedModel = winged;
    }

    static boolean detailedReady() {
        return skeletonModel != null && clothedModel != null && wingedModel != null;
    }

    // --- Shared with BossSusanooLayer -------------------------------------------------
    // The bosses manifest the same Susanoo the player does, so the model/texture/geometry
    // choice per stage lives here once instead of being copied and drifting out of sync.

    static Model detailedBodyForStage(int stage) {
        return stage >= 4 ? wingedModel : stage == 3 ? clothedModel : skeletonModel;
    }

    static ResourceLocation detailedTextureForStage(int stage) {
        return stage >= 4 ? WINGED_TEXTURE : stage == 3 ? CLOTHED_TEXTURE : SKELETON_TEXTURE;
    }

    static float detailedHeightForStage(int stage) {
        return stage >= 4 ? WINGED_HEIGHT_U : stage == 3 ? CLOTHED_HEIGHT_U : SKELETON_HEIGHT_U;
    }

    static float detailedBottomForStage(int stage) {
        return stage >= 4 ? WINGED_BOTTOM_U : stage == 3 ? CLOTHED_BOTTOM_U : SKELETON_BOTTOM_U;
    }

    static float targetHeightForStage(int stage) {
        return STAGE_TARGET_HEIGHT[Mth.clamp(stage, 0, STAGE_TARGET_HEIGHT.length - 1)];
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
            if (!ninjaData.isSusanooActive()) {
                return;
            }
            int stage = ninjaData.getSusanooStage();
            // Stage 4 (Complete Body) is rendered via renderFullBody() from Pre instead,
            // since the player is hidden at that point and Post won't fire for them.
            if (stage <= 0 || stage >= 4) {
                return;
            }

            render(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(),
                    player, event.getPartialTick(), stage, ninjaData.getTransformPower(),
                    ninjaData.getMangekyoForm(), ninjaData.getSusanooColor());
        });
    }

    /**
     * Draws the full Complete Body Susanoo in place of the (now hidden) player, called
     * directly from RenderEvents.playerRenderEvent(Pre) while that event is being canceled.
     */
    public static void renderFullBody(RenderPlayerEvent.Pre event, INinjaData ninjaData) {
        if (model == null) {
            return;
        }
        render(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(),
                event.getEntity(), event.getPartialTick(), ninjaData.getSusanooStage(),
                ninjaData.getTransformPower(), ninjaData.getMangekyoForm(),
                ninjaData.getSusanooColor());
    }

    /**
     * Which colour the shell is drawn in.
     *
     * A wielder's Mangekyo form sets a canon default - Itachi red-orange, Madara blue - but a
     * chosen colour overrides it. Uchiha inherit an eye, not a palette, and picking the shade
     * of your own Susanoo is the one piece of self-expression the form system never offered.
     *
     * @param customColor packed 0xRRGGBB, or negative for "use the form's own colour"
     * @param fallback    used when there is neither a form nor a choice
     */
    private static float[] resolveTint(MangekyoBossVariant form, int customColor, float[] fallback) {
        if (customColor >= 0) {
            return new float[]{
                    ((customColor >> 16) & 0xFF) / 255f,
                    ((customColor >> 8) & 0xFF) / 255f,
                    (customColor & 0xFF) / 255f};
        }
        if (form == null) {
            return fallback;
        }
        return new float[]{form.susanooRed(), form.susanooGreen(), form.susanooBlue()};
    }

    private static void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                Player player, float partialTick, int stage, float power,
                                String mangekyoForm, int customColor) {
        // The sword swing, on whichever body is about to be drawn. Wrapped around both
        // branches rather than repeated inside each: the ported meshes and the procedural
        // model are the same giant as far as the strike is concerned.
        poseStack.pushPose();
        com.sekwah.narutomod.client.renderer.GiantSwing.apply(poseStack,
                player.getCapability(com.sekwah.narutomod.capabilities.NinjaCapabilityHandler.NINJA_DATA)
                        .map(com.sekwah.narutomod.capabilities.INinjaData::getSusanooSwingTicks).orElse(0),
                com.sekwah.narutomod.capabilities.NinjaData.SUSANOO_SWING_TICKS, partialTick);

        if (detailedReady()) {
            renderDetailed(poseStack, bufferSource, packedLight, player, partialTick, stage, power,
                    mangekyoForm, customColor);
            poseStack.popPose();
            return;
        }
        VertexConsumer consumer = bufferSource.getBuffer(RENDER_TYPE);

        poseStack.pushPose();
        // No offsets at all: the hover ribcage (stages 1-3) is authored centered on the
        // caster and the Complete Body's legs are ground-anchored at local Y=0, so the
        // player's own position is the correct origin for every stage.
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        float scale = STAGE_BASE_SCALE[Mth.clamp(stage, 1, 4)] + power * 2.0f;
        // Vanilla model-space flip: SusanooModel is authored in the standard entity-model
        // convention (negative Y = up), which vanilla renderers handle via scale(-1,-1,1).
        // Without this flip everything authored "upward" renders straight down into the
        // ground — this was the visible "avatar sinks into the terrain" bug. Negating both
        // X and Y keeps the matrix determinant positive, so face winding/culling stay valid.
        poseStack.scale(-scale, -scale, scale);

        model.setStage(stage);
        // Phase 16: the shell takes the colour of whichever Mangekyo the wielder carries —
        // Itachi's red-orange, Madara's blue, and so on. Plain Mangekyo keeps the violet.
        MangekyoBossVariant form = MangekyoBossVariant.byFormId(mangekyoForm);
        float[] tint = resolveTint(form, customColor,
                new float[]{DEFAULT_RED, DEFAULT_GREEN, DEFAULT_BLUE});
        float red = tint[0];
        float green = tint[1];
        float blue = tint[2];
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                red, green, blue, ALPHA);

        poseStack.popPose();
        poseStack.popPose(); // the swing scope opened at the top
    }

    /**
     * Draws one of the detailed ported bodies. These carry their own painted texture, so
     * the per-form colour is applied as a light tint on top rather than as the whole
     * colour — otherwise the artwork would be flattened into a single hue.
     */
    private static void renderDetailed(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                       Player player, float partialTick, int stage, float power,
                                       String mangekyoForm, int customColor) {
        int clamped = Mth.clamp(stage, 1, 4);
        Model body;
        ResourceLocation texture;
        float modelHeightU;
        float modelBottomU;
        if (clamped >= 4) {
            body = wingedModel;
            texture = WINGED_TEXTURE;
            modelHeightU = WINGED_HEIGHT_U;
            modelBottomU = WINGED_BOTTOM_U;
        } else if (clamped == 3) {
            body = clothedModel;
            texture = CLOTHED_TEXTURE;
            modelHeightU = CLOTHED_HEIGHT_U;
            modelBottomU = CLOTHED_BOTTOM_U;
        } else {
            body = skeletonModel;
            texture = SKELETON_TEXTURE;
            modelHeightU = SKELETON_HEIGHT_U;
            modelBottomU = SKELETON_BOTTOM_U;
        }
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));

        MangekyoBossVariant form = MangekyoBossVariant.byFormId(mangekyoForm);
        // Blend the form colour only part-way toward white so the painted detail survives
        float[] base = resolveTint(form, customColor, new float[]{1.0f, 1.0f, 1.0f});
        boolean tinted = form != null || customColor >= 0;
        float red = tinted ? 0.5f + base[0] * 0.5f : 1.0f;
        float green = tinted ? 0.5f + base[1] * 0.5f : 1.0f;
        float blue = tinted ? 0.5f + base[2] * 0.5f : 1.0f;

        // Derive the scale from the height this stage should stand, so each body lands at
        // the right size regardless of how it happened to be authored.
        //
        // No power-surge multiplier. It used to add up to another 35% here and nowhere else,
        // so a player at full surge stood a third taller than Madara's Susanoo at the same
        // stage - which is what "my Susanoo is much bigger than theirs" was. The surge still
        // does what it always did to damage and duration; it no longer silently changes who
        // is bigger in a fight between two of the same technique.
        float scale = STAGE_TARGET_HEIGHT[clamped] / (modelHeightU / 16f);

        poseStack.pushPose();
        // Lift the model so its lowest geometry rests at the player's feet. These bodies
        // are authored in the vanilla convention where +Y runs DOWNWARD, so without this
        // the whole Susanoo renders below ground — the same offset vanilla applies as its
        // translate(0, -1.501, 0) for a standard biped, generalised per model.
        poseStack.translate(0.0D, (modelBottomU / 16f) * scale, 0.0D);
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        // Same model-space flip the vanilla entity renderers apply (negative Y is up).
        poseStack.scale(-scale, -scale, scale);

        body.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                red, green, blue, DETAILED_ALPHA);
        poseStack.popPose();
    }

    /** The ported bodies are painted, so they read better closer to opaque. */
    private static final float DETAILED_ALPHA = 0.85f;
}
