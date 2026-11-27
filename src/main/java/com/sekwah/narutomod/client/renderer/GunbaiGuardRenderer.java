package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.GunbaiGuardPacket;
import com.sekwah.narutomod.util.GustFront;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;

public final class GunbaiGuardRenderer {
    private static final ArrayDeque<Contact> IMPACTS = new ArrayDeque<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private GunbaiGuardRenderer() {
    }

    public static void add(GunbaiGuardPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        reset(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (IMPACTS.size() >= 24) {
            IMPACTS.removeFirst();
        }
        IMPACTS.addLast(new Contact(packet, level.getGameTime()));
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float pt) {
        reset(level);
        long now = level.getGameTime();
        IMPACTS.removeIf(contact -> now - contact.started() >= 9);
        if (IMPACTS.isEmpty()) {
            return;
        }
        int segments = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST ? 8 : 16;
        VertexConsumer mesh = buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW);
        for (Contact contact : IMPACTS) {
            float t = Mth.clamp((now - contact.started() + pt) / 9, 0, 1);
            Vec3 point = contact.packet().contact().subtract(camera);
            var frame = GustFront.frame(contact.packet().normal());
            point = point.add(frame.forward().scale(t * 0.55));
            float alpha = (1 - t) * (1 - t) * (float) Mth.clamp((point.length() - 0.28) * 1.6, 0, 1);
            ripple(matrix, mesh, new Ripple(point, frame, 0.10 + 0.85 * t, alpha, segments));
            if (segments > 8) {
                ripple(matrix, mesh, new Ripple(point.add(frame.forward().scale(0.10)), frame,
                        0.055 + 0.53 * t, alpha * 0.66f, segments));
            }
        }
        buffers.endBatch(NarutoRenderTypes.CHAKRA_GLOW);
    }

    private static void ripple(Matrix4f matrix, VertexConsumer mesh, Ripple ripple) {
        for (int i = 0; i < ripple.segments(); i++) {
            double a = Math.PI * 2 * i / ripple.segments();
            double b = Math.PI * 2 * (i + 1) / ripple.segments();
            Vec3 first = edge(ripple, a);
            Vec3 second = edge(ripple, b);
            float brightness = (i & 1) == 0 ? ripple.alpha() : ripple.alpha() * 0.63f;
            vertex(matrix, mesh, ripple.center().add(first), brightness);
            vertex(matrix, mesh, ripple.center().add(second), brightness);
            vertex(matrix, mesh, ripple.center().add(second.scale(0.78)), brightness * 0.19f);
            vertex(matrix, mesh, ripple.center().add(first.scale(0.78)), brightness * 0.19f);
        }
    }

    private static Vec3 edge(Ripple ripple, double a) {
        double fan = ripple.radius() * (0.91 + 0.13 * Math.cos(a * 3));
        return ripple.frame().right().scale(fan * Math.cos(a)).add(ripple.frame().up().scale(fan * Math.sin(a) * 1.2));
    }

    private static void vertex(Matrix4f matrix, VertexConsumer mesh, Vec3 at, float alpha) {
        mesh.vertex(matrix, (float) at.x, (float) at.y, (float) at.z).color(1.0f, 0.71f, 0.44f, alpha).endVertex();
    }

    private static void reset(ClientLevel level) {
        if (world.get() != level) {
            world = new WeakReference<>(level);
            IMPACTS.clear();
        }
    }

    private record Contact(GunbaiGuardPacket packet, long started) {
    }

    private record Ripple(Vec3 center, GustFront.Frame frame, double radius, float alpha, int segments) {
    }
}
