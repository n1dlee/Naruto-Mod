package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.network.s2c.HiraishinPacket;
import com.sekwah.narutomod.network.s2c.HiraishinPacket.Stage;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class HiraishinRenderer {
    private static final Deque<Visible> FLASHES = new ArrayDeque<>();
    private static final int AMBER = 0xF4A52D;
    private static final int IVORY = 0xFFF2B4;
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);
    private static Visible preview;

    private HiraishinRenderer() {
    }

    public static void receive(HiraishinPacket packet) {
        var game = Minecraft.getInstance();
        ClientLevel level = game.level;
        sync(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        Visible visible = new Visible(packet, level.getGameTime());
        if (packet.stage() == Stage.PREVIEW || packet.stage() == Stage.CLEAR) {
            if (game.player != null && game.player.getId() == packet.owner()) {
                preview = packet.stage() == Stage.PREVIEW ? visible : null;
            }
        } else {
            if (FLASHES.size() >= 36) {
                FLASHES.removeFirst();
            }
            FLASHES.addLast(visible);
        }
    }

    static void draw(ClientLevel level, Matrix4f transform, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        sync(level);
        double now = level.getGameTime() + partial;
        FLASHES.removeIf(flash -> flash.expired(now));
        if (FLASHES.isEmpty() && preview == null) {
            return;
        }
        boolean low = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        var frame = new Frame(camera, now, partial, low);
        var mesh = new Mesh(transform, buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW));
        for (Visible flash : FLASHES) {
            paint(level, mesh, flash, frame);
        }
        if (preview != null) {
            paint(level, mesh, preview, frame);
        }
        buffers.endBatch(NarutoRenderTypes.CHAKRA_GLOW);
    }

    @SubscribeEvent
    public static void drawHint(RenderGuiOverlayEvent.Post event) {
        var game = Minecraft.getInstance();
        sync(game.level);
        if (preview == null || event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type() || game.options.hideGui
                || game.player == null || game.getCameraEntity() != game.player || game.player.isSpectator()) {
            return;
        }
        String key = switch (preview.packet().glyph().kind()) {
            case GROUND -> "hiraishin.preview.ground";
            case CREATURE -> "hiraishin.preview.creature";
            case KUNAI -> "hiraishin.preview.kunai";
        };
        event.getGuiGraphics().drawCenteredString(game.font, Component.translatable(key),
                event.getWindow().getGuiScaledWidth() / 2, event.getWindow().getGuiScaledHeight() / 2 + 13, IVORY);
    }

    private static void sync(ClientLevel level) {
        if (world.get() != level || level == null) {
            FLASHES.clear();
            preview = null;
            world = new WeakReference<>(level);
        } else if (preview != null && preview.expired(level.getGameTime())) {
            preview = null;
        }
    }

    private static void paint(ClientLevel level, Mesh mesh, Visible visible, Frame frame) {
        var glyph = visible.packet().glyph();
        var stage = visible.packet().stage();
        Vec3 point = glyph.point();
        Direction face = glyph.face();
        if (level.getEntity(glyph.entityId()) instanceof LivingEntity target) {
            face = target.getDirection().getOpposite();
            point = target.getPosition(frame.partial()).add(0, target.getBbHeight() * 0.72, 0)
                    .add(Vec3.atLowerCornerOf(face.getNormal()).scale(target.getBbWidth() * 0.5));
        }
        Vec3 at = point.subtract(frame.camera());
        if (at.lengthSqr() > 45 * 45) {
            return;
        }
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 across = face.getAxis().isVertical() ? new Vec3(1, 0, 0) : normal.cross(new Vec3(0, 1, 0));
        Vec3 up = face.getAxis().isVertical() ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
        var plane = new Plane(at.add(normal.scale(0.018)), across, up);
        double age = frame.now() - visible.born();
        if (stage == Stage.DEPART || stage == Stage.ARRIVE) {
            flash(mesh, plane, stage, age, frame.low());
        } else {
            double progress = stage == Stage.PREVIEW ? 1 : Mth.clamp((age + 1) / 4, 0, 1);
            int alpha = stage == Stage.PREVIEW ? 125 : (int) (225 * (1 - age / 12));
            seal(mesh, plane, progress, alpha);
        }
    }

    private static void seal(Mesh mesh, Plane plane, double progress, int alpha) {
        Vec3 point = plane.origin();
        mesh.slash(plane, 0, 0, 0, 0.23 * progress, new Ink(IVORY, alpha, 0.015));
        mesh.slash(plane, 0, 0, 0, -0.23 * progress, new Ink(IVORY, alpha, 0.015));
        for (int sign : new int[]{-1, 1}) {
            Plane side = new Plane(point, plane.across().scale(sign), plane.up());
            mesh.slash(side, 0.025, 0.07, 0.28 * progress, 0.13, new Ink(AMBER, alpha, 0.024));
            mesh.slash(side, 0.06, -0.06, 0.24 * progress, -0.13, new Ink(AMBER, alpha, 0.017));
            if (progress >= 0.75) {
                mesh.slash(side, 0.16, 0.02, 0.18, -0.052, new Ink(IVORY, alpha, 0.012));
            }
        }
    }

    private static void flash(Mesh mesh, Plane plane, Stage stage, double age, boolean low) {
        double life = Mth.clamp(age / (stage == Stage.DEPART ? 5 : 8), 0, 1);
        double width = stage == Stage.ARRIVE ? 0.13 + life * 0.6 : 0.3 * (1 - life);
        int alpha = (int) ((1 - life) * 224);
        mesh.slash(plane, 0, 0.14, 0, 1.82 - life * 0.55, new Ink(AMBER, alpha / 2, width * 0.35));
        mesh.slash(plane, 0, 0.06, 0, 1.60 - life * 0.36, new Ink(IVORY, alpha, 0.018));
        for (int side : new int[]{-1, 1}) {
            double edge = side * width;
            mesh.slash(plane, edge, 0.21, edge * 0.42, 1.21, new Ink(AMBER, alpha, 0.021));
            if (!low) {
                mesh.slash(plane, edge * 1.6, 0.72, edge, 1.34, new Ink(IVORY, alpha / 2, 0.011));
            }
        }
    }

    private record Visible(HiraishinPacket packet, long born) {
        private boolean expired(double now) {
            int duration = switch (packet.stage()) {
                case DEPART -> 5;
                case ARRIVE -> 8;
                case PREVIEW -> 13;
                case MARK, CLEAR -> 12;
            };
            return now - born >= duration;
        }
    }

    private record Plane(Vec3 origin, Vec3 across, Vec3 up) {
        private Vec3 at(double u, double v) {
            return origin.add(across.scale(u)).add(up.scale(v));
        }
    }

    private record Frame(Vec3 camera, double now, float partial, boolean low) {
    }

    private record Ink(int color, int alpha, double width) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void slash(Plane plane, double x, double y, double endX, double endY, Ink ink) {
            Vec3 a = plane.at(x, y);
            Vec3 b = plane.at(endX, endY);
            Vec3 normal = plane.across().scale(y - endY).add(plane.up().scale(endX - x)).normalize().scale(ink.width());
            vertex(a.add(normal), ink);
            vertex(b, ink);
            vertex(a.subtract(normal), ink);
            vertex(a.subtract(normal).lerp(b, 0.34), ink);
        }

        private void vertex(Vec3 point, Ink ink) {
            int color = ink.color();
            buffer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color((color >>> 16) & 255, (color >>> 8) & 255, color & 255, ink.alpha()).endVertex();
        }
    }
}
