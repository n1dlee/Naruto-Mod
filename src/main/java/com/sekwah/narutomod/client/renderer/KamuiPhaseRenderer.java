package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.events.KamuiPhaseEvents;
import com.sekwah.narutomod.network.s2c.KamuiPhasePacket;
import com.sekwah.narutomod.util.GustFront;
import com.sekwah.narutomod.util.KamuiPhaseWindow;
import com.sekwah.narutomod.util.KamuiPhaseWindow.Stage;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

public final class KamuiPhaseRenderer {
    private static final Map<Integer, Reveal> STATES = new LinkedHashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private KamuiPhaseRenderer() {
    }

    public static void receive(KamuiPhasePacket packet) {
        Minecraft game = Minecraft.getInstance();
        ClientLevel level = game.level;
        useLevel(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (game.player != null && game.player.getId() == packet.actor()) {
            KamuiPhaseEvents.clientState(game.player, packet.stage());
        }
        if (STATES.size() >= 32 && !STATES.containsKey(packet.actor())) {
            STATES.remove(STATES.keySet().iterator().next());
        }
        STATES.put(packet.actor(), new Reveal(packet, level.getGameTime()));
    }

    public static float veil(LivingEntity actor, float partial) {
        Reveal reveal = STATES.get(actor.getId());
        if (reveal == null || actor.level() != world.get()) {
            return 0;
        }
        float since = actor.level().getGameTime() - reveal.received() + partial;
        return switch (reveal.packet().stage()) {
            case OPENING -> Mth.clamp(since / KamuiPhaseWindow.OPEN, 0, 1) * 0.63f;
            case PHASED -> since <= 12 ? 0.67f : 0;
            case RESOLVING -> since <= 12 ? 0.50f : 0;
            case SEALED -> Mth.clamp(1 - since / KamuiPhaseWindow.RETURN, 0, 1) * 0.55f;
        };
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        useLevel(level);
        long now = level.getGameTime();
        STATES.values().removeIf(reveal -> now - reveal.received() > 13);
        if (STATES.isEmpty()) {
            return;
        }
        Minecraft game = Minecraft.getInstance();
        int steps = game.options.graphicsMode().get() == GraphicsStatus.FAST ? 12 : 24;
        VertexConsumer mesh = buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW);
        for (Reveal read : STATES.values()) {
            var packet = read.packet();
            if (!(level.getEntity(packet.actor()) instanceof LivingEntity actor)) {
                continue;
            }
            float amount = veil(actor, partial);
            Vec3 feet = actor.getPosition(partial).subtract(camera);
            if (amount <= 0.01f || feet.lengthSqr() > 38 * 38) {
                continue;
            }
            if (actor != game.player || !game.options.getCameraType().isFirstPerson()) {
                shell(matrix, mesh, new Shell(feet, actor.getBbHeight(), amount, steps,
                        (packet.age() + now - read.received() + partial) * 0.53f));
            }
            if (packet.normal().lengthSqr() > 0.5) {
                float ripple = Mth.clamp(1 - (now - read.received() + partial) / 5.0f, 0, 1);
                if (ripple > 0.01f) {
                    boundary(matrix, mesh, new Boundary(packet.contact().subtract(camera), GustFront.frame(packet.normal()),
                            ripple, steps));
                }
            }
        }
        buffers.endBatch(NarutoRenderTypes.CHAKRA_GLOW);
    }

    private static void shell(Matrix4f matrix, VertexConsumer mesh, Shell body) {
        for (int k = 0; k < 2; k++) {
            for (int i = 0; i < body.steps(); i++) {
                double t = i / (double) body.steps();
                double to = (i + 1.0) / body.steps();
                Vec3 a = helix(body, t, k * Math.PI);
                Vec3 b = helix(body, to, k * Math.PI);
                float alpha = body.alpha() * (float) Math.sin(Math.PI * (t + to) * 0.5) * 0.47f;
                ribbon(matrix, mesh, a, b, new Vec3(0, 0.028, 0), alpha);
            }
        }
    }

    private static Vec3 helix(Shell body, double t, double offset) {
        double angle = body.spin() + offset + t * 10.6;
        double radius = 0.18 + Math.sin(Math.PI * t) * 0.23 + Math.sin(angle * 2) * 0.03;
        return body.feet().add(Math.cos(angle) * radius, body.height() * (0.20 + t * 0.76), Math.sin(angle) * radius);
    }

    private static void boundary(Matrix4f matrix, VertexConsumer mesh, Boundary boundary) {
        var frame = boundary.frame();
        Vec3 point = boundary.point().add(frame.forward().scale(0.035));
        for (int i = 0; i < boundary.steps(); i++) {
            double t0 = i / (double) boundary.steps();
            double t1 = (i + 1.0) / boundary.steps();
            Vec3 a = point.add(edge(boundary, t0));
            Vec3 b = point.add(edge(boundary, t1));
            ribbon(matrix, mesh, a, b, frame.up().scale(0.018), boundary.fade() * 0.55f);
        }
    }

    private static Vec3 edge(Boundary boundary, double t) {
        double angle = t * Math.PI * 4.2 + (1 - boundary.fade()) * 2.3;
        double radius = (0.06 + 0.69 * t) * (1.20 - boundary.fade() * 0.60);
        return boundary.frame().right().scale(Math.cos(angle) * radius)
                .add(boundary.frame().up().scale(Math.sin(angle) * radius * 1.17));
    }

    private static void ribbon(Matrix4f matrix, VertexConsumer mesh, Vec3 a, Vec3 b, Vec3 width, float alpha) {
        vertex(matrix, mesh, a, alpha);
        vertex(matrix, mesh, b, alpha);
        vertex(matrix, mesh, b.add(width), 0);
        vertex(matrix, mesh, a.add(width), 0);
    }

    private static void vertex(Matrix4f matrix, VertexConsumer mesh, Vec3 p, float alpha) {
        mesh.vertex(matrix, (float) p.x, (float) p.y, (float) p.z).color(0.59f, 0.55f, 0.70f, alpha).endVertex();
    }

    private static void useLevel(ClientLevel level) {
        if (world.get() != level) {
            STATES.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Reveal(KamuiPhasePacket packet, long received) {
    }

    private record Shell(Vec3 feet, float height, float alpha, int steps, float spin) {
    }

    private record Boundary(Vec3 point, GustFront.Frame frame, float fade, int steps) {
    }
}
