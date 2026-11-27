package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.LaserCircusPacket;
import com.sekwah.narutomod.network.s2c.LaserCircusPacket.Stage;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.JutsuVfx;
import com.sekwah.narutomod.util.LaserCircusPath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LaserCircusEvents {
    public static final float COST = 48;
    private static final double CONE = Math.cos(Math.toRadians(31));
    private static final Map<ServerPlayer, Cast> CASTS = new WeakHashMap<>();

    private LaserCircusEvents() {
    }

    public static boolean start(ServerPlayer actor, INinjaData data, BiConsumer<ServerPlayer, INinjaData> success) {
        if (!available(actor) || CASTS.containsKey(actor) || data.getChakra() < COST) {
            return false;
        }
        ServerLevel level = actor.serverLevel();
        var cast = new Cast(level, level.getGameTime(), actor.position(), actor.getLookAngle(),
                new Receipt(data, data.getRankDamageMultiplier(), success));
        data.useChakra(COST, 25);
        data.setCastPoseTicks(LaserCircusPath.GATHER);
        CASTS.put(actor, cast);
        Vec3 hand = hand(actor);
        send(actor, cast, Stage.GATHER, List.of(hand));
        tone(level, hand, NarutoSounds.LASER_CIRCUS_FOCUS.get(), 0.72f);
        return true;
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer actor)) {
            return;
        }
        Cast cast = CASTS.get(actor);
        if (cast == null) {
            return;
        }
        if (!available(actor) || cast.level() != actor.level() || !cast.receipt().data().isNinjaModeEnabled()
                || actor.position().distanceToSqr(cast.origin()) > 3.3 * 3.3) {
            cancel(actor, cast);
        } else if (cast.level().getGameTime() - cast.start() >= LaserCircusPath.GATHER) {
            CASTS.remove(actor);
            release(actor, cast);
            cast.receipt().success().accept(actor, cast.receipt().data());
        }
    }

    private static void release(ServerPlayer actor, Cast cast) {
        Vec3 start = hand(actor);
        var targets = targets(actor, cast, start);
        var struck = new HashSet<UUID>();
        for (int lane = 0; lane < LaserCircusPath.BRANCHES; lane++) {
            Vec3 end = lane < targets.size() ? JutsuAim.center(targets.get(lane)) : LaserCircusPath.endpoint(start, cast.axis(), lane);
            var path = LaserCircusPath.route(start, end, lane);
            var result = LaserCircusTrace.trace(cast.level(), actor, path);
            var target = result.target();
            Stage stage = result.blocked() ? Stage.BLOCK : Stage.BEAM;
            if (target != null && struck.add(target.getUUID())) {
                float damage = Math.min(6.5f * cast.receipt().rank(), target instanceof ServerPlayer ? 6.7f : 13);
                if (target.hurt(actor.damageSources().playerAttack(actor), damage)) {
                    stage = Stage.HIT;
                }
            }
            send(actor, cast, stage, result.points());
        }
        tone(cast.level(), start, NarutoSounds.LASER_CIRCUS_CAST.get(), 0.85f);
    }

    private static List<LivingEntity> targets(ServerPlayer actor, Cast cast, Vec3 start) {
        return cast.level().getEntitiesOfClass(LivingEntity.class, actor.getBoundingBox().inflate(LaserCircusPath.RANGE),
                        target -> JutsuAim.attackable(actor, target) && actor.hasLineOfSight(target)
                                && JutsuAim.aimed(start, cast.axis(), JutsuAim.center(target), LaserCircusPath.RANGE, CONE))
                .stream().sorted(Comparator.comparingDouble((LivingEntity t) -> -JutsuAim.center(t).subtract(start).normalize().dot(cast.axis()))
                        .thenComparingDouble(actor::distanceToSqr).thenComparing(LivingEntity::getUUID))
                .limit(LaserCircusPath.BRANCHES).toList();
    }

    private static boolean available(ServerPlayer actor) {
        if (!actor.isAlive() || actor.isSpectator() || actor.noPhysics) {
            return false;
        }
        Vec3 from = actor.getEyePosition();
        return actor.level().clip(new ClipContext(from, hand(actor), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, actor)).getType() == HitResult.Type.MISS;
    }

    private static Vec3 hand(ServerPlayer actor) {
        return JutsuVfx.handPosition(actor, actor.getMainArm() == HumanoidArm.RIGHT ? 1 : -1, 1);
    }

    private static void tone(ServerLevel level, Vec3 at, SoundEvent sound, float volume) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, 0.97f);
    }

    private static void send(ServerPlayer actor, Cast cast, Stage stage, List<Vec3> points) {
        PacketHandler.sendNear(new LaserCircusPacket(cast.level().dimension().location(), actor.getId(), stage, points),
                cast.level(), points.get(0).lerp(points.get(points.size() - 1), 0.43), 38);
    }

    private static void cancel(ServerPlayer actor, Cast cast) {
        if (CASTS.remove(actor, cast)) {
            cast.receipt().data().addChakra(COST);
            send(actor, cast, Stage.CANCEL, List.of(cast.origin()));
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer actor) {
            Cast cast = CASTS.get(actor);
            if (cast != null) {
                cancel(actor, cast);
            }
        }
    }

    private record Cast(ServerLevel level, long start, Vec3 origin, Vec3 axis, Receipt receipt) {
    }

    private record Receipt(INinjaData data, float rank, BiConsumer<ServerPlayer, INinjaData> success) {
    }
}
