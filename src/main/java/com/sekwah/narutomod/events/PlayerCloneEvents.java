package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.util.HiraishinTargeting;
import com.sekwah.narutomod.util.ShadowCloneChakra;
import com.sekwah.narutomod.util.ShadowCloneFormation;
import com.sekwah.narutomod.util.ShadowCloneFormation.Profile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerCloneEvents {
    private static final Map<ServerPlayer, Cast> CASTS = new WeakHashMap<>();
    private static final Map<MinecraftServer, Map<UUID, Set<UUID>>> ACTIVE = new WeakHashMap<>();

    private PlayerCloneEvents() {
    }

    public static void start(ServerPlayer actor, INinjaData data, Profile profile,
                             BiConsumer<ServerPlayer, INinjaData> success) {
        if (CASTS.containsKey(actor) || !eligible(actor, data)) {
            return;
        }
        int room = ShadowCloneFormation.OWNER_CAP - active(actor);
        var sites = new ArrayDeque<Vec3>();
        for (Vec3 candidate : ShadowCloneFormation.candidates(actor.position(), actor.getYRot(), profile)) {
            if (sites.size() >= Math.min(room, profile.count)) {
                break;
            }
            Vec3 feet = HiraishinTargeting.supportedNear(actor, candidate);
            if (feet != null) {
                sites.add(feet);
            }
        }
        if (sites.isEmpty()) {
            actor.displayClientMessage(Component.translatable("jutsu.shadow_clone.no_space"), true);
            return;
        }
        var state = new Cast(actor, profile, success, sites);
        CASTS.put(actor, state);
        data.setCastPoseTicks(profile.windup + 6);
        data.setCrossSealPose(true);
        ShadowCloneEffects.prepare(actor, profile);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer actor)) {
            return;
        }
        if (actor.isAlive() && actor.tickCount % 5 == 0) {
            actor.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                float returned = ShadowCloneChakra.of(actor.server).claim(actor.getUUID(),
                        data.getMaxChakra() - data.getChakra());
                if (returned > 0) {
                    data.addChakra(returned);
                }
            });
        }
        Cast cast = CASTS.get(actor);
        if (cast != null && actor.level().getGameTime() >= cast.next) {
            actor.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().ifPresentOrElse(data -> release(actor, data, cast),
                    () -> discardPending(actor));
        }
    }

    private static void release(ServerPlayer actor, INinjaData data, Cast cast) {
        if (!eligible(actor, data) || actor.level() != cast.world || data.getChakra() < cast.profile.cost) {
            discardPending(actor);
            return;
        }
        if (actor.position().distanceToSqr(cast.origin) > 4) {
            fund(actor, data, cast);
            return;
        }
        List<Vec3> spawned = new ArrayList<>();
        while (!cast.sites.isEmpty() && spawned.size() < cast.profile.wave
                && active(actor) < ShadowCloneFormation.OWNER_CAP) {
            Vec3 feet = cast.sites.removeFirst();
            if (HiraishinTargeting.safe(actor, feet) && spawn(actor, cast, feet)) {
                spawned.add(feet);
            }
        }
        if (!spawned.isEmpty()) {
            if (!cast.started) {
                cast.started = true;
                cast.success.accept(actor, data);
                data.setCrossSealPose(true);
            }
            ShadowCloneEffects.wave(actor, cast.profile, spawned);
        }
        if (cast.sites.isEmpty() || active(actor) >= ShadowCloneFormation.OWNER_CAP) {
            fund(actor, data, cast);
            if (!cast.started) {
                actor.displayClientMessage(Component.translatable("jutsu.shadow_clone.no_space"), true);
            }
        }
        cast.next += 3;
    }

    private static void fund(ServerPlayer actor, INinjaData data, Cast cast) {
        CASTS.remove(actor);
        var living = cast.clones.stream().filter(Entity::isAlive).toList();
        if (living.isEmpty()) {
            return;
        }
        float balance = data.getChakra();
        float share = ShadowCloneChakra.split(balance, living.size());
        if (share <= 0) {
            living.forEach(Entity::discard);
            return;
        }
        data.useChakra(balance - share, cast.profile == Profile.MASS ? 60 : 40);
        var ledger = ShadowCloneChakra.of(actor.server);
        long expires = actor.server.overworld().getGameTime() + ShadowCloneFormation.LIFETIME;
        for (ShadowCloneEntity clone : living) {
            ledger.allocate(clone.getUUID(), actor.getUUID(), share, expires);
            clone.setNoAi(false);
        }
    }

    private static void discardPending(ServerPlayer actor) {
        Cast pending = CASTS.remove(actor);
        if (pending != null) {
            pending.clones.forEach(Entity::discard);
        }
    }

    private static boolean spawn(ServerPlayer actor, Cast cast, Vec3 feet) {
        ShadowCloneEntity clone = NarutoEntities.SHADOW_CLONE.get().create(cast.world);
        if (clone == null) {
            return false;
        }
        clone.setOwner(actor);
        clone.configurePlayerFormation(cast.world.getGameTime());
        clone.setNoAi(true);
        clone.moveTo(feet.x, feet.y, feet.z, actor.getYRot(), 0);
        clone.setYHeadRot(actor.getYHeadRot());
        clone.setYBodyRot(actor.getYRot());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            var stack = actor.getItemBySlot(slot).copy();
            if (!stack.isEmpty()) {
                stack.setCount(1);
                clone.setItemSlot(slot, stack);
            }
        }
        if (!cast.world.addFreshEntity(clone)) {
            return false;
        }
        cast.clones.add(clone);
        return true;
    }

    private static boolean eligible(ServerPlayer actor, INinjaData data) {
        return actor.isAlive() && !actor.isSpectator() && !actor.isPassenger() && data.isNinjaModeEnabled();
    }

    private static int active(ServerPlayer actor) {
        var owners = ACTIVE.get(actor.server);
        var ledger = ShadowCloneChakra.of(actor.server);
        var loaded = owners == null ? Set.<UUID>of() : owners.getOrDefault(actor.getUUID(), Set.of());
        int unfunded = (int) loaded.stream().filter(id -> !ledger.allocated(id)).count();
        return ledger.count(actor.getUUID()) + unfunded;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void join(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ShadowCloneEntity clone)
                || !clone.isPlayerFormation()) {
            return;
        }
        var owner = clone.getOwnerUUID();
        if (owner.isEmpty() || clone.playerFormationExpired(level.getGameTime())) {
            ShadowCloneChakra.of(level.getServer()).release(clone.getUUID());
            event.setCanceled(true);
            return;
        }
        var ids = ACTIVE.computeIfAbsent(level.getServer(), server -> new HashMap<>())
                .computeIfAbsent(owner.get(), id -> new HashSet<>());
        if (ids.size() >= ShadowCloneFormation.OWNER_CAP) {
            ShadowCloneChakra.of(level.getServer()).release(clone.getUUID());
            event.setCanceled(true);
        } else {
            ids.add(clone.getUUID());
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            discardPending(player);
        } else if (event.getEntity() instanceof ShadowCloneEntity clone && clone.isPlayerFormation()) {
            var reason = clone.getRemovalReason();
            if (reason != null && reason.shouldDestroy()) {
                ShadowCloneChakra.of(level.getServer()).release(clone.getUUID());
            }
            var owners = ACTIVE.get(level.getServer());
            if (owners != null) {
                clone.getOwnerUUID().ifPresent(owner -> untrack(owners, owner, clone.getUUID()));
            }
        }
    }

    @SubscribeEvent
    public static void expireShares(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level
                && level == level.getServer().overworld() && level.getGameTime() % 20 == 0) {
            ShadowCloneChakra.of(level.getServer()).expire(level.getGameTime());
        }
    }

    private static void untrack(Map<UUID, Set<UUID>> owners, UUID owner, UUID clone) {
        Set<UUID> ids = owners.get(owner);
        if (ids != null && ids.remove(clone) && ids.isEmpty()) {
            owners.remove(owner);
        }
    }

    private static final class Cast {
        private final ServerLevel world;
        private final Vec3 origin;
        private final Profile profile;
        private final BiConsumer<ServerPlayer, INinjaData> success;
        private final Deque<Vec3> sites;
        private final List<ShadowCloneEntity> clones = new ArrayList<>();
        private long next;
        private boolean started;

        private Cast(ServerPlayer actor, Profile profile, BiConsumer<ServerPlayer, INinjaData> success,
                     Deque<Vec3> sites) {
            this.world = actor.serverLevel();
            this.origin = actor.position();
            this.profile = profile;
            this.sites = sites;
            this.success = success;
            this.next = world.getGameTime() + profile.windup;
        }
    }
}
