package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.NarakaScenePacket;
import com.sekwah.narutomod.util.KamuiStep;
import com.sekwah.narutomod.util.NarakaRestoration;
import com.sekwah.narutomod.util.NarakaRestoration.Stage;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NarakaSceneRenderer {
    private static final Map<Integer, Scene> ACTIVE = new LinkedHashMap<>();
    private static final int GOLD = 0xE9AB60;
    private static final int IVORY = 0xFFF0BC;
    private static final int STONE = 0x482A3E;
    private static final int VIOLET = 0x8A4F77;
    private static final int RED = 0xED8472;
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private NarakaSceneRenderer() {
    }

    public static void receive(NarakaScenePacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        use(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (packet.stage() == Stage.ABORT) {
            ACTIVE.remove(packet.owner());
            return;
        }
        if (ACTIVE.size() >= 16 && !ACTIVE.containsKey(packet.owner())) {
            ACTIVE.remove(ACTIVE.keySet().iterator().next());
        }
        ACTIVE.put(packet.owner(), new Scene(packet, level.getGameTime()));
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        use(level);
        double now = level.getGameTime() + partial;
        ACTIVE.values().removeIf(frame -> now - frame.born() >= lifetime(frame.packet().stage()));
        if (ACTIVE.isEmpty()) {
            return;
        }
        boolean fast = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        var mesh = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        for (Scene frame : ACTIVE.values()) {
            var packet = frame.packet();
            Vec3 pos = packet.center();
            if (level.getEntity(packet.subject()) instanceof Player patient) {
                pos = patient.getPosition(partial);
            }
            Vec3 at = pos.subtract(camera);
            if (at.lengthSqr() > 43 * 43) {
                continue;
            }
            double age = Math.max(0, now - frame.born());
            boolean low = fast || at.lengthSqr() > 24 * 24;
            if (packet.stage() == Stage.OPEN || packet.stage() == Stage.REBUILT) {
                judge(mesh, at, packet.yaw(), age, packet.stage() == Stage.REBUILT, low);
            } else {
                mend(mesh, at, age, packet.stage() == Stage.HEALED, low);
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static int lifetime(Stage stage) {
        return switch (stage) {
            case HEALING -> NarakaRestoration.HEAL_TICKS + 3;
            case OPEN -> NarakaRestoration.REBUILD_TICKS + 3;
            case HEALED, REBUILT -> 9;
            case ABORT -> 1;
        };
    }

    private static void mend(Mesh mesh, Vec3 at, double age, boolean release, boolean low) {
        double mix = Mth.clamp(age / (release ? 9 : NarakaRestoration.HEAL_TICKS), 0, 1);
        double radius = release ? 0.49 + mix * 0.55 : 0.86 - 0.41 * mix;
        double height = release ? 1.56 + mix * 0.44 : 0.66 + 1.0 * mix;
        int opacity = (int) (release ? 220 * (1 - mix) : 112 + mix * 91);
        int count = low ? 12 : 24;
        for (int i = 0; i < count; i++) {
            double a = i * Math.PI * 2 / count + age * 0.21;
            double b = (i + 1) * Math.PI * 2 / count + age * 0.21;
            double h = height * i / count;
            Vec3 p = at.add(Math.cos(a) * radius, h + 0.055, Math.sin(a) * radius);
            Vec3 q = at.add(Math.cos(b) * radius, h + 0.055 + height / count, Math.sin(b) * radius);
            mesh.strip(p, q, new Vec3(0, 0.047, 0), i % 3 == 0 ? IVORY : GOLD, opacity);
            if (!low && i % 2 == 0) {
                Vec3 glint = at.add(Math.cos(a) * radius * 0.71, h + 0.15, Math.sin(a) * radius * 0.71);
                mesh.strip(glint, glint.add(0, 0.14, 0), new Vec3(0.025, 0, 0.027), IVORY, opacity * 2 / 3);
            }
        }
    }

    private static void judge(Mesh mesh, Vec3 base, float yaw, double age, boolean done, boolean low) {
        double t = Mth.clamp(age / (done ? 9 : NarakaRestoration.REBUILD_TICKS), 0, 1);
        double rise = done ? 1 - t : Math.min(1, t * 3.2);
        Vec3 front = KamuiStep.facing(yaw);
        Vec3 side = new Vec3(-front.z, 0, front.x);
        var mask = new Mask(base.subtract(front.scale(0.53)), front, side, rise);
        int opacity = (int) (232 * rise);
        crown(mesh, mask, opacity, low);
        double opening = done ? (1 - t) * 0.20 : NarakaRestoration.jaw(t);
        maw(mesh, mask, opening, opacity, low);
    }

    private static void crown(Mesh mesh, Mask m, int opacity, boolean low) {
        for (int sign : new int[]{-1, 1}) {
            Vec3 top = m.point(0.10 * sign, 2.79, -0.09);
            Vec3 crest = m.point(0.72 * sign, 2.62, -0.27);
            Vec3 brow = m.point(0.68 * sign, 1.94, 0.38);
            Vec3 snout = m.point(0.13 * sign, 1.67, 0.75);
            Vec3 cheek = m.point(1.06 * sign, 1.17, 0.22);
            Vec3 flank = m.point(1.26 * sign, 1.57, -0.41);
            mesh.quad(top, crest, brow, snout, VIOLET, opacity);
            mesh.quad(crest, flank, cheek, brow, STONE, opacity);
            mesh.quad(brow, cheek, m.point(0.39 * sign, 1.26, 0.63), snout, VIOLET, opacity);
            mesh.quad(m.point(0.85 * sign, 2.03, -0.18), m.point(1.25 * sign, 2.53, -0.40),
                    m.point(1.24 * sign, 1.53, -0.40), flank, STONE, opacity);
            Vec3 inner = m.point(0.20 * sign, 1.97, 0.67);
            Vec3 outer = m.point(0.70 * sign, 1.84, 0.46);
            mesh.strip(inner, outer, new Vec3(0, 0.045 * m.height(), 0), RED, opacity);
            if (!low) {
                mesh.strip(crest, brow, m.side().scale(0.029), GOLD, opacity / 2);
            }
        }
        mesh.quad(m.point(-0.17, 1.59, 0.71), m.point(-0.14, 2.59, 0.36),
                m.point(0.14, 2.59, 0.36), m.point(0.17, 1.59, 0.71), STONE, opacity);
    }

    private static void maw(Mesh mesh, Mask m, double jaw, int opacity, boolean low) {
        double bottom = 1.05 - jaw * 0.77;
        Vec3 a = m.point(-0.73, 1.40, 0.54);
        Vec3 b = m.point(0.73, 1.40, 0.54);
        Vec3 c = m.point(0.64, bottom, 0.82);
        Vec3 d = m.point(-0.64, bottom, 0.82);
        mesh.quad(a, b, c, d, 0x211421, opacity);
        mesh.quad(d, c, m.point(0.51, bottom - 0.24, 0.47), m.point(-0.51, bottom - 0.24, 0.47), VIOLET, opacity);
        int teeth = low ? 4 : 6;
        for (int i = 0; i < teeth; i++) {
            double x = -0.6 + (i + 0.5) / teeth * 1.2;
            Vec3 edge = m.point(x, 1.40, 0.56);
            mesh.quad(edge.add(m.side().scale(0.063)), edge.subtract(m.side().scale(0.063)),
                    m.point(x, 1.23, 0.59), m.point(x, 1.23, 0.59), IVORY, opacity);
            Vec3 base = m.point(x, bottom, 0.84);
            mesh.strip(base, m.point(x, bottom + 0.10, 0.83), m.side().scale(0.047), IVORY, opacity);
        }
    }

    private static void use(ClientLevel level) {
        if (world.get() != level) {
            ACTIVE.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Scene(NarakaScenePacket packet, long born) {
    }

    private record Mask(Vec3 origin, Vec3 front, Vec3 side, double height) {
        private Vec3 point(double x, double y, double z) {
            return this.origin.add(this.side.scale(x)).add(this.front.scale(z)).add(0, y * this.height, 0);
        }
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void strip(Vec3 a, Vec3 b, Vec3 w, int color, int alpha) {
            quad(a.add(w), b.add(w.scale(0.78)), b.subtract(w.scale(0.78)), a.subtract(w), color, alpha);
        }

        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, int alpha) {
            vertex(a, color, alpha);
            vertex(b, color, alpha);
            vertex(c, color, alpha);
            vertex(d, color, alpha);
        }

        private void vertex(Vec3 v, int color, int alpha) {
            this.buffer.vertex(this.matrix, (float) v.x, (float) v.y, (float) v.z)
                    .color((color >>> 16) & 255, (color >>> 8) & 255, color & 255, alpha).endVertex();
        }
    }
}
