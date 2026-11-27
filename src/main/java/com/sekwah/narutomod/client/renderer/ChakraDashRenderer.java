package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.util.DashStride;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChakraDashRenderer {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final int MAX_TICKS = 6;
    private static final Map<Player, Trace> TRACES = new WeakHashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);

    private ChakraDashRenderer() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || minecraft.isPaused()) {
            return;
        }
        ClientLevel level = minecraft.level;
        if (world.get() != level) {
            world = new WeakReference<>(level);
            TRACES.clear();
        }
        if (level == null) {
            return;
        }
        for (Player player : level.players()) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                boolean enabled = data.isNinjaModeEnabled() && !data.isWallWalkAttached() && !player.isSpectator()
                        && !player.isPassenger() && !player.isFallFlying() && !player.isSwimming()
                        && data.getToggleAbilityData().getAbilitiesHashSet().contains(NarutoAbilities.CHAKRA_DASH.getId());
                TRACES.computeIfAbsent(player, ignored -> new Trace()).tick(player.position(), level.getGameTime(), enabled);
            });
        }
    }

    public static float runWeight(Entity entity, float partialTick) {
        Trace trace = entity instanceof Player player ? TRACES.get(player) : null;
        if (trace == null || !trace.motion.moving()) {
            return 0;
        }
        return Mth.clamp((trace.motion.runTicks() - 0.5f + partialTick) / DashStride.LEAN_TICKS, 0, 1);
    }

    public static float brakeWeight(Entity entity, float partialTick) {
        Trace trace = entity instanceof Player player ? TRACES.get(player) : null;
        return trace == null ? 0 : Mth.clamp((trace.motion.brakeTicks() - partialTick) / DashStride.BRAKE_TICKS, 0, 1);
    }

    static void draw(ClientLevel level, Matrix4f matrix, MultiBufferSource.BufferSource buffers,
                     Vec3 camera, float partialTick) {
        if (level != world.get()) {
            return;
        }
        boolean low = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        VertexConsumer mesh = null;
        for (var entry : TRACES.entrySet()) {
            if (entry.getKey().position().distanceToSqr(camera) > 48.0 * 48.0) {
                continue;
            }
            Trace trace = entry.getValue();
            if (trace.segments.isEmpty()) {
                continue;
            }
            if (mesh == null) {
                mesh = buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW);
            }
            for (Segment segment : trace.segments) {
                float lifetime = level.getGameTime() - segment.tick() + partialTick;
                float fade = Mth.clamp(1f - lifetime / MAX_TICKS, 0, 1);
                drawSegment(matrix, mesh, segment, camera, fade);
                if (!low && (int) lifetime % 2 == 0 && lifetime >= 2 && lifetime < MAX_TICKS
                        && segment.to().distanceToSqr(camera) < 19 * 19 && segment.to().distanceToSqr(camera) > 1) {
                    silhouette(matrix, mesh, segment, camera, fade);
                }
            }
        }
        if (mesh != null) {
            buffers.endBatch(NarutoRenderTypes.CHAKRA_GLOW);
        }
    }

    private static void drawSegment(Matrix4f matrix, VertexConsumer mesh, Segment segment, Vec3 camera, float fade) {
        Vec3 a = segment.from().subtract(camera);
        Vec3 b = segment.to().subtract(camera);
        Vec3 side = segment.to().subtract(segment.from()).cross(UP).normalize();
        double strength = Math.min(segment.from().distanceTo(segment.to()) * 1.25, 1.0);
        Vec3 width = side.scale((0.08 + strength * 0.19) * fade);
        strip(matrix, mesh, a.add(0, 0.76, 0), b.add(0, 0.76, 0), width, fade * (float) (0.2 + strength * 0.5));
        for (int sign = -1; sign <= 1; sign += 2) {
            Vec3 bank = side.scale(sign * 0.22);
            strip(matrix, mesh, a.add(bank).add(0, 0.12, 0), b.add(bank).add(0, 0.14, 0),
                    UP.scale(0.039 + strength * 0.033), fade * 0.47f);
        }
    }

    private static void silhouette(Matrix4f matrix, VertexConsumer mesh, Segment segment, Vec3 camera, float fade) {
        Vec3 motion = segment.to().subtract(segment.from()).normalize();
        Vec3 coarseSide = motion.cross(UP).normalize();
        Vec3 hips = segment.to().subtract(camera).add(0, 0.66, 0);
        Vec3 shoulders = hips.add(motion.scale(0.24)).add(0, 0.61, 0);
        Vec3 head = shoulders.add(motion.scale(0.14)).add(0, 0.25, 0);
        strip(matrix, mesh, hips, shoulders, coarseSide.scale(0.27), fade * 0.19f);
        strip(matrix, mesh, shoulders, head, coarseSide.scale(0.14), fade * 0.19f);
    }

    private static void strip(Matrix4f matrix, VertexConsumer mesh, Vec3 a, Vec3 b, Vec3 w, float alpha) {
        vertex(matrix, mesh, a.subtract(w.scale(0.47)), 0.34f, 0.83f, alpha * 0.15f);
        vertex(matrix, mesh, a.add(w.scale(0.47)), 0.37f, 0.89f, alpha * 0.37f);
        vertex(matrix, mesh, b.add(w), 0.61f, 0.91f, alpha * 0.55f);
        vertex(matrix, mesh, b.subtract(w), 0.81f, 1.0f, alpha * 0.69f);
    }

    private static void vertex(Matrix4f matrix, VertexConsumer mesh, Vec3 p, float r, float g, float alpha) {
        mesh.vertex(matrix, (float) p.x, (float) p.y, (float) p.z).color(r, g, 0.92f, alpha).endVertex();
    }

    private static final class Trace {
        private final DashStride motion = new DashStride();
        private final ArrayDeque<Segment> segments = new ArrayDeque<>();

        private void tick(Vec3 point, long now, boolean enabled) {
            this.motion.update(point, enabled);
            this.segments.removeIf(segment -> now - segment.tick() >= MAX_TICKS);
            if (this.motion.reset()) {
                this.segments.clear();
            } else if (this.motion.moving()) {
                this.segments.addLast(new Segment(point.subtract(this.motion.step()), point, now));
            }
        }
    }

    private record Segment(Vec3 from, Vec3 to, long tick) {
    }
}
