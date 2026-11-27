package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.anims.PoseBlender;
import com.sekwah.narutomod.network.s2c.BaikaPacket;
import com.sekwah.narutomod.network.s2c.BaikaPacket.Stage;
import com.sekwah.narutomod.util.BaikaScale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BaikaRenderer {
    private static final Map<Integer, Shape> SHAPES = new LinkedHashMap<>();
    private static final Tint DUST = new Tint(202, 166, 124);
    private static final Tint ABORT = new Tint(227, 157, 108);
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private BaikaRenderer() {
    }

    public static void receive(BaikaPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        checkWorld(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        var old = SHAPES.get(packet.actor());
        if (old == null && SHAPES.size() >= 32) {
            Integer first = SHAPES.keySet().iterator().next();
            reset(level, first);
            SHAPES.remove(first);
        }
        long now = level.getGameTime();
        long flash = old == null ? now - 20 : old.flash();
        Vec3 print = old == null ? packet.feet() : old.print();
        boolean denial = old != null && old.denial();
        if (packet.stage() == Stage.STEP || packet.stage() == Stage.DENIED) {
            flash = now;
            print = packet.feet();
            denial = packet.stage() == Stage.DENIED;
        }
        SHAPES.put(packet.actor(), new Shape(packet.scale(), packet.stage(), now, flash, print, denial));
        if (level.getEntity(packet.actor()) instanceof Player player) {
            BaikaScale.apply(player, packet.scale());
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        checkWorld(level);
        if (level != null) {
            long now = level.getGameTime();
            SHAPES.entrySet().removeIf(entry -> {
                var shape = entry.getValue();
                if (now - shape.stamp() > 27) {
                    reset(level, entry.getKey());
                    return true;
                }
                return false;
            });
        }
    }

    private static void reset(ClientLevel level, int id) {
        if (level.getEntity(id) instanceof Player player) {
            BaikaScale.apply(player, 1);
        }
    }

    public static void pose(PlayerModel<?> model, LivingEntity actor, float partial) {
        var level = world.get();
        Shape shape = actor.level() == level ? SHAPES.get(actor.getId()) : null;
        if (shape == null) {
            return;
        }
        float age = level.getGameTime() - shape.flash() + partial;
        float load = Math.max(0, 1 - age / 6);
        if (shape.stage() == Stage.GROW) {
            float t = (shape.scale() - 1) / (BaikaScale.FULL - 1);
            load = Math.max(load, Mth.sin(Mth.PI * t) * 0.69f);
        }
        if (load > 0) {
            PoseBlender.addRotation(model.body, load, 0.19f, 0, 0);
            PoseBlender.addRotation(model.rightArm, load, -0.29f, -0.12f, -0.27f);
            PoseBlender.addRotation(model.leftArm, load, -0.29f, 0.12f, 0.27f);
            PoseBlender.addRotation(model.rightLeg, load, -0.18f, 0, -0.13f);
            PoseBlender.addRotation(model.leftLeg, load, -0.13f, 0, 0.13f);
        }
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        checkWorld(level);
        double now = level.getGameTime() + partial;
        if (SHAPES.isEmpty()) {
            return;
        }
        var mesh = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        for (var shape : SHAPES.values()) {
            double t = (now - shape.flash()) / 7;
            Vec3 foot = shape.print().subtract(camera).add(0, 0.051, 0);
            if (t >= 0 && t < 1 && foot.lengthSqr() < 32 * 32) {
                ripple(mesh, foot, t, shape.denial());
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static void ripple(Mesh mesh, Vec3 center, double age, boolean blocked) {
        int arcs = blocked ? 8 : 18;
        Tint tint = blocked ? ABORT : DUST;
        double radius = 0.38 + age * (blocked ? 0.34 : 1.03);
        for (int i = 0; i < arcs; i++) {
            if (i % 4 == 2) {
                continue;
            }
            double a = (i + age * 0.71) * 2 * Math.PI / arcs;
            double b = a + Math.PI / arcs * (i % 2 == 0 ? 1.33 : 0.78);
            double stroke = 0.061 * (1 - age);
            mesh.quad(point(center, radius - stroke, a), point(center, radius - stroke, b), point(center, radius + stroke, b),
                    point(center, radius + stroke, a), tint, (int) (143 * (1 - age)));
        }
    }

    private static Vec3 point(Vec3 center, double radius, double angle) {
        return center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
    }

    private static void checkWorld(ClientLevel level) {
        if (world.get() != level) {
            SHAPES.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Shape(float scale, Stage stage, long stamp, long flash, Vec3 print, boolean denial) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Tint tint, int alpha) {
            vertex(a, tint, alpha);
            vertex(b, tint, alpha);
            vertex(c, tint, alpha);
            vertex(d, tint, alpha);
        }

        private void vertex(Vec3 pos, Tint tint, int alpha) {
            this.buffer.vertex(this.matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                    .color(tint.red(), tint.green(), tint.blue(), alpha).endVertex();
        }
    }

    private record Tint(int red, int green, int blue) {
    }
}
