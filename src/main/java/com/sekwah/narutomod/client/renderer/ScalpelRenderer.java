package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.ScalpelPacket;
import com.sekwah.narutomod.util.JutsuVfx;
import com.sekwah.narutomod.util.ScalpelEdge;
import com.sekwah.narutomod.util.ScalpelEdge.Phase;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
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

public final class ScalpelRenderer {
    private static final Map<Integer, Blade> BLADES = new LinkedHashMap<>();
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Tint EDGE = new Tint(227, 255, 245);
    private static final Tint TEAL = new Tint(99, 242, 195);
    private static final Tint DARK = new Tint(36, 155, 134);
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private ScalpelRenderer() {
    }

    public static void receive(ScalpelPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        checkWorld(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (!BLADES.containsKey(packet.actor()) && BLADES.size() >= 24) {
            BLADES.remove(BLADES.keySet().iterator().next());
        }
        BLADES.put(packet.actor(), new Blade(packet, level.getGameTime()));
    }

    public static float weight(LivingEntity actor, float partial) {
        var level = world.get();
        Blade blade = level == actor.level() ? BLADES.get(actor.getId()) : null;
        return blade == null ? 0 : weight(blade, level.getGameTime() + partial);
    }

    private static float weight(Blade edge, double now) {
        float age = (float) (now - edge.stamp());
        return switch (edge.packet().phase()) {
            case FORM -> Mth.clamp(age / ScalpelEdge.FORM, 0, 1);
            case READY -> 1;
            case CUT -> 0.22f + 0.7f * Mth.clamp((age - 3) / (ScalpelEdge.RECOVER - 3), 0, 1);
            case RELEASE -> Mth.clamp(1 - age / 4, 0, 1);
        };
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        checkWorld(level);
        double now = level.getGameTime() + partial;
        BLADES.values().removeIf(blade -> now - blade.stamp() > (blade.packet().phase() == Phase.RELEASE ? 5 : 12));
        if (BLADES.isEmpty()) {
            return;
        }
        var mesh = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        boolean fast = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        for (Blade blade : BLADES.values()) {
            var event = blade.packet();
            Vec3 root = event.palm().subtract(camera);
            Vec3 direction = event.axis();
            if (event.phase() != Phase.RELEASE && level.getEntity(event.actor()) instanceof Player caster) {
                root = JutsuVfx.handPosition(caster, event.right() ? 1 : -1, partial).subtract(camera);
                direction = caster.getViewVector(partial);
            }
            if (root.lengthSqr() < 32 * 32) {
                boolean low = fast || root.lengthSqr() > 19 * 19;
                edge(mesh, root, direction, weight(blade, now), low);
                if (event.phase() == Phase.CUT && now - blade.stamp() < 4.2) {
                    cut(mesh, event.point().subtract(camera), event.axis(), (float) ((now - blade.stamp()) / 4.2));
                }
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static void edge(Mesh mesh, Vec3 root, Vec3 aim, float growth, boolean low) {
        if (growth <= 0) {
            return;
        }
        Vec3 side = axis(aim);
        Vec3 normal = aim.cross(side);
        Vec3 tip = root.add(aim.scale(0.52 * growth)).add(normal.scale(0.037 * growth));
        Vec3 flare = root.lerp(tip, 0.39);
        Vec3 spine = flare.add(normal.scale(0.067 * growth));
        int alpha = (int) (214 * growth);
        mesh.quad(root, flare.add(side.scale(0.047)), tip, spine, TEAL, alpha);
        mesh.quad(root, spine, tip, flare.subtract(side.scale(0.046)), DARK, alpha);
        mesh.ribbon(root, tip, normal.scale(0.009 * growth), EDGE, Math.min(255, alpha + 32));
        if (!low) {
            Vec3 skirt = flare.add(side.scale(0.083)).subtract(normal.scale(0.027));
            mesh.quad(root, skirt, tip, flare.subtract(side.scale(0.083)), TEAL, (int) (71 * growth));
        }
    }

    private static void cut(Mesh mesh, Vec3 contact, Vec3 direction, float age) {
        Vec3 side = axis(direction);
        Vec3 local = UP.scale(0.43 + age * 0.1).add(side.scale(0.28));
        Vec3 center = contact.add(direction.scale(-0.040));
        Vec3 a = center.subtract(local.scale(0.46 + 0.53 * age));
        Vec3 b = center.add(local.scale(0.65 + 0.15 * age));
        float fade = 1 - age;
        mesh.quad(a, center.add(side.scale(0.061 * fade)), b, center.subtract(side.scale(0.029 * fade)), EDGE, (int) (244 * fade));
        for (int i = 0; i < 4; i++) {
            double bias = (i - 1.5) * 0.115;
            Vec3 start = center.add(local.scale(bias));
            Vec3 end = start.add(side.scale((i % 2 == 0 ? 1 : -1) * (0.12 + 0.19 * age))).add(0, age * 0.15, 0);
            mesh.ribbon(start, end, UP.scale(0.009), TEAL, (int) (137 * fade));
        }
    }

    private static Vec3 axis(Vec3 direction) {
        return direction.horizontalDistanceSqr() < 0.001 ? new Vec3(1, 0, 0) : UP.cross(direction).normalize();
    }

    private static void checkWorld(ClientLevel level) {
        if (world.get() != level) {
            BLADES.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Blade(ScalpelPacket packet, long stamp) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void ribbon(Vec3 from, Vec3 to, Vec3 width, Tint tint, int alpha) {
            quad(from.add(width), to.add(width), to.subtract(width), from.subtract(width), tint, alpha);
        }

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
