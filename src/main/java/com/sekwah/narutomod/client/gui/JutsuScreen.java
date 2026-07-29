package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.JutsuScrolls;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.c2s.ServerToggleNinjaPacket;
import com.sekwah.narutomod.registries.NarutoRegistries;
import com.sekwah.narutomod.util.GuiUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Phase 15 D: the ninja progression screen (J key). Shows rank + XP progress, clan,
 * awakened natures with mastery levels, and every learnable jutsu grouped by element —
 * color-coded by what's blocking it (scroll not studied / nature locked / mastery too
 * low / ready to cast) with its C-V-B combo.
 */
public class JutsuScreen extends Screen {

    private record JutsuEntry(String path, String element, int levelRequired, long combo, boolean needsScroll,
                              Ability ability) {}

    private static final String[] ELEMENT_ORDER = {"fire", "water", "earth", "wind", "lightning"};
    private static final Map<String, Integer> ELEMENT_COLORS = Map.of(
            "fire", 0xFF5533,
            "water", 0x33AAFF,
            "earth", 0xCC8833,
            "wind", 0x55DD77,
            "lightning", 0xEEE055);
    private static final String[] RANK_NAMES = {"Academy", "Genin", "Chunin", "Jonin", "Kage"};
    private static final float[] RANK_XP_THRESHOLDS = {0, 1000, 5000, 15000, 50000};

    // Layout constants — columns are measured, not guessed, so rows never overlap.
    private static final int MAX_COL_WIDTH = 150;
    private static final int COL_GUTTER = 12;
    private static final int SIDE_MARGIN = 10;
    private static final int LINE_HEIGHT = 10;
    private static final int HEADER_HEIGHT = 11;
    private static final int ROW_INDENT = 4;

    private static final int COLOR_READY = 0x55FF55;
    private static final int COLOR_LOW_LEVEL = 0xFFAA33;
    private static final int COLOR_NO_SCROLL = 0xFF5555;
    private static final int COLOR_ELEMENT_LOCKED = 0x666666;
    /** "You have this technique" but we can't cheaply verify clan/rank gates from the GUI. */
    private static final int COLOR_INNATE = 0xDDDDDD;

    private final Map<String, List<JutsuEntry>> byElement = new LinkedHashMap<>();
    /** Every non-elemental jutsu: dojutsu, clan kekkei genkai, utility, summons — all of it. */
    private final List<JutsuEntry> other = new ArrayList<>();

    private Button becomeANinja;
    private Button changeBack;

    // --- Scroll state for the "other" column, since the full catalog no longer fits ---
    private int otherScrollOffset = 0;
    private int otherMaxScroll = 0;
    private int otherColX, otherColWidth, otherListTop, otherListBottom;

    public JutsuScreen() {
        super(Component.translatable("naruto.gui.jutsu.title"));
    }

    @Override
    protected void init() {
        this.buildCatalog();
        this.addButtons();
    }

    /**
     * Every registered ability lands somewhere on this screen now — elemental jutsu go to
     * their nature's column, everything else (dojutsu, clan kekkei genkai, summons,
     * utility techniques) goes into the scrollable "other" list. Previously anything that
     * was neither elemental nor scroll-taught was silently omitted entirely, which is why
     * roughly half the mod's jutsu never showed up here.
     */
    private void buildCatalog() {
        this.byElement.clear();
        this.other.clear();
        for (String element : ELEMENT_ORDER) {
            this.byElement.put(element, new ArrayList<>());
        }
        for (var entry : NarutoAbilities.ABILITY.getEntries()) {
            Ability ability = entry.get();
            var resourceKey = NarutoRegistries.ABILITIES.getResourceKey(ability);
            if (resourceKey.isEmpty()) {
                continue;
            }
            String path = resourceKey.get().location().getPath();
            String element = ability.element();
            boolean needsScroll = JutsuScrolls.requiresScroll(path);
            JutsuEntry jutsuEntry = new JutsuEntry(path, element, ability.elementLevelRequired(),
                    ability.defaultCombo(), needsScroll, ability);
            if (element != null && this.byElement.containsKey(element)) {
                this.byElement.get(element).add(jutsuEntry);
            } else {
                this.other.add(jutsuEntry);
            }
        }
        for (List<JutsuEntry> list : this.byElement.values()) {
            list.sort(Comparator.comparingInt(JutsuEntry::levelRequired));
        }
        this.other.sort(Comparator.comparing(JutsuEntry::path));
        this.otherScrollOffset = 0;
    }

