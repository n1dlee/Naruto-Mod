package com.sekwah.narutomod.client.renderer;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.network.s2c.SharinganGazePacket;
import com.sekwah.narutomod.util.GenjutsuGaze;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SharinganGazeRenderer {
    private static final Map<Integer, Cue> EYES = new LinkedHashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);
    private static Cue victim;

    private SharinganGazeRenderer() {
    }

    public static void add(SharinganGazePacket packet) {
        Minecraft game = Minecraft.getInstance();
        ClientLevel level = game.level;
        useLevel(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        Cue cue = new Cue(packet, level.getGameTime());
        if (packet.personal()) {
            if (game.player != null && game.player.getId() == packet.subject()) {
                victim = cue;
            }
        } else {
            EYES.values().removeIf(old -> level.getGameTime() - old.start() >= 10);
            if (EYES.size() >= 32) {
                EYES.remove(EYES.keySet().iterator().next());
            }
            EYES.put(packet.subject(), cue);
        }
    }

    public static float iris(Player actor, float partial) {
        Cue cue = EYES.get(actor.getId());
        if (actor.level() != world.get() || cue == null) {
            return 0;
        }
        float age = actor.level().getGameTime() - cue.start() + partial;
        return switch (cue.packet().cue()) {
            case FOCUS -> Math.max(0, Math.min(1, Math.min(age / 5f + 0.13f, 7f - age)));
            case LOCKED -> Math.max(0, 1f - age / 4f);
            case MISSED -> Math.max(0, 0.46f * (1f - age / 4f));
            case VICTIM -> 0;
        };
    }

    @SubscribeEvent
    public static void overlay(RenderGuiOverlayEvent.Pre event) {
        Minecraft game = Minecraft.getInstance();
        useLevel(game.level);
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type() || game.level == null || victim == null
                || game.player == null || game.getCameraEntity() != game.player || game.player.isSpectator()
                || game.player.getId() != victim.packet().subject() || game.options.hideGui || TsukuyomiRenderer.active()) {
            return;
        }
        float age = game.level.getGameTime() - victim.start() + event.getPartialTick();
        float strength = GenjutsuGaze.distortion(age, victim.packet().duration(), game.options.screenEffectScale().get());
        if (age >= victim.packet().duration()) {
            victim = null;
        } else if (strength > 0.001f) {
            draw(event.getGuiGraphics(), age, strength, victim.packet().seed());
        }
    }

    private static void draw(GuiGraphics gui, float age, float strength, int seed) {
        int width = gui.guiWidth();
        int height = gui.guiHeight();
        int top = Math.max(1, height / 7);
        int edge = Math.max(1, width / 9);
        int shadow = tint(0x210E21, strength * 0.43f);
        gui.fillGradient(0, 0, width, top, shadow, 0);
        gui.fillGradient(0, height - top, width, height, 0, shadow);
        for (int stripe = 0; stripe < 10; stripe++) {
            int x = (int) (edge * (stripe + 1) / 10.0);
            int previous = (int) (edge * stripe / 10.0);
            int color = tint(0x330B1A, strength * (10 - stripe) * 0.029f);
            gui.fill(previous, top, x, height - top, color);
            gui.fill(width - x, top, width - previous, height - top, color);
        }
        fringes(gui, width, height, age, strength, seed);
    }

    private static void fringes(GuiGraphics gui, int width, int height, float age, float strength, int seed) {
        float slip = strength * Math.max(0.18f, 1f - age / 9f);
        int cold = tint(0x9D698E, slip * 0.20f);
        int warm = tint(0xC84560, slip * 0.27f);
        for (int i = 0; i < 12; i++) {
            double curve = Math.sin(i * 0.46 + ((seed & 31) / 31.0) + age * 0.12);
            int y = (i + 1) * height / 13;
            int x = (int) (width * (0.065 + curve * 0.026));
            int length = (int) (width * (0.033 + slip * 0.079));
            gui.fill(x, y, x + length, y + 1, warm);
            gui.fill(x + 2, y + 2, x + length + 2, y + 3, cold);
            gui.fill(width - x - length, height - y - 1, width - x, height - y, warm);
            gui.fill(width - x - length - 2, height - y - 3, width - x - 2, height - y - 2, cold);
        }
    }

    private static int tint(int rgb, float alpha) {
        return (Math.round(alpha * 255) << 24) | rgb;
    }

    private static void useLevel(ClientLevel level) {
        if (world.get() != level) {
            world = new WeakReference<>(level);
            EYES.clear();
            victim = null;
        }
    }

    private record Cue(SharinganGazePacket packet, long start) {
    }
}
