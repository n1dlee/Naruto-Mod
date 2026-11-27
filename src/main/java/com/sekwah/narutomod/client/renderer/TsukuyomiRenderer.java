package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.network.s2c.TsukuyomiPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.TsukuyomiScene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TsukuyomiRenderer {
    private static Scene current;

    private TsukuyomiRenderer() {
    }

    public static void accept(TsukuyomiPacket packet) {
        Minecraft game = Minecraft.getInstance();
        if (game.level == null || game.player == null || game.player.getId() != packet.victim()
                || !game.level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (packet.ticks() == 0) {
            if (current != null && TsukuyomiScene.matches(current.id(), packet.id())) {
                clear(game);
            }
            return;
        }
        if (packet.ticks() < 0 || packet.ticks() > TsukuyomiScene.MAX_TICKS || !validCamera(game)
                || current != null && current.id() == packet.id()) {
            return;
        }
        clear(game);
        SoundInstance sound = SimpleSoundInstance.forUI(NarutoSounds.TSUKUYOMI_ECHO.get(), 1.0f, 0.78f);
        game.getSoundManager().play(sound);
        current = new Scene(game.level, packet.victim(), packet.id(), game.level.getGameTime(), packet.ticks(), sound);
    }

    static boolean active() {
        return current != null;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || current == null) {
            return;
        }
        Minecraft game = Minecraft.getInstance();
        if (!validCamera(game) || game.level != current.level() || game.player.getId() != current.victim()
                || game.level.getGameTime() - current.start() >= current.ticks()) {
            clear(game);
        }
    }

    @SubscribeEvent
    public static void overlay(RenderGuiOverlayEvent.Pre event) {
        Minecraft game = Minecraft.getInstance();
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type() || current == null || game.level != current.level()
                || !validCamera(game) || game.player.getId() != current.victim() || game.options.hideGui) {
            return;
        }
        float age = game.level.getGameTime() - current.start() + event.getPartialTick();
        float progress = Math.max(0, Math.min(1, age / current.ticks()));
        float alpha = TsukuyomiScene.sceneAlpha(age, current.ticks(), game.options.screenEffectScale().get());
        GuiGraphics gui = event.getGuiGraphics();
        if (alpha > 0.001f) {
            drawScene(gui, alpha, progress);
        }
        timer(gui, game, progress);
    }

    private static void drawScene(GuiGraphics gui, float alpha, float progress) {
        int w = gui.guiWidth();
        int h = gui.guiHeight();
        gui.fillGradient(0, 0, w, h / 2, tint(0x280B19, alpha * 0.83f), tint(0xAB2535, alpha * 0.42f));
        gui.fillGradient(0, h / 2, w, h, tint(0x571D2F, alpha * 0.65f), tint(0x110A15, alpha * 0.90f));
        gui.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Mesh mesh = new Mesh(gui.pose().last().pose(), buffer, Math.min(w, h) * 0.41f, w * 0.5f, h * 0.42f, alpha);
        mesh.moon();
        mesh.floor();
        for (int i = 2; i >= 0; i--) {
            mesh.figure((i - 1) * 0.48f, 0x742C48, TsukuyomiScene.echoAmount(i, progress) * 0.38f, 0.82f);
        }
        mesh.figure(0, 0x130B17, 0.88f, 1.0f);
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    private static void timer(GuiGraphics gui, Minecraft game, float progress) {
        int center = gui.guiWidth() / 2;
        int y = gui.guiHeight() * 2 / 3 + 22;
        int half = 25;
        gui.drawCenteredString(game.font, Component.translatable("narutomod.overlay.tsukuyomi"), center, y, 0xEBC7CB);
        gui.fill(center - half, y + 11, center + half, y + 13, 0xA9271624);
        gui.fill(center - half, y + 11, center - half + (int) (2 * half * (1 - progress)), y + 13, 0xFFA24254);
    }

    private static int tint(int rgb, float alpha) {
        return (Math.round(alpha * 255) << 24) | rgb;
    }

    private static boolean validCamera(Minecraft game) {
        return game.level != null && game.player != null && game.player.isAlive() && !game.player.isSpectator()
                && game.getCameraEntity() == game.player;
    }

    private static void clear(Minecraft game) {
        if (current != null) {
            game.getSoundManager().stop(current.sound());
            current = null;
        }
    }

    private record Scene(ClientLevel level, int victim, int id, long start, int ticks, SoundInstance sound) {
    }

    private record Mesh(Matrix4f matrix, BufferBuilder buffer, float scale, float x, float y, float alpha) {
        private void moon() {
            disk(0.35f, -0.20f, 0.50f, 0xB63049, 0.31f);
            disk(0.35f, -0.20f, 0.38f, 0xD55B59, 0.70f);
            disk(0.25f, -0.22f, 0.34f, 0x3D101F, 0.80f);
        }

        private void disk(float x, float y, float radius, int rgb, float opacity) {
            for (int i = 0; i < 40; i++) {
                double a = Math.PI * i / 20;
                double b = Math.PI * (i + 1) / 20;
                vertex(x, y, rgb, opacity);
                vertex(x + radius * (float) Math.cos(a), y + radius * (float) Math.sin(a), rgb, opacity);
                vertex(x + radius * (float) Math.cos(b), y + radius * (float) Math.sin(b), rgb, opacity);
            }
        }

        private void floor() {
            triangle(new Point(-2.6f, 0.20f), new Point(2.6f, 0.26f), new Point(0.15f, 1.05f), 0x341627, 0.68f);
            for (int i = -4; i <= 4; i++) {
                triangle(new Point(0.022f * i, 0.27f), new Point(i * 0.63f - 0.012f, 1.3f),
                        new Point(i * 0.63f + 0.013f, 1.3f), 0xA85061, i % 2 == 0 ? 0.34f : 0.21f);
            }
        }

        private void figure(float x, int rgb, float opacity, float size) {
            if (opacity <= 0.001f) {
                return;
            }
            disk(x, -0.30f * size, 0.101f * size, rgb, opacity);
            triangle(new Point(x - 0.09f * size, -0.27f * size), new Point(x + 0.13f * size, -0.34f * size),
                    new Point(x + 0.035f * size, -0.49f * size), rgb, opacity);
            triangle(new Point(x - 0.20f * size, 0.018f), new Point(x + 0.14f * size, -0.15f * size),
                    new Point(x + 0.26f * size, 0.51f * size), rgb, opacity);
            triangle(new Point(x - 0.13f * size, -0.16f * size), new Point(x - 0.18f * size, 0.47f * size),
                    new Point(x + 0.26f * size, 0.51f * size), rgb, opacity);
            triangle(new Point(x - 0.08f * size, 0.49f * size), new Point(x + 0.14f * size, 0.50f * size),
                    new Point(x + 0.02f * size, 0.67f * size), rgb, opacity * 0.61f);
        }

        private void triangle(Point a, Point b, Point c, int rgb, float opacity) {
            vertex(a.x(), a.y(), rgb, opacity);
            vertex(b.x(), b.y(), rgb, opacity);
            vertex(c.x(), c.y(), rgb, opacity);
        }

        private void vertex(float relativeX, float relativeY, int rgb, float opacity) {
            buffer.vertex(matrix, x + relativeX * scale, y + relativeY * scale, 0)
                    .color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, Math.round(alpha * opacity * 255)).endVertex();
        }
    }

    private record Point(float x, float y) {
    }
}
