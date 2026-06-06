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

public class ByakuganOverlayGUI implements PlayerGUI {

    private static final ResourceLocation TEXTURE = new ResourceLocation(NarutoMod.MOD_ID, "textures/effects/byakugan/byakugan.png");

    private final Minecraft minecraft;
    private boolean active;
    private int range;

    public ByakuganOverlayGUI(Minecraft minecraft) {
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
        guiGraphics.fill(0, 0, width, height, 0x160A2230);
        guiGraphics.fill(0, 0, width, 16, 0x330E2835);
        guiGraphics.fill(0, height - 16, width, height, 0x330E2835);
        int iconSize = this.range >= 400 ? 46 : 38;
        guiGraphics.blit(TEXTURE, width - iconSize - 8, 60, 0, 0, iconSize, iconSize, iconSize, iconSize);
        RenderSystem.disableBlend();
    }

    @Override
    public void tick(Player player) {
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            this.active = ninjaData.isByakuganActive();
            this.range = ninjaData.getByakuganRange();
        });
    }
}
