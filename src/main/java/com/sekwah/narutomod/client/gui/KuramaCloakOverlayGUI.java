package com.sekwah.narutomod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Red/orange vignette overlay while Kurama Cloak is active.
 * More intense than Sage Mode overlay — pulsing red edges.
 */
public class KuramaCloakOverlayGUI implements PlayerGUI {

    private final Minecraft minecraft;
    private boolean active;

    public KuramaCloakOverlayGUI(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void render(GuiGraphics guiGraphics, Matrix4f worldMatrix, Vec3 cameraPos) {
        if (!this.active) return;

        int width = this.minecraft.getWindow().getGuiScaledWidth();
        int height = this.minecraft.getWindow().getGuiScaledHeight();

        RenderSystem.enableBlend();

        // Red vignette edges (more intense than sage mode)
        int alpha = 45;
        int vignetteColor = (alpha << 24) | 0xCC2200;

        // Edges
        guiGraphics.fill(0, 0, width, 16, vignetteColor);
        guiGraphics.fill(0, height - 16, width, height, vignetteColor);
        guiGraphics.fill(0, 0, 12, height, vignetteColor);
        guiGraphics.fill(width - 12, 0, width, height, vignetteColor);

        // Pulsing inner glow (faster pulse than sage mode)
        float pulse = (float)(0.4 + 0.3 * Math.sin(System.currentTimeMillis() / 200.0));
        int pulseAlpha = (int)(pulse * 30);
        int pulseColor = (pulseAlpha << 24) | 0xFF4400;
        guiGraphics.fill(0, 0, width, 8, pulseColor);
        guiGraphics.fill(0, height - 8, width, height, pulseColor);

        RenderSystem.disableBlend();
    }

    @Override
    public void tick(Player player) {
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            this.active = ninjaData.isKuramaCloakActive();
        });
    }
}
