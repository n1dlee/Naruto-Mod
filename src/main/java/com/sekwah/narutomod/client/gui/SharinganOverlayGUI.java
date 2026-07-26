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

public class SharinganOverlayGUI implements PlayerGUI {

    private static final ResourceLocation[] TEXTURES = {
            null,
            new ResourceLocation(NarutoMod.MOD_ID, "textures/effects/sharingan/sharingan_1.png"),
            new ResourceLocation(NarutoMod.MOD_ID, "textures/effects/sharingan/sharingan_2.png"),
            new ResourceLocation(NarutoMod.MOD_ID, "textures/effects/sharingan/sharingan_3.png"),
            new ResourceLocation(NarutoMod.MOD_ID, "textures/effects/sharingan/mangekyou.png")
    };

    private static ResourceLocation emsTexture(String form) {
        return new ResourceLocation(NarutoMod.MOD_ID, "textures/effects/sharingan/ems_" + form + ".png");
    }

    private final Minecraft minecraft;
    private boolean active;
    private int level;
    /** Phase 16: which pinwheel to draw once the Mangekyo has gone Eternal. */
    private String emsForm = "";

    public SharinganOverlayGUI(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void render(GuiGraphics guiGraphics, Matrix4f worldMatrix, Vec3 cameraPos) {
        if (!this.active || this.level <= 0) {
            return;
        }

        int width = this.minecraft.getWindow().getGuiScaledWidth();
        int height = this.minecraft.getWindow().getGuiScaledHeight();
        int alpha = this.level >= 4 ? 46 : 32;

        RenderSystem.enableBlend();
        guiGraphics.fill(0, 0, width, height, (alpha << 24) | 0x550000);
        guiGraphics.fill(0, 0, width, 18, 0x66000000);
        guiGraphics.fill(0, height - 18, width, height, 0x66000000);

        ResourceLocation texture = this.emsForm.isEmpty()
                ? TEXTURES[Math.min(this.level, 4)]
                : emsTexture(this.emsForm);
        int iconSize = this.level >= 4 ? 48 : 40;
        guiGraphics.blit(texture, width - iconSize - 8, 8, 0, 0, iconSize, iconSize, iconSize, iconSize);
        RenderSystem.disableBlend();
    }

    @Override
    public void tick(Player player) {
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            this.active = ninjaData.isSharinganActive();
            this.level = ninjaData.getSharinganLevel();
            this.emsForm = ninjaData.isEternalMangekyoAwakened() ? ninjaData.getMangekyoForm() : "";
        });
    }
}
