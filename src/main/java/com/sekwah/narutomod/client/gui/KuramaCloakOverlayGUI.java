package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.config.NarutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class KuramaCloakOverlayGUI implements PlayerGUI {
    private final Minecraft minecraft;
    private int tails;

    public KuramaCloakOverlayGUI(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void render(GuiGraphics gui, Matrix4f worldMatrix, Vec3 cameraPos) {
        if (tails == 0 || NarutoConfig.reducedBodyRiteEffects || minecraft.getCameraEntity() != minecraft.player) {
            return;
        }
        int width = gui.guiWidth();
        int height = gui.guiHeight();
        int edge = ((22 + tails * 2) << 24) | 0x8E251D;
        int transparent = edge & 0xFFFFFF;
        gui.fillGradient(0, 0, width, 18, edge, transparent);
        gui.fillGradient(0, height - 18, width, height, transparent, edge);
    }

    @Override
    public void tick(Player player) {
        this.tails = player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                .map(data -> data.isKuramaCloakActive() ? data.getKuramaTailCount() : 0).orElse(0);
    }
}
