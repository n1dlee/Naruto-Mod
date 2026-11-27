package com.sekwah.narutomod.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;


public class NarutoInGameGUI {

    private final ChakraAndStaminaGUI charkaOverlay;
    private final SubstitutionGUI substitutionOverlay;
    private final WorldMarkerGUI worldMarkerOverlay;
    private final SharinganOverlayGUI sharinganOverlay;
    private final RinneganOverlayGUI rinneganOverlay;
    private final ByakuganOverlayGUI byakuganOverlay;
    private final ByakuganEntityVisionGUI byakuganEntityVisionOverlay;
    private final SageModeOverlayGUI sageModeOverlay;
    private final KuramaCloakOverlayGUI kuramaCloakOverlay;
    private final SusanooOverlayGUI susanooOverlay;
    private final KuramaChakraModeOverlayGUI kcmOverlay;
    private final JutsuHudGUI jutsuHudOverlay;
    private final ActiveJutsuGUI activeJutsuOverlay;
    private final Minecraft minecraft;

    private final PlayerGUI[] overlays;

    /**
     * Will be false if the entity is not a ninja
     */
    private boolean shouldRender;

    private static Matrix4f worldMatrix;
    private static Vec3 cameraPos;

    public NarutoInGameGUI(){
        this.minecraft = Minecraft.getInstance();
        this.charkaOverlay = new ChakraAndStaminaGUI(this.minecraft);
        this.substitutionOverlay = new SubstitutionGUI(this.minecraft);
        this.worldMarkerOverlay = new WorldMarkerGUI(this.minecraft);
        this.sharinganOverlay = new SharinganOverlayGUI(this.minecraft);
        this.rinneganOverlay = new RinneganOverlayGUI(this.minecraft);
        this.byakuganOverlay = new ByakuganOverlayGUI(this.minecraft);
        this.byakuganEntityVisionOverlay = new ByakuganEntityVisionGUI(this.minecraft);
        this.sageModeOverlay = new SageModeOverlayGUI(this.minecraft);
        this.kuramaCloakOverlay = new KuramaCloakOverlayGUI(this.minecraft);
        this.susanooOverlay = new SusanooOverlayGUI(this.minecraft);
        this.kcmOverlay = new KuramaChakraModeOverlayGUI(this.minecraft);
        this.jutsuHudOverlay = new JutsuHudGUI(this.minecraft);
        this.activeJutsuOverlay = new ActiveJutsuGUI(this.minecraft);

        this.overlays = new PlayerGUI[]{this.byakuganEntityVisionOverlay, this.byakuganOverlay, this.sharinganOverlay, this.rinneganOverlay,
                this.sageModeOverlay, this.kuramaCloakOverlay, this.susanooOverlay, this.kcmOverlay, this.worldMarkerOverlay,
                this.substitutionOverlay, this.charkaOverlay, this.jutsuHudOverlay, this.activeJutsuOverlay, new SusanooIntegrityGUI(this.minecraft)};

        MinecraftForge.EVENT_BUS.addListener(this::renderGameOverlay);
        MinecraftForge.EVENT_BUS.addListener(this::clientTickEvent);
        MinecraftForge.EVENT_BUS.addListener(this::renderLevelLast);
    }

    // TODO switch over to new renderer
//    @SubscribeEvent
//    public void registerOverlays(RegisterGuiOverlaysEvent event) {
//        for (var overlay : this.overlays) {
//            event.registerAboveAll("test", overlay);
//        }
//    }


    public static void registerEvents() {
    }

    @SubscribeEvent
    public void clientTickEvent(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = this.minecraft.player;
        if (player != null && !player.isSpectator()) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                shouldRender = ninjaData.isNinjaModeEnabled();
                // Show clan selection screen if ninja mode enabled but no clan chosen
                if (ninjaData.isNinjaModeEnabled() && ninjaData.getClanId().isEmpty()
                        && minecraft.screen == null) {
                    minecraft.setScreen(new ClanSelectionScreen());
                }
            });
            for (PlayerGUI  overlay : overlays) {
                overlay.tick(player);
            }
        } else {
            shouldRender = false;
            worldMatrix = null;
            cameraPos = null;
        }
    }

    /**
     * THIS SHOULD NOT BE DONE THIS WAY, TAKE A LOOK AT THE NEW RegisterGuiOverlaysEvent
     *
     * This was just a quick fix for now so I could focus on updating other parts.
     *
     * @deprecated
     * @param event
     */
    @SubscribeEvent
    @Deprecated
    public void renderGameOverlay(RenderGuiOverlayEvent.Post event) {
        if(event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }
        if(!shouldRender) {
            return;
        }
        if(Minecraft.getInstance().options.hideGui) {
            return;
        }
        GuiGraphics guiGraphics = event.getGuiGraphics();
        for (PlayerGUI  overlay : overlays) {
            overlay.render(guiGraphics, worldMatrix, cameraPos);
        }
    }

    @SubscribeEvent
    public void renderLevelLast(RenderLevelStageEvent event) {
        if(event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if(mc.options.hideGui || mc.level == null || mc.player == null) {
            return;
        }
        Camera camera = mc.getEntityRenderDispatcher().camera;
        //noinspection ConstantConditions the IDE inspects this as not possible to be null. There has been at least one error report proving this to be false.
        if(camera == null) return;
        PoseStack poseStack = event.getPoseStack();
        worldMatrix = new Matrix4f(event.getProjectionMatrix());
        worldMatrix.mul(poseStack.last().pose());
        cameraPos = camera.getPosition();
        //float partialTicks = event.getPartialTick();
        //MultiBufferSource multiBufferSource = mc.renderBuffers().bufferSource();
    }

}
