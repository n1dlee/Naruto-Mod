package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.util.SusanooIntegrityMeter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class SusanooIntegrityGUI implements PlayerGUI {
    private final Minecraft minecraft;

    SusanooIntegrityGUI(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void tick(Player player) {
    }

    @Override
    public void render(GuiGraphics gui, Matrix4f worldMatrix, Vec3 cameraPos) {
        Player player = minecraft.player;
        if (player == null || player.isSpectator() || minecraft.screen instanceof JutsuScreen) return;
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
            int lockout = data.getSusanooBrokenTicks();
            float maximum = data.getSusanooMaxDurability();
            if (lockout > 0 || data.isSusanooActive() && maximum > 0) {
                draw(gui, new SusanooIntegrityMeter(data.getSusanooDurability(), maximum, lockout));
            }
        });
    }

    private void draw(GuiGraphics gui, SusanooIntegrityMeter meter) {
        int width = Mth.clamp(gui.guiWidth() / 4, 126, 164);
        int x = gui.guiWidth() - width - 7;
        int y = Math.min(gui.guiHeight() - 46, Math.max(91, (int) (gui.guiHeight() * .58)));
        int colour = meter.broken() ? 0xFFE56878 : meter.critical() ? 0xFFFF986B : 0xFFAD8FF8;
        gui.fill(x, y, x + width, y + 34, 0xDC14121D);
        gui.fill(x, y, x + 2, y + 34, colour);
        gui.drawString(minecraft.font, Component.translatable("susanoo.integrity.title"), x + 8, y + 5, 0xFFF0EAFE);
        int track = width - 16;
        gui.fill(x + 8, y + 16, x + 8 + track, y + 21, 0xFF382C4D);
        int fill = Math.round(track * meter.fraction());
        if (fill > 0) {
            gui.fill(x + 8, y + 16, x + 8 + fill, y + 21, colour);
            gui.fill(x + 8, y + 16, x + 8 + fill, y + 17, 0x98F7E6FF);
        }
        for (int quarter = 1; quarter < 4; quarter++) {
            int mark = x + 8 + track * quarter / 4;
            gui.fill(mark, y + 16, mark + 1, y + 21, 0x7014121D);
        }
        var label = meter.broken() ? Component.translatable("susanoo.integrity.broken", meter.seconds())
                : Component.translatable("susanoo.integrity.value", meter.remainingText(), meter.maximumText(), meter.percent());
        gui.drawString(minecraft.font, label, x + 8, y + 24, colour);
    }
}
