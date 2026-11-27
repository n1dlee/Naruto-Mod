package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.ShinranshinPacket;
import com.sekwah.narutomod.util.ProjectileAxis;
import com.sekwah.narutomod.util.Shinranshin;
import com.sekwah.narutomod.util.Shinranshin.Stage;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ShinranshinRenderer {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 X = new Vec3(1, 0, 0);
    private static final Map<Integer, Thread> THREADS = new LinkedHashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);
    private static final Tint VIOLET = new Tint(138, 103, 180);
    private static final Tint SILVER = new Tint(215, 187, 244);
    private static final Tint PALE = new Tint(238, 222, 251);

    private ShinranshinRenderer() {
    }

    public static void receive(ShinranshinPacket packet) {
        var level = Minecraft.getInstance().level;
        use(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        long now = level.getGameTime();
        Thread previous = THREADS.get(packet.cast());
        if (previous == null && THREADS.size() >= 20) {
            THREADS.remove(THREADS.keySet().iterator().next());
        }
        boolean same = previous != null && previous.packet().stage() == packet.stage();
        THREADS.put(packet.cast(), new Thread(packet, same ? previous.start() : now, now));
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        use(level);
        double now = level.getGameTime() + partial;
        THREADS.values().removeIf(thread -> now - thread.received() > thread.packet().ticks() + 2);
        if (THREADS.isEmpty()) {
            return;
        }
        var mesh = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        boolean low = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        for (Thread thread : THREADS.values()) {
            var packet = thread.packet();
            Vec3 from = packet.origin().subtract(camera);
            if (from.lengthSqr() > 32 * 32) {
                continue;
            }
            Vec3 to = packet.contact().subtract(camera);
            if (packet.stage() != Stage.BREAK) {
                from = anchor(level, packet.actor(), packet.origin(), partial, 0.76).subtract(camera);
                to = anchor(level, packet.target(), packet.contact(), partial, 0.64).subtract(camera);
            }
            float age = (float) (now - thread.start());
            var axis = ProjectileAxis.direction(to.subtract(from), X);
            Vec3 side = axis.cross(Math.abs(axis.y) <= 0.92 ? UP : X).normalize();
            var strip = new Strip(from, to, side, axis.cross(side), age);
            if (packet.stage() == Stage.WINDUP) {
                drawThread(mesh, strip, Mth.clamp(age / Shinranshin.FOCUS, 0, 1), 0.55f, low);
            } else if (packet.stage() == Stage.GRIP) {
                drawThread(mesh, strip, 1, 1, low);
                targetMark(mesh, strip, Mth.clamp(age * 0.42f, 0, 1), low);
            } else {
                float fade = Mth.clamp(1 - age / Shinranshin.FADE, 0, 1);
                drawThread(mesh, strip, fade, fade, low);
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static Vec3 anchor(ClientLevel level, int actor, Vec3 fallback, float partial, double height) {
        return level.getEntity(actor) instanceof LivingEntity entity
                ? entity.getPosition(partial).add(0, entity.getBbHeight() * height, 0) : fallback;
    }

    private static void drawThread(Mesh mesh, Strip strip, float reveal, float fade, boolean low) {
        int segments = low ? 14 : 24;
        for (int strand = 0; strand < 2; strand++) {
            for (int i = 0; i < segments && i / (double) segments < reveal; i++) {
                double a = i / (double) segments;
                double b = Math.min(reveal, (i + 1) / (double) segments);
                Vec3 start = strip.point(a, strand);
                Vec3 end = strip.point(b, strand);
                float glint = 0.58f + 0.42f * Mth.sin(strip.age() * 0.29f - i * 0.63f + strand);
                mesh.ribbon(start, end, strip.side().scale(0.012 + 0.008 * glint),
                        strand == 0 ? SILVER : VIOLET, (int) ((80 + 96 * glint) * fade));
            }
        }
    }

    private static void targetMark(Mesh mesh, Strip strip, float opacity, boolean low) {
        int edges = low ? 4 : 8;
        for (int i = 0; i < edges; i++) {
            double angle = i * Math.PI * 2 / edges + strip.age() * 0.075;
            Vec3 a = strip.side().scale(Math.cos(angle)).add(strip.up().scale(Math.sin(angle)));
            Vec3 b = strip.side().scale(Math.cos(angle + 0.29)).add(strip.up().scale(Math.sin(angle + 0.29)));
            Vec3 contact = strip.to().add(strip.from().subtract(strip.to()).normalize().scale(0.24));
            mesh.ribbon(contact.add(a.scale(0.38)), contact.add(b.scale(0.22)), a.scale(0.026), PALE, (int) (143 * opacity));
        }
    }

    private static void use(ClientLevel level) {
        if (world.get() != level) {
            THREADS.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Thread(ShinranshinPacket packet, long start, long received) {
    }

    private record Strip(Vec3 from, Vec3 to, Vec3 side, Vec3 up, float age) {
        private Vec3 point(double t, int strand) {
            double phase = t * 18 - this.age * 0.36 + strand * Math.PI;
            double width = 0.025 + Math.sin(t * Math.PI) * 0.12;
            return this.from.lerp(this.to, t).add(this.side.scale(Math.cos(phase) * width))
                    .add(this.up.scale(Math.sin(phase) * width * 0.61));
        }
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void ribbon(Vec3 from, Vec3 to, Vec3 width, Tint tint, int alpha) {
            vertex(from.add(width), tint, alpha);
            vertex(to.add(width), tint, alpha);
            vertex(to.subtract(width), tint, alpha);
            vertex(from.subtract(width), tint, alpha);
        }

        private void vertex(Vec3 v, Tint t, int alpha) {
            this.buffer.vertex(this.matrix, (float) v.x, (float) v.y, (float) v.z)
                    .color(t.red(), t.green(), t.blue(), alpha).endVertex();
        }
    }

    private record Tint(int red, int green, int blue) {
    }
}
