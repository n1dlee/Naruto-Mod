package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.anims.PoseBlender;
import com.sekwah.narutomod.network.s2c.BoulderRollPacket;
import com.sekwah.narutomod.util.BoulderRoll;
import com.sekwah.narutomod.util.BoulderRoll.Stage;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BoulderRollRenderer {
    private static final Map<Integer, Body> BODIES = new LinkedHashMap<>();
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Tint CLOTH = new Tint(107, 42, 30);
    private static final Tint PLATE = new Tint(173, 82, 54);
    private static final Tint STITCH = new Tint(207, 158, 106);
    private static final Tint DUST = new Tint(219, 182, 139);
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private BoulderRollRenderer() {
    }

    public static void receive(BoulderRollPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        checkWorld(level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        Body before = BODIES.get(packet.actor());
        if (before == null && BODIES.size() >= 22) {
            Integer id = BODIES.keySet().iterator().next();
            reset(level, id);
            BODIES.remove(id);
        }
        long time = level.getGameTime();
        Vec3 feet = before == null ? packet.feet() : before.position(time);
        float rotation = before == null ? packet.rotation() : before.rotation(time);
        BODIES.put(packet.actor(), new Body(packet, time, feet, rotation));
        if (level.getEntity(packet.actor()) instanceof Player player) {
            BoulderRoll.apply(player, folded(packet.stage()) ? packet.radius() : 0);
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
            BODIES.entrySet().removeIf(entry -> {
                if (now - entry.getValue().time() > 10) {
                    reset(level, entry.getKey());
                    return true;
                }
                return false;
            });
        }
    }

    private static void reset(ClientLevel level, int id) {
        if (level.getEntity(id) instanceof Player player) {
            BoulderRoll.apply(player, 0);
        }
    }

    public static boolean hidesPlayer(Player actor) {
        var track = world.get() == actor.level() ? BODIES.get(actor.getId()) : null;
        return track != null && folded(track.packet().stage());
    }

    private static boolean folded(Stage phase) {
        return phase == Stage.ROLL || phase == Stage.HOLD;
    }

    @SubscribeEvent
    public static void hideFirstPersonHands(RenderHandEvent event) {
        var player = Minecraft.getInstance().player;
        if (player != null && hidesPlayer(player)) {
            event.setCanceled(true);
        }
    }

    public static void pose(HumanoidModel<?> model, LivingEntity actor, float partial) {
        var level = world.get();
        Body track = level == actor.level() ? BODIES.get(actor.getId()) : null;
        if (track == null || folded(track.packet().stage())) {
            return;
        }
        float age = level.getGameTime() - track.time() + partial;
        float weight = track.packet().stage() == Stage.TUCK ? Mth.clamp(age / BoulderRoll.WINDUP, 0, 1)
                : Mth.clamp(1 - age / BoulderRoll.RECOVERY, 0, 1);
        PoseBlender.addRotation(model.body, weight, 0.44f, 0, 0);
        PoseBlender.addRotation(model.head, weight, 0.19f, 0, 0);
        PoseBlender.rotate(model.rightArm, weight, -1.21f, -0.63f, -0.19f);
        PoseBlender.rotate(model.leftArm, weight, -1.21f, 0.63f, 0.19f);
        PoseBlender.rotate(model.rightLeg, weight * 0.66f, -0.53f, 0, 0.13f);
        PoseBlender.rotate(model.leftLeg, weight * 0.66f, -0.53f, 0, -0.13f);
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        checkWorld(level);
        if (BODIES.isEmpty()) {
            return;
        }
        double now = level.getGameTime() + partial;
        boolean fast = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        var shells = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.BOULDER_BODY));
        for (Body body : BODIES.values()) {
            if (folded(body.packet().stage()) && visible(level, body.packet().actor())) {
                Vec3 feet = body.position(now).subtract(camera);
                if (feet.lengthSqr() < 38 * 38) {
                    shell(shells, body, feet, now, fast || feet.lengthSqr() > 22 * 22);
                }
            }
        }
        buffers.endBatch(NarutoRenderTypes.BOULDER_BODY);
        var dust = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        for (Body body : BODIES.values()) {
            float age = (float) (now - body.time());
            if (body.packet().stage() == Stage.BUMP && age < 6) {
                impact(dust, body.packet().feet().subtract(camera), body.packet().direction(), age / 6);
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static boolean visible(ClientLevel world, int id) {
        if (!(world.getEntity(id) instanceof LivingEntity actor)) {
            return true;
        }
        var game = Minecraft.getInstance();
        return !actor.isInvisible() && (game.getCameraEntity() != actor || !game.options.getCameraType().isFirstPerson());
    }

    private static void shell(Mesh mesh, Body body, Vec3 feet, double now, boolean low) {
        var packet = body.packet();
        var shell = new Sphere(feet.add(0, packet.radius(), 0), packet.direction(), UP.cross(packet.direction()),
                packet.radius(), body.rotation(now));
        int columns = low ? 12 : 20;
        int levels = low ? 8 : 13;
        for (int y = 0; y < levels; y++) {
            double a = Math.PI * y / levels - Math.PI * 0.5;
            double b = Math.PI * (y + 1) / levels - Math.PI * 0.5;
            for (int x = 0; x < columns; x++) {
                double c = x * 2 * Math.PI / columns;
                double d = (x + 1) * 2 * Math.PI / columns;
                Tint tint = y == levels / 2 ? STITCH : (x + y * 3) % 6 < 3 ? PLATE : CLOTH;
                double light = 0.74 + 0.20 * Math.cos(c + shell.phase() * 0.28) * Math.cos(a);
                mesh.quad(shell.point(a, c), shell.point(a, d), shell.point(b, d), shell.point(b, c), tint.shade(light), 255);
            }
        }
    }

    private static void impact(Mesh mesh, Vec3 base, Vec3 direction, float age) {
        Vec3 side = UP.cross(direction);
        for (int i = -4; i <= 4; i++) {
            Vec3 a = base.add(direction.scale(0.24 + age * 0.31)).add(side.scale(i * (0.078 + age * 0.17))).add(0, 0.06, 0);
            Vec3 b = a.add(side.scale(0.20)).add(0, 0.011, 0);
            Vec3 lift = UP.scale((0.10 + age * 0.26) * (1 - Math.abs(i) * 0.12));
            mesh.quad(a, b, b.add(lift).subtract(direction.scale(0.32 * age)), a.add(lift.scale(0.44)), DUST,
                    (int) (149 * (1 - age)));
        }
    }

    private static void checkWorld(ClientLevel level) {
        if (world.get() != level) {
            BODIES.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Sphere(Vec3 center, Vec3 front, Vec3 right, float radius, float phase) {
        private Vec3 point(double lat, double lon) {
            double x = Math.cos(lat) * Math.cos(lon);
            double y = Math.sin(lat);
            double z = Math.cos(lat) * Math.sin(lon);
            double spinY = y * Math.cos(this.phase) - z * Math.sin(this.phase);
            double spinZ = y * Math.sin(this.phase) + z * Math.cos(this.phase);
            double folds = 1 - 0.027 * Math.pow(Math.cos(lon * 4), 4) * Math.cos(lat);
            return this.center.add(this.right.scale(x * this.radius * folds)).add(0, spinY * this.radius * folds, 0)
                    .add(this.front.scale(spinZ * this.radius * folds));
        }
    }

    private record Body(BoulderRollPacket packet, long time, Vec3 from, float oldAngle) {
        private Vec3 position(double now) {
            return this.from.lerp(this.packet.feet(), Mth.clamp(now - this.time, 0, 1));
        }

        private float rotation(double now) {
            return Mth.lerp((float) Mth.clamp(now - this.time, 0, 1), this.oldAngle, this.packet.rotation());
        }
    }

    private record Tint(int red, int green, int blue) {
        private Tint shade(double scale) {
            return new Tint((int) (this.red * scale), (int) (this.green * scale), (int) (this.blue * scale));
        }
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Tint tint, int alpha) {
            vertex(a, tint, alpha);
            vertex(b, tint, alpha);
            vertex(c, tint, alpha);
            vertex(d, tint, alpha);
        }

        private void vertex(Vec3 value, Tint tint, int alpha) {
            this.buffer.vertex(this.matrix, (float) value.x, (float) value.y, (float) value.z)
                    .color(tint.red(), tint.green(), tint.blue(), alpha).endVertex();
        }
    }
}
