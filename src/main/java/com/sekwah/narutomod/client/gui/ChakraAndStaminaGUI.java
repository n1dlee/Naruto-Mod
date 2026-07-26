package com.sekwah.narutomod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sekwah.narutomod.util.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

public class ChakraAndStaminaGUI implements PlayerGUI {


    public static final BarDesigns.BarInfo[] barTypes = BarDesigns.BarInfo.values();

    private final Minecraft minecraft;
    private int screenWidth;
    private int screenHeight;

    private float chakra;
    private float stamina;

    private float maxChakra;
    private float maxStamina;
    private float chakraXp;
    private int ninjaRank;
    private String clanId = "";
    private float kuramaBond;
    private float maxKuramaBond;
    /** Awakened natures for the HUD card: [display name, level, argb color as string]. */
    private java.util.List<String[]> elementDisplay = java.util.List.of();

    // Charge bar state
    private boolean isChanneling = false;
    private int channelingTicks = 0;

    // Sage mode state
    private int sageCharge = 0;
    private boolean sageModeActive = false;
    private int sageModeTicks = 0;

    private static final String[] RANK_NAMES = {"Academy", "Genin", "Chunin", "Jonin", "Kage"};
    private static final int[] RANK_COLORS = {0xAAAAAA, 0x55FF55, 0x5555FF, 0xFFAA00, 0xFF5555};

    private static final net.minecraft.resources.ResourceLocation[] CLAN_ICONS = {
            new net.minecraft.resources.ResourceLocation("narutomod", "textures/gui/clans/uzumaki.png"),
            new net.minecraft.resources.ResourceLocation("narutomod", "textures/gui/clans/uchiha.png"),
            new net.minecraft.resources.ResourceLocation("narutomod", "textures/gui/clans/hyuga.png"),
            new net.minecraft.resources.ResourceLocation("narutomod", "textures/gui/clans/nara.png"),
            new net.minecraft.resources.ResourceLocation("narutomod", "textures/gui/clans/haruno.png"),
    };
    private static final String[] CLAN_IDS = {"uzumaki", "uchiha", "hyuga", "nara", "haruno"};

    private static final net.minecraft.resources.ResourceLocation[] RANK_ICONS = {
            null, // Academy has no icon
            new net.minecraft.resources.ResourceLocation("narutomod", "textures/gui/ranks/genin.png"),
            new net.minecraft.resources.ResourceLocation("narutomod", "textures/gui/ranks/chunin.png"),
            new net.minecraft.resources.ResourceLocation("narutomod", "textures/gui/ranks/jonin.png"),
            new net.minecraft.resources.ResourceLocation("narutomod", "textures/gui/ranks/kage.png"),
    };

    public ChakraAndStaminaGUI(Minecraft mc) {
        this.minecraft = mc;
    }

    public void render(GuiGraphics guiGraphics, Matrix4f worldMatrix, Vec3 cameraPos) {
        this.screenWidth = this.minecraft.getWindow().getGuiScaledWidth();
        this.screenHeight = this.minecraft.getWindow().getGuiScaledHeight();
        int barDesign = NarutoConfig.chakraBarDesign;

        float currentChakraPercent = maxChakra > 0 ? (chakra) / maxChakra : 0;
        float currentStaminaPercent = maxStamina > 0 ? (stamina) / maxStamina : 0;

        int width = 100;
        int offset = 128;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, barTypes[barDesign].texture);
        int barWidth = barTypes[barDesign].width;
        int xOffset = barTypes[barDesign].offset;

        int valuesOffset = 128 + (barWidth / 2);
        int valuesHeight = 26;

        int screenMid = this.screenWidth / 2;

        float darkenFactor = 0.25f;

        Color chakraColor = new Color(20,179,255);
        Color staminaColor = new Color(0,255,0);
        int intStaminaColor = ColorUtil.toMCColor(staminaColor).getValue();
        int intChakraColor = ColorUtil.toMCColor(chakraColor).getValue();

        int intStaminaColorDarker = ColorUtil.toMCColor(new Color((int) (staminaColor.getRed() * darkenFactor),
                (int) (staminaColor.getGreen() * darkenFactor),
                (int) (staminaColor.getBlue() * darkenFactor))).getValue();
        int intChakraColorDarker = ColorUtil.toMCColor(new Color((int) (chakraColor.getRed() * darkenFactor),
                (int) (chakraColor.getGreen() * darkenFactor),
                (int) (chakraColor.getBlue() * darkenFactor))).getValue();


        // Charka Bar underlay
        int chakraWidth = (int) (barWidth * currentChakraPercent);
        // stack, x, y, tx, ty, width, height, textureWidth, textureHeight
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(barTypes[barDesign].texture, screenMid - width - offset, this.screenHeight - 22,
                0 , 22,
                width, 22,
                width, 44);


        // Stamina Bar underlay
        int staminaWidth = (int) (barWidth * currentStaminaPercent);
        guiGraphics.blit(barTypes[barDesign].texture, screenMid + offset, this.screenHeight - 22,
                0, 22,
                width, 22,
                -width, 44);


