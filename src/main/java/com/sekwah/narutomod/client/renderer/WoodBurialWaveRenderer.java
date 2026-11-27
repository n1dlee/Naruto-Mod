package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.WoodBurialWavePacket;
import com.sekwah.narutomod.network.s2c.WoodBurialWavePacket.Mode;
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

public final class WoodBurialWaveRenderer {
    private static final List<Sample> SAMPLES = new ArrayList<>();
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Tint DARK = new Tint(97, 58, 34);
    private static final Tint BARK = new Tint(160, 102, 57);
    private static final Tint SAP = new Tint(181, 221, 133);
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private WoodBurialWaveRenderer() {
    }

    public static void receive(WoodBurialWavePacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        use(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (SAMPLES.size() >= 52) {
            SAMPLES.remove(0);
        }
        SAMPLES.add(new Sample(packet, level.getGameTime()));
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        use(level);
        double now = level.getGameTime() + partial;
        SAMPLES.removeIf(sample -> now - sample.start() >= ageLimit(sample.packet()));
        if (SAMPLES.isEmpty()) {
            return;
        }
        boolean fast = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        var mesh = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        for (Sample sample : SAMPLES) {
            var packet = sample.packet();
            Vec3 first = packet.route().get(0).subtract(camera);
            if (first.lengthSqr() > 32 * 32) {
                continue;
            }
            double t = Mth.clamp((now - sample.start()) / ageLimit(packet), 0, 1);
            if (packet.mode() == Mode.TRACE) {
                trace(mesh, packet, camera, t);
            } else {
                Vec3 apex = packet.route().get(packet.route().size() - 1).subtract(camera);
                var local = new Bloom(apex, UP.cross(ProjectileAxis.direction(apex.subtract(first).multiply(1, 0, 1),
                        new Vec3(0, 0, 1))), packet.heavy(), t, packet.seed());
                erupt(mesh, local, fast || first.lengthSqr() > 20 * 20);
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static int ageLimit(WoodBurialWavePacket packet) {
        return packet.mode() == Mode.TRACE ? packet.heavy() ? 5 : 3 : packet.heavy() ? 11 : 8;
    }

    private static void trace(Mesh mesh, WoodBurialWavePacket packet, Vec3 camera, double time) {
        List<Vec3> path = packet.route();
        int count = path.size() - 1;
        for (int i = 0; i < count && i < count * time; i++) {
            Vec3 a = path.get(i).subtract(camera).add(0, 0.027, 0);
            Vec3 b = path.get(i + 1).subtract(camera).add(0, 0.027, 0);
            Vec3 shown = a.lerp(b, Math.min(1, count * time - i));
            Vec3 side = UP.cross(ProjectileAxis.direction(b.subtract(a), new Vec3(0, 0, 1))).normalize();
            mesh.ribbon(a, shown, side.scale(0.04), DARK, 169);
            mesh.ribbon(a.add(0, 0.015, 0), shown.add(0, 0.015, 0), side.scale(0.009), SAP, 213);
            if (((i + packet.seed()) & 1) == 0) {
                Vec3 fork = shown.add(side.scale(packet.heavy() ? 0.41 : 0.23)).add(0, 0.019, 0);
                mesh.ribbon(a.lerp(shown, 0.5), fork, side.scale(0.014), BARK, 151);
            }
        }
    }

    private static void erupt(Mesh mesh, Bloom bloom, boolean low) {
        int count = low ? 4 : 8;
        double spread = bloom.heavy() ? 1.65 : 0.93;
        double height = (bloom.heavy() ? 2.05 : 1.19) * Math.sin(Math.PI * Math.min(0.5, bloom.time() * 0.87));
        double fade = Math.pow(1 - bloom.time(), 1.17);
        for (int i = 0; i < count; i++) {
            double sign = i % 2 == 0 ? -1 : 1;
            double seed = ((bloom.seed() >>> (i & 15)) & 7) / 7d;
            Vec3 root = bloom.center().add(bloom.side().scale(sign * spread * (0.49 + (i % 4) * 0.12))).add(0, 0.055, 0);
            Vec3 elbow = root.add(0, 0.34 * height, 0).add(bloom.side().scale(sign * 0.13));
            Vec3 tip = root.add(0, height * (0.65 + seed * 0.34), 0).subtract(bloom.side().scale(sign * (0.27 + 0.42 * seed)));
            mesh.ribbon(root, elbow, bloom.side().scale(0.093), DARK, (int) (163 * fade));
            mesh.ribbon(elbow, tip, bloom.side().scale(0.044), BARK, (int) (171 * fade));
            mesh.ribbon(elbow.add(0, 0.028, 0), tip, bloom.side().scale(0.011), SAP, (int) (138 * fade));
        }
    }

    private static void use(ClientLevel level) {
        if (world.get() != level) {
            SAMPLES.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Sample(WoodBurialWavePacket packet, long start) {
    }

    private record Bloom(Vec3 center, Vec3 side, boolean heavy, double time, int seed) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void ribbon(Vec3 from, Vec3 to, Vec3 width, Tint tint, int alpha) {
            vertex(from.add(width), tint, alpha);
            vertex(to.add(width), tint, alpha);
            vertex(to.subtract(width), tint, alpha);
            vertex(from.subtract(width), tint, alpha);
        }

        private void vertex(Vec3 point, Tint tint, int alpha) {
            this.buffer.vertex(this.matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color(tint.red(), tint.green(), tint.blue(), alpha).endVertex();
        }
    }

    private record Tint(int red, int green, int blue) {
    }
}
