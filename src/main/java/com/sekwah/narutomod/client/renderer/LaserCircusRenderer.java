package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.LaserCircusPacket;
import com.sekwah.narutomod.network.s2c.LaserCircusPacket.Stage;
import com.sekwah.narutomod.util.JutsuVfx;
import com.sekwah.narutomod.util.LaserCircusPath;
import com.sekwah.narutomod.util.ProjectileAxis;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public final class LaserCircusRenderer {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 FRONT = new Vec3(0, 0, 1);
    private static final int SHADOW = 0x2B447D;
    private static final int REFRACTION = 0x73CFEE;
    private static final int CORE = 0xFFF1CD;
    private static final List<Flash> ACTIVE = new ArrayList<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private LaserCircusRenderer() {
    }

    public static void receive(LaserCircusPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        use(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        ACTIVE.removeIf(flash -> flash.packet().caster() == packet.caster() && flash.packet().stage() == Stage.GATHER);
        if (packet.stage() == Stage.CANCEL) {
            return;
        }
        if (ACTIVE.size() >= 48) {
            ACTIVE.remove(0);
        }
        ACTIVE.add(new Flash(packet, level.getGameTime()));
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        use(level);
        double now = level.getGameTime() + partial;
        ACTIVE.removeIf(flash -> now - flash.born() >= lifespan(flash.packet().stage()));
        if (ACTIVE.isEmpty()) {
            return;
        }
        boolean low = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        var backdrop = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        for (Flash flash : ACTIVE) {
            if (flash.packet().stage() != Stage.GATHER) {
                beam(backdrop, flash, camera, now - flash.born(), low, false);
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
        var light = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW));
        for (Flash flash : ACTIVE) {
            double age = now - flash.born();
            if (flash.packet().stage() == Stage.GATHER) {
                gather(light, level, flash, camera, (float) age, low);
            } else {
                beam(light, flash, camera, age, low, true);
            }
        }
        buffers.endBatch(NarutoRenderTypes.CHAKRA_GLOW);
    }

    private static int lifespan(Stage stage) { return stage == Stage.GATHER ? LaserCircusPath.GATHER + 4 : 8; }

    private static void beam(Mesh mesh, Flash flash, Vec3 camera, double age, boolean low, boolean bright) {
        var points = flash.packet().points();
        if (points.get(0).distanceToSqr(camera) > 46 * 46 && points.get(points.size() - 1).distanceToSqr(camera) > 32 * 32) {
            return;
        }
        double life = age / lifespan(flash.packet().stage());
        int segments = points.size() - 1;
        for (int i = 0; i < segments; i++) {
            Vec3 a = points.get(i).subtract(camera);
            Vec3 b = points.get(i + 1).subtract(camera);
            double t = i / (double) segments;
            double pulse = Math.pow(Math.max(0, 1 - Math.abs(life * 2.4 - t) * 2.1), 1.8);
            double opacity = Math.pow(1 - life, 0.98) * (0.73 + pulse * 0.27);
            Vec3 across = crossView(a, b);
            double radius = (bright ? 0.025 : 0.064) * (0.85 + pulse * 0.57) * (1 - life * 0.55);
            mesh.ribbon(a, b, across.scale(radius), bright ? CORE : SHADOW, (int) ((bright ? 224 : 139) * opacity));
            if (bright && !low) {
                Vec3 blade = ProjectileAxis.direction(b.subtract(a), FRONT).cross(across);
                mesh.ribbon(a, b, blade.scale(radius * 1.28), REFRACTION, (int) (117 * opacity));
            }
        }
        Stage stage = flash.packet().stage();
        if (bright && (stage == Stage.HIT || stage == Stage.BLOCK)) {
            Vec3 tip = points.get(points.size() - 1).subtract(camera);
            glint(mesh, tip, life, !low && stage == Stage.HIT);
        }
    }

    private static Vec3 crossView(Vec3 a, Vec3 b) {
        Vec3 axis = ProjectileAxis.direction(b.subtract(a), FRONT);
        return ProjectileAxis.direction(axis.cross(a.lerp(b, 0.5)), axis.cross(Math.abs(axis.y) > 0.9 ? FRONT : UP));
    }

    private static void gather(Mesh mesh, ClientLevel level, Flash flash, Vec3 camera, float age, boolean low) {
        Vec3 anchor = flash.packet().points().get(0);
        if (level.getEntity(flash.packet().caster()) instanceof Player caster) {
            anchor = JutsuVfx.handPosition(caster, caster.getMainArm() == HumanoidArm.RIGHT ? 1 : -1, Mth.frac(age));
        }
        Vec3 center = anchor.subtract(camera);
        if (center.lengthSqr() > 42 * 42) {
            return;
        }
        double amount = Mth.clamp(age / LaserCircusPath.GATHER, 0.09, 1);
        for (int i = 0; i < (low ? 4 : 8); i++) {
            double t = i * Math.PI * 0.25 + age * 0.41;
            double range = (0.65 - amount * 0.42) * (0.77 + i % 3 * 0.11);
            Vec3 a = center.add(Math.cos(t) * range, 0.08 + Math.sin(t * 2) * range * 0.44, Math.sin(t) * range);
            Vec3 b = center.lerp(a, 0.13 + amount * 0.12);
            mesh.ribbon(a, b, crossView(a, b).scale(0.021), i % 2 == 0 ? CORE : REFRACTION, (int) (87 + amount * 123));
        }
        glint(mesh, center, 0.79 - amount * 0.49, false);
    }

    private static void glint(Mesh mesh, Vec3 point, double life, boolean large) {
        double size = (large ? 0.43 : 0.26) * (0.53 + life * 0.47);
        Vec3 across = ProjectileAxis.direction(UP.cross(point), new Vec3(1, 0, 0));
        int opacity = (int) (222 * (1 - life));
        mesh.ribbon(point.add(UP.scale(size)), point.subtract(UP.scale(size)), across.scale(0.036), CORE, opacity);
        mesh.ribbon(point.add(across.scale(size * 0.85)), point.subtract(across.scale(size * 0.85)), UP.scale(0.030), REFRACTION, opacity);
    }

    private static void use(ClientLevel level) {
        if (world.get() != level) {
            ACTIVE.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Flash(LaserCircusPacket packet, long born) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void ribbon(Vec3 from, Vec3 to, Vec3 width, int tint, int alpha) {
            vertex(from.add(width), tint, alpha);
            vertex(to.add(width.scale(0.85)), tint, alpha);
            vertex(to.subtract(width.scale(0.85)), tint, alpha);
            vertex(from.subtract(width), tint, alpha);
        }

        private void vertex(Vec3 point, int tint, int alpha) {
            this.buffer.vertex(this.matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color((tint >>> 16) & 255, (tint >>> 8) & 255, tint & 255, alpha).endVertex();
        }
    }
}
