package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.network.s2c.ChidoriImpactPacket;
import com.sekwah.narutomod.network.s2c.ChidoriImpactPacket.Kind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Deque;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChidoriImpactRenderer {
    private static final int MAX_FLASHES = 32;
    private static final float LIFETIME = 6.0F;
    private static final Deque<Flash> FLASHES = new ArrayDeque<>();
    private static ClientLevel currentLevel;

    private ChidoriImpactRenderer() {
    }

    public static void addImpact(ChidoriImpactPacket packet) {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        useLevel(level);
        Vec3 axis = packet.direction().normalize();
        if (axis.lengthSqr() == 0.0) {
            return;
        }
        Vec3 reference = Math.abs(axis.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = axis.cross(reference).normalize();
        if (FLASHES.size() == MAX_FLASHES) {
            FLASHES.removeFirst();
        }
        FLASHES.addLast(new Flash(packet, axis, right, axis.cross(right), level.getGameTime()));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        useLevel(null);
    }

    static boolean draw(ClientLevel level, Matrix4f matrix, VertexConsumer consumer, Vec3 camera, float partialTick) {
        useLevel(level);
        long now = level.getGameTime();
        while (!FLASHES.isEmpty() && now - FLASHES.getFirst().started >= LIFETIME) {
            FLASHES.removeFirst();
        }
        boolean drew = false;
        for (Flash flash : FLASHES) {
            float time = (now - flash.started + partialTick) / LIFETIME;
            if (time < 1.0F && camera.distanceToSqr(flash.packet.point()) <= 48.0 * 48.0) {
                drawFlash(matrix, consumer, camera, flash, time);
                drew = true;
            }
        }
        return drew;
    }

    private static void useLevel(ClientLevel level) {
        if (currentLevel != level) {
            FLASHES.clear();
            currentLevel = level;
        }
    }

    private static void drawFlash(Matrix4f matrix, VertexConsumer consumer, Vec3 camera, Flash flash, float time) {
        Kind kind = flash.packet.kind();
        boolean wall = kind == Kind.BLOCK;
        float fade = (1.0F - time) * (1.0F - time);
        int branches = kind == Kind.MISS ? 2 : 5;
        double radius = switch (kind) {
            case ENTITY -> 0.95;
            case BLOCK -> 0.68;
            case MISS -> 0.28;
        };
        double offset = wall ? 0.035 : -0.06;
        Vec3 root = flash.packet.point().add(flash.axis.scale(offset));
        var random = RandomSource.create(flash.packet.seed());
        for (int i = 0; i < branches; i++) {
            double angle = 2.0 * Math.PI * (i + 0.55 * random.nextDouble()) / branches;
            Vec3 radial = flash.right.scale(Math.cos(angle)).add(flash.up.scale(Math.sin(angle)));
            Vec3 bend = flash.axis.cross(radial);
            double reach = radius * (0.55 + 0.45 * random.nextDouble()) * (0.72 + 0.45 * time);
            Vec3 previous = root;
            for (int k = 1; k <= 4; k++) {
                double t = k / 4.0;
                double side = k == 4 ? 0.0 : (random.nextDouble() - 0.5) * reach * 0.45;
                Vec3 next = root.add(radial.scale(reach * t)).add(bend.scale(side))
                        .add(flash.axis.scale((wall ? 0.06 : -0.22) * t));
                double width = 0.036 * (1.0 - 0.65 * t) * (0.4 + fade * 0.6);
                ribbon(matrix, consumer, camera, previous, next, flash.right, width, fade * (float) (1.0 - 0.4 * t));
                previous = next;
            }
        }
    }

    private static void ribbon(Matrix4f matrix, VertexConsumer consumer, Vec3 camera,
                               Vec3 from, Vec3 to, Vec3 fallback, double width, float alpha) {
        Vec3 side = to.subtract(from).cross(camera.subtract(from)).normalize();
        if (side.lengthSqr() == 0.0) {
            side = fallback;
        }
        strip(matrix, consumer, from, to, side.scale(width * 2.8), 0.15F, 0.50F, alpha * 0.27F);
        strip(matrix, consumer, from, to, side.scale(width), 0.83F, 0.94F, alpha);
    }

    private static void strip(Matrix4f matrix, VertexConsumer consumer, Vec3 from, Vec3 to,
                              Vec3 width, float red, float green, float alpha) {
        vertex(matrix, consumer, from.add(width), red, green, alpha);
        vertex(matrix, consumer, from.subtract(width), red, green, alpha);
        vertex(matrix, consumer, to.subtract(width.scale(0.55)), red, green, alpha * 0.7F);
        vertex(matrix, consumer, to.add(width.scale(0.55)), red, green, alpha * 0.7F);
    }

    private static void vertex(Matrix4f matrix, VertexConsumer consumer, Vec3 point,
                               float red, float green, float alpha) {
        consumer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, 1.0F, alpha).endVertex();
    }

    private record Flash(ChidoriImpactPacket packet, Vec3 axis, Vec3 right, Vec3 up, long started) {
    }
}
