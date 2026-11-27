package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.util.SurfaceStride;
import com.sekwah.narutomod.util.SurfaceSupport;
import com.sekwah.narutomod.util.SurfaceWalkFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SurfaceWalkRenderer {
    private static final float TRANSITION = 0.37f;
    private static final int RIPPLE_DURATION = 13;
    private static final int RIPPLE_LIMIT = 32;
    private static final Map<Player, Frame> FRAMES = new WeakHashMap<>();
    private static final ArrayDeque<Ripple> RIPPLES = new ArrayDeque<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private SurfaceWalkRenderer() {
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || minecraft.isPaused()) {
            return;
        }
        ClientLevel level = minecraft.level;
        if (world.get() != level) {
            FRAMES.clear();
            RIPPLES.clear();
            world = new WeakReference<>(level);
        }
        if (level == null) {
            return;
        }
        RIPPLES.removeIf(ripple -> level.getGameTime() - ripple.started() >= RIPPLE_DURATION);
        for (Player player : level.players()) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> tickPlayer(level, player, data));
        }
    }

    private static void tickPlayer(ClientLevel level, Player player, INinjaData data) {
        Direction face = data.isWallWalkAttached() ? data.getWallWalkDirection() : null;
        boolean onWater = face == null && !player.isSpectator()
                && data.getToggleAbilityData().getAbilitiesHashSet().contains(NarutoAbilities.WATER_WALK.getId())
                && SurfaceSupport.waterAtFeet(player, player.position()) != null;
        SurfaceStride stride = data.getSurfaceStride();
        int foot = stride.advance(player.position(), onWater ? Direction.DOWN : face);
        if (onWater && foot != 0) {
            Vec3 forward = stride.forward();
            Vec3 position = player.position().add(forward.scale(0.14))
                    .add(forward.cross(new Vec3(0, 1, 0)).scale(foot * 0.14));
            Vec3 point = SurfaceSupport.waterAtFeet(player, position);
            if (point != null) {
                if (RIPPLES.size() == RIPPLE_LIMIT) {
                    RIPPLES.removeFirst();
                }
                RIPPLES.addLast(new Ripple(point, level.getGameTime()));
            }
        }
        Frame frame = FRAMES.get(player);
        if (face != null || frame != null) {
            FRAMES.computeIfAbsent(player, key -> new Frame()).tick(player, face, stride.forward());
        }
    }

    public static void orient(Player player, PoseStack stack, float partialTick) {
        Frame frame = FRAMES.get(player);
        if (frame == null || frame.weight < 0.001f && frame.lastWeight < 0.001f) {
            return;
        }
        Vec3 pivot = frame.lastPivot.lerp(frame.pivot, partialTick);
        stack.translate(pivot.x, pivot.y, pivot.z);
        stack.mulPose(new Quaternionf(frame.lastRotation).slerp(frame.rotation, partialTick));
    }

    @SubscribeEvent
    public static void bankCamera(ViewportEvent.ComputeCameraAngles event) {
        if (event.getCamera().getEntity() instanceof Player player) {
            Frame frame = FRAMES.get(player);
            if (frame != null) {
                event.setRoll(event.getRoll() + Mth.lerp((float) event.getPartialTick(), frame.lastRoll, frame.roll));
            }
        }
    }

    static void drawRipples(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers,
                            Vec3 camera, float partialTick) {
        if (level != world.get() || RIPPLES.isEmpty()) {
            return;
        }
        int segments = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST ? 12 : 20;
        var consumer = buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE);
        for (Ripple ring : RIPPLES) {
            double t = (level.getGameTime() - ring.started() + partialTick) / RIPPLE_DURATION;
            if (t >= 1 || ring.point().distanceToSqr(camera) > 40 * 40) {
                continue;
            }
            Vec3 center = ring.point().subtract(camera);
            drawRing(matrix, consumer, center, segments, t, 0.42f);
            if (t > 0.18) {
                drawRing(matrix, consumer, center, segments, t - 0.18, 0.25f);
            }
        }
        buffers.endBatch(NarutoRenderTypes.WATER_SURFACE);
    }

    private static void drawRing(Matrix4f matrix, com.mojang.blaze3d.vertex.VertexConsumer consumer,
                                 Vec3 center, int segments, double t, float strength) {
        float alpha = (float) (1.0 - t) * strength;
        double radius = 0.12 + 0.72 * t;
        double width = 0.034 * (1.0 - t) + 0.01;
        for (int i = 0; i < segments; i++) {
            double a = i * Mth.TWO_PI / segments;
            double b = (i + 1) * Mth.TWO_PI / segments;
            Vec3 first = new Vec3(Math.cos(a), 0, Math.sin(a));
            Vec3 second = new Vec3(Math.cos(b), 0, Math.sin(b));
            WaterDragonImpactRenderer.vertex(consumer, matrix, center.add(first.scale(radius)), 0.8f, 0.98f, 1.0f, alpha);
            WaterDragonImpactRenderer.vertex(consumer, matrix, center.add(second.scale(radius)), 0.8f, 0.98f, 1.0f, alpha);
            WaterDragonImpactRenderer.vertex(consumer, matrix, center.add(second.scale(radius + width)), 0.24f, 0.74f, 1.0f, 0.0f);
            WaterDragonImpactRenderer.vertex(consumer, matrix, center.add(first.scale(radius + width)), 0.24f, 0.74f, 1.0f, 0.0f);
        }
    }

    private static final class Frame {
        private final Quaternionf rotation = new Quaternionf();
        private final Quaternionf lastRotation = new Quaternionf();
        private Vec3 pivot = Vec3.ZERO;
        private Vec3 lastPivot = Vec3.ZERO;
        private float roll;
        private float lastRoll;
        private float weight;
        private float lastWeight;

        private void tick(Player player, @Nullable Direction face, Vec3 forward) {
            this.lastPivot = this.pivot;
            this.lastRotation.set(this.rotation);
            this.lastRoll = this.roll;
            this.lastWeight = this.weight;
            if (face == null || face == Direction.DOWN) {
                this.blend(new Quaternionf(), Vec3.ZERO, 0, 0);
                return;
            }
            var support = SurfaceSupport.find(player, face, SurfaceSupport.GRIP_REACH);
            Vec3 contact = SurfaceSupport.pivot(player, face, support);
            Vec3 heading = forward.lengthSqr() > 1.0e-6 ? forward : player.getLookAngle();
            this.blend(SurfaceWalkFrame.orientation(face, player.yBodyRot, heading), contact,
                    SurfaceWalkFrame.cameraRoll(face, player.getYRot(), NarutoConfig.wallWalkCameraBank), 1);
        }

        private void blend(Quaternionf target, Vec3 contact, float targetRoll, float targetWeight) {
            this.pivot = this.pivot.lerp(contact, TRANSITION);
            this.rotation.slerp(target, TRANSITION);
            this.roll = Mth.lerp(TRANSITION, this.roll, targetRoll);
            this.weight = Mth.lerp(TRANSITION, this.weight, targetWeight);
        }
    }

    private record Ripple(Vec3 point, long started) {
    }
}
