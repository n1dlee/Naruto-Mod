package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.entity.MangekyoBossEntity;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.KamuiTransitPacket;
import com.sekwah.narutomod.network.s2c.KamuiTransitPacket.Cue;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.KamuiTransitRoute;
import com.sekwah.narutomod.world.KamuiDimension;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KamuiTransitEvents {
    private static final double RANGE = 18;
    private static final Map<ServerPlayer, Journey> PENDING = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> RETRY = new WeakHashMap<>();
    private static int serial;

    private KamuiTransitEvents() {
    }

    public static void start(ServerPlayer actor, Ability ability, float cost, BiConsumer<ServerPlayer, INinjaData> confirmed) {
        if (!ready(actor) || KamuiDimension.isKamui(actor.level())) {
            return;
        }
        if (actor.server.getLevel(KamuiDimension.KAMUI) == null) {
            fail(actor, "jutsu.kamui.nodimension");
            return;
        }
        boolean capture = actor.isShiftKeyDown();
        LivingEntity traveler = capture ? target(actor) : actor;
        if (traveler == null) {
            fail(actor, "jutsu.kamui.warp_miss");
            return;
        }
        enqueue(actor, traveler, capture ? Mode.CAPTURE : Mode.ENTER, new Cost(ability, cost, confirmed));
    }

    public static void returnPlayer(ServerPlayer actor) {
        if (KamuiDimension.isKamui(actor.level()) && ready(actor)) {
            enqueue(actor, actor, Mode.EXIT, null);
        }
    }

    private static boolean ready(ServerPlayer actor) {
        return usable(actor) && !PENDING.containsKey(actor)
                && actor.serverLevel().getGameTime() >= RETRY.getOrDefault(actor, Long.MIN_VALUE);
    }

    private static boolean usable(LivingEntity actor) {
        return actor.isAlive() && !actor.isSpectator() && !actor.isPassenger() && !actor.isVehicle()
                && !actor.isSleeping() && !KamuiPhaseEvents.isPhased(actor) && !actor.noPhysics;
    }

    private static void enqueue(ServerPlayer actor, LivingEntity traveler, Mode mode, @Nullable Cost cost) {
        ServerLevel level = actor.serverLevel();
        int ticks = mode == Mode.EXIT ? KamuiTransitRoute.EXIT_WINDUP : KamuiTransitRoute.WINDUP;
        var trip = new Journey(++serial, mode, new WeakReference<>(traveler), level, actor.position(), traveler.position(),
                traveler.getBbHeight(), level.getGameTime() + ticks, cost);
        PENDING.put(actor, trip);
        visual(level, trip.id(), Cue.GATHER, traveler.position(), traveler.getBbHeight(), ticks);
        sound(level, traveler.position(), 0.56f, mode == Mode.EXIT ? 1.01f : 0.88f);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer actor)) {
            return;
        }
        long now = actor.serverLevel().getGameTime();
        if (now >= RETRY.getOrDefault(actor, Long.MAX_VALUE)) {
            RETRY.remove(actor);
        }
        Journey trip = PENDING.get(actor);
        if (trip != null) {
            advance(actor, trip, now);
        } else if (KamuiDimension.isKamui(actor.level()) && actor.getY() < 11 && ready(actor)) {
            var pocket = actor.serverLevel();
            Vec3 from = actor.position();
            float height = actor.getBbHeight();
            LivingEntity moved = KamuiDimension.exit(actor);
            if (moved == null) {
                fail(actor, "jutsu.kamui.nodimension");
            } else {
                arrival(++serial, pocket, from, height, moved);
            }
        }
    }

    @SubscribeEvent
    public static void onIsland(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getEntity().isShiftKeyDown()
                || !KamuiDimension.isKamui(event.getLevel())
                || !event.getLevel().getBlockState(event.getPos()).is(NarutoBlocks.KAMUI_BLOCK.get())) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            returnPlayer(player);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && KamuiDimension.isKamui(player.level())) {
            hint(player);
        }
    }

    @SubscribeEvent
    public static void mobTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel pocket)
                || !mob.isAlive() || !KamuiDimension.isKamui(pocket) || mob.tickCount % 15 != 0
                || !KamuiDimension.mobReturnDue(mob, pocket.getGameTime())) {
            return;
        }
        Vec3 from = mob.position();
        float height = mob.getBbHeight();
        LivingEntity moved = KamuiDimension.exitMob(mob, pocket);
        if (moved != null) {
            arrival(++serial, pocket, from, height, moved);
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Journey trip = PENDING.remove(player);
            if (trip != null) {
                visual(trip.source(), trip.id(), Cue.CANCEL, trip.focus(), trip.height(), 0);
            }
            RETRY.remove(player);
        }
    }

    private static void advance(ServerPlayer actor, Journey trip, long now) {
        LivingEntity traveler = trip.traveler().get();
        var data = actor.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve().orElse(null);
        if (!viable(actor, traveler, trip, data)) {
            PENDING.remove(actor);
            cancel(actor, trip);
        } else if (now >= trip.release()) {
            PENDING.remove(actor);
            transfer(actor, traveler, trip, data);
        }
    }

    private static boolean viable(ServerPlayer actor, @Nullable LivingEntity traveler, Journey trip, @Nullable INinjaData data) {
        if (traveler == null || traveler.level() != trip.source() || actor.level() != trip.source() || !usable(traveler)
                || !usable(actor) || !KamuiTransitRoute.maintains(trip.focus(), traveler.position(), 1.45)
                || !KamuiTransitRoute.maintains(trip.casterStart(), actor.position(), 1.35)) {
            return false;
        }
        if (trip.mode() == Mode.EXIT) {
            return true;
        }
        Cost cost = trip.cost();
        return cost != null && data != null && data.isNinjaModeEnabled() && cost.ability().hasEyeAccess(data)
                && data.getChakra() >= cost.chakra() && (trip.mode() != Mode.CAPTURE || target(actor) == traveler);
    }

    private static void transfer(ServerPlayer actor, LivingEntity traveler, Journey trip, @Nullable INinjaData data) {
        Vec3 from = traveler.position();
        LivingEntity moved = trip.mode() == Mode.EXIT ? KamuiDimension.exit(actor) : intoPocket(actor, traveler);
        if (moved == null) {
            cancel(actor, trip);
            return;
        }
        var cost = trip.cost();
        if (data != null && cost != null) {
            data.useChakra(cost.chakra(), 40);
            cost.confirmed().accept(actor, data);
        }
        arrival(trip.id(), trip.source(), from, trip.height(), moved);
        if (moved instanceof ServerPlayer player) {
            if (trip.mode() == Mode.EXIT) {
                player.displayClientMessage(Component.translatable("jutsu.kamui.returned").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            } else {
                hint(player);
            }
        }
    }

    @Nullable
    private static LivingEntity intoPocket(ServerPlayer caster, LivingEntity traveler) {
        ServerLevel pocket = caster.server.getLevel(KamuiDimension.KAMUI);
        return pocket == null ? null : KamuiDimension.enter(traveler, pocket);
    }

    private static void arrival(int id, ServerLevel source, Vec3 from, float height, LivingEntity moved) {
        visual(source, id, Cue.FOLD, from, height, 0);
        sound(source, from, 0.71f, 0.79f);
        if (moved.level() instanceof ServerLevel destination) {
            visual(destination, id, Cue.OPEN, moved.position(), moved.getBbHeight(), 0);
            sound(destination, moved.position(), 0.80f, 1.20f);
        }
    }

    private static void cancel(ServerPlayer actor, Journey trip) {
        visual(trip.source(), trip.id(), Cue.CANCEL, trip.focus(), trip.height(), 0);
        fail(actor, "jutsu.kamui.warp_miss");
    }

    private static void fail(ServerPlayer actor, String key) {
        RETRY.put(actor, actor.serverLevel().getGameTime() + 12);
        actor.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
    }

    private static void hint(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("jutsu.kamui.island_exit").withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    @Nullable
    private static LivingEntity target(ServerPlayer actor) {
        Vec3 eye = actor.getEyePosition();
        Vec3 ray = actor.getLookAngle().scale(RANGE);
        HitResult cover = actor.pick(RANGE, 1, false);
        Vec3 end = cover.getType() == HitResult.Type.MISS ? eye.add(ray) : cover.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(actor.level(), actor, eye, end,
                actor.getBoundingBox().expandTowards(ray).inflate(0.8), candidate -> candidate != actor && candidate.isPickable());
        return hit != null && hit.getEntity() instanceof LivingEntity living && eligible(actor, living) ? living : null;
    }

    private static boolean eligible(ServerPlayer actor, LivingEntity other) {
        if (!usable(other) || other.isInvulnerable() || actor.isAlliedTo(other) || Faction.sameSide(actor, other)
                || other.isInvisibleTo(actor) || !actor.hasLineOfSight(other)) {
            return false;
        }
        if (other instanceof ServerPlayer player) {
            return !player.isCreative() && actor.canHarmPlayer(player);
        }
        return other instanceof Mob && other instanceof Enemy && !(other instanceof MangekyoBossEntity)
                && Faction.ownerOf(other).isEmpty();
    }

    private static void visual(ServerLevel level, int id, Cue cue, Vec3 at, float height, int ticks) {
        PacketHandler.sendNear(new KamuiTransitPacket(level.dimension().location(), id, cue, at, height, ticks), level, at, 42);
    }

    private static void sound(ServerLevel world, Vec3 at, float volume, float pitch) {
        world.playSound(null, at.x, at.y + 0.7, at.z, NarutoSounds.KAMUI_TRANSIT.get(), SoundSource.PLAYERS, volume, pitch);
    }

    private enum Mode { ENTER, CAPTURE, EXIT }

    private record Journey(int id, Mode mode, WeakReference<LivingEntity> traveler, ServerLevel source, Vec3 casterStart,
                           Vec3 focus, float height, long release, @Nullable Cost cost) {
    }

    private record Cost(Ability ability, float chakra, BiConsumer<ServerPlayer, INinjaData> confirmed) {
    }
}