        // Chakra Bar color
        this.setColor(chakraColor);
        guiGraphics.blit(barTypes[barDesign].texture, screenMid - chakraWidth - offset - (width - xOffset - barWidth), this.screenHeight - 22,
                xOffset + (barWidth - chakraWidth), 0,
                chakraWidth, 22,
                width, 44);

        // Stamina Bar color
        this.setColor(staminaColor);
        guiGraphics.blit(barTypes[barDesign].texture, screenMid + offset + (100 - barWidth - xOffset), this.screenHeight - 22,
                -barWidth - xOffset, 0,
                staminaWidth, 22,
                -width, 44);

        this.setColor(Color.WHITE);

        String chakraText = (int) chakra + "/" + (int) maxChakra;
        GuiUtils.centeredTextOutlined(guiGraphics, this.getFont(),
                chakraText,
                screenMid - valuesOffset,
                this.screenHeight - valuesHeight,
                intChakraColor,
                intChakraColorDarker);


        String staminaText = (int) stamina + "/" + (int) maxStamina;
        GuiUtils.centeredTextOutlined(guiGraphics, this.getFont(),
                staminaText,
                screenMid + valuesOffset,
                this.screenHeight - valuesHeight,
                intStaminaColor,
                intStaminaColorDarker);

        // --- Rank + Clan display: top-left corner "ninja card" (kept out of the
        // vanilla XP-bar/health/hunger band which sits centered above the hotbar) ---
        int rankIdx = Math.min(ninjaRank, 4);
        String rankName = RANK_NAMES[rankIdx];
        int rankColor = RANK_COLORS[rankIdx];
        int cardX = 6;
        int cardY = 6;

        // Clan icon (top-left corner)
        int clanIconWidth = 0;
        if (!clanId.isEmpty()) {
            for (int i = 0; i < CLAN_IDS.length; i++) {
                if (CLAN_IDS[i].equals(clanId)) {
                    this.setColor(Color.WHITE);
                    guiGraphics.blit(CLAN_ICONS[i], cardX, cardY, 0, 0, 12, 12, 12, 12);
                    clanIconWidth = 14;
                    break;
                }
            }
        }

        // Rank icon (next to clan icon)
        int rankIconX = cardX + clanIconWidth;
        int rankIconWidth = 0;
        if (RANK_ICONS[rankIdx] != null) {
            this.setColor(Color.WHITE);
            guiGraphics.blit(RANK_ICONS[rankIdx], rankIconX, cardY + 1, 0, 0, 10, 10, 10, 10);
            rankIconWidth = 12;
        }

        // Rank name + XP text, left-aligned after the icons
        String rankText = rankName + " [" + (int) chakraXp + " XP]";
        GuiUtils.leftTextOutlined(guiGraphics, this.getFont(), rankText,
                rankIconX + rankIconWidth, cardY + 2, rankColor, 0x222222);

        // --- Phase 15: awakened natures with mastery levels ---
        if (!elementDisplay.isEmpty()) {
            int elementX = cardX;
            int elementY = cardY + 24;
            for (String[] entry : elementDisplay) {
                String label = entry[0] + " " + entry[1];
                int color = Integer.parseInt(entry[2]);
                GuiUtils.leftTextOutlined(guiGraphics, this.getFont(), label,
                        elementX, elementY, color, 0x222222);
                elementX += this.getFont().width(label) + 8;
            }
        }

        // --- Kurama bond meter (Uzumaki only) — Kurama's own chakra, separate from the player's ---
        if ("uzumaki".equals(clanId) && maxKuramaBond > 0) {
            int bondBarWidth = 60;
            int bondBarHeight = 3;
            int bondBarX = cardX;
            int bondBarY = cardY + 14;
            float bondPercent = kuramaBond / maxKuramaBond;
            int filledW = (int) (bondBarWidth * bondPercent);

            guiGraphics.fill(bondBarX - 1, bondBarY - 1,
                    bondBarX + bondBarWidth + 1, bondBarY + bondBarHeight + 1,
                    0xAA222222);
            guiGraphics.fill(bondBarX, bondBarY,
                    bondBarX + filledW, bondBarY + bondBarHeight,
                    0xFFFF8800);

            GuiUtils.leftTextOutlined(guiGraphics, this.getFont(), "Kurama",
                    bondBarX + bondBarWidth + 4, bondBarY - 3, 0xFF8800, 0x331a00);
        }

