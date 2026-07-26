package com.sekwah.narutomod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Shows the Rinnegan (or Rinne Sharingan) crest in the corner.
 *
 * Unlike the Sharingan and Byakugan this eye is never toggled off, so it deliberately
 * draws no screen tint — a permanent vignette would be exhausting to play behind. The
 * icon alone marks that the Six Paths are available.
 */
public class RinneganOverlayGUI implements PlayerGUI {

    private static final ResourceLocation RINNEGAN =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/effects/rinnegan/rinnegan.png");
    private static final ResourceLocation RINNE_SHARINGAN =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/effects/rinnegan/rinne_sharingan.png");
    private static final int ICON_SIZE = 36;

    private final Minecraft minecraft;
    private boolean awakened;
    private boolean rinneSharingan;
    /** True while a Sharingan overlay is already drawing in the same corner. */
    private boolean sharinganDrawing;

    public RinneganOverlayGUI(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void render(GuiGraphics guiGraphics, Matrix4f worldMatrix, Vec3 cameraPos) {
        if (!this.awakened) {
            return;
        }
        int width = this.minecraft.getWindow().getGuiScaledWidth();
        // Slide down when the Sharingan crest already occupies the top-right slot
        int y = this.sharinganDrawing ? 8 + 48 + 6 : 8;

        RenderSystem.enableBlend();
        guiGraphics.blit(this.rinneSharingan ? RINNE_SHARINGAN : RINNEGAN,
                width - ICON_SIZE - 8, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        RenderSystem.disableBlend();
    }

    @Override
    public void tick(Player player) {
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            this.rinneSharingan = ninjaData.isRinneSharinganAwakened();
            this.awakened = this.rinneSharingan || ninjaData.isRinneganAwakened();
            this.sharinganDrawing = ninjaData.isSharinganActive() && ninjaData.getSharinganLevel() > 0;
        });
    }
}
