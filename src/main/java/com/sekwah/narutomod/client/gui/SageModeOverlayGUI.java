package com.sekwah.narutomod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Orange/golden vignette overlay while Sage Mode is active.
 * Similar style to SharinganOverlayGUI but with warm orange tones.
 */
public class SageModeOverlayGUI implements PlayerGUI {

    private final Minecraft minecraft;
    private boolean active;
    private int ticksRemaining;

    public SageModeOverlayGUI(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void render(GuiGraphics guiGraphics, Matrix4f worldMatrix, Vec3 cameraPos) {
        if (!this.active) {
            return;
        }

        int width = this.minecraft.getWindow().getGuiScaledWidth();
        int height = this.minecraft.getWindow().getGuiScaledHeight();

        RenderSystem.enableBlend();

        // Orange-golden vignette edges
        int alpha = 30;
        int vignetteColor = (alpha << 24) | 0xCC7700; // Warm orange

        // Top edge
        guiGraphics.fill(0, 0, width, 12, vignetteColor);
        // Bottom edge
        guiGraphics.fill(0, height - 12, width, height, vignetteColor);
        // Left edge
        guiGraphics.fill(0, 0, 8, height, vignetteColor);
        // Right edge
        guiGraphics.fill(width - 8, 0, width, height, vignetteColor);

        // Pulsing inner glow (subtle)
        float pulse = (float)(0.3 + 0.2 * Math.sin(System.currentTimeMillis() / 400.0));
        int pulseAlpha = (int)(pulse * 20);
        int pulseColor = (pulseAlpha << 24) | 0xFF8800;
        guiGraphics.fill(0, 0, width, 6, pulseColor);
        guiGraphics.fill(0, height - 6, width, height, pulseColor);

        RenderSystem.disableBlend();
    }

    @Override
    public void tick(Player player) {
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            this.active = ninjaData.isSageModeActive();
            this.ticksRemaining = ninjaData.getSageModeTicks();
        });
    }
}
