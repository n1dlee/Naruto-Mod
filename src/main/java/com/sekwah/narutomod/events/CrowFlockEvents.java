package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.CrowFlockPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.CrowFlock;
import com.sekwah.narutomod.util.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CrowFlockEvents {
    private static final int WORLD_BUDGET = 24;
    private static final Map<ServerLevel, List<Flock>> WAVES = new WeakHashMap<>();
    private static int serial;

    private CrowFlockEvents() {
    }

    public static boolean hasSlot(LivingEntity actor) {
        return actor.level() instanceof ServerLevel level && WAVES.getOrDefault(level, List.of()).size() < WORLD_BUDGET;
    }

    public static void start(LivingEntity actor, float yaw) {
        if (!(actor.level() instanceof ServerLevel level) || !actor.isAlive() || actor.isSpectator() || !hasSlot(actor)) {
            return;
        }
        Vec3 origin = actor.position().add(0, actor.getBbHeight() * 0.72, 0);
        double[] limits = new double[CrowFlock.LANES];
        Arrays.fill(limits, CrowFlock.RANGE + 0.08);
        var flock = new Flock(++serial, new WeakReference<>(actor), origin, yaw, level.getGameTime(), limits, new HashSet<>());
        trim(level, actor, flock, CrowFlock.RANGE + 0.08);
        WAVES.computeIfAbsent(level, ignored -> new ArrayList<>()).add(flock);
        sync(level, flock);
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        var flocks = WAVES.get(level);
        if (flocks == null) {
            return;
        }
        flocks.removeIf(flock -> advance(level, flock));
        if (flocks.isEmpty()) {
            WAVES.remove(level);
        }
    }

    private static boolean advance(ServerLevel level, Flock flock) {
        LivingEntity actor = flock.owner().get();
        long age = level.getGameTime() - flock.start();
        if (actor == null || actor.isRemoved() || !actor.isAlive() || actor.isSpectator() || actor.level() != level
                || age > CrowFlock.GATHER + CrowFlock.FLIGHT) {
            return true;
        }
        if (age == CrowFlock.GATHER) {
            Vec3 at = flock.origin();
            level.playSound(null, at.x, at.y, at.z, NarutoSounds.CROW_FLOCK_WINGS.get(), SoundSource.PLAYERS, 0.86f, 1.0f);
        }
        if (age > CrowFlock.GATHER) {
            double progress = CrowFlock.travel(age);
            if (trim(level, actor, flock, progress)) {
                sync(level, flock);
            }
            catchTargets(level, actor, flock, CrowFlock.travel(age - 1), progress);
        }
        return false;
    }

    private static boolean trim(ServerLevel level, LivingEntity actor, Flock flock, double progress) {
        boolean changed = false;
        for (int lane = 0; lane < CrowFlock.LANES; lane++) {
            double previous = flock.limits()[lane];
            Vec3 dir = CrowFlock.heading(flock.yaw(), lane);
            double reach = Math.min(previous, progress);
            reach = loadedDistance(level, flock.origin(), dir, reach);
            Vec3 end = flock.origin().add(dir.scale(reach));
            var clip = level.clip(new ClipContext(flock.origin(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, actor));
            if (clip.getType() != HitResult.Type.MISS) {
                reach = flock.origin().distanceTo(clip.getLocation());
                flock.limits()[lane] = Math.min(previous, reach);
            } else if (reach < Math.min(previous, progress) - 0.001) {
                flock.limits()[lane] = reach;
            }
            changed |= Math.floor(previous * 100) != Math.floor(flock.limits()[lane] * 100);
        }
        return changed;
    }

    private static double loadedDistance(ServerLevel level, Vec3 origin, Vec3 heading, double distance) {
        int steps = Math.max(1, (int) Math.ceil(distance));
        for (int i = 1; i <= steps; i++) {
            BlockPos point = BlockPos.containing(origin.add(heading.scale(distance * i / steps)));
            if (!level.hasChunkAt(point) || !level.getWorldBorder().isWithinBounds(point)) {
                return distance * (i - 1) / steps;
            }
        }
        return distance;
    }

    private static void catchTargets(ServerLevel level, LivingEntity actor, Flock flock, double prior, double next) {
        Vec3 direction = CrowFlock.heading(flock.yaw(), CrowFlock.LANES / 2);
        AABB search = new AABB(flock.origin(), flock.origin().add(direction.scale(next))).inflate(next * 0.56 + 0.4, 1.2, next * 0.56 + 0.4);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search, candidate -> hostile(actor, candidate))) {
            if (!flock.hit().contains(target.getId()) && crosses(flock, target, prior, next)
                    && visible(level, actor, flock.origin(), target)) {
                flock.hit().add(target.getId());
                boolean affected = target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        36, 0, false, false, true), actor);
                if (affected && target instanceof ServerPlayer player) {
                    PacketHandler.sendToPlayer(new CrowFlockPacket(level.dimension().location(), flock.id(), player.getId(),
                            flock.origin(), flock.yaw(), 0, List.of()), player);
                }
            }
        }
    }

    private static boolean visible(ServerLevel level, LivingEntity actor, Vec3 from, LivingEntity target) {
        var hit = level.clip(new ClipContext(from, target.getBoundingBox().getCenter(), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, actor));
        return hit.getType() == HitResult.Type.MISS;
    }

    private static boolean crosses(Flock flock, LivingEntity target, double prior, double next) {
        for (int lane = 0; lane < CrowFlock.LANES; lane++) {
            if (CrowFlock.touches(target.getBoundingBox(), flock.origin(), CrowFlock.heading(flock.yaw(), lane),
                    prior, next, flock.limits()[lane])) {
                return true;
            }
        }
        return false;
    }

    private static boolean hostile(LivingEntity actor, LivingEntity candidate) {
        return candidate.isAlive() && !candidate.isSpectator() && !actor.isAlliedTo(candidate) && !Faction.sameSide(actor, candidate)
                && (!(candidate instanceof Player player) || !player.isCreative()
                && (!(actor instanceof Player caster) || caster.canHarmPlayer(player)));
    }

    private static void sync(ServerLevel level, Flock flock) {
        var limits = Arrays.stream(flock.limits()).mapToObj(value -> (int) Math.floor(value * 100)).toList();
        int age = (int) (level.getGameTime() - flock.start());
        PacketHandler.sendNear(new CrowFlockPacket(level.dimension().location(), flock.id(), 0, flock.origin(), flock.yaw(), age, limits),
                level, flock.origin(), 42);
    }

    private record Flock(int id, WeakReference<LivingEntity> owner, Vec3 origin, float yaw, long start, double[] limits,
                         Set<Integer> hit) {
    }
}
