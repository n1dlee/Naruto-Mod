package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.ShadowTetherPacket;
import com.sekwah.narutomod.network.s2c.ShadowTetherPacket.Cue;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.KamuiStep;
import com.sekwah.narutomod.util.ShadowSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShadowTetherEvents {
    private static final Map<LivingEntity, Tether> CASTS = new WeakHashMap<>();
    private static final Map<LivingEntity, WeakReference<LivingEntity>> VICTIMS = new WeakHashMap<>();
    private static int sequence;

    private ShadowTetherEvents() {
    }

    public static boolean canCast(LivingEntity caster) {
        return caster.level() instanceof ServerLevel level && caster.isAlive() && !caster.isSpectator()
                && caster.onGround() && !caster.isPassenger() && !caster.noPhysics && !CASTS.containsKey(caster) && !VICTIMS.containsKey(caster)
                && ground(level, caster, caster.position(), caster.position()) != null;
    }

    public static void start(LivingEntity actor, Vec3 direction, double range, @Nullable LivingEntity marked) {
        if (!(actor.level() instanceof ServerLevel level) || !canCast(actor)) {
            return;
        }
        Vec3 aim = new Vec3(direction.x, 0, direction.z).normalize();
        if (aim.lengthSqr() < 0.001) {
            aim = KamuiStep.facing(actor.getYRot());
        }
        double reach = Math.max(4, Math.min(18, range));
        Choice chosen = choose(level, actor, aim, reach, marked);
        var tether = new Tether(++sequence, level, chosen.target(), actor.position(), chosen.path(), reach);
        CASTS.put(actor, tether);
        signal(actor, tether, Cue.CAST, tether.path);
        level.playSound(null, actor.blockPosition(), NarutoSounds.SHADOW_WEAVE.get(), SoundSource.PLAYERS, 0.61f, 0.93f);
    }

    public static @Nullable Link link(LivingEntity caster) {
        Tether tether = CASTS.get(caster);
        if (tether == null || tether.until <= tether.level.getGameTime()) {
            return null;
        }
        LivingEntity target = tether.target.get();
        return target != null && valid(caster, target) && caster.distanceToSqr(target) <= tether.range * tether.range
                ? new Link(tether.id, target) : null;
    }

    public static boolean recheck(LivingEntity caster, Link link) {
        Link current = link(caster);
        if (current == null || current.id() != link.id() || current.target() != link.target()) {
            return false;
        }
        Tether tether = CASTS.get(caster);
        List<Vec3> route = trace(tether.level, caster, link.target().position());
        if (ShadowSurface.reaches(route, link.target().position())) {
            return true;
        }
        close(caster, tether, snapPath(tether, route));
        return false;
    }

    private static Choice choose(ServerLevel level, LivingEntity actor, Vec3 aim, double reach, @Nullable LivingEntity marked) {
        Vec3 from = actor.position();
        List<LivingEntity> candidates = marked == null ? level.getEntitiesOfClass(LivingEntity.class,
                actor.getBoundingBox().expandTowards(aim.scale(reach)).inflate(1.15, 2, 1.15),
                target -> valid(actor, target) && inLane(from, target, aim, reach)).stream()
                .sorted(Comparator.comparingDouble(target -> from.distanceToSqr(target.position()))).limit(8).toList()
                : valid(actor, marked) && inLane(from, marked, aim, reach) ? List.of(marked) : List.of();
        for (LivingEntity target : candidates) {
            List<Vec3> route = trace(level, actor, target.position());
            if (ShadowSurface.reaches(route, target.position())) {
                return new Choice(target, route);
            }
        }
        return new Choice(null, trace(level, actor, from.add(aim.scale(reach))));
    }

    private static boolean inLane(Vec3 from, LivingEntity target, Vec3 aim, double reach) {
        Vec3 feet = target.position();
        Vec3 delta = new Vec3(feet.x - from.x, 0, feet.z - from.z);
        double forward = delta.dot(aim);
        double width = 0.69 + Math.min(0.42, target.getBbWidth() * 0.3);
        return forward > 0.22 && forward <= reach && delta.subtract(aim.scale(forward)).lengthSqr() <= width * width;
    }

    private static boolean valid(LivingEntity caster, LivingEntity target) {
        if (caster == target || !target.isAlive() || !target.onGround() || target.isSpectator() || target.noPhysics
                || target.level() != caster.level() || caster.isAlliedTo(target) || Faction.sameSide(caster, target)
                || target instanceof TamableAnimal pet && pet.isTame()) {
            return false;
        }
        return !(target instanceof Player player) || !player.isCreative()
                && (!(caster instanceof Player source) || source.canHarmPlayer(player));
    }

    private static List<Vec3> trace(ServerLevel level, LivingEntity actor, Vec3 to) {
        return surfacePath(level, actor, actor.position(), to);
    }

    public static List<Vec3> surfacePath(ServerLevel level, LivingEntity actor, Vec3 from, Vec3 to) {
        List<Vec3> best = ShadowSurface.path(from, to, 0, (previous, flat) -> ground(level, actor, previous, flat));
        if (ShadowSurface.reaches(best, to)) {
            return best;
        }
        for (double bend : new double[] {-1.05, 1.05}) {
            List<Vec3> alternative = ShadowSurface.path(from, to, bend, (previous, flat) -> ground(level, actor, previous, flat));
            if (ShadowSurface.reaches(alternative, to)) {
                return alternative;
            }
        }
        return best;
    }

    private static @Nullable Vec3 ground(ServerLevel level, LivingEntity actor, Vec3 previous, Vec3 flat) {
        BlockPos column = BlockPos.containing(flat.x, previous.y, flat.z);
        if (!level.hasChunkAt(column) || !level.getWorldBorder().isWithinBounds(column)) {
            return null;
        }
        Vec3 top = new Vec3(flat.x, previous.y + 1.12, flat.z);
        Vec3 bottom = new Vec3(flat.x, previous.y - 1.15, flat.z);
        var hit = level.clip(new ClipContext(top, bottom, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, actor));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP
                || level.getMaxLocalRawBrightness(hit.getBlockPos().above()) < 4 || !level.getFluidState(hit.getBlockPos().above()).isEmpty()) {
            return null;
        }
        Vec3 floor = hit.getLocation();
        double height = Math.max(previous.y, floor.y) + 0.08;
        Vec3 a = new Vec3(previous.x, height, previous.z);
        Vec3 b = new Vec3(floor.x, height, floor.z);
        var air = level.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, actor));
        return air.getType() == HitResult.Type.MISS ? floor : null;
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity actor = event.getEntity();
        if (actor.level().isClientSide) {
            return;
        }
        Tether tether = CASTS.get(actor);
        if (tether == null) {
            return;
        }
        long now = tether.level.getGameTime();
        if (!actor.isAlive() || actor.isSpectator() || actor.level() != tether.level || actor.noPhysics
                || actor instanceof Player player && !ninja(player)) {
            close(actor, tether, tether.path);
        } else if (tether.until > 0) {
            sustain(actor, tether, now);
        } else if (now - tether.started >= ShadowSurface.WINDUP) {
            bind(actor, tether, now);
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof LivingEntity actor) || event.getLevel().isClientSide) {
            return;
        }
        Tether tether = CASTS.get(actor);
        if (tether != null) {
            close(actor, tether, tether.path);
        }
        WeakReference<LivingEntity> owner = VICTIMS.get(actor);
        LivingEntity caster = owner == null ? null : owner.get();
        Tether linked = caster == null ? null : CASTS.get(caster);
        if (linked != null) {
            close(caster, linked, linked.path);
        }
    }

    private static void bind(LivingEntity actor, Tether tether, long now) {
        LivingEntity target = tether.target.get();
        if (target == null || VICTIMS.containsKey(target) || !valid(actor, target)
                || tether.start.distanceToSqr(actor.position()) > 1.0 || actor.distanceToSqr(target) > tether.range * tether.range) {
            close(actor, tether, tether.path);
            return;
        }
        List<Vec3> route = trace(tether.level, actor, target.position());
        if (!ShadowSurface.reaches(route, target.position())) {
            close(actor, tether, snapPath(tether, route));
            return;
        }
        int ticks = target instanceof Player ? ShadowSurface.PLAYER_TIME : ShadowSurface.MOB_TIME;
        tether.until = now + ticks;
        tether.path = route;
        tether.previous = actor.position();
        VICTIMS.put(target, new WeakReference<>(actor));
        remember(actor, target, ticks);
        signal(actor, tether, Cue.BOUND, route);
        tether.level.playSound(null, target.blockPosition(), NarutoSounds.SHADOW_WEAVE.get(), SoundSource.PLAYERS, 0.64f, 0.77f);
        ShadowGripEvents.followBoss(actor);
        if (target instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("jutsu.shadow.bound"), true);
        }
    }

    private static void sustain(LivingEntity actor, Tether tether, long now) {
        LivingEntity target = tether.target.get();
        if (target == null || now >= tether.until || !valid(actor, target) || actor.distanceToSqr(target) > tether.range * tether.range
                || target instanceof Player player && escape(tether, player)) {
            close(actor, tether, tether.path);
            return;
        }
        if ((now - tether.started) % 2 == 0) {
            List<Vec3> route = trace(tether.level, actor, target.position());
            if (!ShadowSurface.reaches(route, target.position())) {
                close(actor, tether, snapPath(tether, route));
                return;
            }
            tether.path = route;
            signal(actor, tether, Cue.BOUND, route);
        }
        Vec3 motion = ShadowSurface.mirror(target.getDeltaMovement(), actor.position().subtract(tether.previous), target instanceof Player);
        if (motion.distanceToSqr(target.getDeltaMovement()) > 0.0009) {
            target.setDeltaMovement(motion);
            target.hurtMarked = true;
        }
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setYRot(actor.getYRot());
            mob.setYHeadRot(actor.getYHeadRot());
        }
        tether.previous = actor.position();
        remember(actor, target, (int) (tether.until - now));
    }

    private static boolean escape(Tether tether, Player player) {
        tether.escapeTicks = player.isShiftKeyDown() ? tether.escapeTicks + 1 : 0;
        return tether.escapeTicks >= 6;
    }

    private static List<Vec3> snapPath(Tether tether, List<Vec3> checked) {
        return checked.size() >= 2 ? checked : tether.path;
    }

    private static boolean ninja(Player actor) {
        return actor.getCapability(NinjaCapabilityHandler.NINJA_DATA).map(data -> data.isNinjaModeEnabled()).orElse(false);
    }

    private static void remember(LivingEntity actor, @Nullable LivingEntity target, int ticks) {
        if (actor instanceof Player) {
            actor.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                    .ifPresent(data -> data.setShadowPossessedTarget(target == null ? null : target.getUUID(), ticks));
        }
    }

    private static void close(LivingEntity actor, Tether tether, List<Vec3> points) {
        CASTS.remove(actor);
        LivingEntity target = tether.target.get();
        if (target != null && tether.until > 0) {
            VICTIMS.remove(target);
        }
        remember(actor, null, 0);
        signal(actor, tether, Cue.SNAP, points);
    }

    private static void signal(LivingEntity actor, Tether tether, Cue cue, List<Vec3> points) {
        if (points.size() >= 2) {
            PacketHandler.sendNear(new ShadowTetherPacket(tether.level.dimension().location(), actor.getId(), tether.id,
                    cue, points), tether.level, points.get(0), 44);
        }
    }

    private record Choice(@Nullable LivingEntity target, List<Vec3> path) {
    }

    public record Link(int id, LivingEntity target) {
    }

    private static final class Tether {
        private final int id;
        private final ServerLevel level;
        private final WeakReference<LivingEntity> target;
        private final Vec3 start;
        private final long started;
        private final double range;
        private List<Vec3> path;
        private Vec3 previous;
        private long until;
        private int escapeTicks;

        private Tether(int id, ServerLevel level, @Nullable LivingEntity target, Vec3 start, List<Vec3> path, double range) {
            this.id = id;
            this.level = level;
            this.target = new WeakReference<>(target);
            this.start = start;
            this.started = level.getGameTime();
            this.range = range;
            this.path = path;
            this.previous = start;
        }
    }
}
