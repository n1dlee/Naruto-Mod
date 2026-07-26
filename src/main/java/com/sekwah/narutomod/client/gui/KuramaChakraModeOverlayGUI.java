package com.sekwah.narutomod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Bright gold/white screen-edge glow while Kurama Chakra Mode is active — lighter and
 * brighter than the tailed Kurama Cloak's red-orange vignette, since KCM has no shell.
 */
public class KuramaChakraModeOverlayGUI implements PlayerGUI {

    private final Minecraft minecraft;
    private boolean active;

    public KuramaChakraModeOverlayGUI(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void render(GuiGraphics guiGraphics, Matrix4f worldMatrix, Vec3 cameraPos) {
        if (!this.active) return;

        int width = this.minecraft.getWindow().getGuiScaledWidth();
        int height = this.minecraft.getWindow().getGuiScaledHeight();

        RenderSystem.enableBlend();

        int alpha = 35;
        int vignetteColor = (alpha << 24) | 0xFFD966;

        guiGraphics.fill(0, 0, width, 12, vignetteColor);
        guiGraphics.fill(0, height - 12, width, height, vignetteColor);
        guiGraphics.fill(0, 0, 10, height, vignetteColor);
        guiGraphics.fill(width - 10, 0, width, height, vignetteColor);

        float pulse = (float) (0.4 + 0.3 * Math.sin(System.currentTimeMillis() / 180.0));
        int pulseAlpha = (int) (pulse * 24);
        int pulseColor = (pulseAlpha << 24) | 0xFFFFCC;
        guiGraphics.fill(0, 0, width, 6, pulseColor);
        guiGraphics.fill(0, height - 6, width, height, pulseColor);

        RenderSystem.disableBlend();
    }

    @Override
    public void tick(Player player) {
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            this.active = ninjaData.isKcmActive();
        });
    }
}
