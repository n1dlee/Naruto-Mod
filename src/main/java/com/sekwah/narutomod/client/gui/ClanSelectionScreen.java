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

    private static final String[] CLAN_IDS = {"uzumaki", "uchiha", "hyuga", "nara", "haruno", "senju"};
    private static final String[] CLAN_NAMES = {"Uzumaki", "Uchiha", "Hyuga", "Nara", "Haruno", "Senju"};
    private static final String[] CLAN_DESCRIPTIONS = {
            "Chakra x1.5, Regen x2",
            "Fire jutsu +30% damage",
            "Melee attack +30%",
            "Movement speed +20%",
            "HP regen 0.5/sec",
            "Wood Release, +20% HP"
    };

    private static final ResourceLocation[] CLAN_ICONS = {
            new ResourceLocation("narutomod", "textures/gui/clans/uzumaki.png"),
            new ResourceLocation("narutomod", "textures/gui/clans/uchiha.png"),
            new ResourceLocation("narutomod", "textures/gui/clans/hyuga.png"),
            new ResourceLocation("narutomod", "textures/gui/clans/nara.png"),
            new ResourceLocation("narutomod", "textures/gui/clans/haruno.png"),
            new ResourceLocation("narutomod", "textures/gui/clans/senju.png"),
    };

    public ClanSelectionScreen() {
        super(Component.literal("Choose Your Clan"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 4;

        for (int i = 0; i < CLAN_IDS.length; i++) {
            final String clanId = CLAN_IDS[i];
            String label = CLAN_NAMES[i] + " — " + CLAN_DESCRIPTIONS[i];
            this.addRenderableWidget(Button.builder(Component.literal(label), (btn) -> {
                PacketHandler.sendToServer(new ServerSelectClanPacket(clanId));
                this.minecraft.setScreen(null);
            }).pos(centerX - 150, startY + i * 28).size(300, 24).build());
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
