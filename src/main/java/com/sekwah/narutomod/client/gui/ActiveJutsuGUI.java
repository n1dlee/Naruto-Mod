package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.abilities.jutsus.SusanooAbility;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists every state currently billing the player, and what it costs per second.
 *
 * The complaint this answers is "chakra drains and I have no idea where it goes". Several
 * things here bill every single tick - Susanoo alone is 160 chakra a second before the scroll
 * wheel adds surge on top - and none of them announced themselves once activated. The bars at
 * the bottom of the screen showed the pool emptying without ever saying who was spending it.
 *
 * Every rate is read from the constant the drain itself uses rather than copied. A number in
 * a HUD that quietly stops matching the code is worse than no number, because it gets
 * believed.
 */
public class ActiveJutsuGUI implements PlayerGUI {

    private static final int TICKS_PER_SECOND = 20;
    private static final int LINE_HEIGHT = 10;
    private static final int LEFT_MARGIN = 6;

    /** Where the panel starts, as a fraction of screen height. Clear of the vanilla HUD. */
    private static final float TOP_FRACTION = 0.32f;

    private static final int COLOUR_CHAKRA = 0x66CCFF;
    private static final int COLOUR_STAMINA = 0x88DD66;
    private static final int COLOUR_BOND = 0xFF9944;
    private static final int COLOUR_FATAL = 0xFF4444;
    private static final int COLOUR_FREE = 0xBBBBBB;

    private final Minecraft minecraft;

    public ActiveJutsuGUI(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void tick(Player player) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, Matrix4f worldMatrix, Vec3 cameraPos) {
        Player player = this.minecraft.player;
        if (player == null) {
            return;
        }
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            List<Entry> entries = collect(ninjaData);
            if (entries.isEmpty()) {
                return;
            }
            int y = (int) (guiGraphics.guiHeight() * TOP_FRACTION);
            for (Entry entry : entries) {
                guiGraphics.drawString(this.minecraft.font, entry.text, LEFT_MARGIN, y, entry.colour);
                y += LINE_HEIGHT;
            }
        });
    }

    private List<Entry> collect(INinjaData data) {
        List<Entry> entries = new ArrayList<>();

        if (data.getGatesOpen() > 0) {
            int gates = data.getGatesOpen();
            boolean deathGate = gates >= NinjaData.MAX_GATES;
            float perSecond = gates * NinjaData.GATE_STAMINA_PER_TICK_PER_GATE * TICKS_PER_SECOND;
            String label = deathGate ? "Gate of Death 8/8" : "Eight Gates " + gates + "/8";
            entries.add(new Entry(
                    Component.literal(label
                            + timer(data.getGatesTicks())
                            + rate(perSecond, "sta")),
                    deathGate ? COLOUR_FATAL : COLOUR_STAMINA));
        }

        if (data.isSageModeActive()) {
            entries.add(new Entry(
                    Component.literal("Sage Mode"
                            + timer(data.getSageModeTicks())
                            + rate(NinjaData.SAGE_CHAKRA_PER_TICK * TICKS_PER_SECOND, "ch")),
                    COLOUR_CHAKRA));
        }

        if (data.isSusanooActive()) {
            // Two separate bills: the toggle's flat upkeep, plus a surge that scales with
            // however far the scroll wheel has been pushed.
            float perSecond = (SusanooAbility.CHAKRA_COST
                    + data.getTransformPower() * NinjaData.SUSANOO_SURGE_DRAIN) * TICKS_PER_SECOND;
            // Integrity is the number that decides the fight now, so it leads the line.
            int integrity = Math.round(data.getSusanooDurability());
            int max = Math.round(data.getSusanooMaxDurability());
            entries.add(new Entry(
                    Component.literal("Susanoo stage " + data.getSusanooStage()
                            + "  " + integrity + "/" + max
                            + rate(perSecond, "ch")),
                    integrity * 4 < max ? COLOUR_FATAL : COLOUR_CHAKRA));
        }

        // Shown while it is DOWN as well: the wait is the consequence of losing the shell and
        // the wearer needs to know how long they are fighting without it.
        if (data.getSusanooBrokenTicks() > 0) {
            entries.add(new Entry(
                    Component.literal("Susanoo shattered" + timer(data.getSusanooBrokenTicks())),
                    COLOUR_FATAL));
        }

        if (data.isKuramaCloakActive()) {
            // Paid out of the Kurama bond, not the chakra pool - and it pays chakra back.
            float perSecond = (NinjaData.KURAMA_CHAKRA_PER_TICK
                    + data.getTransformPower() * NinjaData.KURAMA_SURGE_DRAIN
                    + NinjaData.KURAMA_CHAKRA_DONATION_PER_TICK) * TICKS_PER_SECOND;
            entries.add(new Entry(
                    Component.literal("Kurama Cloak " + data.getKuramaTailCount() + " tails"
                            + rate(perSecond, "bond")),
                    COLOUR_BOND));
        }

        if (data.isChidoriActive()) {
            entries.add(new Entry(
                    Component.literal("Chidori"
                            + rate(NinjaData.CHIDORI_TICK_COST * TICKS_PER_SECOND, "ch")),
                    COLOUR_CHAKRA));
        }

        if (data.isTransplantedSharingan()) {
            // Billed once a second rather than once a tick - a transplanted eye never closes.
            entries.add(new Entry(
                    Component.literal("Transplanted eye"
                            + rate(NinjaData.TRANSPLANT_IDLE_DRAIN, "ch")),
                    COLOUR_CHAKRA));
        }

        // Free states, listed so the panel is a complete answer to "what is switched on"
        // rather than only a bill.
        if (data.isSharinganActive()) {
            entries.add(new Entry(Component.literal("Sharingan"), COLOUR_FREE));
        }
        if (data.isByakuganActive()) {
            entries.add(new Entry(Component.literal("Byakugan"), COLOUR_FREE));
        }
        if (data.isWallWalkAttached()) {
            entries.add(new Entry(Component.literal("Wall Walk"), COLOUR_FREE));
        }

        return entries;
    }

    /** Seconds remaining, or nothing at all when the state has no timer. */
    private static String timer(int ticks) {
        if (ticks <= 0) {
            return "";
        }
        return String.format("  %.1fs", ticks / (float) TICKS_PER_SECOND);
    }

    private static String rate(float perSecond, String unit) {
        if (perSecond <= 0f) {
            return "";
        }
        return String.format("  -%.0f %s/s", perSecond, unit);
    }

    private record Entry(Component text, int colour) {
    }
}