    /**
     * Best-effort live status colour for an "other" entry. Scroll-gated and eye-gated
     * jutsu have cheap, side-effect-free checks (isJutsuLearned / Ability.hasEyeAccess) so
     * those get real green/red feedback. Clan- and rank-gated innate techniques only
     * expose their gate inside handleCost() (which spends chakra as a side effect), so
     * there's no safe way to preview them here — those get a neutral "you have this"
     * colour instead of a possibly-wrong green.
     */
    private int otherStatusColor(JutsuEntry entry, INinjaData ninjaData) {
        if (entry.needsScroll() && !ninjaData.isJutsuLearned(entry.path())) {
            return COLOR_NO_SCROLL;
        }
        if (entry.ability() != null && entry.ability().requiredClan() != null) {
            // Clan gates are now declared on the ability instead of being buried inside
            // handleCost(), so these no longer have to settle for a noncommittal grey.
            return entry.ability().hasClanAccess(ninjaData) ? COLOR_READY : COLOR_ELEMENT_LOCKED;
        }
        if (entry.ability() != null && entry.ability().requiredEye() != null) {
            return entry.ability().hasEyeAccess(ninjaData) ? COLOR_READY : COLOR_ELEMENT_LOCKED;
        }
        return COLOR_INNATE;
    }

    public void addButtons() {
        int buttonY = this.height - 24;

        becomeANinja = this.addRenderableWidget(Button.builder(Component.translatable("naruto.gui.jutsu.enable"), (button) -> {
            PacketHandler.sendToServer(new ServerToggleNinjaPacket(true));
        }).pos(this.width / 2 - 154, buttonY).size(98, 20).build());
        becomeANinja.active = false;

        changeBack = this.addRenderableWidget(Button.builder(Component.translatable("naruto.gui.jutsu.disable"), (button) -> {
            PacketHandler.sendToServer(new ServerToggleNinjaPacket(false));
        }).pos(this.width / 2 - 49, buttonY).size(98, 20).build());
        changeBack.active = false;

        this.addRenderableWidget(Button.builder(Component.translatable("naruto.gui.jutsu.done"), (button) -> {
            this.onClose();
        }).pos(this.width / 2 + 56, buttonY).size(98, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        AtomicBoolean isNinja = new AtomicBoolean(false);
        var player = this.minecraft.player;
        if (player != null) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                isNinja.set(ninjaData.isNinjaModeEnabled());
            });
        }
        becomeANinja.active = !isNinja.get();
        changeBack.active = isNinja.get();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        GuiUtils.centeredText(guiGraphics, this.font, this.title, this.width / 2, 8);

