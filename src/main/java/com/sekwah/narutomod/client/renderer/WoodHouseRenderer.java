package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.network.s2c.WoodHousePacket;
import com.sekwah.narutomod.network.s2c.WoodHousePacket.Stage;
import com.sekwah.narutomod.util.ProjectileAxis;
import com.sekwah.narutomod.util.WoodHouseLayout;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WoodHouseRenderer {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 FRONT = new Vec3(0, 0, 1);
    private static final int ROOTS = 0x85502B;
    private static final int BARK = 0xAD7546;
    private static final int SAP = 0xE7BF74;
    private static final Map<BlockPos, Survey> SITES = new LinkedHashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private WoodHouseRenderer() {
    }

    public static void receive(WoodHousePacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        use(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        if (SITES.size() >= 8 && !SITES.containsKey(packet.corner())) {
            SITES.remove(SITES.keySet().iterator().next());
        }
        SITES.put(packet.corner(), new Survey(packet, level.getGameTime()));
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        use(level);
        double now = level.getGameTime() + partial;
        SITES.values().removeIf(survey -> now - survey.start() >= lifetime(survey.packet().stage()));
        if (SITES.isEmpty()) {
            return;
        }
        boolean fast = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        var mesh = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        for (Survey survey : SITES.values()) {
            Vec3 base = Vec3.atLowerCornerOf(survey.packet().corner()).subtract(camera);
            if (base.lengthSqr() <= 64 * 64) {
                drawSite(mesh, survey.packet(), base, now - survey.start(), fast || base.lengthSqr() > 35 * 35);
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static int lifetime(Stage stage) {
        return switch (stage) {
            case SURVEY -> WoodHouseLayout.WINDUP + 5;
            case REVEAL -> 11;
            case CANCEL -> 7;
        };
    }

    private static void drawSite(Mesh mesh, WoodHousePacket packet, Vec3 base, double age, boolean low) {
        double width = packet.size().getX();
        double depth = packet.size().getZ();
        double height = packet.size().getY();
        Vec3 floor = base.add(0, 0.043, 0);
        Vec3[] corners = {floor, floor.add(width, 0, 0), floor.add(width, 0, depth), floor.add(0, 0, depth)};
        if (packet.stage() != Stage.SURVEY) {
            settle(mesh, corners, packet.stage(), age);
            return;
        }
        for (int i = 0; i < corners.length; i++) {
            Vec3 corner = corners[i];
            Vec3 next = corners[(i + 1) % corners.length];
            double outline = WoodHouseLayout.phase(age, 0, 7);
            mesh.beam(corner, corner.lerp(next, outline), 0.048, ROOTS, 170);
            mesh.beam(corner.add(0, 0.037, 0), corner.lerp(next, outline).add(0, 0.037, 0), 0.016, SAP, 218);
            column(mesh, corner, next, age, height, low);
        }
    }

    private static void column(Mesh mesh, Vec3 corner, Vec3 next, double age, double height, boolean low) {
        double growth = WoodHouseLayout.phase(age, 4, 12);
        double roof = WoodHouseLayout.phase(age, 13, 9);
        if (growth <= 0) {
            return;
        }
        double eave = height * 0.72;
        Vec3 top = corner.add(0, eave * growth, 0);
        mesh.beam(corner, top, 0.154, BARK, 150);
        mesh.beam(corner.add(0.07, 0.02, 0.05), top.add(0.07, 0.02, 0.05), 0.032, SAP, 224);
        Vec3 bridge = top.lerp(next.add(0, eave * growth, 0), roof);
        mesh.beam(top, bridge, 0.12, BARK, 149);
        mesh.beam(top.add(0, 0.1, 0), bridge.add(0, 0.1, 0), 0.026, SAP, 186);
        if (!low && roof > 0) {
            Vec3 elbow = top.lerp(bridge, 0.35).add(0, height * 0.17 * roof, 0);
            mesh.beam(top, elbow, 0.076, ROOTS, 132);
            mesh.beam(elbow, top.lerp(bridge, 0.68), 0.078, BARK, 145);
        }
    }

    private static void settle(Mesh mesh, Vec3[] corners, Stage stage, double age) {
        boolean reveal = stage == Stage.REVEAL;
        double t = WoodHouseLayout.phase(age, 0, lifetime(stage));
        int tint = reveal ? SAP : 0xD66249;
        int opacity = (int) (198 * (1 - t));
        for (int i = 0; i < corners.length; i++) {
            Vec3 p = corners[i];
            Vec3 q = corners[(i + 1) % corners.length];
            Vec3 normal = q.subtract(p).cross(UP).normalize();
            double offset = t * (reveal ? 0.44 : -0.36);
            mesh.beam(p.add(normal.scale(offset)), q.add(normal.scale(offset)), reveal ? 0.11 : 0.055, tint, opacity);
        }
    }

    private static void use(ClientLevel level) {
        if (world.get() != level) {
            SITES.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Survey(WoodHousePacket packet, long start) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void beam(Vec3 p, Vec3 q, double width, int tint, int alpha) {
            if (p.distanceToSqr(q) < 1.0e-7) {
                return;
            }
            Vec3 axis = ProjectileAxis.direction(q.subtract(p), FRONT);
            Vec3 cross = Math.abs(axis.y) < 0.9 ? UP : FRONT;
            Vec3 side = cross.cross(axis).normalize();
            face(p, q, side.scale(width), tint, alpha);
            face(p, q, axis.cross(side).scale(width), tint, alpha);
        }

        private void face(Vec3 p, Vec3 q, Vec3 width, int tint, int alpha) {
            vertex(p.add(width), tint, alpha);
            vertex(q.add(width), tint, alpha);
            vertex(q.subtract(width), tint, alpha);
            vertex(p.subtract(width), tint, alpha);
        }

        private void vertex(Vec3 at, int tint, int alpha) {
            this.buffer.vertex(this.matrix, (float) at.x, (float) at.y, (float) at.z)
                    .color((tint >>> 16) & 255, (tint >>> 8) & 255, tint & 255, alpha).endVertex();
        }
    }
}
