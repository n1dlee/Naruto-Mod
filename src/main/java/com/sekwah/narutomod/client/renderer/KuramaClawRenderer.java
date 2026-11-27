package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.network.s2c.KuramaCloakPacket;
import com.sekwah.narutomod.network.s2c.KuramaCloakPacket.Phase;
import com.sekwah.narutomod.util.KuramaCloakMotion;
import com.sekwah.narutomod.util.KuramaCloakMotion.Blow;
import com.sekwah.narutomod.util.KuramaCloakMotion.Strike;
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

public final class KuramaClawRenderer {
    private static final int CRIMSON = 0xD2302A;
    private static final int CINDER = 0xFA7748;
    private static final Map<Integer, Scene> SCENES = new LinkedHashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private KuramaClawRenderer() {
    }

    public static void receive(KuramaCloakPacket packet) {
        var level = Minecraft.getInstance().level;
        use(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (SCENES.size() >= 36 && !SCENES.containsKey(packet.actor())) {
            SCENES.remove(SCENES.keySet().iterator().next());
        }
        SCENES.put(packet.actor(), new Scene(packet, level.getGameTime()));
    }

    public static Clip pose(Player player, float partial) {
        if (world.get() != player.level()) {
            return null;
        }
        Scene scene = SCENES.get(player.getId());
        if (scene == null || scene.packet.phase() == Phase.CANCEL) {
            return null;
        }
        var kind = scene.packet.blow().kind();
        double age = player.level().getGameTime() + partial - scene.born;
        if (age >= scene.length()) {
            return null;
        }
        float elapsed = (float) Math.max(0, age);
        float t = scene.packet.phase() == Phase.WINDUP ? Math.min(kind.windup(), elapsed) : kind.windup() + elapsed;
        return new Clip(kind, t / kind.duration());
    }

    static void draw(ClientLevel level, Matrix4f pose, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        use(level);
        double now = level.getGameTime() + partial;
        SCENES.values().removeIf(scene -> now - scene.born >= scene.length());
        if (SCENES.isEmpty()) {
            return;
        }
        var mesh = new Mesh(pose, buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW));
        boolean low = low();
        for (Scene scene : SCENES.values()) {
            Blow blow = scene.packet.blow();
            if (blow.feet().distanceToSqr(camera) < 60 * 60 && scene.packet.phase() != Phase.CANCEL) {
                double ratio = Mth.clamp((now - scene.born) / scene.length(), 0, 1);
                travel(mesh, blow, camera, ratio, scene.packet.phase() == Phase.CONTACT, low);
            }
        }
        buffers.endBatch(NarutoRenderTypes.CHAKRA_GLOW);
    }

    private static void travel(Mesh mesh, Blow blow, Vec3 camera, double age, boolean contact, boolean low) {
        int pieces = blow.kind() == Strike.PAW ? (low ? 6 : 11) : (low ? 13 : 27);
        double shown = contact ? Math.min(1, 0.30 + age * 2.2) : 1;
        int alpha = (int) ((contact ? (low ? 104 : 196) : 72) * (contact ? 1 - age : 0.67 + 0.33 * age));
        double width = (blow.giant() ? 0.62 : 0.10) * (contact ? 1 : 0.35);
        int lanes = blow.kind() == Strike.PAW && !low ? 3 : 1;
        for (int lane = 0; lane < lanes; lane++) {
            double offset = (lane - (lanes - 1) * 0.5) * blow.pawHalfWidth() * 0.55;
            var scene = new Trace(blow, camera, shown, offset, contact ? CINDER : CRIMSON, alpha);
            line(mesh, scene, pieces, width);
        }
    }

    private static void line(Mesh mesh, Trace trace, int pieces, double width) {
        Blow blow = trace.blow();
        Vec3 lateral = new Vec3(blow.axis().z, 0, -blow.axis().x);
        for (int i = 0; i < pieces; i++) {
            double f = trace.shown() * i / pieces;
            double g = trace.shown() * (i + 1) / pieces;
            Vec3 a = trace.at(f, lateral);
            Vec3 b = trace.at(g, lateral);
            Vec3 side = b.subtract(a).cross(new Vec3(0, 1, 0)).normalize().scale(width);
            mesh.ribbon(a, b, side, trace.color(), trace.alpha());
        }
    }

    static boolean coat(Matrix4f pose, VertexConsumer buffer, Player player, int tails, int age, float partial) {
        if (tails <= 0 || tails == 9 || player.isInvisible()) {
            return false;
        }
        var mesh = new Mesh(pose, buffer);
        double f = KuramaCloakMotion.unfold(age + partial, tails);
        Vec3 center = player.getPosition(partial);
        int count = low() ? 4 : 8;
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count + (player.tickCount + partial) * 0.033;
            Vec3 radial = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            Vec3 foot = center.add(radial.scale(0.37 + tails * 0.012)).add(0, 0.10, 0);
            Vec3 tip = foot.add(radial.scale(0.20 + f * 0.14)).add(0, 0.55 + f * 0.85, 0);
            mesh.ribbon(foot, tip, new Vec3(radial.z, 0, -radial.x).scale(0.037), CRIMSON, low() ? 56 : 94);
        }
        return true;
    }

    private static boolean low() {
        return NarutoConfig.reducedBodyRiteEffects || Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
    }

    private static void use(ClientLevel level) {
        if (world.get() != level) {
            SCENES.clear();
            world = new WeakReference<>(level);
        }
    }

    public record Clip(Strike kind, float progress) {
    }

    private record Scene(KuramaCloakPacket packet, long born) {
        private int length() {
            var kind = packet.blow().kind();
            return switch (packet.phase()) {
                case WINDUP -> kind.windup() + 5;
                case CONTACT -> kind.duration() - kind.windup();
                case CANCEL -> 2;
            };
        }
    }

    private record Trace(Blow blow, Vec3 camera, double shown, double offset, int color, int alpha) {
        private Vec3 at(double fraction, Vec3 lateral) {
            double drop = blow.kind() == Strike.PAW ? (blow.giant() ? 1.22 : 0.62) * fraction : 0;
            return blow.endpoint(fraction).subtract(camera).add(lateral.scale(offset)).add(0, -drop, 0);
        }
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void ribbon(Vec3 a, Vec3 b, Vec3 side, int rgb, int alpha) {
            vertex(a, rgb, alpha);
            vertex(b, rgb, alpha);
            vertex(b.add(side), rgb, 0);
            vertex(a.add(side), rgb, 0);
        }

        private void vertex(Vec3 at, int rgb, int alpha) {
            buffer.vertex(matrix, (float) at.x, (float) at.y, (float) at.z)
                    .color((rgb >>> 16) & 255, (rgb >>> 8) & 255, rgb & 255, alpha).endVertex();
        }
    }
}
