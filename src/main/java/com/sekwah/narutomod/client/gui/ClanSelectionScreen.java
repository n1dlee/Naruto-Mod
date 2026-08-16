package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.c2s.ServerSelectClanPacket;
import com.sekwah.narutomod.util.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Full-screen clan selection shown when a ninja hasn't chosen a clan yet.
 */
public class ClanSelectionScreen extends Screen {

    // The clan list lives in NinjaClan. Four parallel arrays here plus a fifth hard-coded
    // set in ServerSelectClanPacket is exactly how four implemented clans ended up
    // unreachable: adding one meant editing five places and nothing failed if you missed one.

    public ClanSelectionScreen() {
        super(Component.literal("Choose Your Clan"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 4;

        com.sekwah.narutomod.clan.NinjaClan[] clans = com.sekwah.narutomod.clan.NinjaClan.values();
        // Ten clans no longer fit at the old 28px pitch on a short window, so the rows tighten
        // up when there are enough of them to run off the bottom.
        int pitch = this.height < 300 ? 20 : 24;
        for (int i = 0; i < clans.length; i++) {
            final com.sekwah.narutomod.clan.NinjaClan clan = clans[i];
            // ASCII hyphen, not an em-dash. With Embeddium/Oculus installed a non-ASCII glyph
            // switches the font to another page mid-string and everything after it renders as
            // garbage - which is exactly what the clan buttons were showing.
            String label = clan.displayName() + " - " + clan.description();
            this.addRenderableWidget(Button.builder(Component.literal(label), (btn) -> {
                PacketHandler.sendToServer(new ServerSelectClanPacket(clan.id()));
                this.minecraft.setScreen(null);
            }).pos(centerX - 150, startY + i * pitch).size(300, pitch - 4).build());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        this.renderBackground(guiGraphics);
        GuiUtils.centeredText(guiGraphics, this.font, this.title, this.width / 2, this.height / 4 - 30);
        GuiUtils.centeredText(guiGraphics, this.font,
                Component.literal("This choice is permanent!").withStyle(net.minecraft.ChatFormatting.RED),
                this.width / 2, this.height / 4 - 15);
        super.render(guiGraphics, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Prevent ESC from closing — must choose
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
