package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.WoodArmPacket;
import com.sekwah.narutomod.network.s2c.WoodArmPacket.Stage;
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

public final class WoodArmRenderer {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 FORWARD = new Vec3(0, 0, 1);
    private static final int[] BARK = {0x4E2D22, 0x825134, 0xAA7146, 0xCE9962, 0x9A5C36, 0x66412C};
    private static final Map<Integer, Update> ARMS = new LinkedHashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private WoodArmRenderer() {
    }

    public static void receive(WoodArmPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        use(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (ARMS.size() >= 20 && !ARMS.containsKey(packet.cast())) {
            ARMS.remove(ARMS.keySet().iterator().next());
        }
        var previous = ARMS.get(packet.cast());
        boolean closing = packet.stage() == Stage.BREAK || packet.stage() == Stage.RETRACT;
        Vec3 from = closing && previous != null ? previous.from() : packet.from();
        Vec3 oldBase = previous == null ? packet.base() : previous.packet().base();
        ARMS.put(packet.cast(), new Update(packet, level.getGameTime(), from, oldBase));
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        use(level);
        double now = level.getGameTime() + partial;
        ARMS.values().removeIf(update -> now - update.start() >= duration(update.packet().stage()));
        if (ARMS.isEmpty()) {
            return;
        }
        boolean fast = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        var mesh = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.BOULDER_BODY));
        for (Update update : ARMS.values()) {
            double age = Math.max(0, now - update.start());
            Vec3 base = update.oldBase().lerp(update.packet().base(), Math.min(1, age)).subtract(camera);
            if (base.lengthSqr() > 37 * 37 && update.packet().tip().distanceToSqr(camera) > 28 * 28) {
                continue;
            }
            render(mesh, update, base, camera, age, fast || base.lengthSqr() > 23 * 23);
        }
        buffers.endBatch(NarutoRenderTypes.BOULDER_BODY);
    }

    private static int duration(Stage stage) {
        return switch (stage) {
            case BRACE -> 6;
            case EXTEND, GRIP -> 5;
            case BREAK -> 8;
            case RETRACT -> 6;
        };
    }

    private static void render(Mesh mesh, Update update, Vec3 base, Vec3 camera, double age, boolean low) {
        var packet = update.packet();
        Stage stage = packet.stage();
        Vec3 tip = update.from().lerp(packet.tip(), Math.min(1, age)).subtract(camera);
        double dissolve = stage == Stage.BREAK || stage == Stage.RETRACT ? age / duration(stage) : 0;
        if (stage == Stage.BRACE) {
            tip = base.lerp(tip, Mth.clamp(0.23 + age * 0.15, 0, 1));
        } else if (dissolve > 0) {
            tip = tip.lerp(base, 1 - Math.pow(1 - dissolve, 1.8));
        }
        Basis basis = Basis.between(base, tip);
        double length = base.distanceTo(tip);
        double scale = Mth.clamp(length * 2, 0.04, 1) * (1 - dissolve * 0.70);
        if (length < 0.025) {
            return;
        }
        var rig = new Rig(base, tip, basis, scale, stage, low);
        forearm(mesh, rig);
        fingers(mesh, rig);
        if (stage == Stage.BREAK) {
            fracture(mesh, update.packet().tip().subtract(camera), basis, dissolve, low);
        }
    }

    private static void forearm(Mesh mesh, Rig rig) {
        int slices = rig.low() ? 4 : 7;
        Vec3 prior = rig.base();
        for (int i = 1; i <= slices; i++) {
            double t = i / (double) slices * 0.94;
            Vec3 at = rig.base().lerp(rig.tip(), t).add(rig.basis().up().scale(Math.sin(t * Math.PI) * 0.18))
                    .add(rig.basis().side().scale(Math.sin(t * 2 * Math.PI) * 0.065));
            double thick = ((0.30 - t * 0.17) + (i % 2 == 0 ? 0.036 : 0)) * rig.scale();
            mesh.timber(new Timber(prior, at, thick, thick * 0.91), rig.low());
            if (!rig.low()) {
                Vec3 stem = prior.lerp(at, 0.48).add(rig.basis().side().scale(thick * 0.60));
                Vec3 bud = stem.add(rig.basis().side().scale(0.084 * rig.scale())).add(rig.basis().up().scale(thick));
                mesh.timber(new Timber(stem, bud, 0.034 * rig.scale(), 0.006 * rig.scale()), true);
            }
            prior = at;
        }
        Vec3 palm = rig.tip().subtract(rig.basis().forward().scale(0.13 * rig.scale()));
        mesh.timber(new Timber(prior, palm, 0.17 * rig.scale(), 0.233 * rig.scale()), rig.low());
    }

    private static void fingers(Mesh mesh, Rig rig) {
        Basis b = rig.basis();
        double open = rig.stage() == Stage.GRIP ? 0.36 : 1.0;
        for (int sign : new int[]{-1, 1}) {
            Vec3 lateral = b.side().scale(sign * rig.scale());
            Vec3 root = rig.tip().add(lateral.scale(0.17)).subtract(b.forward().scale(0.17 * rig.scale()));
            Vec3 knuckle = root.add(lateral.scale(0.14 * open)).add(b.forward().scale(0.17 * rig.scale()));
            Vec3 nail = knuckle.subtract(lateral.scale(open == 1.0 ? 0.08 : 0.23))
                    .add(b.forward().scale((0.10 + 0.09 * open) * rig.scale())).add(b.up().scale(-0.05 * rig.scale()));
            mesh.timber(new Timber(root, knuckle, 0.092 * rig.scale(), 0.080 * rig.scale()), rig.low());
            mesh.timber(new Timber(knuckle, nail, 0.080 * rig.scale(), 0.024 * rig.scale()), rig.low());
        }
    }

    private static void fracture(Mesh mesh, Vec3 tip, Basis basis, double age, boolean low) {
        for (int i = 0; i < (low ? 3 : 7); i++) {
            double spin = i * Math.PI * 2 / 7;
            Vec3 radius = basis.side().scale(Math.cos(spin)).add(basis.up().scale(Math.sin(spin)));
            Vec3 splinter = tip.add(radius.scale(age * (0.32 + (i % 3) * 0.19))).add(0, -age * age * 0.36, 0);
            double size = (1 - age) * (0.10 + (i % 3) * 0.08);
            mesh.timber(new Timber(splinter, splinter.add(radius.scale(size)).subtract(basis.forward().scale(size)),
                    size * 0.23, size * 0.075), true);
        }
    }

    private static void use(ClientLevel level) {
        if (world.get() != level) {
            ARMS.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Update(WoodArmPacket packet, long start, Vec3 from, Vec3 oldBase) {
    }

    private record Rig(Vec3 base, Vec3 tip, Basis basis, double scale, Stage stage, boolean low) {
    }

    private record Timber(Vec3 from, Vec3 to, double root, double tip) {
    }

    private record Basis(Vec3 forward, Vec3 side, Vec3 up) {
        private static Basis between(Vec3 from, Vec3 to) {
            Vec3 axis = ProjectileAxis.direction(to.subtract(from), FORWARD);
            Vec3 side = (Math.abs(axis.y) > 0.9 ? FORWARD : UP).cross(axis).normalize();
            return new Basis(axis, side, axis.cross(side).normalize());
        }

        private Vec3 radius(double angle) {
            return this.side.scale(Math.cos(angle)).add(this.up.scale(Math.sin(angle)));
        }
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void timber(Timber wood, boolean low) {
            Basis b = Basis.between(wood.from(), wood.to());
            int sides = low ? 4 : 6;
            for (int i = 0; i < sides; i++) {
                double theta = Math.PI * 2 / sides;
                Vec3 a = b.radius(i * theta);
                Vec3 c = b.radius((i + 1) * theta);
                int shade = BARK[i];
                quad(wood.from().add(a.scale(wood.root())), wood.to().add(a.scale(wood.tip())),
                        wood.to().add(c.scale(wood.tip())), wood.from().add(c.scale(wood.root())), shade);
                quad(wood.to().add(a.scale(wood.tip())), wood.to().add(c.scale(wood.tip())), wood.to(), wood.to(), shade);
                if (!low && (i == 1 || i == 3)) {
                    grain(wood, a, c, i == 3 ? 0xAEA171 : 0x593529);
                }
            }
        }

        private void grain(Timber wood, Vec3 a, Vec3 b, int shade) {
            Vec3 one = a.lerp(b, 0.23).scale(1.016);
            Vec3 two = a.lerp(b, 0.31).scale(1.016);
            quad(wood.from().add(one.scale(wood.root())), wood.to().add(one.scale(wood.tip())),
                    wood.to().add(two.scale(wood.tip())), wood.from().add(two.scale(wood.root())), shade);
        }

        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int shade) {
            vertex(a, shade);
            vertex(b, shade);
            vertex(c, shade);
            vertex(d, shade);
        }

        private void vertex(Vec3 point, int shade) {
            this.buffer.vertex(this.matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color((shade >>> 16) & 255, (shade >>> 8) & 255, shade & 255, 255).endVertex();
        }
    }
}
