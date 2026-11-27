package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.anims.PoseBlender;
import com.sekwah.narutomod.network.s2c.KikaichuPacket;
import com.sekwah.narutomod.util.JutsuVfx;
import com.sekwah.narutomod.util.KikaichuFlight;
import com.sekwah.narutomod.util.KikaichuFlight.Stage;
import com.sekwah.narutomod.util.ProjectileAxis;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

public final class KikaichuRenderer {
    private static final Map<Integer, Swarm> SWARMS = new LinkedHashMap<>();
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 X = new Vec3(1, 0, 0);
    private static final Vec3 FRONT = new Vec3(0, 0, 1);
    private static final Tint BARK = new Tint(32, 32, 38);
    private static final Tint BACK = new Tint(68, 57, 71);
    private static final Tint WING = new Tint(133, 112, 125);
    private static final Tint CARGO = new Tint(151, 221, 179);
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private KikaichuRenderer() {
    }

    public static void receive(KikaichuPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        use(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        long now = level.getGameTime();
        Swarm old = SWARMS.get(packet.cast());
        if (old == null && SWARMS.size() >= 20) {
            SWARMS.remove(SWARMS.keySet().iterator().next());
        }
        Vec3 prior = old == null ? packet.from() : old.position(now);
        Vec3 axis = ProjectileAxis.direction(packet.center().subtract(packet.from()), old == null ? FRONT : old.axis());
        long start = old == null || old.packet().stage() != packet.stage() ? now : old.start();
        SWARMS.put(packet.cast(), new Swarm(packet, now, start, prior, axis));
    }

    public static void sleevePose(HumanoidModel<?> model, LivingEntity actor, float partial) {
        ClientLevel level = world.get();
        if (actor.level() != level) {
            return;
        }
        for (Swarm swarm : SWARMS.values()) {
            if (swarm.packet().owner() == actor.getId() && swarm.packet().stage() == Stage.GATHER) {
                float age = level.getGameTime() - swarm.start() + partial;
                float weight = Mth.clamp(age / 4, 0, 1) * 0.82f;
                PoseBlender.rotate(model.rightArm, weight, -0.79f, -0.22f, -0.39f);
                PoseBlender.rotate(model.leftArm, weight, -0.79f, 0.22f, 0.39f);
                return;
            }
        }
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        use(level);
        double now = level.getGameTime() + partial;
        SWARMS.values().removeIf(swarm -> now - swarm.stamp() > expiry(swarm.packet().stage()));
        if (SWARMS.isEmpty()) {
            return;
        }
        var mesh = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        boolean fast = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        for (Swarm swarm : SWARMS.values()) {
            var packet = swarm.packet();
            Vec3 root = swarm.position(now).subtract(camera);
            if (root.lengthSqr() > 33 * 33) {
                continue;
            }
            Vec3 left = root;
            Vec3 right = root;
            if (packet.stage() == Stage.GATHER && level.getEntity(packet.owner()) instanceof Player owner) {
                right = JutsuVfx.handPosition(owner, 1, partial).subtract(camera).add(0, -0.16, 0);
                left = JutsuVfx.handPosition(owner, -1, partial).subtract(camera).add(0, -0.16, 0);
            }
            var volume = new Volume(root, new Cuffs(left, right), swarm.axis(), now - swarm.start(), packet.stage(), packet.cargo());
            cloud(mesh, volume, packet.cast(), fast || root.lengthSqr() > 20 * 20);
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static int expiry(Stage stage) {
        return switch (stage) {
            case GATHER -> KikaichuFlight.GATHER + 3;
            case FEED -> KikaichuFlight.FEED + 3;
            case DISSOLVE, HOME -> 7;
            default -> 9;
        };
    }

    private static void cloud(Mesh mesh, Volume volume, int seed, boolean low) {
        int size = low ? 13 : 29;
        var basis = Basis.of(volume.axis());
        for (int i = 0; i < size; i++) {
            double phase = i * 2.399963 + seed * 0.119 + volume.age() * (0.58 + i % 4 * 0.12);
            double spread = volume.spread();
            double ring = (0.23 + i % 8 * 0.092) * spread;
            Vec3 base = volume.origin(i);
            Vec3 spot = base.add(basis.side().scale(Math.cos(phase) * ring))
                    .add(basis.up().scale(Math.sin(phase) * ring * 0.69))
                    .add(basis.axis().scale(Math.sin(phase * 0.36 + i) * (volume.stage() == Stage.FEED ? 0.25 : 0.57) * spread));
            Vec3 heading = ProjectileAxis.direction(basis.axis().add(basis.side().scale(Math.sin(phase) * 0.53)), volume.axis());
            int alpha = (int) (volume.visibility() * (195 + i % 4 * 13));
            boolean glows = volume.cargo() && i % 7 == 2;
            beetle(mesh, new Beetle(spot, Basis.of(heading), 0.035 + i % 4 * 0.009, Math.sin(phase * 5.2), alpha, glows), low);
        }
    }

    private static void beetle(Mesh mesh, Beetle bug, boolean low) {
        Vec3 at = bug.center();
        Vec3 nose = bug.frame().axis().scale(bug.radius() * 1.65);
        Vec3 flank = bug.frame().side().scale(bug.radius() * 0.83);
        Vec3 wing = bug.frame().up().scale(bug.radius() * (0.32 + bug.flap() * 0.37));
        Vec3 back = at.subtract(nose.scale(0.78));
        Vec3 front = at.add(nose);
        mesh.quad(back, at.add(flank), front, at.subtract(flank), BARK, bug.alpha());
        mesh.quad(back, at.add(wing.scale(0.50)), front, at.subtract(flank.scale(0.72)), bug.glows() ? CARGO : BACK, bug.alpha());
        if (!low) {
            Vec3 mid = at.add(nose.scale(0.33));
            mesh.quad(at, mid, at.add(flank.scale(1.7)).add(wing), back, WING, bug.alpha() / 2);
            mesh.quad(at, back, at.subtract(flank.scale(1.7)).add(wing), mid, WING, bug.alpha() / 2);
        }
    }

    private static void use(ClientLevel level) {
        if (world.get() != level) {
            SWARMS.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Swarm(KikaichuPacket packet, long stamp, long start, Vec3 prior, Vec3 axis) {
        private Vec3 position(double now) {
            return this.prior.lerp(this.packet.center(), Mth.clamp((now - this.stamp) / 2, 0, 1));
        }
    }

    private record Cuffs(Vec3 left, Vec3 right) {
    }

    private record Volume(Vec3 center, Cuffs cuffs, Vec3 axis, double age, Stage stage, boolean cargo) {
        private double visibility() {
            return switch (this.stage) {
                case GATHER -> Mth.clamp(this.age / KikaichuFlight.GATHER, 0, 1);
                case DISSOLVE, HOME -> Mth.clamp(1 - this.age / 7.0, 0, 1);
                default -> 1;
            };
        }

        private double spread() {
            return switch (this.stage) {
                case GATHER -> 0.33 + this.visibility() * 0.34;
                case FEED -> 0.54;
                case DISSOLVE -> 0.69 + this.age * 0.075;
                case HOME -> 0.7 * this.visibility();
                default -> 0.71;
            };
        }

        private Vec3 origin(int i) {
            if (this.stage != Stage.GATHER) {
                return this.center;
            }
            return (i % 2 == 0 ? this.cuffs.left() : this.cuffs.right()).lerp(this.center, this.visibility() * this.visibility());
        }
    }

    private record Basis(Vec3 axis, Vec3 side, Vec3 up) {
        private static Basis of(Vec3 axis) {
            Vec3 side = axis.cross(Math.abs(axis.y) < 0.95 ? UP : X).normalize();
            return new Basis(axis, side, side.cross(axis));
        }
    }

    private record Beetle(Vec3 center, Basis frame, double radius, double flap, int alpha, boolean glows) {
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
