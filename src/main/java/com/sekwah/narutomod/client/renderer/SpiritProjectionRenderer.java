package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.c2s.SpiritProjectionInputPacket;
import com.sekwah.narutomod.network.s2c.SpiritProjectionPacket;
import com.sekwah.narutomod.network.s2c.SpiritProjectionPacket.State;
import com.sekwah.narutomod.util.SpiritProjectionPath;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpiritProjectionRenderer {
    private static final Map<Integer, Vision> VISIONS = new LinkedHashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);
    private static ArmorStand cameraProxy;
    private static EntityModelSet modelSet;
    private static PlayerModel<AbstractClientPlayer> model;
    private static PlayerModel<AbstractClientPlayer> slim;

    private SpiritProjectionRenderer() {
    }

    public static void receive(SpiritProjectionPacket packet) {
        Minecraft game = Minecraft.getInstance();
        ClientLevel level = game.level;
        useLevel(game, level);
        if (level == null || !level.dimension().location().equals(packet.dimension())) {
            return;
        }
        long now = level.getGameTime();
        Vision previous = VISIONS.get(packet.actor());
        if (packet.state() == State.RELEASE && (previous == null || previous.packet().session() != packet.session())) {
            return;
        }
        Vec3 from = previous == null || previous.packet().session() != packet.session()
                ? packet.eye() : interpolate(previous, now, 0);
        if (VISIONS.size() >= 24 && previous == null) {
            VISIONS.remove(VISIONS.keySet().iterator().next());
        }
        VISIONS.put(packet.actor(), new Vision(packet, from, now));
        if (game.player != null && game.player.getId() == packet.actor() && packet.state() == State.RELEASE) {
            releaseCamera(game);
        }
    }

    @SubscribeEvent
    public static void input(MovementInputUpdateEvent event) {
        Minecraft game = Minecraft.getInstance();
        if (event.getEntity() != game.player || game.player == null || game.level != world.get()) {
            return;
        }
        Vision self = liveVision(game);
        if (self == null) {
            return;
        }
        var controls = event.getInput();
        byte rise = (byte) ((controls.jumping ? 1 : 0) - (controls.shiftKeyDown ? 1 : 0));
        var command = new SpiritProjectionPath.Input(game.player.getYRot(), (byte) Math.signum(controls.leftImpulse),
                (byte) Math.signum(controls.forwardImpulse), rise);
        PacketHandler.sendToServer(new SpiritProjectionInputPacket(self.packet().session(), command));
        controls.leftImpulse = 0;
        controls.forwardImpulse = 0;
        controls.jumping = false;
        controls.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft game = Minecraft.getInstance();
        useLevel(game, game.level);
        if (game.level == null || game.player == null) {
            return;
        }
        long now = game.level.getGameTime();
        VISIONS.values().removeIf(vision -> now - vision.received() > expiry(vision));
        Vision own = liveVision(game);
        if (own == null || own.packet().state() != State.DRIFT || !game.player.isAlive() || game.player.isSpectator()) {
            releaseCamera(game);
            return;
        }
        setCamera(game, interpolate(own, now, 0));
    }

    static void draw(ClientLevel level, PoseStack poses, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        Minecraft game = Minecraft.getInstance();
        useLevel(game, level);
        if (VISIONS.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        VertexConsumer aura = buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW);
        int steps = game.options.graphicsMode().get() == GraphicsStatus.FAST ? 12 : 22;
        for (Vision vision : VISIONS.values()) {
            drawAnchors(poses.last().pose(), aura, vision, new Scene(camera, now, partial, steps));
        }
        buffers.endBatch(NarutoRenderTypes.CHAKRA_GLOW);
        for (Vision vision : VISIONS.values()) {
            if (level.getEntity(vision.packet().actor()) instanceof AbstractClientPlayer actor
                    && !(actor == game.player && cameraProxy != null && game.options.getCameraType().isFirstPerson())) {
                renderAvatar(game, actor, poses, buffers, vision, new Scene(camera, now, partial, steps));
            }
        }
    }

    private static void drawAnchors(Matrix4f matrix, VertexConsumer aura, Vision vision, Scene scene) {
        Vec3 center = vision.packet().body().subtract(scene.camera());
        if (center.lengthSqr() > 42 * 42) {
            return;
        }
        float intensity = intensity(vision, scene.now(), scene.partial());
        double glide = interpolate(vision, scene.now(), scene.partial()).distanceTo(vision.packet().body().add(0, 1.62, 0));
        ring(matrix, aura, center.add(0, 0.07, 0), new Ring(0.50 + glide * 0.016, scene.steps(), intensity * 0.57f));
        if (center.lengthSqr() <= 21 * 21) {
            Vec3 ghost = interpolate(vision, scene.now(), scene.partial()).subtract(scene.camera()).add(0, -1.37, 0);
            ring(matrix, aura, ghost, new Ring(0.42, scene.steps(), intensity * 0.39f));
        }
    }

    private static void renderAvatar(Minecraft game, AbstractClientPlayer actor, PoseStack stack, MultiBufferSource buffers,
                                     Vision vision, Scene scene) {
        Vec3 at = interpolate(vision, scene.now(), scene.partial()).subtract(scene.camera());
        if (at.lengthSqr() > 42 * 42 || at.lengthSqr() < 0.25) {
            return;
        }
        prepareModel(game);
        var puppet = "slim".equals(actor.getModelName()) ? slim : model;
        float t = actor.tickCount + scene.partial();
        float alpha = intensity(vision, scene.now(), scene.partial()) * 0.46f;
        setPose(puppet, t);
        stack.pushPose();
        stack.translate(at.x, at.y - 0.17, at.z);
        stack.mulPose(Axis.YP.rotationDegrees(180 - vision.packet().yaw()));
        stack.scale(-1, -1, 1);
        var material = RenderType.entityTranslucent(actor.getSkinTextureLocation());
        puppet.renderToBuffer(stack, buffers.getBuffer(material), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                0.65f, 0.87f, 0.91f, alpha);
        stack.popPose();
    }

    private static void prepareModel(Minecraft game) {
        if (modelSet != game.getEntityModels()) {
            modelSet = game.getEntityModels();
            model = new PlayerModel<>(modelSet.bakeLayer(ModelLayers.PLAYER), false);
            slim = new PlayerModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        }
    }

    private static void setPose(PlayerModel<AbstractClientPlayer> puppet, float time) {
        puppet.rightArm.xRot = -0.18f + Mth.sin(time * 0.20f) * 0.11f;
        puppet.leftArm.xRot = -0.17f - Mth.sin(time * 0.20f) * 0.11f;
        puppet.rightArm.zRot = 0.12f;
        puppet.leftArm.zRot = -0.12f;
        puppet.rightLeg.xRot = Mth.sin(time * 0.16f) * 0.13f;
        puppet.leftLeg.xRot = -puppet.rightLeg.xRot;
        puppet.rightSleeve.copyFrom(puppet.rightArm);
        puppet.leftSleeve.copyFrom(puppet.leftArm);
        puppet.rightPants.copyFrom(puppet.rightLeg);
        puppet.leftPants.copyFrom(puppet.leftLeg);
    }

    @SubscribeEvent
    public static void hud(RenderGuiOverlayEvent.Post event) {
        Minecraft game = Minecraft.getInstance();
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type() || game.options.hideGui || liveVision(game) == null) {
            return;
        }
        Vision own = liveVision(game);
        var gui = event.getGuiGraphics();
        int mid = gui.guiWidth() / 2;
        int y = gui.guiHeight() / 2 + 25;
        double range = own.packet().eye().distanceTo(own.packet().body().add(0, 1.62, 0));
        float fraction = (float) Mth.clamp(1 - range / SpiritProjectionPath.RANGE, 0, 1);
        gui.drawCenteredString(game.font, Component.translatable("narutomod.overlay.spirit_projection"), mid, y, 0xA9D9D6);
        gui.fill(mid - 26, y + 10, mid + 26, y + 12, 0xAA2F4149);
        gui.fill(mid - 26, y + 10, mid - 26 + Math.round(52 * fraction), y + 12, 0xCC8CBAB5);
    }

    private static void setCamera(Minecraft game, Vec3 targetEye) {
        if (cameraProxy == null) {
            if (game.getCameraEntity() != game.player) {
                return;
            }
            cameraProxy = new ArmorStand(EntityType.ARMOR_STAND, game.level);
            cameraProxy.setInvisible(true);
            cameraProxy.setNoGravity(true);
            cameraProxy.moveTo(targetEye.subtract(0, cameraProxy.getEyeHeight(), 0));
            game.setCameraEntity(cameraProxy);
        }
        if (game.getCameraEntity() != cameraProxy) {
            return;
        }
        cameraProxy.xo = cameraProxy.getX();
        cameraProxy.yo = cameraProxy.getY();
        cameraProxy.zo = cameraProxy.getZ();
        cameraProxy.yRotO = cameraProxy.getYRot();
        cameraProxy.xRotO = cameraProxy.getXRot();
        cameraProxy.setYRot(game.player.getYRot());
        cameraProxy.setXRot(game.player.getXRot());
        cameraProxy.setPos(targetEye.x, targetEye.y - cameraProxy.getEyeHeight(), targetEye.z);
    }

    private static void releaseCamera(Minecraft game) {
        if (cameraProxy != null) {
            if (game.getCameraEntity() == cameraProxy) {
                game.setCameraEntity(game.player);
            }
            cameraProxy = null;
        }
    }

    private static Vision liveVision(Minecraft game) {
        if (game.player == null || game.level == null || game.level != world.get()) {
            return null;
        }
        Vision own = VISIONS.get(game.player.getId());
        return own == null || own.packet().state() == State.RELEASE ? null : own;
    }

    private static int expiry(Vision vision) {
        return vision.packet().state() == State.RELEASE ? SpiritProjectionPath.RETURN : 12;
    }

    private static Vec3 interpolate(Vision vision, long now, float partial) {
        var packet = vision.packet();
        float passed = now - vision.received() + partial;
        if (packet.state() == State.RELEASE) {
            float t = Mth.clamp(passed / SpiritProjectionPath.RETURN, 0, 1);
            return packet.eye().lerp(packet.body().add(0, 1.62, 0), t * t);
        }
        return vision.from().lerp(packet.eye(), Mth.clamp(passed / 2.0, 0, 1));
    }

    private static float intensity(Vision vision, long now, float partial) {
        float passed = now - vision.received() + partial;
        return switch (vision.packet().state()) {
            case GATHER -> Mth.clamp(passed / SpiritProjectionPath.OPEN, 0, 1) * 0.73f;
            case DRIFT -> 0.86f;
            case RELEASE -> Mth.clamp(1 - passed / SpiritProjectionPath.RETURN, 0, 1) * 0.82f;
        };
    }

    private static void ring(Matrix4f matrix, VertexConsumer mesh, Vec3 center, Ring ring) {
        for (int i = 0; i < ring.steps(); i++) {
            double a = Math.PI * 2 * i / ring.steps();
            double b = Math.PI * 2 * (i + 1) / ring.steps();
            Vec3 u = new Vec3(Math.cos(a), 0, Math.sin(a));
            Vec3 v = new Vec3(Math.cos(b), 0, Math.sin(b));
            vertex(matrix, mesh, center.add(u.scale(ring.radius())), ring.alpha());
            vertex(matrix, mesh, center.add(v.scale(ring.radius())), ring.alpha());
            vertex(matrix, mesh, center.add(v.scale(ring.radius() * 1.13)).add(0, 0.015, 0), 0);
            vertex(matrix, mesh, center.add(u.scale(ring.radius() * 1.13)).add(0, 0.015, 0), 0);
        }
    }

    private static void vertex(Matrix4f matrix, VertexConsumer mesh, Vec3 point, float alpha) {
        mesh.vertex(matrix, (float) point.x, (float) point.y, (float) point.z).color(0.64f, 0.81f, 0.83f, alpha).endVertex();
    }

    private static void useLevel(Minecraft game, ClientLevel level) {
        if (world.get() != level) {
            releaseCamera(game);
            VISIONS.clear();
            world = new WeakReference<>(level);
        }
    }

    private record Vision(SpiritProjectionPacket packet, Vec3 from, long received) {
    }

    private record Scene(Vec3 camera, long now, float partial, int steps) {
    }

    private record Ring(double radius, int steps, float alpha) {
    }
}
