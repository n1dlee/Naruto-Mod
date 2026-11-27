package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.util.SageFocus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class SageModeOverlayGUI implements PlayerGUI {
    private final Minecraft minecraft;
    private boolean active;
    private boolean gathering;
    private int charge;
    private int stone;

    public SageModeOverlayGUI(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void render(GuiGraphics gui, Matrix4f worldMatrix, Vec3 cameraPos) {
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.getCameraEntity() != minecraft.player) {
            return;
        }
        int width = gui.guiWidth();
        int height = gui.guiHeight();
        if (gathering) {
            drawBalance(gui, width / 2, height / 2 + 19);
        }
        if (stone > 0) {
            gui.drawCenteredString(minecraft.font, Component.translatable("sage.stone.hud", (stone + 19) / 20),
                    width / 2, height / 2 + 19, 0xC8CFCA);
        }
        if (!NarutoConfig.reducedBodyRiteEffects && (active || stone > 0)) {
            int color = stone > 0 ? 0x29727873 : 0x23C28731;
            gui.fillGradient(0, 0, width, 20, color, color & 0xFFFFFF);
            gui.fillGradient(0, height - 20, width, height, color & 0xFFFFFF, color);
        }
    }

    private void drawBalance(GuiGraphics gui, int x, int y) {
        int tint = charge >= SageFocus.MAXIMUM ? 0xE1DCC7 : charge >= SageFocus.MINIMUM ? 0xFFC76D : 0xC1B484;
        int bar = (int) (charge * 0.62f);
        gui.fill(x - 32, y, x + 32, y + 4, 0xB12A2422);
        gui.fill(x - 31, y + 1, x - 31 + bar, y + 3, 0xFF000000 | tint);
        gui.fill(x, y - 1, x + 1, y + 5, 0xFFE1DFCB);
        String key = charge >= SageFocus.MAXIMUM ? "sage.balance.overflow"
                : charge >= SageFocus.MINIMUM ? "sage.balance.ready" : "sage.balance.draw";
        gui.drawCenteredString(minecraft.font, Component.translatable(key), x, y + 7, tint);
    }

    @Override
    public void tick(Player player) {
        var data = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        this.active = data != null && data.isSageModeActive();
        this.gathering = data != null && NarutoAbilities.SAGE_MODE.getId().equals(data.getCurrentlyChanneledAbility());
        this.charge = data == null ? 0 : data.getSageCharge();
        this.stone = data == null ? 0 : data.getSageStoneTicks();
    }
}
