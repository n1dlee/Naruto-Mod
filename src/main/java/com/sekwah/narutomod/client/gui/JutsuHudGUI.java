package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.client.keybinds.NarutoKeyHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Map;

/**
 * Combined jutsu HUD:
 * - Combo hints: while a C/V/B combo is being typed, shows the keys entered so far plus
 *   every registered jutsu that combo could still become (exact match highlighted) —
 *   no more memorising all ~45 combos by heart.
 * - Cooldown list: active jutsu cooldowns with remaining seconds, top-right corner
 *   (fed by ClientCooldownTracker via ClientCooldownPacket).
 */
public class JutsuHudGUI implements PlayerGUI {

    private static final int MAX_HINTS = 5;
    private static final int MAX_COOLDOWN_LINES = 8;

    private final Minecraft minecraft;

    public JutsuHudGUI(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void tick(Player player) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, Matrix4f worldMatrix, Vec3 cameraPos) {
        renderComboHints(guiGraphics);
        renderCooldowns(guiGraphics);
    }

    private void renderComboHints(GuiGraphics guiGraphics) {
        long combo = NarutoKeyHandler.getCurrentJutsuCombo();
        if (combo <= 0) {
            return;
        }
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        String typed = comboToKeys(combo);
        guiGraphics.drawCenteredString(minecraft.font,
                Component.literal(typed).withStyle(style -> style.withBold(true)),
                screenWidth / 2, screenHeight - 65, 0xFFFFFF);

        // All registered combos this prefix could still become
        String prefix = String.valueOf(combo);
        int line = 0;
        for (Map.Entry<Long, net.minecraft.resources.ResourceLocation> entry : NarutoAbilities.COMBO_MAP.entrySet()) {
            if (line >= MAX_HINTS) {
                break;
            }
            String candidate = String.valueOf(entry.getKey());
            if (!candidate.startsWith(prefix)) {
                continue;
            }
            boolean exact = candidate.equals(prefix);
            Component name = Component.translatable(entry.getValue().toString());
            Component hint = Component.literal(comboToKeys(entry.getKey()) + " ")
                    .append(name);
            guiGraphics.drawCenteredString(minecraft.font, hint,
                    screenWidth / 2, screenHeight - 55 + line * 10,
                    exact ? 0x55FF55 : 0xAAAAAA);
            line++;
        }
    }

    private void renderCooldowns(GuiGraphics guiGraphics) {
        Map<String, Integer> cooldowns = ClientCooldownTracker.activeCooldowns();
        if (cooldowns.isEmpty()) {
            return;
        }
        int screenWidth = guiGraphics.guiWidth();
        int y = 6;
        int lines = 0;
        for (Map.Entry<String, Integer> entry : cooldowns.entrySet()) {
            if (lines >= MAX_COOLDOWN_LINES) {
                break;
            }
            float seconds = entry.getValue() / 20f;
            Component text = Component.translatable(entry.getKey())
                    .append(Component.literal(String.format(" %.1fs", seconds)));
            int width = minecraft.font.width(text);
            guiGraphics.drawString(minecraft.font, text, screenWidth - width - 6, y, 0xFFB347);
            y += 10;
            lines++;
        }
    }

    /** 1/2/3 digits -> the actual C/V/B keys the player presses. */
    private static String comboToKeys(long combo) {
        StringBuilder keys = new StringBuilder();
        for (char digit : String.valueOf(combo).toCharArray()) {
            keys.append(switch (digit) {
                case '1' -> 'C';
                case '2' -> 'V';
                case '3' -> 'B';
                default -> '?';
            });
        }
        return keys.toString();
    }
}