        // --- Charge bar (shown during channeling) ---
        if (isChanneling && channelingTicks > 0) {
            int chargeBarWidth = 80;
            int chargeBarHeight = 4;
            int chargeBarX = screenMid - chargeBarWidth / 2;
            int chargeBarY = this.screenHeight - 46;

            // Max charge = 60 ticks for most abilities
            float chargePercent = Math.min(channelingTicks / 60.0f, 1.0f);
            int filledWidth = (int)(chargeBarWidth * chargePercent);

            // Background (dark gray)
            guiGraphics.fill(chargeBarX - 1, chargeBarY - 1,
                    chargeBarX + chargeBarWidth + 1, chargeBarY + chargeBarHeight + 1,
                    0xAA222222);

            // Color transitions: blue → cyan → white as charge increases
            int r = (int)(60 + 195 * chargePercent);
            int g = (int)(140 + 115 * chargePercent);
            int b = 255;
            int barColor = 0xFF000000 | (r << 16) | (g << 8) | b;

            // Filled portion
            guiGraphics.fill(chargeBarX, chargeBarY,
                    chargeBarX + filledWidth, chargeBarY + chargeBarHeight,
                    barColor);

            // "CHARGE" label
            String chargeLabel = (int)(chargePercent * 100) + "%";
            GuiUtils.centeredTextOutlined(guiGraphics, this.getFont(), chargeLabel,
                    screenMid, chargeBarY - 10, 0xFFFFFF, 0x333333);
        }

        // --- Sage Mode display ---
        if (sageCharge > 0 || sageModeActive) {
            int sageBarWidth = 60;
            int sageBarHeight = 3;
            int sageBarX = screenMid - sageBarWidth / 2;
            int sageBarY = this.screenHeight - 54;

            if (sageModeActive) {
                // Show remaining sage mode time
                float timePercent = Math.min(sageModeTicks / (30.0f * 20.0f), 1.0f);
                int filledW = (int)(sageBarWidth * timePercent);

                // Background
                guiGraphics.fill(sageBarX - 1, sageBarY - 1,
                        sageBarX + sageBarWidth + 1, sageBarY + sageBarHeight + 1,
                        0xAA222222);
                // Gold fill
                guiGraphics.fill(sageBarX, sageBarY,
                        sageBarX + filledW, sageBarY + sageBarHeight,
                        0xFFFFAA00);

                String label = "SAGE " + (sageModeTicks / 20) + "s";
                GuiUtils.centeredTextOutlined(guiGraphics, this.getFont(), label,
                        screenMid, sageBarY - 10, 0xFFAA00, 0x442200);
            } else if (sageCharge > 0) {
                // Show charge accumulation
                float chargePercent2 = sageCharge / 100.0f;
                int filledW = (int)(sageBarWidth * chargePercent2);

                // Background
                guiGraphics.fill(sageBarX - 1, sageBarY - 1,
                        sageBarX + sageBarWidth + 1, sageBarY + sageBarHeight + 1,
                        0x88222222);
                // Orange-yellow fill
                int r2 = (int)(200 + 55 * chargePercent2);
                int g2 = (int)(130 + 80 * chargePercent2);
                int barColor2 = 0xFF000000 | (r2 << 16) | (g2 << 8) | 0x00;
                guiGraphics.fill(sageBarX, sageBarY,
                        sageBarX + filledW, sageBarY + sageBarHeight,
                        barColor2);

                String label = "Nature " + sageCharge + "%";
                GuiUtils.centeredTextOutlined(guiGraphics, this.getFont(), label,
                        screenMid, sageBarY - 10, 0xDDAA22, 0x332200);
            }
        }
    }

    public void tick(Player player) {
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            this.chakra = ninjaData.getChakra();
            this.stamina = ninjaData.getStamina();
            this.maxChakra = ninjaData.getMaxChakra();
            this.maxStamina = ninjaData.getMaxStamina();
            this.chakraXp = ninjaData.getChakraXp();
            this.ninjaRank = ninjaData.getNinjaRank();
            this.clanId = ninjaData.getClanId();
            this.isChanneling = ninjaData.getCurrentlyChanneledAbility() != null;
            this.channelingTicks = ninjaData.getCurrentlyChanneledTicks();
            this.sageCharge = ninjaData.getSageCharge();
            this.sageModeActive = ninjaData.isSageModeActive();
            this.sageModeTicks = ninjaData.getSageModeTicks();
            this.kuramaBond = ninjaData.getKuramaBond();
            this.maxKuramaBond = ninjaData.getMaxKuramaBond();

            java.util.List<String[]> elements = new java.util.ArrayList<>();
            for (String element : ninjaData.getUnlockedElements()) {
                String name = switch (element) {
                    case "fire" -> "Fire";
                    case "water" -> "Water";
                    case "earth" -> "Earth";
                    case "wind" -> "Wind";
                    case "lightning" -> "Lightning";
                    default -> element;
                };
                int color = switch (element) {
                    case "fire" -> 0xFF5533;
                    case "water" -> 0x33AAFF;
                    case "earth" -> 0xCC8833;
                    case "wind" -> 0x55DD77;
                    case "lightning" -> 0xEEE055;
                    default -> 0xFFFFFF;
                };
                elements.add(new String[]{name, String.valueOf(ninjaData.getElementLevel(element)), String.valueOf(color)});
            }
            this.elementDisplay = elements;
        });
    }

    private void setColor(Color color) {
        RenderSystem.setShaderColor(color.getRed() / 255f,
                color.getGreen() / 255f,
                color.getBlue() / 255f,
                1.0F);
    }

    private Font getFont() {
        return this.minecraft.font;
    }
}
