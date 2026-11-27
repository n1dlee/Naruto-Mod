package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.NinkenCuePacket;
import com.sekwah.narutomod.network.s2c.NinkenCuePacket.Stage;
import com.sekwah.narutomod.util.ProjectileAxis;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public final class NinkenCueRenderer {
    private static final List<Cue> CUES = new ArrayList<>();
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Tint INK = new Tint(53, 68, 71);
    private static final Tint TEAL = new Tint(106, 201, 183);
    private static final Tint MIST = new Tint(212, 216, 203);
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private NinkenCueRenderer() {
    }

    public static void receive(NinkenCuePacket packet) {
        var level = Minecraft.getInstance().level;
        use(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (CUES.size() >= 36) {
            CUES.remove(0);
        }
        CUES.add(new Cue(packet, level.getGameTime()));
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        use(level);
        double now = level.getGameTime() + partial;
        CUES.removeIf(cue -> now - cue.start() >= length(cue.packet().stage()));
        if (CUES.isEmpty()) {
            return;
        }
        var mesh = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        boolean cheap = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        for (Cue cue : CUES) {
            var packet = cue.packet();
            Vec3 root = packet.feet().subtract(camera);
            if (root.lengthSqr() <= 30 * 30) {
                float age = (float) Mth.clamp((now - cue.start()) / length(packet.stage()), 0, 1);
                boolean low = cheap || root.lengthSqr() > 19 * 19;
                if (packet.stage() == Stage.SCENT) {
                    scent(mesh, root, packet.axis(), age);
                } else if (packet.stage() == Stage.SEAL) {
                    seal(mesh, root.add(0, 0.025, 0), packet.seed(), 0.3 + age * 0.54, age * 0.93f);
                } else {
                    haze(mesh, new Haze(root, packet.seed(), age, packet.stage() == Stage.SEND_HOME), low);
                    seal(mesh, root.add(0, 0.032, 0), packet.seed(), 0.72 + age * 0.31, 1 - age);
                }
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static int length(Stage stage) {
        return switch (stage) {
            case SEAL -> 5;
            case ARRIVE, SEND_HOME -> 11;
            case SCENT -> 12;
        };
    }

    private static void seal(Mesh mesh, Vec3 center, int seed, double radius, float alpha) {
        double spin = (seed & 255) * 0.076;
        for (int i = 0; i < 14; i++) {
            double a = spin + i * Math.PI / 7;
            double b = a + Math.PI / 10;
            mesh.quad(polar(center, a, radius - 0.062), polar(center, b, radius - 0.062),
                    polar(center, b, radius), polar(center, a, radius), i % 2 == 0 ? TEAL : INK, (int) (156 * alpha));
            if (i % 2 == 0) {
                Vec3 h = polar(center, a + 0.01, radius * 0.6);
                Vec3 k = polar(center, a + 0.17, radius * 0.74);
                mesh.quad(h, h.add(0.019, 0, 0.022), k.add(0.017, 0, 0.018), k, INK, (int) (124 * alpha));
            }
        }
    }

    private static void haze(Mesh mesh, Haze haze, boolean low) {
        float age = haze.age();
        int pieces = low ? 6 : 13;
        Vec3 side = ProjectileAxis.direction(new Vec3(haze.root().z, 0, -haze.root().x), new Vec3(1, 0, 0));
        for (int i = 0; i < pieces; i++) {
            double angle = i * 2.399963 + (haze.seed() & 255) * 0.066;
            double radius = (haze.inward() ? 0.98 - 0.66 * age : 0.2 + age * 0.71) * (0.63 + (i % 6) * 0.082);
            Vec3 center = polar(haze.root(), angle, radius).add(0, (0.19 + age * 0.39) * (0.48 + i % 4 * 0.24), 0);
            double width = 0.11 + age * 0.23 + 0.035 * (i % 3);
            int alpha = (int) (106 * Math.sin(Math.PI * age) * (1 - age * 0.15));
            var edge = side.scale(width);
            var up = UP.scale(width * 0.65);
            mesh.quad(center.subtract(edge), center.add(up), center.add(edge), center.subtract(up), MIST, alpha);
            if (!low) {
                Vec3 glint = center.add(0, 0.018, 0);
                mesh.quad(glint.subtract(edge.scale(0.52)), glint.add(up.scale(0.46)), glint.add(edge.scale(0.61)),
                        glint.subtract(up.scale(0.34)), TEAL, alpha / 3);
            }
        }
    }

    private static void scent(Mesh mesh, Vec3 root, Vec3 direction, float age) {
        Vec3 axis = ProjectileAxis.direction(new Vec3(direction.x, 0, direction.z), new Vec3(0, 0, 1));
        Vec3 side = UP.cross(axis);
        for (int i = 0; i < 3; i++) {
            Vec3 dot = root.add(axis.scale(0.49 + i * 0.40 + age * 0.23)).add(0, 0.13 + i * 0.035, 0);
            Vec3 width = side.scale(0.047 + i * 0.013);
            Vec3 length = axis.scale(0.14);
            mesh.quad(dot.subtract(width), dot.add(length), dot.add(width), dot.subtract(length), TEAL,
                    (int) (152 * Math.sin(age * Math.PI)));
        }
    }

    private static Vec3 polar(Vec3 center, double angle, double radius) {
        return center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
    }

    private static void use(ClientLevel level) {
        if (world.get() != level) {
            CUES.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Cue(NinkenCuePacket packet, long start) {
    }

    private record Haze(Vec3 root, int seed, float age, boolean inward) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Tint tint, int alpha) {
            vertex(a, tint, alpha);
            vertex(b, tint, alpha);
            vertex(c, tint, alpha);
            vertex(d, tint, alpha);
        }

        private void vertex(Vec3 point, Tint tint, int alpha) {
            this.buffer.vertex(this.matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color(tint.red(), tint.green(), tint.blue(), alpha).endVertex();
        }
    }

    private record Tint(int red, int green, int blue) {
    }
}
