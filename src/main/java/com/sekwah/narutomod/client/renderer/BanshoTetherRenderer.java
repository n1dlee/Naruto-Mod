package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.BanshoTetherPacket;
import com.sekwah.narutomod.util.BanshoPull;
import com.sekwah.narutomod.util.BanshoPull.Stage;
import com.sekwah.narutomod.util.ProjectileAxis;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BanshoTetherRenderer {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 FRONT = new Vec3(0, 0, 1);
    private static final int SILVER = 0xE9DCF9;
    private static final int LILAC = 0xA692D5;
    private static final int SHADOW = 0x50376D;
    private static final Map<Integer, Sample> ACTIVE = new LinkedHashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private BanshoTetherRenderer() {
    }

    public static void receive(BanshoTetherPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        use(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (ACTIVE.size() >= 16 && !ACTIVE.containsKey(packet.id())) {
            ACTIVE.remove(ACTIVE.keySet().iterator().next());
        }
        Sample prev = ACTIVE.get(packet.id());
        long time = level.getGameTime();
        long beat = prev != null && prev.packet().stage() == packet.stage() ? prev.beat() : time;
        ACTIVE.put(packet.id(), new Sample(packet, prev == null ? packet : prev.packet(), time, beat));
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        use(level);
        double now = level.getGameTime() + partial;
        ACTIVE.values().removeIf(sample -> now - sample.time() >= ttl(sample.packet().stage()));
        if (ACTIVE.isEmpty()) {
            return;
        }
        boolean low = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        var mesh = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        for (Sample sample : ACTIVE.values()) {
            double t = Mth.clamp(now - sample.time(), 0, 1);
            Vec3 start = sample.previous().hand().lerp(sample.packet().hand(), t).subtract(camera);
            Vec3 end = sample.previous().target().lerp(sample.packet().target(), t).subtract(camera);
            if (start.lengthSqr() <= 43 * 43 || end.lengthSqr() <= 38 * 38) {
                strand(mesh, new Span(start, end, now - sample.beat(), now, sample.packet().stage(), low));
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static int ttl(Stage stage) {
        return switch (stage) {
            case FOCUS, DRAG -> 6;
            case BREAK -> 5;
            case RELEASE -> 7;
        };
    }

    private static void strand(Mesh mesh, Span span) {
        boolean tearing = span.stage() == Stage.BREAK;
        boolean release = span.stage() == Stage.RELEASE;
        double weight = tearing || release ? 1 - Mth.clamp(span.age() / ttl(span.stage()), 0, 1) : 1;
        double charge = span.stage() == Stage.FOCUS ? Mth.clamp((span.age() + 0.5) / BanshoPull.FOCUS, 0.08, 1) : 1;
        Vec3 axis = ProjectileAxis.direction(span.end().subtract(span.start()), FRONT);
        Vec3 lateral = ProjectileAxis.direction(axis.cross(span.start().lerp(span.end(), 0.5)),
                axis.cross(Math.abs(axis.y) > 0.9 ? FRONT : UP));
        int bars = span.low() ? 6 : 12;
        int trails = span.low() ? 2 : 3;
        for (int ribbon = 0; ribbon < trails; ribbon++) {
            double slide = ribbon / (double) trails;
            for (int i = 0; i < bars; i++) {
                double a = i / (double) bars;
                double b = (i + 1.0) / bars;
                double signal = 0.42 + 0.58 * Math.pow(Math.max(0, Math.cos((a + span.time() * 0.18 + slide) * Math.PI * 4)), 2);
                if (tearing && (Math.abs(a - 0.5) < (1 - weight) * 0.34 || i % 3 == 1)) {
                    continue;
                }
                Vec3 from = fiber(span, lateral, a, ribbon);
                Vec3 to = fiber(span, lateral, b, ribbon);
                double width = (ribbon == 0 ? 0.045 : 0.014) * weight * charge;
                int alpha = (int) ((ribbon == 0 ? 153 : 204) * weight * charge * signal);
                mesh.ribbon(from, to, lateral.scale(width), ribbon == 0 ? SHADOW : ribbon == 1 ? SILVER : LILAC, alpha);
            }
        }
    }

    private static Vec3 fiber(Span span, Vec3 lateral, double t, int ribbon) {
        double curl = Math.sin(t * Math.PI) * (ribbon - 1) * 0.145;
        double jitter = Math.sin(t * 11 + span.time() * 0.72 + ribbon) * 0.035;
        return span.start().lerp(span.end(), t).add(lateral.scale(curl + jitter * Math.sin(Math.PI * t)));
    }

    private static void use(ClientLevel level) {
        if (world.get() != level) {
            ACTIVE.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Sample(BanshoTetherPacket packet, BanshoTetherPacket previous, long time, long beat) {
    }

    private record Span(Vec3 start, Vec3 end, double age, double time, Stage stage, boolean low) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void ribbon(Vec3 a, Vec3 b, Vec3 width, int color, int alpha) {
            vertex(a.add(width), color, alpha);
            vertex(b.add(width.scale(0.88)), color, alpha);
            vertex(b.subtract(width.scale(0.88)), color, alpha);
            vertex(a.subtract(width), color, alpha);
        }

        private void vertex(Vec3 at, int color, int alpha) {
            this.buffer.vertex(this.matrix, (float) at.x, (float) at.y, (float) at.z)
                    .color((color >>> 16) & 255, (color >>> 8) & 255, color & 255, alpha).endVertex();
        }
    }
}
