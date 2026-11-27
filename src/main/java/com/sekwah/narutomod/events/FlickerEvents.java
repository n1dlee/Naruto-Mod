package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.FlickerPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.FlickerPath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FlickerEvents {
    public static final int WINDUP = 3;
    private static final Map<LivingEntity, Step> PENDING = new WeakHashMap<>();

    private FlickerEvents() {
    }

    public static boolean canStart(LivingEntity actor, Vec3 movement) {
        return actor.level() instanceof ServerLevel level && valid(actor) && !PENDING.containsKey(actor)
                && endpoint(level, actor, movement).distanceToSqr(actor.position())
                >= FlickerPath.MIN_MOVEMENT * FlickerPath.MIN_MOVEMENT;
    }

    private static boolean valid(LivingEntity actor) {
        return actor.isAlive() && !actor.isSpectator() && !actor.isPassenger() && !actor.isSleeping()
                && !actor.isFallFlying() && !actor.isNoGravity();
    }

    public static void start(LivingEntity actor, Vec3 movement, @Nullable Runnable onArrival) {
        if (!(actor.level() instanceof ServerLevel level) || !valid(actor) || PENDING.containsKey(actor)) {
            return;
        }
        int seed = level.random.nextInt();
        PENDING.put(actor, new Step(movement, level.getGameTime() + WINDUP, seed, onArrival));
        visual(level, actor, actor.position(), actor.position(), seed);
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        for (LivingEntity actor : List.copyOf(PENDING.keySet())) {
            Step step = PENDING.get(actor);
            if (step == null || actor.level() != level || level.getGameTime() < step.release()) {
                continue;
            }
            PENDING.remove(actor);
            if (valid(actor)) {
                release(level, actor, step);
            }
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity actor) {
            PENDING.remove(actor);
        }
    }

    private static Vec3 endpoint(ServerLevel level, LivingEntity actor, Vec3 movement) {
        Vec3 from = actor.position();
        Vec3 to = from.add(movement);
        AABB actorBounds = actor.getBoundingBox();
        double length = movement.length();
        if (length < FlickerPath.MIN_MOVEMENT) {
            return from;
        }
        double limit = traversable(level, actorBounds, movement);
        AABB corridor = actorBounds.expandTowards(movement.scale(limit)).inflate(0.002);
        for (var shape : level.getBlockCollisions(actor, corridor)) {
            for (AABB block : shape.toAabbs()) {
                limit = Math.min(limit, FlickerPath.contact(from, to, block, actor.getBbWidth(), actor.getBbHeight()));
            }
        }
        double safe = limit < 1.0 ? Math.max(0, limit - 0.035 / length) : 1.0;
        for (int retreat = 0; retreat < 7 && safe > 0; retreat++, safe -= 0.17 / length) {
            Vec3 offset = movement.scale(safe);
            if (level.noCollision(actor, actorBounds.move(offset))) {
                return from.add(offset);
            }
        }
        return from;
    }

    private static double traversable(ServerLevel level, AABB bounds, Vec3 movement) {
        int samples = Math.max(1, (int) Math.ceil(movement.length()));
        for (int i = 1; i <= samples; i++) {
            var next = bounds.move(movement.scale(i / (double) samples));
            if (!worldContains(level, next)) {
                return (i - 1.0) / samples;
            }
        }
        return 1.0;
    }

    private static boolean worldContains(Level level, AABB bounds) {
        BlockPos min = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
        BlockPos max = BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ);
        return min.getY() >= level.getMinBuildHeight() && max.getY() < level.getMaxBuildHeight()
                && level.getWorldBorder().isWithinBounds(min) && level.getWorldBorder().isWithinBounds(max)
                && level.hasChunkAt(min) && level.hasChunkAt(max)
                && level.hasChunkAt(BlockPos.containing(bounds.minX, bounds.minY, bounds.maxZ))
                && level.hasChunkAt(BlockPos.containing(bounds.maxX, bounds.minY, bounds.minZ));
    }

    private static void release(ServerLevel level, LivingEntity actor, Step step) {
        Vec3 from = actor.position();
        Vec3 to = endpoint(level, actor, step.movement());
        if (from.distanceToSqr(to) < FlickerPath.MIN_MOVEMENT * FlickerPath.MIN_MOVEMENT) {
            return;
        }
        if (actor instanceof ServerPlayer player) {
            player.connection.teleport(to.x, to.y, to.z, actor.getYRot(), actor.getXRot());
        } else {
            actor.teleportTo(to.x, to.y, to.z);
        }
        if (actor.position().distanceToSqr(to) > 1.0e-6) {
            return;
        }
        visual(level, actor, from, actor.position(), step.seed());
        var surface = level.getBlockState(BlockPos.containing(from.add(0, -0.11, 0)));
        if (!surface.isAir() && !surface.getCollisionShape(level, BlockPos.containing(from.add(0, -0.11, 0))).isEmpty()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, surface),
                    from.x, from.y + 0.07, from.z, 6, 0.29, 0.04, 0.08, 0.09);
        }
        level.playSound(null, from.x, from.y, from.z, NarutoSounds.CHAKRA_DASH.get(), SoundSource.PLAYERS, 0.65f, 1.38f);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 0.55f, 1.25f);
        if (step.onArrival() != null) {
            step.onArrival().run();
        }
    }

    private static void visual(ServerLevel level, LivingEntity actor, Vec3 from, Vec3 to, int seed) {
        PacketHandler.sendNear(new FlickerPacket(level.dimension().location(), actor.getId(), from, to,
                actor.getBbHeight(), seed), level, from.lerp(to, 0.5), 32 + from.distanceTo(to) * 0.5);
    }

    private record Step(Vec3 movement, long release, int seed, @Nullable Runnable onArrival) {
    }
}
