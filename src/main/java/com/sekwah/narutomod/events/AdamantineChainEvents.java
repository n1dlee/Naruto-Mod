package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.entity.TailedBeastEntity;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.AdamantineChainPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.AdamantineChain;
import com.sekwah.narutomod.util.AdamantineChain.Phase;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.KamuiStep;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AdamantineChainEvents {
    private static final Map<Player, Cast> CASTS = new WeakHashMap<>();
    private static final Map<LivingEntity, Branch> BOUND = new WeakHashMap<>();
    private static final UUID SLOW_ID = UUID.fromString("9b2b1840-5c6a-4188-ae21-11e9635755bf");
    private static int serial;

    private AdamantineChainEvents() {
    }

    public static boolean sealed(LivingEntity target) {
        if (target.level().isClientSide) {
            return false;
        }
        Branch branch = BOUND.get(target);
        return branch != null && branch.until > target.level().getGameTime();
    }

    public static void start(Player player) {
        if (!(player.level() instanceof ServerLevel level) || CASTS.containsKey(player)) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 origin = root(player, 1);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(AdamantineChain.REACH), target -> eligible(player, target)
                        && AdamantineChain.inCone(origin, chest(target), look)
                        && endpoint(level, player, origin, chest(target), false).miss());
        targets.sort(Comparator.comparingDouble(target -> chest(target).distanceToSqr(origin)));

        var cast = new Cast(level, level.getGameTime(), look, new ArrayList<>());
        CASTS.put(player, cast);
        for (int i = 0; i < AdamantineChain.BRANCHES; i++) {
            LivingEntity target = i < targets.size() ? targets.get(i) : null;
            var branch = new Branch(++serial, i, target);
            branch.root = root(player, i);
            Vec3 end = target == null ? emptyEnd(look, branch.root, i) : chest(target);
            branch.end = end;
            branch.fraction = endpoint(level, player, branch.root, end, false).fraction();
            cast.branches().add(branch);
            publish(cast, branch, Phase.SEEK, 0);
        }
        level.playSound(null, player.blockPosition(), NarutoSounds.ADAMANTINE_SPOOL.get(), SoundSource.PLAYERS, 0.92f, 1);
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        Cast cast = CASTS.get(player);
        if (cast == null) {
            return;
        }
        if (!player.isAlive() || player.isSpectator() || player.noPhysics || player.level() != cast.level()) {
            stop(player, cast);
            return;
        }
        int age = (int) (cast.level().getGameTime() - cast.start());
        cast.branches().removeIf(branch -> !update(player, cast, branch, age));
        if (cast.branches().isEmpty()) {
            CASTS.remove(player);
        }
    }

    private static boolean update(Player player, Cast cast, Branch branch, int age) {
        LivingEntity target = branch.target.get();
        boolean bound = branch.until != 0;
        boolean present = target != null && eligible(player, target);
        if (bound && (!present || branch.until <= cast.level().getGameTime())) {
            release(cast, branch);
            return false;
        }
        branch.root = root(player, branch.lane);
        if (bound && branch.root.distanceToSqr(chest(target)) > Math.pow(AdamantineChain.REACH + 0.45, 2)) {
            release(cast, branch);
            return false;
        }
        boolean nearEnough = present && branch.root.distanceToSqr(chest(target)) <= AdamantineChain.REACH * AdamantineChain.REACH;
        branch.end = bound || nearEnough ? chest(target) : emptyEnd(cast.aim(), branch.root, branch.lane);
        var reach = endpoint(cast.level(), player, branch.root, branch.end, bound);
        branch.fraction = reach.fraction();
        if (bound) {
            if (!reach.miss()) {
                release(cast, branch);
                return false;
            }
            if (age % 3 == 0) {
                publish(cast, branch, Phase.SEALED, 0);
            }
            return true;
        }
        if (age < AdamantineChain.WINDUP) {
            if (age % 2 == 0) {
                publish(cast, branch, Phase.SEEK, age);
            }
            return true;
        }
        if (canBind(player, cast, branch, target, reach)) {
            bind(cast, branch, target);
            return true;
        }
        release(cast, branch);
        return false;
    }

    private static boolean canBind(Player player, Cast cast, Branch branch, @Nullable LivingEntity target, Trace lead) {
        return target != null && eligible(player, target) && lead.miss() && !BOUND.containsKey(target)
                && AdamantineChain.inCone(branch.root, chest(target), cast.aim())
                && endpoint(cast.level(), player, branch.root, chest(target), true).miss();
    }

    private static void bind(Cast cast, Branch branch, LivingEntity target) {
        boolean player = target instanceof Player;
        boolean beast = target instanceof TailedBeastEntity;
        branch.until = cast.level().getGameTime() + AdamantineChain.duration(player, beast);
        branch.girth = (float) Math.min(beast ? 1.8 : 0.64, target.getBbWidth() * 0.57);
        BOUND.put(target, branch);
        var speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addTransientModifier(new AttributeModifier(SLOW_ID, "Adamantine chain restraint",
                    player ? -0.47 : beast ? -0.78 : -0.69, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        if (target instanceof Player victim) {
            victim.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                if (data.getCurrentlyChanneledAbility() != null) {
                    data.setCurrentlyChanneledAbility(victim, null);
                }
            });
        }
        cast.level().playSound(null, target.blockPosition(), NarutoSounds.ADAMANTINE_LOCK.get(), SoundSource.PLAYERS, 0.83f,
                beast ? 0.71f : 1);
        publish(cast, branch, Phase.SEALED, 0);
    }

    private static void release(Cast cast, Branch branch) {
        LivingEntity target = branch.target.get();
        if (target != null && BOUND.remove(target, branch)) {
            var speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(SLOW_ID);
            }
            cast.level().playSound(null, target.blockPosition(), NarutoSounds.ADAMANTINE_RELEASE.get(), SoundSource.PLAYERS, 0.58f, 1);
        }
        publish(cast, branch, Phase.RETRACT, 0);
    }

    private static void stop(Player player, Cast cast) {
        cast.branches().forEach(branch -> release(cast, branch));
        CASTS.remove(player);
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        if (entity instanceof Player player) {
            Cast cast = CASTS.get(player);
            if (cast != null) {
                stop(player, cast);
            }
        }
        Branch incoming = BOUND.get(entity);
        if (incoming != null) {
            CASTS.values().stream().filter(cast -> cast.branches().remove(incoming)).findFirst()
                    .ifPresent(cast -> release(cast, incoming));
        }
    }

    private static boolean eligible(Player player, LivingEntity target) {
        return target != player && target.level() == player.level() && target.isAlive() && !target.isRemoved()
                && target.isPickable() && !target.isSpectator() && !target.noPhysics
                && !player.isAlliedTo(target) && !Faction.sameSide(player, target)
                && !(target instanceof TamableAnimal tame && tame.isOwnedBy(player))
                && (!(target instanceof Player other) || !other.isCreative() && player.canHarmPlayer(other));
    }

    private static Trace endpoint(ServerLevel level, Player actor, Vec3 root, Vec3 end, boolean taut) {
        var spine = actor.position().add(0, actor.getBbHeight() * 0.72, 0);
        var atBack = level.clip(new ClipContext(spine, root, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, actor));
        if (atBack.getType() != HitResult.Type.MISS) {
            return new Trace(0);
        }
        Vec3 previous = root;
        for (int i = 1; i <= AdamantineChain.SEGMENTS; i++) {
            Vec3 next = AdamantineChain.point(root, end, i / (double) AdamantineChain.SEGMENTS, taut);
            BlockPos column = BlockPos.containing(next);
            if (!level.hasChunkAt(column) || !level.getWorldBorder().isWithinBounds(column)) {
                return new Trace((i - 1) / (float) AdamantineChain.SEGMENTS);
            }
            var hit = level.clip(new ClipContext(previous, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, actor));
            if (hit.getType() != HitResult.Type.MISS) {
                double fraction = Mth.clamp(previous.distanceTo(hit.getLocation()) / Math.max(0.001, previous.distanceTo(next)), 0, 0.999);
                return new Trace((float) ((i - 1 + fraction) / AdamantineChain.SEGMENTS));
            }
            previous = next;
        }
        return new Trace(1);
    }

    private static Vec3 root(Player player, int lane) {
        Vec3 forward = KamuiStep.facing(player.yBodyRot);
        Vec3 side = new Vec3(forward.z, 0, -forward.x);
        return player.position().add(0, player.getBbHeight() * (lane == 1 ? 0.79 : 0.69), 0)
                .add(forward.scale(-0.25)).add(side.scale(0.17 * (lane - 1)));
    }

    private static Vec3 chest(LivingEntity target) {
        return target.position().add(0, target.getBbHeight() * 0.57, 0);
    }

    private static Vec3 emptyEnd(Vec3 aim, Vec3 root, int lane) {
        return root.add(aim.yRot((lane - 1) * 0.43f).scale(lane == 1 ? 7 : 5.5));
    }

    private static void publish(Cast cast, Branch branch, Phase phase, int age) {
        var packet = new AdamantineChainPacket(cast.level().dimension().location(), branch.id, phase, age, branch.root,
                branch.end, branch.girth, branch.fraction);
        PacketHandler.sendNear(packet, cast.level(), branch.root.lerp(branch.end, 0.5), 36);
    }

    private record Cast(ServerLevel level, long start, Vec3 aim, List<Branch> branches) {
    }

    private record Trace(float fraction) {
        private boolean miss() { return this.fraction == 1; }
    }

    private static final class Branch {
        private final int id;
        private final int lane;
        private final WeakReference<LivingEntity> target;
        private Vec3 root;
        private Vec3 end;
        private long until;
        private float girth;
        private float fraction;

        private Branch(int id, int lane, @Nullable LivingEntity target) {
            this.id = id;
            this.lane = lane;
            this.target = new WeakReference<>(target);
        }
    }
}
