package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.KikaichuPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.JutsuVfx;
import com.sekwah.narutomod.util.KamuiStep;
import com.sekwah.narutomod.util.KikaichuFlight;
import com.sekwah.narutomod.util.KikaichuFlight.Stage;
import com.sekwah.narutomod.util.WaterDragonFlight;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KikaichuEvents {
    private static final Map<Player, Swarm> ACTIVE = new WeakHashMap<>();
    private static final Map<Player, Long> RETRY = new WeakHashMap<>();
    private static int serial;

    private KikaichuEvents() {
    }

    public static boolean queue(Player player) {
        if (!(player.level() instanceof ServerLevel level) || !suitable(player) || ACTIVE.containsKey(player)
                || level.getGameTime() < RETRY.getOrDefault(player, Long.MIN_VALUE)) {
            return false;
        }
        LivingEntity target = JutsuAim.select(player, KikaichuFlight.RANGE, 22, candidate -> true);
        if (target == null) {
            return false;
        }
        ACTIVE.put(player, new Swarm(level, player, ++serial, target));
        return true;
    }

    public static boolean canMaintain(Player player, INinjaData data) {
        Swarm swarm = ACTIVE.get(player);
        return swarm != null && suitable(player) && player.level() == swarm.level && data.isNinjaModeEnabled()
                && (swarm.paid || data.getChakra() >= KikaichuFlight.COST);
    }

    public static void update(Player player, INinjaData data) {
        Swarm swarm = ACTIVE.get(player);
        if (swarm == null) {
            return;
        }
        long now = swarm.level.getGameTime();
        swarm.last = now;
        if (!swarm.paid) {
            data.useChakra(KikaichuFlight.COST, 30);
            swarm.paid = true;
            swarm.begin = now;
            publish(player, swarm, Stage.GATHER, swarm.position);
        }
        switch (swarm.phase) {
            case GATHER -> gather(player, swarm, now);
            case SEEK -> seek(player, data, swarm, now);
            case FEED -> dwell(player, swarm, now);
            case RETURN -> collect(player, data, swarm, now);
            default -> {
            }
        }
    }

    private static void gather(Player actor, Swarm swarm, long now) {
        LivingEntity target = swarm.target.get();
        if (!validTarget(actor, target) || !actor.hasLineOfSight(target)) {
            close(actor, swarm, false);
            return;
        }
        swarm.position = sleeve(actor);
        if (now - swarm.begin >= KikaichuFlight.GATHER) {
            swarm.phase = Stage.SEEK;
            swarm.begin = now;
            publish(actor, swarm, Stage.SEEK, swarm.position);
            tone(swarm, NarutoSounds.KIKAICHU_LAUNCH.get(), 0.66f);
        }
    }

    private static void seek(Player actor, INinjaData data, Swarm swarm, long now) {
        LivingEntity target = swarm.target.get();
        if (!validTarget(actor, target) || now - swarm.begin >= KikaichuFlight.MAX_LEG_TICKS) {
            close(actor, swarm, false);
            return;
        }
        Vec3 center = JutsuAim.center(target);
        Vec3 strike = WaterDragonFlight.nearestPoint(target.getBoundingBox().inflate(0.08), swarm.position.lerp(center, 0.75));
        if (!advance(actor, swarm, strike, false)) {
            return;
        }
        if (KikaichuFlight.reached(swarm.position, strike)) {
            feed(actor, data, swarm, target, now);
        } else if ((now - swarm.begin) % 9 == 0) {
            tone(swarm, NarutoSounds.KIKAICHU_LAUNCH.get(), 0.39f);
        }
    }

    private static void feed(Player actor, INinjaData ownerData, Swarm swarm, LivingEntity target, long now) {
        float damage = Math.min(2.2f * ownerData.getRankDamageMultiplier(), target instanceof Player ? 2.6f : 5f);
        if (!target.hurt(actor.damageSources().indirectMagic(actor, actor), damage)) {
            close(actor, swarm, false);
            return;
        }
        if (target.isAlive()) {
            target.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                float before = data.getChakra();
                data.useChakra(Math.min(before, KikaichuFlight.MAX_DRAIN), 20);
                swarm.cargo.collect(before, data.getChakra());
            });
        }
        swarm.phase = Stage.FEED;
        swarm.begin = now;
        publish(actor, swarm, Stage.FEED, swarm.position);
        tone(swarm, NarutoSounds.KIKAICHU_LAUNCH.get(), 0.54f);
    }

    private static void dwell(Player actor, Swarm swarm, long now) {
        LivingEntity target = swarm.target.get();
        if (!validTarget(actor, target) || now - swarm.begin >= KikaichuFlight.FEED
                || swarm.position.distanceToSqr(JutsuAim.center(target)) > 2.2 * 2.2) {
            swarm.phase = Stage.RETURN;
            swarm.begin = now;
            publish(actor, swarm, Stage.RETURN, swarm.position);
        }
    }

    private static void collect(Player actor, INinjaData data, Swarm swarm, long now) {
        Vec3 home = sleeve(actor);
        if (now - swarm.begin >= KikaichuFlight.MAX_LEG_TICKS
                || home.distanceToSqr(swarm.position) > KikaichuFlight.RETURN_DISTANCE * KikaichuFlight.RETURN_DISTANCE) {
            close(actor, swarm, false);
            return;
        }
        if (advance(actor, swarm, home, true) && KikaichuFlight.reached(swarm.position, home)) {
            float returned = swarm.cargo.deliver(true);
            if (returned > 0) {
                data.addChakra(returned);
            }
            tone(swarm, NarutoSounds.KIKAICHU_RETURN.get(), 0.61f);
            close(actor, swarm, true);
        }
    }

    private static boolean advance(Player actor, Swarm swarm, Vec3 goal, boolean returning) {
        Vec3 prev = swarm.position;
        Vec3 next = KikaichuFlight.advance(prev, goal, returning);
        if (!clear(swarm.level, actor, prev, next)) {
            close(actor, swarm, false);
            return false;
        }
        swarm.position = next;
        if ((swarm.level.getGameTime() - swarm.begin) % 2 == 0) {
            publish(actor, swarm, swarm.phase, prev);
        }
        return true;
    }

    private static boolean clear(ServerLevel level, Player actor, Vec3 from, Vec3 to) {
        var box = new AABB(from, to).inflate(KikaichuFlight.RADIUS);
        var border = level.getWorldBorder();
        if (!level.hasChunkAt(BlockPos.containing(box.minX, box.minY, box.minZ))
                || !level.hasChunkAt(BlockPos.containing(box.maxX, box.minY, box.maxZ))
                || !level.hasChunkAt(BlockPos.containing(box.minX, box.minY, box.maxZ))
                || !level.hasChunkAt(BlockPos.containing(box.maxX, box.minY, box.minZ))
                || !KamuiStep.insideBorder(box, border.getMinX(), border.getMinZ(), border.getMaxX(), border.getMaxZ())) {
            return false;
        }
        for (var shape : level.getBlockCollisions(actor, box)) {
            for (AABB block : shape.toAabbs()) {
                if (WaterDragonFlight.sweep(from, to, block, KikaichuFlight.RADIUS) != null) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean suitable(Player actor) {
        return actor.isAlive() && !actor.isSpectator() && !actor.noPhysics && !actor.isInWaterOrBubble();
    }

    private static boolean validTarget(Player actor, @Nullable LivingEntity target) {
        return target != null && JutsuAim.attackable(actor, target) && actor.distanceToSqr(target) <= 11 * 11;
    }

    private static Vec3 sleeve(Player actor) {
        return JutsuVfx.handPosition(actor, actor.getMainArm() == HumanoidArm.RIGHT ? 1 : -1, 1).add(0, -0.16, 0);
    }

    public static void end(Player player) {
        Swarm swarm = ACTIVE.get(player);
        if (swarm != null) {
            close(player, swarm, false);
        }
    }

    private static void close(Player actor, Swarm swarm, boolean home) {
        swarm.cargo.deliver(false);
        if (ACTIVE.remove(actor, swarm)) {
            RETRY.put(actor, swarm.level.getGameTime() + 27);
            if (swarm.paid) {
                publish(actor, swarm, home ? Stage.HOME : Stage.DISSOLVE, swarm.position);
            }
        }
    }

    @SubscribeEvent
    public static void clean(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player actor) || actor.level().isClientSide) {
            return;
        }
        Swarm swarm = ACTIVE.get(actor);
        if (swarm != null && (!suitable(actor) || actor.level() != swarm.level || swarm.level.getGameTime() - swarm.last > 2)) {
            close(actor, swarm, false);
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Player actor) {
            end(actor);
        }
    }

    private static void tone(Swarm swarm, net.minecraft.sounds.SoundEvent event, float volume) {
        Vec3 p = swarm.position;
        swarm.level.playSound(null, p.x, p.y, p.z, event, SoundSource.PLAYERS, volume, 1);
    }

    private static void publish(Player actor, Swarm swarm, Stage phase, Vec3 previous) {
        PacketHandler.sendNear(new KikaichuPacket(swarm.level.dimension().location(), swarm.id, actor.getId(), phase,
                previous, swarm.position, swarm.cargo.glowing()), swarm.level, swarm.position, 32);
    }

    private static final class Swarm {
        private final ServerLevel level;
        private final int id;
        private final WeakReference<LivingEntity> target;
        private final KikaichuFlight.Cargo cargo = new KikaichuFlight.Cargo();
        private Vec3 position;
        private Stage phase = Stage.GATHER;
        private boolean paid;
        private long begin;
        private long last;

        private Swarm(ServerLevel level, Player actor, int id, LivingEntity target) {
            this.level = level;
            this.id = id;
            this.target = new WeakReference<>(target);
            this.position = sleeve(actor);
            this.last = level.getGameTime();
        }
    }
}