        var player = this.minecraft.player;
        if (player == null) {
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            // --- Profile line ---
            int rank = Math.min(Math.max(ninjaData.getNinjaRank(), 0), 4);
            String xpText = rank >= 4
                    ? (int) ninjaData.getChakraXp() + " XP"
                    : (int) ninjaData.getChakraXp() + " / " + (int) RANK_XP_THRESHOLDS[rank + 1] + " XP";
            String clan = ninjaData.getClanId().isEmpty() ? "-" : capitalize(ninjaData.getClanId());
            // Nature slots are shown because an empty slot is the only thing that lets
            // chakra paper work, and without this the paper just refuses with no context.
            String natures = ninjaData.getUnlockedElements().size() + "/" + ninjaData.getMaxElementSlots();
            GuiUtils.centeredText(guiGraphics, this.font,
                    Component.literal(RANK_NAMES[rank] + "  |  " + xpText + "  |  ")
                            .append(Component.translatable("naruto.gui.jutsu.clan").append(": " + clan))
                            .append("  |  ")
                            .append(Component.translatable("naruto.gui.jutsu.natures").append(": " + natures))
                            .withStyle(ChatFormatting.GRAY),
                    this.width / 2, 20, 0xCCCCCC);

            // --- Three columns: elements split in two + signature techniques ---
            // Columns are laid out from the real screen width with explicit gutters, and
            // every row is clipped to its own column, so long jutsu names can no longer
            // spill sideways into the neighbouring list.
            int colWidth = Math.min(MAX_COL_WIDTH, (this.width - 2 * SIDE_MARGIN - 2 * COL_GUTTER) / 3);
            int totalWidth = colWidth * 3 + COL_GUTTER * 2;
            int col1X = (this.width - totalWidth) / 2;
            int col2X = col1X + colWidth + COL_GUTTER;
            int col3X = col2X + colWidth + COL_GUTTER;
            int topY = 34;
            // Never draw into the button row at the bottom.
            int bottomY = this.height - 30;

            int y1 = topY;
            for (String element : new String[]{"fire", "water", "earth"}) {
                y1 = renderElementSection(guiGraphics, ninjaData, element, col1X, y1, colWidth, bottomY);
            }
            int y2 = topY;
            for (String element : new String[]{"wind", "lightning"}) {
                y2 = renderElementSection(guiGraphics, ninjaData, element, col2X, y2, colWidth, bottomY);
            }

            // "Other" column: every non-elemental jutsu (dojutsu, clan techniques, summons,
            // utility) — the whole catalog now fits somewhere on screen, scrolled if needed.
            guiGraphics.drawString(this.font,
                    Component.translatable("naruto.gui.jutsu.other").withStyle(ChatFormatting.BOLD),
                    col3X, topY, 0xD9C2FF, false);

            List<PanelLine> dojutsuLines = buildDojutsuLines(ninjaData);
            int dojutsuHeight = dojutsuLines.isEmpty() ? 0 : HEADER_HEIGHT + dojutsuLines.size() * LINE_HEIGHT + 6;

            this.otherColX = col3X;
            this.otherColWidth = colWidth;
            this.otherListTop = topY + HEADER_HEIGHT;
            this.otherListBottom = bottomY - dojutsuHeight;

            int viewportHeight = Math.max(0, this.otherListBottom - this.otherListTop);
            int contentHeight = this.other.size() * LINE_HEIGHT;
            this.otherMaxScroll = Math.max(0, contentHeight - viewportHeight);
            this.otherScrollOffset = Mth.clamp(this.otherScrollOffset, 0, this.otherMaxScroll);

            guiGraphics.enableScissor(col3X, this.otherListTop, col3X + colWidth, this.otherListBottom);
            int rowY = this.otherListTop - this.otherScrollOffset;
            for (JutsuEntry entry : this.other) {
                if (rowY + LINE_HEIGHT > this.otherListTop && rowY < this.otherListBottom) {
                    drawJutsuRow(guiGraphics, entry, col3X, rowY, colWidth, otherStatusColor(entry, ninjaData), false);
                }
                rowY += LINE_HEIGHT;
            }
            guiGraphics.disableScissor();

            if (this.otherMaxScroll > 0) {
                if (this.otherScrollOffset > 0) {
                    guiGraphics.drawString(this.font, "^", col3X + colWidth - 6, this.otherListTop, 0x999999, false);
                }
                if (this.otherScrollOffset < this.otherMaxScroll) {
                    guiGraphics.drawString(this.font, "v", col3X + colWidth - 6, this.otherListBottom - LINE_HEIGHT, 0x999999, false);
                }
            }

            renderDojutsuPanel(guiGraphics, dojutsuLines, col3X, this.otherListBottom + 6, colWidth, bottomY);
        });

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.otherMaxScroll > 0 && mouseX >= this.otherColX && mouseX <= this.otherColX + this.otherColWidth
                && mouseY >= this.otherListTop && mouseY <= this.otherListBottom) {
            this.otherScrollOffset = Mth.clamp(
                    this.otherScrollOffset - (int) Math.signum(delta) * LINE_HEIGHT * 3,
                    0, this.otherMaxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private record PanelLine(String text, int color) {}

    /**
     * Phase 16: what the ninja's eyes can currently do — tomoe count, Mangekyo tier and
     * which wielders' techniques have been taken, plus Rinnegan state. Built as plain data
     * first (not rendered directly) so the "other" list above can reserve exactly the
     * space this panel needs, rather than the panel's position jumping around as the list
     * above it scrolls.
     */
    private List<PanelLine> buildDojutsuLines(INinjaData ninjaData) {
        List<PanelLine> lines = new ArrayList<>();
        boolean hasSharingan = ninjaData.getSharinganTomoe() > 0 || ninjaData.isMangekyoAwakened();
        boolean hasByakugan = ninjaData.getByakuganLevel() > 0;
        boolean hasRinnegan = ninjaData.isRinneganAwakened() || ninjaData.isRinneSharinganAwakened();

        if (hasSharingan) {
            String tier;
            if (ninjaData.isEternalMangekyoAwakened()) {
                tier = "Eternal Mangekyo";
            } else if (ninjaData.isMangekyoAwakened()) {
                tier = "Mangekyo";
            } else {
                tier = ninjaData.getSharinganTomoe() + " tomoe";
            }
            lines.add(new PanelLine("Sharingan: " + tier,
                    ninjaData.isEternalMangekyoAwakened() ? COLOR_READY : 0xFF6666));

            String defeated = ninjaData.getDefeatedMsBosses();
            if (!defeated.isEmpty()) {
                lines.add(new PanelLine("Forms: " + defeated.replace(",", ", "), 0xBBBBBB));
            } else if (ninjaData.isMangekyoAwakened()) {
                // Explain the blindness the player is about to run into, before they do
                lines.add(new PanelLine(Component.translatable("naruto.gui.jutsu.strain").getString(), 0xAA3333));
            }
        }
        if (hasByakugan) {
            lines.add(new PanelLine("Byakugan: Lv " + ninjaData.getByakuganLevel(), 0xCCDDFF));
        }
        if (hasRinnegan) {
            lines.add(new PanelLine(ninjaData.isRinneSharinganAwakened() ? "Rinne Sharingan" : "Rinnegan", 0xC0B0E0));
        }
        return lines;
    }

    private void renderDojutsuPanel(GuiGraphics guiGraphics, List<PanelLine> lines, int x, int y, int colWidth, int bottomY) {
        if (lines.isEmpty() || y + HEADER_HEIGHT > bottomY) {
            return;
        }
        guiGraphics.drawString(this.font,
                Component.translatable("naruto.gui.jutsu.dojutsu").withStyle(ChatFormatting.BOLD),
                x, y, 0xFF8888, false);
        y += HEADER_HEIGHT;

        int rowWidth = colWidth - ROW_INDENT;
        for (PanelLine line : lines) {
            y = drawPanelRow(guiGraphics, line.text(), x, y, rowWidth, bottomY, line.color());
        }
    }

    /** One clipped line of the dojutsu panel; returns the next free Y. */
    private int drawPanelRow(GuiGraphics guiGraphics, String text, int x, int y, int rowWidth, int bottomY, int color) {
        if (y + LINE_HEIGHT > bottomY) {
            return y;
        }
        guiGraphics.drawString(this.font, Component.literal(fitToWidth(text, rowWidth)),
                x + ROW_INDENT, y, color, false);
        return y + LINE_HEIGHT;
    }

    private int renderElementSection(GuiGraphics guiGraphics, com.sekwah.narutomod.capabilities.INinjaData ninjaData,
                                     String element, int x, int y, int colWidth, int bottomY) {
        if (y + HEADER_HEIGHT > bottomY) {
            return y;
        }
        boolean unlocked = ninjaData.isElementUnlocked(element);
        int level = ninjaData.getElementLevel(element);
        int headerColor = unlocked ? ELEMENT_COLORS.get(element) : COLOR_ELEMENT_LOCKED;

        // Header: element name on the left, mastery (or LOCKED) pinned to the right edge
        Component status = unlocked
                ? Component.literal("Lv " + level)
                : Component.translatable("naruto.gui.jutsu.locked");
        String statusText = status.getString();
        int statusWidth = this.font.width(statusText);
        guiGraphics.drawString(this.font, Component.literal(statusText), x + colWidth - statusWidth, y,
                headerColor, false);

        String name = Component.translatable("element.narutomod." + element).getString();
        guiGraphics.drawString(this.font,
                Component.literal(fitToWidth(name, colWidth - statusWidth - 4)).withStyle(ChatFormatting.BOLD),
                x, y, headerColor, false);
        y += HEADER_HEIGHT;

        for (JutsuEntry entry : this.byElement.get(element)) {
            if (y + LINE_HEIGHT > bottomY) {
                drawOverflowMark(guiGraphics, x + ROW_INDENT, y);
                y += LINE_HEIGHT;
                break;
            }
            int color;
            if (entry.ability() != null && !entry.ability().hasClanAccess(ninjaData)) {
                // Wrong bloodline entirely - no amount of training opens this one.
                color = COLOR_ELEMENT_LOCKED;
            } else if (!unlocked) {
                color = COLOR_ELEMENT_LOCKED;
            } else if (entry.needsScroll() && !ninjaData.isJutsuLearned(entry.path())) {
                color = COLOR_NO_SCROLL;
            } else if (level < entry.levelRequired()) {
                color = COLOR_LOW_LEVEL;
            } else if (entry.ability() != null && !entry.ability().hasElementAccess(ninjaData)) {
                // Reached only by kekkei genkai: this column's nature is trained enough, so
                // what is missing has to be the second one the bloodline also demands.
                color = COLOR_LOW_LEVEL;
            } else {
                color = COLOR_READY;
            }
            drawJutsuRow(guiGraphics, entry, x, y, colWidth, color, true);
            y += LINE_HEIGHT;
        }
        return y + 4;
    }

    /**
     * Draws one jutsu row inside its column: the combo (and required level) are pinned to
     * the column's right edge, and the name gets whatever space is left — truncated with
     * an ellipsis rather than allowed to run over the next column.
     */
    private void drawJutsuRow(GuiGraphics guiGraphics, JutsuEntry entry, int colX, int y, int colWidth,
                              int nameColor, boolean showLevel) {
        StringBuilder badge = new StringBuilder();
        if (entry.combo() > 0) {
            badge.append('[').append(comboToKeys(entry.combo())).append(']');
        }
        if (showLevel && entry.levelRequired() > 0) {
            if (badge.length() > 0) {
                badge.append(' ');
            }
            badge.append("Lv").append(entry.levelRequired());
            // Kekkei genkai demand a second nature the column header cannot show. Without
            // this the row just looks broken: the column says Lv 12, the row says Lv 8, and
            // it is still orange.
            String secondary = entry.ability() == null ? null : entry.ability().secondaryElement();
            if (secondary != null) {
                badge.append(" +")
                        .append(Component.translatable("element.narutomod." + secondary).getString())
                        .append(entry.ability().secondaryElementLevelRequired());
            }
        }

        int rowRight = colX + colWidth;
        int badgeWidth = badge.length() == 0 ? 0 : this.font.width(badge.toString());
        if (badgeWidth > 0) {
            guiGraphics.drawString(this.font,
                    Component.literal(badge.toString()).withStyle(ChatFormatting.DARK_GRAY),
                    rowRight - badgeWidth, y, 0x777777, false);
        }

        int nameX = colX + ROW_INDENT;
        int nameSpace = rowRight - nameX - (badgeWidth > 0 ? badgeWidth + 4 : 0);
        String name = Component.translatable("narutomod:" + entry.path()).getString();
        guiGraphics.drawString(this.font, Component.literal(fitToWidth(name, nameSpace)), nameX, y, nameColor, false);
    }

    /**
     * Trims text to the given pixel width, appending "..." when it had to cut. Plain ASCII
     * dots on purpose — a real ellipsis glyph (…) sits outside the font's ASCII page, and on
     * this client (Embeddium/Oculus) mixing ASCII and extended-glyph pages mid-string makes
     * everything after the switch render as garbage. Every player-visible string in this mod
     * is kept ASCII-only for that reason.
     */
    private String fitToWidth(String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String trimmed = this.font.plainSubstrByWidth(text, Math.max(0, maxWidth - this.font.width("...")));
        return trimmed + "...";
    }

    private void drawOverflowMark(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.drawString(this.font,
                Component.literal("...").withStyle(ChatFormatting.DARK_GRAY), x, y, 0x777777, false);
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

    private static String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
