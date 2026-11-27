package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.KuramaCloakPacket;
import com.sekwah.narutomod.network.s2c.KuramaCloakPacket.Phase;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.GiantForm;
import com.sekwah.narutomod.util.KuramaCloakMotion.Blow;
import com.sekwah.narutomod.util.KuramaCloakMotion.Strike;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID)
public final class KuramaCloakEvents {
    private static final Map<ServerPlayer, Swing> SWINGS = new WeakHashMap<>();

    private KuramaCloakEvents() {
    }

    public static boolean canGrow(Player actor) {
        if (!(actor.level() instanceof ServerLevel level) || actor.isPassenger() || !actor.onGround()) {
            return false;
        }
        Vec3 at = actor.position();
        double r = GiantForm.HEIGHT_BLOCKS * 0.17;
        AABB space = new AABB(at.x - r, at.y, at.z - r, at.x + r, at.y + GiantForm.HEADROOM, at.z + r);
        return loaded(level, new Vec3(space.minX, space.minY, space.minZ), new Vec3(space.maxX, space.maxY, space.maxZ))
                && level.getWorldBorder().isWithinBounds(space) && space.maxY < level.getMaxBuildHeight()
                && level.noCollision(actor, space) && !level.containsAnyLiquid(space);
    }

    public static void start(ServerPlayer actor, INinjaData data) {
        if (!eligible(actor, data) || SWINGS.containsKey(actor)) {
            return;
        }
        ServerLevel level = actor.serverLevel();
        boolean giant = data.getKuramaTailCount() == 9;
        Blow paw = new Blow(Strike.PAW, actor.position(), actor.getYRot(), giant);
        Blow blow = hasPawTarget(actor, paw) ? paw : new Blow(Strike.TAIL, paw.feet(), paw.yaw(), giant);
        long now = level.getGameTime();
        SWINGS.put(actor, new Swing(level, blow, now + blow.kind().windup(), now + blow.kind().duration()));
        emit(actor, blow, Phase.WINDUP);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer actor)) {
            return;
        }
        Swing swing = SWINGS.get(actor);
        if (swing == null) {
            return;
        }
        long now = actor.level().getGameTime();
        if (now >= swing.finished) {
            SWINGS.remove(actor);
        } else if (!swing.released && now >= swing.release) {
            swing.released = true;
            actor.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> strike(actor, data, swing));
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer actor) {
            SWINGS.remove(actor);
        }
    }

    private static boolean eligible(ServerPlayer actor, INinjaData data) {
        return actor.isAlive() && !actor.isSpectator() && data.isNinjaModeEnabled() && data.isKuramaCloakActive()
                && data.getKuramaTailCount() >= 4 && !actor.isPassenger() && !AdamantineChainEvents.sealed(actor);
    }

    private static void strike(ServerPlayer actor, INinjaData data, Swing swing) {
        Blow blow = swing.blow;
        if (!eligible(actor, data) || actor.level() != swing.level || blow.giant() != (data.getKuramaTailCount() == 9)
                || actor.position().distanceToSqr(blow.feet()) > (blow.giant() ? 9 : 2.25)
                || Math.abs(Mth.wrapDegrees(actor.getYRot() - blow.yaw())) > 50) {
            emit(actor, blow, Phase.CANCEL);
            return;
        }
        emit(actor, blow, Phase.CONTACT);
        ServerLevel level = actor.serverLevel();
        AABB search = new AABB(blow.feet(), blow.feet()).inflate(blow.reach() + 3, blow.giant() ? 8 : 3, blow.reach() + 3);
        boolean paw = blow.kind() == Strike.PAW;
        float damage = (blow.giant() ? (paw ? 52 : 42) : (paw ? 9 : 6)) * data.getRankDamageMultiplier();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search, candidate -> canHit(actor, blow, candidate))) {
            if (target.hurt(actor.damageSources().playerAttack(actor), damage)) {
                Vec3 away = target.position().subtract(blow.feet()).multiply(1, 0, 1).normalize();
                target.knockback(blow.giant() ? (paw ? 1.9 : 3.6) : 0.85, -away.x, -away.z);
                target.hurtMarked = true;
            }
        }
    }

    private static boolean hasPawTarget(ServerPlayer actor, Blow paw) {
        var search = new AABB(paw.feet(), paw.feet()).inflate(paw.reach() + 4, paw.giant() ? 8 : 3, paw.reach() + 4);
        return !actor.level().getEntitiesOfClass(LivingEntity.class, search, candidate -> canHit(actor, paw, candidate)).isEmpty();
    }

    private static boolean canHit(ServerPlayer actor, Blow blow, LivingEntity candidate) {
        if (!candidate.isAlive() || candidate.isSpectator() || Faction.sameSide(actor, candidate) || actor.isAlliedTo(candidate)
                || candidate instanceof Player player && !actor.canHarmPlayer(player) || !blow.covers(candidate.getBoundingBox())) {
            return false;
        }
        Vec3 root = blow.origin();
        Vec3 target = candidate.position().add(0, Math.min(candidate.getBbHeight() * 0.55, 3.5), 0);
        ServerLevel level = actor.serverLevel();
        return loaded(level, blow.feet().add(0, 0.8, 0), root)
                && clear(level, actor, blow.feet().add(0, 0.8, 0), root)
                && loaded(level, root, target) && clear(level, actor, root, target);
    }

    private static boolean clear(ServerLevel level, Player actor, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, actor))
                .getType() == HitResult.Type.MISS;
    }

    private static boolean loaded(ServerLevel level, Vec3 from, Vec3 to) {
        return level.hasChunksAt(BlockPos.containing(Math.min(from.x, to.x), Math.min(from.y, to.y), Math.min(from.z, to.z)),
                BlockPos.containing(Math.max(from.x, to.x), Math.max(from.y, to.y), Math.max(from.z, to.z)));
    }

    private static void emit(ServerPlayer actor, Blow blow, Phase phase) {
        ServerLevel level = actor.serverLevel();
        var packet = new KuramaCloakPacket(level.dimension().location(), actor.getId(), phase, blow);
        PacketHandler.sendNear(packet, level, blow.feet(), 56);
        if (phase == Phase.CONTACT) {
            var sound = blow.kind() == Strike.PAW ? NarutoSounds.KURAMA_PAW : NarutoSounds.KURAMA_SWEEP;
            var origin = blow.origin();
            level.playSound(null, origin.x, origin.y, origin.z, sound.get(), SoundSource.PLAYERS, blow.giant() ? 1.5f : 0.72f, 1);
        }
    }

    private static final class Swing {
        private final ServerLevel level;
        private final Blow blow;
        private final long release;
        private final long finished;
        private boolean released;

        private Swing(ServerLevel level, Blow blow, long release, long finished) {
            this.level = level;
            this.blow = blow;
            this.release = release;
            this.finished = finished;
        }
    }
}
