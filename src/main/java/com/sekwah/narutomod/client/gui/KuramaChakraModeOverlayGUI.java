package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.config.NarutoConfig;
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
        if (!this.active || NarutoConfig.reducedBodyRiteEffects || this.minecraft.getCameraEntity() != this.minecraft.player) {
            return;
        }
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        guiGraphics.fillGradient(0, 0, width, 12, 0x26FAD782, 0x00FAD782);
        guiGraphics.fillGradient(0, height - 12, width, height, 0x00FAD782, 0x26FAD782);
    }

    @Override
    public void tick(Player player) {
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            this.active = ninjaData.isKcmActive();
        });
    }
}
