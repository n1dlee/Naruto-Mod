package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.ShadowCloneCloudPacket;
import com.sekwah.narutomod.network.s2c.ShadowCloneCloudPacket.Beat;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;

public final class ShadowCloneCloudRenderer {
    private static final Deque<Cloud> CLOUDS = new ArrayDeque<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private ShadowCloneCloudRenderer() {
    }

    public static void receive(ShadowCloneCloudPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        sync(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (CLOUDS.size() >= 48) {
            CLOUDS.removeFirst();
        }
        CLOUDS.addLast(new Cloud(packet, level.getGameTime()));
    }

    static void draw(ClientLevel level, Matrix4f transform, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        sync(level);
        double now = level.getGameTime() + partial;
        CLOUDS.removeIf(cloud -> now - cloud.born() >= lifetime(cloud.packet().beat()));
        if (CLOUDS.isEmpty()) {
            return;
        }
        var game = Minecraft.getInstance();
        boolean low = game.options.graphicsMode().get() == GraphicsStatus.FAST;
        var mesh = new Mesh(transform, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        for (Cloud cloud : CLOUDS) {
            Beat beat = cloud.packet().beat();
            double life = Mth.clamp((now - cloud.born()) / lifetime(beat), 0, 1);
            for (Vec3 site : cloud.packet().sites()) {
                Vec3 at = site.subtract(camera);
                if (at.lengthSqr() < 45 * 45) {
                    drawSite(mesh, at, beat, life, low || at.lengthSqr() > 25 * 25);
                }
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static void drawSite(Mesh mesh, Vec3 at, Beat beat, double life, boolean low) {
        boolean mark = beat == Beat.SEAL || beat == Beat.MASS_SEAL;
        boolean mass = beat == Beat.MASS_SEAL || beat == Beat.MASS_FORM;
        boolean dispel = beat == Beat.DISPEL;
        double expansion = 1 - (1 - life) * (1 - life);
        double reach = (mark ? (mass ? 0.84 : 0.43) : 0.64) * (0.32 + expansion)
                * (dispel ? 1 - life * 0.55 : 1);
        int alpha = (int) ((mark ? 118 : 174) * (1 - life) * (1 - life * 0.68));
        int layers = low ? 2 : 5;
        for (int i = 0; i < layers; i++) {
            double angle = i * 2.39996 + (mass ? 0.41 : 0);
            double height = mark ? 0.18 + life * 0.74 : 0.32 + (i % 3) * 0.26 + expansion * 0.73;
            Vec3 center = at.add(Math.cos(angle) * reach, height, Math.sin(angle) * reach);
            double radius = reach * (0.68 - i * 0.055);
            int tint = i % 3 == 0 ? 0xEAF2FF : (mass ? 0xBBC4D9 : 0xD2D7E0);
            mesh.puff(center, radius, new Ink(tint, alpha), low);
        }
    }

    private static int lifetime(Beat beat) {
        return switch (beat) {
            case SEAL -> 6;
            case MASS_SEAL -> 10;
            case FORM, DISPEL -> 13;
            case MASS_FORM -> 15;
        };
    }

    private static void sync(ClientLevel level) {
        if (world.get() != level) {
            CLOUDS.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Cloud(ShadowCloneCloudPacket packet, long born) {
    }

    private record Ink(int color, int alpha) {
    }

    private record Puff(Vec3 center, Vec3 right, double radius) {
        private Vec3 edge(double angle) {
            double scallop = 1 + 0.13 * Math.cos(angle * 5 + 0.39);
            return center.add(right.scale(Math.cos(angle) * radius * scallop))
                    .add(0, Math.sin(angle) * radius * 0.76 * scallop, 0);
        }
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void puff(Vec3 center, double radius, Ink ink, boolean low) {
            Vec3 right = new Vec3(center.z, 0, -center.x).normalize();
            var shape = new Puff(center, right.lengthSqr() > 0 ? right : new Vec3(1, 0, 0), radius);
            int segments = low ? 8 : 14;
            for (int i = 0; i < segments; i++) {
                Vec3 a = shape.edge(i * Math.PI * 2 / segments);
                Vec3 b = shape.edge((i + 1) * Math.PI * 2 / segments);
                vertex(center, ink, ink.alpha());
                vertex(a, ink, 0);
                vertex(b, ink, 0);
                vertex(center, ink, ink.alpha());
            }
        }

        private void vertex(Vec3 point, Ink ink, int alpha) {
            int rgb = ink.color();
            buffer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color((rgb >>> 16) & 255, (rgb >>> 8) & 255, rgb & 255, alpha).endVertex();
        }
    }
}
