package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.SummonSealPacket;
import com.sekwah.narutomod.util.SummoningCircle.Stage;
import com.sekwah.narutomod.util.SummoningCircle.Style;
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

public final class SummonSealRenderer {
    private static final Map<Key, Seal> ACTIVE = new LinkedHashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private SummonSealRenderer() {
    }

    public static void receive(SummonSealPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        use(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        var key = new Key(packet.owner(), packet.style());
        if (ACTIVE.size() >= 22 && !ACTIVE.containsKey(key)) {
            ACTIVE.remove(ACTIVE.keySet().iterator().next());
        }
        ACTIVE.put(key, new Seal(packet, level.getGameTime()));
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        use(level);
        double now = level.getGameTime() + partial;
        ACTIVE.values().removeIf(mark -> now - mark.born() >= duration(mark.packet().stage()));
        if (ACTIVE.isEmpty()) {
            return;
        }
        boolean fast = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        var mesh = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        for (Seal seal : ACTIVE.values()) {
            var packet = seal.packet();
            Vec3 at = packet.center().subtract(camera).add(0, 0.041, 0);
            if (at.lengthSqr() < 52 * 52) {
                double age = now - seal.born();
                var design = new Design(at, Math.max(1.15, packet.span() * 0.65), color(packet), age,
                        packet.stage(), fast || at.lengthSqr() > 27 * 27);
                drawSeal(mesh, design);
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static int duration(Stage stage) {
        return switch (stage) {
            case PREPARE -> 26;
            case EMERGE -> 16;
            case FADE, CANCEL -> 9;
        };
    }

    private static int color(SummonSealPacket packet) {
        if (packet.stage() == Stage.CANCEL) {
            return 0x9F524B;
        }
        return switch (packet.style()) {
            case RHINO -> 0xBC94DD;
            case TOAD -> 0xDB8453;
            case SNAKE -> 0xAB8BCE;
            case SLUG -> 0x8FD1C0;
            case MONKEY -> 0xE3B96C;
        };
    }

    private static void drawSeal(Mesh mesh, Design mark) {
        double reveal = mark.stage() == Stage.PREPARE ? Mth.clamp((mark.age() + 0.5) / 9, 0, 1) : 1;
        double tail = mark.stage() == Stage.PREPARE ? 1 : 1 - Mth.clamp(mark.age() / duration(mark.stage()), 0, 1);
        double spread = mark.stage() == Stage.EMERGE ? 1 + (1 - tail) * 0.49 : 0.95 + reveal * 0.05;
        double radius = mark.radius() * spread;
        int pieces = mark.low() ? 20 : 38;
        for (int i = 0; i < pieces && i < pieces * reveal; i++) {
            double a = i * Math.PI * 2 / pieces;
            double b = (i + 1) * Math.PI * 2 / pieces;
            int ink = (int) ((i % 3 == 0 ? 199 : 166) * tail);
            mesh.band(mark.center(), a, b, radius * 0.96, 0.059, new Ink(0x31283C, ink));
            mesh.band(mark.center().add(0, 0.018, 0), a, b, radius * 0.86, 0.027, new Ink(mark.tint(), ink));
            if (i % (mark.low() ? 4 : 3) == 0) {
                glyph(mesh, mark, a, radius, ink);
            }
        }
    }

    private static void glyph(Mesh mesh, Design mark, double angle, double radius, int alpha) {
        Vec3 dir = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        Vec3 across = new Vec3(-dir.z, 0, dir.x);
        Vec3 a = mark.center().add(dir.scale(radius * 0.69)).add(0, 0.016, 0);
        Vec3 b = a.add(dir.scale(radius * 0.15));
        Vec3 c = b.add(across.scale(radius * 0.078));
        mesh.line(a, b, across.scale(0.026), mark.tint(), alpha);
        mesh.line(b, c, dir.scale(0.023), mark.tint(), alpha);
        if (!mark.low() && mark.stage() == Stage.EMERGE) {
            double f = mark.age() / duration(mark.stage());
            Vec3 foot = a.add(dir.scale(radius * f * 0.23));
            Vec3 tip = foot.add(dir.scale(radius * 0.11)).add(0, (1 - f) * 0.70, 0);
            mesh.line(foot, tip, across.scale(0.048), mark.tint(), alpha / 2);
        }
    }

    private static void use(ClientLevel level) {
        if (world.get() != level) {
            ACTIVE.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Key(int owner, Style style) {
    }

    private record Seal(SummonSealPacket packet, long born) {
    }

    private record Design(Vec3 center, double radius, int tint, double age, Stage stage, boolean low) {
    }

    private record Ink(int tint, int alpha) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void band(Vec3 center, double a, double b, double r, double width, Ink ink) {
            Vec3 first = new Vec3(Math.cos(a), 0, Math.sin(a));
            Vec3 last = new Vec3(Math.cos(b), 0, Math.sin(b));
            quad(center.add(first.scale(r)), center.add(last.scale(r)),
                    center.add(last.scale(r - width)), center.add(first.scale(r - width)), ink.tint(), ink.alpha());
        }

        private void line(Vec3 a, Vec3 b, Vec3 width, int tint, int alpha) {
            quad(a.add(width), b.add(width), b.subtract(width), a.subtract(width), tint, alpha);
        }

        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int tint, int alpha) {
            vertex(a, tint, alpha);
            vertex(b, tint, alpha);
            vertex(c, tint, alpha);
            vertex(d, tint, alpha);
        }

        private void vertex(Vec3 v, int tint, int alpha) {
            this.buffer.vertex(this.matrix, (float) v.x, (float) v.y, (float) v.z)
                    .color((tint >>> 16) & 255, (tint >>> 8) & 255, tint & 255, alpha).endVertex();
        }
    }
}
