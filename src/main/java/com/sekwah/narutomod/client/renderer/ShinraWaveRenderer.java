package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.ShinraWavePacket;
import com.sekwah.narutomod.util.ShinraWave;
import com.sekwah.narutomod.util.ShinraWave.Mode;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ShinraWaveRenderer {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 RIGHT = new Vec3(1, 0, 0);
    private static final Vec3 FRONT = new Vec3(0, 0, 1);
    private static final int SILVER = 0xF4F2FF;
    private static final int LILAC = 0xAD9CD4;
    private static final int RIM = 0x594B80;
    private static final Map<Integer, Event> ACTIVE = new LinkedHashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private ShinraWaveRenderer() {
    }

    public static void receive(ShinraWavePacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        use(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (packet.mode() == Mode.CANCEL) {
            ACTIVE.remove(packet.caster());
            return;
        }
        if (ACTIVE.size() >= 16 && !ACTIVE.containsKey(packet.caster())) {
            Iterator<Integer> keys = ACTIVE.keySet().iterator();
            keys.next();
            keys.remove();
        }
        ACTIVE.put(packet.caster(), new Event(packet, level.getGameTime()));
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        use(level);
        double now = level.getGameTime() + partial;
        ACTIVE.values().removeIf(event -> now - event.start() >= lifetime(event.packet()));
        if (ACTIVE.isEmpty()) {
            return;
        }
        boolean fast = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        var palette = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        for (Event event : ACTIVE.values()) {
            Vec3 origin = anchor(level, event, partial).subtract(camera);
            if (origin.lengthSqr() > 58 * 58) {
                continue;
            }
            double age = Math.max(0, now - event.start());
            boolean low = fast || origin.lengthSqr() > 30 * 30;
            if (event.packet().mode() == Mode.CHARGE) {
                compress(palette, origin, age, low);
            } else {
                release(palette, origin, age, low);
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static Vec3 anchor(ClientLevel level, Event event, float partial) {
        var packet = event.packet();
        if (packet.mode() == Mode.CHARGE && level.getEntity(packet.caster()) instanceof Player caster) {
            return caster.getPosition(partial).add(0, caster.getBbHeight() * 0.63, 0);
        }
        return packet.center();
    }

    private static int lifetime(ShinraWavePacket packet) {
        return packet.mode() == Mode.CHARGE ? ShinraWave.CHARGE + 4 : ShinraWave.EXPAND + ShinraWave.AFTERSHOCK;
    }

    private static void compress(Mesh mesh, Vec3 center, double age, boolean low) {
        double build = Mth.clamp(age / ShinraWave.CHARGE, 0.05, 1);
        double radius = 1.40 - 0.61 * build;
        int density = low ? 14 : 24;
        mesh.ring(new Ring(center, RIGHT, FRONT, radius, 0.030 + build * 0.031), new Ink(LILAC, (int) (87 + 114 * build), density));
        mesh.ring(new Ring(center, RIGHT, UP, radius * 0.84, 0.036), new Ink(SILVER, (int) (70 + 100 * build), density));
        if (!low) {
            Vec3 shifted = center.add(0, -0.35 * build, 0);
            mesh.ring(new Ring(shifted, RIGHT, FRONT, radius * 0.91, 0.025), new Ink(RIM, (int) (85 + 119 * build), density));
        }
    }

    private static void release(Mesh mesh, Vec3 center, double age, boolean low) {
        double radius = Math.max(0.16, ShinraWave.radius(age));
        double linger = Mth.clamp((age - ShinraWave.EXPAND) / ShinraWave.AFTERSHOCK, 0, 1);
        int count = low ? 18 : 38;
        int alpha = (int) ((1 - linger * linger) * (age < 3 ? 227 - age * 20 : 166));
        var edge = new Ink(SILVER, alpha, count);
        mesh.ring(new Ring(center, RIGHT, FRONT, radius, 0.045 + radius * 0.012), edge);
        mesh.ring(new Ring(center, RIGHT, UP, radius, 0.033 + radius * (low ? 0.010 : 0.012)), edge);
        mesh.ring(new Ring(center, FRONT, UP, radius, 0.041 + radius * 0.010), new Ink(LILAC, alpha, count));
        if (!low) {
            plume(mesh, new Shell(center, radius, count, alpha, linger));
        }
    }

    private static void plume(Mesh mesh, Shell shell) {
        double radius = shell.radius();
        for (double latitude : new double[]{-0.97, -0.56, -0.28, 0.28, 0.56, 0.97}) {
            Vec3 at = shell.center().add(0, radius * Math.sin(latitude), 0);
            double cross = radius * Math.cos(latitude);
            mesh.ring(new Ring(at, RIGHT, FRONT, cross, 0.020 + cross * 0.008), new Ink(LILAC, shell.alpha() / 3, shell.count()));
        }
        double wake = radius * (0.88 - shell.linger() * 0.07);
        mesh.ring(new Ring(shell.center().add(0, -0.13, 0), RIGHT, FRONT, wake, 0.055),
                new Ink(RIM, shell.alpha() * 3 / 5, shell.count()));
    }

    private static void use(ClientLevel level) {
        if (world.get() != level) {
            ACTIVE.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Event(ShinraWavePacket packet, long start) {
    }

    private record Ring(Vec3 origin, Vec3 x, Vec3 y, double radius, double width) {
        private Vec3 axis(double theta) { return this.x.scale(Math.cos(theta)).add(this.y.scale(Math.sin(theta))); }
    }

    private record Ink(int tint, int opacity, int count) {
    }

    private record Shell(Vec3 center, double radius, int count, int alpha, double linger) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void ring(Ring ring, Ink ink) {
            Vec3 normal = ring.x().cross(ring.y()).scale(ring.width() * 0.43);
            double angle = Math.PI * 2 / ink.count();
            for (int i = 0; i < ink.count(); i++) {
                Vec3 a = ring.axis(i * angle);
                Vec3 b = ring.axis((i + 1) * angle);
                Vec3 p = ring.origin().add(a.scale(ring.radius()));
                Vec3 q = ring.origin().add(b.scale(ring.radius()));
                int alpha = (int) (ink.opacity() * (0.72 + 0.28 * (i % 4) / 3));
                vertex(p, ink.tint(), alpha);
                vertex(q, ink.tint(), alpha);
                vertex(q.subtract(b.scale(ring.width())), ink.tint(), alpha / 6);
                vertex(p.subtract(a.scale(ring.width())), ink.tint(), alpha / 6);
                vertex(p, ink.tint(), alpha);
                vertex(q, ink.tint(), alpha);
                vertex(q.add(normal), ink.tint(), alpha / 5);
                vertex(p.add(normal), ink.tint(), alpha / 5);
            }
        }

        private void vertex(Vec3 at, int tint, int alpha) {
            this.buffer.vertex(this.matrix, (float) at.x, (float) at.y, (float) at.z)
                    .color((tint >>> 16) & 255, (tint >>> 8) & 255, tint & 255, alpha).endVertex();
        }
    }
}
