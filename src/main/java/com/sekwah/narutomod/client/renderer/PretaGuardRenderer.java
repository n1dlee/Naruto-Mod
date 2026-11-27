package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.PretaGuardPacket;
import com.sekwah.narutomod.util.PretaReservoir;
import com.sekwah.narutomod.util.PretaReservoir.Stage;
import com.sekwah.narutomod.util.ProjectileAxis;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PretaGuardRenderer {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 FORWARD = new Vec3(0, 0, 1);
    private static final int AMETHYST = 0xA892D9;
    private static final int EDGE = 0xE4DCFA;
    private static final int DARK = 0x4A3E72;
    private static final Map<Integer, Flash> SHIELDS = new LinkedHashMap<>();
    private static final List<Flash> SIPS = new ArrayList<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private PretaGuardRenderer() {
    }

    public static void receive(PretaGuardPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        use(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        var frame = new Flash(packet, level.getGameTime());
        if (packet.stage() == Stage.SIP) {
            if (SIPS.size() >= 28) {
                SIPS.remove(0);
            }
            SIPS.add(frame);
        } else {
            if (SHIELDS.size() >= 19 && !SHIELDS.containsKey(packet.actor())) {
                SHIELDS.remove(SHIELDS.keySet().iterator().next());
            }
            SHIELDS.put(packet.actor(), frame);
        }
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        use(level);
        double now = level.getGameTime() + partial;
        SHIELDS.values().removeIf(frame -> now - frame.time() >= ttl(frame.packet().stage()));
        SIPS.removeIf(frame -> now - frame.time() >= 8);
        if (SHIELDS.isEmpty() && SIPS.isEmpty()) {
            return;
        }
        boolean fast = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        var glass = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        for (Flash flash : SHIELDS.values()) {
            Vec3 at = origin(level, flash.packet(), partial).subtract(camera);
            if (at.lengthSqr() < 42 * 42) {
                shell(glass, at, flash.packet().stage(), now - flash.time(), fast || at.lengthSqr() > 22 * 22);
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
        var light = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW));
        for (Flash flash : SIPS) {
            Vec3 at = origin(level, flash.packet(), partial).subtract(camera);
            if (at.lengthSqr() < 38 * 38) {
                siphon(light, flash.packet(), at, flash.packet().contact().subtract(camera), now - flash.time(), fast);
            }
        }
        buffers.endBatch(NarutoRenderTypes.CHAKRA_GLOW);
    }

    private static int ttl(Stage stage) {
        return switch (stage) {
            case OPEN -> PretaReservoir.WINDUP + 4;
            case READY -> 22;
            case FADE -> 6;
            case SIP -> 8;
        };
    }

    private static Vec3 origin(ClientLevel level, PretaGuardPacket packet, float partial) {
        return level.getEntity(packet.actor()) instanceof Player actor
                ? actor.getPosition(partial).add(0, 0.64 * actor.getBbHeight(), 0) : packet.center();
    }

    private static void shell(Mesh mesh, Vec3 at, Stage stage, double age, boolean low) {
        double scale = switch (stage) {
            case OPEN -> Mth.clamp((age + 0.5) / PretaReservoir.WINDUP, 0.10, 1);
            case FADE -> Mth.clamp(1 - age / 6, 0, 1);
            default -> 1;
        };
        int sides = low ? 10 : 20;
        int alpha = (int) (126 * scale);
        var pane = new Pane(at, 0.82 * scale, 0.87 * scale, sides, alpha);
        for (int i = 0; i < sides; i++) {
            panel(mesh, pane, i, low);
        }
    }

    private static void panel(Mesh mesh, Pane pane, int i, boolean low) {
        double a = i * Math.PI * 2 / pane.sides();
        double b = (i + 1) * Math.PI * 2 / pane.sides();
        Vec3 p = pane.at(a, 0);
        Vec3 q = pane.at(b, 0);
        Vec3 upper = pane.at((a + b) * 0.5, 0.83);
        Vec3 lower = pane.at((a + b) * 0.5, -0.89);
        int piece = i % 2 == 0 ? AMETHYST : DARK;
        mesh.quad(p, upper, q, p, piece, pane.alpha() / 5);
        mesh.quad(p, q, lower, p, piece, pane.alpha() / 6);
        Vec3 inside = p.lerp(pane.center(), 0.042);
        Vec3 insideEnd = q.lerp(pane.center(), 0.042);
        mesh.quad(p, q, insideEnd, inside, EDGE, pane.alpha());
        if (!low && i % 2 == 0) {
            mesh.strip(p, upper, pane.center().subtract(p).normalize().scale(0.018), AMETHYST, pane.alpha() / 2);
            mesh.strip(q, lower, pane.center().subtract(q).normalize().scale(0.014), EDGE, pane.alpha() / 4);
        }
    }

    private static void siphon(Mesh mesh, PretaGuardPacket packet, Vec3 at, Vec3 contact, double time, boolean low) {
        double t = Mth.clamp(time / 8, 0, 1);
        Vec3 axis = ProjectileAxis.direction(contact.subtract(at), FORWARD);
        Vec3 side = (Math.abs(axis.y) > 0.85 ? FORWARD : UP).cross(axis).normalize();
        Vec3 vertical = axis.cross(side);
        int beams = low ? 4 : 8;
        for (int i = 0; i < beams; i++) {
            double spin = i * Math.PI * 2 / beams + t * 2.6;
            Vec3 radius = side.scale(Math.cos(spin)).add(vertical.scale(Math.sin(spin)));
            Vec3 end = contact.lerp(at, t * 0.89).add(radius.scale(0.23 * (1 - t)));
            Vec3 tip = end.lerp(at, 0.52).add(radius.scale(0.057 * (1 - t)));
            mesh.strip(end, tip, radius.scale(0.036 * (1 - t)), i % 3 == 1 ? EDGE : packet.tint(), (int) (221 * (1 - t)));
        }
    }

    private static void use(ClientLevel level) {
        if (world.get() != level) {
            SIPS.clear();
            SHIELDS.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Flash(PretaGuardPacket packet, long time) {
    }

    private record Pane(Vec3 center, double width, double height, int sides, int alpha) {
        private Vec3 at(double bearing, double y) {
            double radial = width * (1 - Math.abs(y) * 0.63);
            return center.add(Math.cos(bearing) * radial, height * y, Math.sin(bearing) * radial);
        }
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void strip(Vec3 a, Vec3 b, Vec3 side, int tint, int alpha) {
            quad(a.add(side), b.add(side.scale(0.56)), b.subtract(side.scale(0.56)), a.subtract(side), tint, alpha);
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
