package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.util.ByakuganSight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class ByakuganOverlayGUI implements PlayerGUI {

    private static final ResourceLocation TEXTURE = new ResourceLocation(NarutoMod.MOD_ID, "textures/effects/byakugan/byakugan.png");

    private final Minecraft minecraft;
    private Player owner;
    private int age;
    private int range;

    public ByakuganOverlayGUI(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void render(GuiGraphics guiGraphics, Matrix4f worldMatrix, Vec3 cameraPos) {
        if (this.range <= 0 || this.owner != this.minecraft.player) {
            return;
        }

        int width = this.minecraft.getWindow().getGuiScaledWidth();
        int height = this.minecraft.getWindow().getGuiScaledHeight();
        float reveal = ByakuganSight.reveal(this.age + this.minecraft.getFrameTime());
        if (this.minecraft.options.getCameraType().isFirstPerson()) {
            int mist = (int) (48 * reveal) << 24 | 0x678B94;
            int rim = Math.max(13, Math.min(32, height / 8));
            guiGraphics.fillGradient(0, 0, width, rim, mist, 0x00678B94);
            guiGraphics.fillGradient(0, height - rim, width, height, 0x00678B94, mist);
        }
        int iconSize = 36;
        int x = width - iconSize - 8;
        guiGraphics.blit(TEXTURE, x, 78, 0, 0, iconSize, iconSize, iconSize, iconSize);
        var text = Component.translatable("jutsu.byakugan.range", this.range);
        guiGraphics.drawString(this.minecraft.font, text, width - 8 - this.minecraft.font.width(text),
                116, 0xFFC6DBE2, true);
    }

    @Override
    public void tick(Player player) {
        if (this.owner != player) {
            this.owner = player;
            this.range = 0;
            this.age = 0;
        }
        this.range = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).map(data -> data.getByakuganRange()).orElse(0);
        this.age = this.range > 0 ? Math.min(ByakuganSight.REVEAL, this.age + 1) : 0;
    }
}
