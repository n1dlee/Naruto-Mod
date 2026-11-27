package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.KotoInfluencePacket;
import com.sekwah.narutomod.util.KotoInfluence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

public final class KotoInfluenceRenderer {
    private static final Map<Integer, Long> GLINTS = new LinkedHashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);
    private static Vision vision;

    private KotoInfluenceRenderer() {
    }

    public static void add(KotoInfluencePacket packet) {
        Minecraft game = Minecraft.getInstance();
        ClientLevel level = game.level;
        useLevel(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        long now = level.getGameTime();
        if (packet.cue() == KotoInfluencePacket.Cue.GLINT) {
            if (GLINTS.size() >= 32) {
                GLINTS.remove(GLINTS.keySet().iterator().next());
            }
            GLINTS.put(packet.subject(), now);
        } else if (game.player != null && game.player.getId() == packet.subject()) {
            acceptVision(packet, now);
        }
    }

    private static void acceptVision(KotoInfluencePacket packet, long now) {
        if (packet.cue() == KotoInfluencePacket.Cue.VISION && packet.ticks() <= KotoInfluence.PLAYER_TICKS
                && packet.ticks() > 0) {
            vision = new Vision(packet, now);
        } else if (packet.cue() == KotoInfluencePacket.Cue.END && vision != null && vision.packet().id() == packet.id()) {
            vision = null;
        }
    }

    public static float iris(Player player, float partial) {
        Long start = GLINTS.get(player.getId());
        if (start == null || player.level() != world.get()) {
            return 0;
        }
        return KotoInfluence.afterimage(player.level().getGameTime() - start + partial, 0.74);
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        useLevel(level);
        long now = level.getGameTime();
        GLINTS.values().removeIf(start -> now - start >= 12);
        if (vision == null) {
            return;
        }
        Minecraft game = Minecraft.getInstance();
        long ticks = now - vision.start();
        if (game.player == null || !game.player.isAlive() || game.player.getId() != vision.packet().subject()
                || game.player.isSpectator() || ticks >= vision.packet().ticks()) {
            vision = null;
            return;
        }
        if (game.getCameraEntity() != game.player || TsukuyomiRenderer.active()) {
            return;
        }
        Vec3 base = vision.packet().anchor().subtract(camera);
        if (base.lengthSqr() < 1.15 || base.lengthSqr() > 24 * 24) {
            return;
        }
        float alpha = KotoInfluence.afterimage(ticks + partial, game.options.screenEffectScale().get());
        if (alpha <= 0.01f) {
            return;
        }
        Vec3 right = new Vec3(-base.z, 0, base.x).normalize();
        double sign = (vision.packet().id() & 1) == 0 ? 1 : -1;
        Vec3 offset = right.scale(sign * (0.23 + 0.19 * alpha));
        var mesh = new Contour(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE), base.add(offset), right, alpha);
        mesh.draw();
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static void useLevel(ClientLevel level) {
        if (world.get() != level) {
            world = new WeakReference<>(level);
            GLINTS.clear();
            vision = null;
        }
    }

    private record Vision(KotoInfluencePacket packet, long start) {
    }

    private record Contour(Matrix4f matrix, VertexConsumer mesh, Vec3 feet, Vec3 right, float alpha) {
        private void draw() {
            line(-0.13, 1.72, 0.14, 1.76, 0.56f);
            line(-0.13, 1.72, -0.16, 1.47, 0.62f);
            line(0.14, 1.76, 0.15, 1.49, 0.57f);
            line(-0.16, 1.47, -0.36, 1.30, 0.56f);
            line(0.15, 1.49, 0.30, 1.34, 0.62f);
            line(-0.36, 1.30, -0.31, 0.81, 0.37f);
            line(0.30, 1.34, 0.35, 0.89, 0.48f);
            line(0.35, 0.89, 0.18, 0.38, 0.38f);
            line(-0.31, 0.81, -0.22, 0.30, 0.34f);
            line(-0.22, 0.30, -0.14, 0.09, 0.18f);
        }

        private void line(double ax, double ay, double bx, double by, float opacity) {
            Vec3 start = feet.add(right.scale(ax)).add(0, ay, 0);
            Vec3 end = feet.add(right.scale(bx)).add(0, by, 0);
            var edge = right.scale(-(by - ay)).add(0, bx - ax, 0).normalize().scale(0.027);
            vertex(start.subtract(edge), opacity * 0.19f);
            vertex(end.subtract(edge), opacity * 0.14f);
            vertex(end.add(edge), opacity * 0.56f);
            vertex(start.add(edge), opacity);
        }

        private void vertex(Vec3 at, float opacity) {
            mesh.vertex(matrix, (float) at.x, (float) at.y, (float) at.z)
                    .color(0.46f, 0.75f, 0.64f, alpha * opacity * 0.48f).endVertex();
        }
    }
}
