package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.FlickerPacket;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FlickerRenderer {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Map<Integer, Long> GATHERING = new LinkedHashMap<>();
    private static final ArrayDeque<Burst> TRACES = new ArrayDeque<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private FlickerRenderer() {
    }

    public static void add(FlickerPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        useLevel(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (packet.gathering()) {
            if (GATHERING.size() >= 48) {
                GATHERING.remove(GATHERING.keySet().iterator().next());
            }
            GATHERING.put(packet.actor(), level.getGameTime());
        } else {
            GATHERING.remove(packet.actor());
            if (TRACES.size() >= 28) {
                TRACES.removeFirst();
            }
            TRACES.addLast(new Burst(packet, level.getGameTime()));
        }
    }

    public static float pose(Entity actor, float partialTick) {
        Long start = GATHERING.get(actor.getId());
        if (actor.level() != world.get() || start == null) {
            return 0;
        }
        float age = actor.level().getGameTime() - start + partialTick;
        return Mth.clamp(age * 0.44f + 0.32f, 0, 1) * Mth.clamp(4.2f - age, 0, 1);
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers,
                     Vec3 camera, float partialTick) {
        useLevel(level);
        long now = level.getGameTime();
        TRACES.removeIf(trace -> now - trace.started() >= 7);
        GATHERING.values().removeIf(start -> now - start >= 5);
        if (TRACES.isEmpty()) {
            return;
        }
        VertexConsumer mesh = buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW);
        boolean low = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        for (Burst trace : TRACES) {
            float age = now - trace.started() + partialTick;
            FlickerPacket packet = trace.packet();
            Vec3 a = packet.from().subtract(camera);
            Vec3 b = packet.to().subtract(camera);
            float fade = Mth.clamp(1.0f - age / 7, 0, 1);
            Vec3 axis = b.subtract(a).normalize();
            Vec3 side = axis.cross(Math.abs(axis.y) > 0.94 ? new Vec3(1, 0, 0) : UP).normalize();
            float height = packet.height();
            ribbon(matrix, mesh, a.add(0, height * 0.60, 0), b.add(0, height * 0.40, 0),
                    side.scale(0.14 + fade * 0.16), fade * 0.42f);
            int ghosts = low || a.lengthSqr() > 22 * 22 ? 1 : 3;
            for (int i = 0; i < ghosts; i++) {
                double offset = ((packet.seed() >>> (i * 5)) & 7) * 0.006;
                Vec3 position = a.lerp(b, ghosts == 1 ? 0.52 : 0.20 + i * 0.25 + offset);
                ghost(matrix, mesh, position, axis, side, height, fade * (1.0f - i * 0.20f));
            }
        }
        buffers.endBatch(NarutoRenderTypes.CHAKRA_GLOW);
    }

    private static void ghost(Matrix4f matrix, VertexConsumer mesh, Vec3 feet, Vec3 axis, Vec3 side,
                              float height, float fade) {
        Vec3 hips = feet.add(0, height * 0.47, 0);
        Vec3 shoulders = hips.add(axis.scale(0.29)).add(0, height * 0.29, 0);
        Vec3 head = shoulders.add(axis.scale(0.16)).add(0, height * 0.13, 0);
        ribbon(matrix, mesh, hips, shoulders, side.scale(height * 0.23), fade * 0.31f);
        ribbon(matrix, mesh, shoulders, head, side.scale(height * 0.108), fade * 0.34f);
        ribbon(matrix, mesh, feet.add(side.scale(0.09)), hips, side.scale(0.07), fade * 0.20f);
    }

    private static void ribbon(Matrix4f matrix, VertexConsumer mesh, Vec3 a, Vec3 b, Vec3 edge, float alpha) {
        vertex(matrix, mesh, a.add(edge.scale(0.32)), alpha * 0.12f);
        vertex(matrix, mesh, a.subtract(edge.scale(0.32)), alpha * 0.32f);
        vertex(matrix, mesh, b.subtract(edge), alpha * 0.85f);
        vertex(matrix, mesh, b.add(edge), alpha);
    }

    private static void vertex(Matrix4f matrix, VertexConsumer mesh, Vec3 point, float alpha) {
        mesh.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(0.79f, 0.93f, 0.95f, alpha).endVertex();
    }

    private static void useLevel(ClientLevel level) {
        if (world.get() != level) {
            world = new WeakReference<>(level);
            GATHERING.clear();
            TRACES.clear();
        }
    }

    private record Burst(FlickerPacket packet, long started) {
    }
}
