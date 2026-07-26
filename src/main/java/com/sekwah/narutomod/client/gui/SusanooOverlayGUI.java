package com.sekwah.narutomod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Purple/blue vignette overlay while Susanoo is active.
 */
public class SusanooOverlayGUI implements PlayerGUI {

    private final Minecraft minecraft;
    private boolean active;

    public SusanooOverlayGUI(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void render(GuiGraphics guiGraphics, Matrix4f worldMatrix, Vec3 cameraPos) {
        if (!this.active) return;

        int width = this.minecraft.getWindow().getGuiScaledWidth();
        int height = this.minecraft.getWindow().getGuiScaledHeight();

        RenderSystem.enableBlend();

        int alpha = 40;
        int vignetteColor = (alpha << 24) | 0x5A1F8C;

        guiGraphics.fill(0, 0, width, 16, vignetteColor);
        guiGraphics.fill(0, height - 16, width, height, vignetteColor);
        guiGraphics.fill(0, 0, 12, height, vignetteColor);
        guiGraphics.fill(width - 12, 0, width, height, vignetteColor);

        float pulse = (float) (0.4 + 0.3 * Math.sin(System.currentTimeMillis() / 250.0));
        int pulseAlpha = (int) (pulse * 28);
        int pulseColor = (pulseAlpha << 24) | 0x7A3FD0;
        guiGraphics.fill(0, 0, width, 8, pulseColor);
        guiGraphics.fill(0, height - 8, width, height, pulseColor);

        RenderSystem.disableBlend();
    }

    @Override
    public void tick(Player player) {
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            this.active = ninjaData.isSusanooActive();
        });
    }
}
