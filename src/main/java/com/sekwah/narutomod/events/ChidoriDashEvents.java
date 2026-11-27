package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.ChidoriDashPacket;
import com.sekwah.narutomod.network.s2c.ChidoriDashPacket.Phase;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.ChidoriDashPath;
import com.sekwah.narutomod.util.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChidoriDashEvents {
    public static final int PLAYER_WINDUP = 5;
    public static final int RECOVERY_TICKS = 10;
    private static final double[] PLAYER_STEPS = {0.9, 1.5, 1.9, 2.0, 2.0, 1.7};
    private static final double[] BOSS_STEPS = {0.9, 1.5, 1.9, 2.0, 1.7};
    private static final Map<LivingEntity, Dash> ACTIVE = new WeakHashMap<>();

    private ChidoriDashEvents() {
    }

    public static boolean canStart(LivingEntity actor) {
        return actor.isAlive() && !actor.isSpectator() && !actor.isPassenger() && !actor.isFallFlying()
                && !actor.isInWaterOrBubble() && !ACTIVE.containsKey(actor);
    }

    public static void start(LivingEntity actor, Vec3 aim, DamageProfile damage) {
        if (!(actor.level() instanceof ServerLevel level) || !canStart(actor)) {
            return;
        }
        Vec3 heading = ChidoriDashPath.groundHeading(aim, actor.getYRot());
        boolean player = actor instanceof Player;
        Dash dash = new Dash(heading, damage, player ? PLAYER_STEPS : BOSS_STEPS, player ? PLAYER_WINDUP : 6);
        ACTIVE.put(actor, dash);
        Vec3 hand = handHeight(actor);
        broadcast(level, actor, dash, Phase.BRACE, hand, hand);
        level.playSound(null, hand.x, hand.y, hand.z, NarutoSounds.CHIDORI.get(), SoundSource.PLAYERS, 0.75f, 0.75f);
    }

    @SubscribeEvent
    public static void tickDash(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        for (LivingEntity actor : List.copyOf(ACTIVE.keySet())) {
            Dash dash = ACTIVE.get(actor);
            if (dash != null && actor.level() == level && !advance(level, actor, dash)) {
                ACTIVE.remove(actor, dash);
            }
        }
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity actor) {
            ACTIVE.remove(actor);
        }
    }

    private static boolean advance(ServerLevel level, LivingEntity actor, Dash dash) {
        if (!actor.isAlive() || actor.isRemoved() || actor.isSpectator()) {
            return false;
        }
        if (dash.recovery > 0) {
            return --dash.recovery > 0;
        }
        if (actor.isPassenger() || actor.isInWaterOrBubble() || actor.isFallFlying() || actor.isSleeping()) {
            return false;
        }
        pinHorizontalMotion(actor);
        if (dash.windup > 0) {
            dash.windup--;
            return true;
        }
        if (dash.index == 0) {
            actor.playSound(NarutoSounds.CHIDORI_DASH_LAUNCH.get(), 1.0f, 1.0f);
        }
        runSegment(level, actor, dash);
        return true;
    }

    private static void pinHorizontalMotion(LivingEntity actor) {
        if (actor instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        actor.setDeltaMovement(0, actor.getDeltaMovement().y, 0);
    }

    private static void runSegment(ServerLevel level, LivingEntity actor, Dash dash) {
        Vec3 feet = actor.position();
        Vec3 from = handHeight(actor);
        Vec3 wanted = dash.heading.scale(dash.steps[dash.index++]);
        if (!level.hasChunkAt(BlockPos.containing(feet.add(wanted)))) {
            finish(level, actor, dash, Phase.RECOVER, from);
            return;
        }
        actor.move(MoverType.SELF, wanted);
        Vec3 reached = handHeight(actor);
        Vec3 actual = actor.position().subtract(feet);
        TargetHit hit = strike(level, actor, dash.damage, from, reached);
        if (hit != null) {
            Vec3 stop = hit.contact().center().subtract(0, actor.getBbHeight() * 0.58, 0);
            if (level.noCollision(actor, actor.getBoundingBox().move(stop.subtract(actor.position())))) {
                actor.setPos(stop);
                reached = handHeight(actor);
            }
        }
        syncMove(actor, feet);
        if (feet.subtract(actor.position()).horizontalDistanceSqr() > 1.0e-5) {
            broadcast(level, actor, dash, Phase.TRAVEL, from, reached);
        }
        if (hit != null) {
            finish(level, actor, dash, Phase.STRIKE, hit.contact().point());
        } else if (ChidoriDashPath.obstructed(wanted, actual)) {
            finish(level, actor, dash, Phase.BLOCK, blockContact(level, actor, from, wanted, reached));
        } else if (dash.index == dash.steps.length) {
            finish(level, actor, dash, Phase.RECOVER, reached);
        }
    }

    private static void syncMove(LivingEntity actor, Vec3 previous) {
        if (actor instanceof ServerPlayer player && actor.position().distanceToSqr(previous) > 1.0e-8) {
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        } else if (actor.position().distanceToSqr(previous) > 1.0e-8) {
            actor.hurtMarked = true;
        }
    }

    @Nullable
    private static TargetHit strike(ServerLevel level, LivingEntity actor, DamageProfile profile, Vec3 from, Vec3 reached) {
        if (from.subtract(reached).horizontalDistanceSqr() < 1.0e-5) {
            return null;
        }
        AABB sweep = new AABB(from, reached).inflate(0.39, 0.31, 0.39);
        var candidates = new ArrayList<TargetHit>();
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, sweep, target -> canHit(actor, target))) {
            var contact = ChidoriDashPath.contact(from, reached, victim.getBoundingBox());
            if (contact != null && (contact.point().distanceToSqr(from) < 1.0e-8
                    || clip(level, actor, from, contact.point()).getType() == HitResult.Type.MISS)) {
                candidates.add(new TargetHit(victim, contact));
            }
        }
        candidates.sort(Comparator.comparingDouble(target -> target.contact().distanceSqr()));
        var damageSource = NarutoDamageTypes.getDamageSource(level, NarutoDamageTypes.CHIDORI, actor, actor);
        for (TargetHit candidate : candidates) {
            LivingEntity target = candidate.target();
            float damage = profile.amount(target);
            if (damage > 0 && target.hurt(damageSource, damage)) {
                if (profile.slowTarget()) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true));
                }
                return candidate;
            }
        }
        return null;
    }

    private static boolean canHit(LivingEntity actor, LivingEntity target) {
        if (target == actor || !target.isAlive() || target.isSpectator() || !target.isPickable()
                || Faction.sameSide(actor, target)) {
            return false;
        }
        if (actor instanceof Player player && target instanceof Player victim) {
            return !victim.isCreative() && player.canHarmPlayer(victim);
        }
        return !actor.isAlliedTo(target);
    }

    private static Vec3 blockContact(ServerLevel level, LivingEntity actor, Vec3 from, Vec3 wanted, Vec3 reached) {
        Vec3 end = from.add(wanted).add(wanted.normalize().scale(actor.getBbWidth() * 0.5));
        BlockHitResult collision = clip(level, actor, from, end);
        if (collision.getType() == HitResult.Type.BLOCK) {
            return collision.getLocation();
        }
        double low = -actor.getBbHeight() * 0.48;
        collision = clip(level, actor, from.add(0, low, 0), end.add(0, low, 0));
        return collision.getType() == HitResult.Type.BLOCK ? collision.getLocation() : reached;
    }

    private static BlockHitResult clip(ServerLevel level, LivingEntity actor, Vec3 from, Vec3 end) {
        return level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, actor));
    }

    private static void finish(ServerLevel level, LivingEntity actor, Dash dash, Phase phase, Vec3 point) {
        dash.recovery = RECOVERY_TICKS;
        actor.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, RECOVERY_TICKS, 1, false, false));
        broadcast(level, actor, dash, phase, point, point);
        if (phase != Phase.RECOVER) {
            level.playSound(null, point.x, point.y, point.z, SoundEvents.LIGHTNING_BOLT_IMPACT,
                    SoundSource.PLAYERS, 0.96f, phase == Phase.BLOCK ? 0.82f : 1.24f);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z,
                    8, 0.22, 0.27, 0.22, 0.08);
        }
    }

    private static Vec3 handHeight(LivingEntity actor) {
        return actor.position().add(0, actor.getBbHeight() * 0.58, 0);
    }

    private static void broadcast(ServerLevel level, LivingEntity actor, Dash dash, Phase phase, Vec3 start, Vec3 end) {
        PacketHandler.sendNear(new ChidoriDashPacket(level.dimension().location(), actor.getId(), phase,
                start, end, dash.heading), level, end, 64.0);
    }

    public record DamageProfile(float mobDamage, float playerDamage, boolean nonLethal, boolean slowTarget) {
        float amount(LivingEntity target) {
            if (!(target instanceof Player)) {
                return this.mobDamage;
            }
            return this.nonLethal ? Math.min(this.playerDamage, target.getHealth() - 1.0f) : this.playerDamage;
        }
    }

    private record TargetHit(LivingEntity target, ChidoriDashPath.Contact contact) {
    }

    private static final class Dash {
        private final Vec3 heading;
        private final DamageProfile damage;
        private final double[] steps;
        private int windup;
        private int index;
        private int recovery;

        private Dash(Vec3 heading, DamageProfile damage, double[] steps, int windup) {
            this.heading = heading;
            this.damage = damage;
            this.steps = steps;
            this.windup = windup;
        }
    }
}
