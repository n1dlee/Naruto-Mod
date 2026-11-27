package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.KaitenPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.KaitenDome;
import com.sekwah.narutomod.util.KaitenDome.Step;
import com.sekwah.narutomod.util.WaterDragonFlight;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KaitenEvents {
    private static final Map<LivingEntity, Spin> SPINS = new WeakHashMap<>();

    private KaitenEvents() {
    }

    public static void maintain(Player player) {
        Spin spin = SPINS.get(player);
        if (spin == null) {
            return;
        }
        if (!valid(player, spin)) {
            end(player);
            return;
        }
        spin.heartbeat = spin.level.getGameTime();
    }

    public static boolean canStart(LivingEntity actor) {
        return actor.level() instanceof ServerLevel && actor.isAlive() && actor.onGround() && !actor.isPassenger()
                && !actor.noPhysics && !actor.isSpectator() && !AdamantineChainEvents.sealed(actor)
                && !TrigramPalmsEvents.running(actor) && !SPINS.containsKey(actor);
    }

    public static void start(LivingEntity actor, float damage, int duration) {
        if (actor.level() instanceof ServerLevel level && canStart(actor)) {
            var spin = new Spin(level, actor.position().add(0, 0.06, 0), damage, duration);
            SPINS.put(actor, spin);
            brace(actor);
            publish(actor, spin, Step.GATHER, spin.center);
        }
    }

    public static void end(LivingEntity actor) {
        Spin spin = SPINS.remove(actor);
        if (spin != null) {
            publish(actor, spin, Step.COLLAPSE, spin.center);
        }
    }

    private static boolean valid(LivingEntity actor, Spin spin) {
        return actor.level() == spin.level && actor.isAlive() && !actor.isSpectator() && !actor.noPhysics
                && !actor.isPassenger() && !AdamantineChainEvents.sealed(actor)
                && actor.position().distanceToSqr(spin.center) <= 0.65 * 0.65
                && spin.age() < KaitenDome.WINDUP + spin.duration
                && (!(actor instanceof Player) || spin.level.getGameTime() - spin.heartbeat <= 2);
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !(event.level instanceof ServerLevel level) || SPINS.isEmpty()) {
            return;
        }
        var reflected = new HashSet<Projectile>();
        for (LivingEntity actor : new ArrayList<>(SPINS.keySet())) {
            Spin spin = SPINS.get(actor);
            if (spin == null || spin.level != level) {
                continue;
            }
            if (!valid(actor, spin)) {
                end(actor);
                continue;
            }
            brace(actor);
            long age = spin.age();
            if (KaitenDome.active(age)) {
                if ((age - KaitenDome.WINDUP) % 8 == 0) {
                    publish(actor, spin, Step.ACTIVE, spin.center);
                }
                if (age == KaitenDome.WINDUP) {
                    tone(spin, spin.center, false);
                }
                reflectIncoming(actor, spin, reflected);
                sweep(actor, spin);
            }
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity actor) {
            end(actor);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void atProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity actor)
                || actor.level().isClientSide || !active(actor)) {
            return;
        }
        Projectile projectile = event.getProjectile();
        Spin spin = SPINS.get(actor);
        if (eligibleProjectile(actor, projectile) && tryReflect(actor, spin, projectile, hit.getLocation())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void deflectMelee(LivingAttackEvent event) {
        LivingEntity actor = event.getEntity();
        var source = event.getSource();
        if (actor.level().isClientSide || !active(actor)
                || !(source.getDirectEntity() instanceof LivingEntity enemy) || !enemy(actor, enemy)
                || !(source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO))) {
            return;
        }
        Spin spin = SPINS.get(actor);
        Vec3 origin = enemy.getEyePosition();
        Vec3 hit = KaitenDome.entry(spin.center, origin, actor.getEyePosition());
        if (hit != null && clear(spin.level, actor, origin, actor.getEyePosition())) {
            event.setCanceled(true);
            contact(actor, spin, hit);
        }
    }

    public static boolean spinning(LivingEntity actor) {
        return !actor.level().isClientSide && SPINS.containsKey(actor);
    }

    private static boolean active(LivingEntity actor) {
        Spin spin = SPINS.get(actor);
        return spin != null && KaitenDome.active(spin.age()) && valid(actor, spin);
    }

    private static void brace(LivingEntity actor) {
        if (actor instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        Vec3 motion = actor.getDeltaMovement();
        if (motion.horizontalDistanceSqr() > 0.0015) {
            actor.setDeltaMovement(motion.x * 0.18, motion.y, motion.z * 0.18);
            actor.hurtMarked = true;
        }
    }

    private static void reflectIncoming(LivingEntity actor, Spin spin, Set<Projectile> reflected) {
        AABB bounds = new AABB(spin.center, spin.center).inflate(KaitenDome.RADIUS + 4.5, KaitenDome.HEIGHT + 2.5, KaitenDome.RADIUS + 4.5);
        for (Projectile projectile : spin.level.getEntitiesOfClass(Projectile.class, bounds,
                test -> !reflected.contains(test) && eligibleProjectile(actor, test))) {
            if (tryReflect(actor, spin, projectile, projectile.position().add(projectile.getDeltaMovement()))) {
                reflected.add(projectile);
            }
        }
    }

    private static boolean eligibleProjectile(LivingEntity actor, Projectile projectile) {
        Class<?> type = projectile.getClass();
        return (type == Arrow.class || type == SpectralArrow.class || type == Snowball.class || type == ThrownEgg.class)
                && projectile.getDeltaMovement().lengthSqr() > 0.01 && enemy(actor, projectile.getOwner());
    }

    private static boolean tryReflect(LivingEntity actor, Spin spin, Projectile projectile, Vec3 destination) {
        Vec3 from = projectile.position();
        Vec3 hit = KaitenDome.entry(spin.center, from, destination);
        if (hit == null || !clear(spin.level, actor, from, hit) || !clear(spin.level, actor, spin.center, hit)) {
            return false;
        }
        Vec3 normal = KaitenDome.normal(hit.subtract(spin.center));
        Vec3 rebound = KaitenDome.reflect(projectile.getDeltaMovement(), normal);
        projectile.setPos(hit.add(normal.scale(0.085)));
        projectile.setOwner(actor);
        if (projectile instanceof AbstractArrow arrow) {
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        }
        projectile.setDeltaMovement(rebound);
        projectile.setYRot((float) Math.toDegrees(Math.atan2(rebound.x, rebound.z)));
        projectile.setXRot((float) Math.toDegrees(Math.atan2(rebound.y, rebound.horizontalDistance())));
        projectile.hasImpulse = true;
        projectile.hurtMarked = true;
        contact(actor, spin, hit);
        return true;
    }

    private static void sweep(LivingEntity actor, Spin spin) {
        AABB box = new AABB(spin.center.add(-KaitenDome.RADIUS, 0, -KaitenDome.RADIUS),
                spin.center.add(KaitenDome.RADIUS, KaitenDome.HEIGHT, KaitenDome.RADIUS));
        for (LivingEntity other : spin.level.getEntitiesOfClass(LivingEntity.class, box, candidate -> candidate.isAlive()
                && !candidate.isSpectator() && !spin.touched.contains(candidate.getUUID()) && enemy(actor, candidate))) {
            Vec3 point = WaterDragonFlight.nearestPoint(other.getBoundingBox(), spin.center.add(0, 0.65, 0));
            if (point.y < spin.center.y || KaitenDome.scaled(point.subtract(spin.center)).lengthSqr() > 1
                    || !clear(spin.level, actor, spin.center, point)) {
                continue;
            }
            Vec3 outward = other.position().subtract(spin.center);
            Vec3 rim = KaitenDome.surface(Math.atan2(outward.z, outward.x), 0.17).add(spin.center);
            boolean touchesShell = KaitenDome.scaled(point.subtract(spin.center)).lengthSqr() >= 0.65;
            var source = actor instanceof Player player ? actor.damageSources().playerAttack(player) : actor.damageSources().mobAttack(actor);
            spin.touched.add(other.getUUID());
            if (!touchesShell || other.hurt(source, Math.min(spin.damage, other instanceof Player ? 4.5f : 9f))) {
                other.knockback(1.32, -outward.x, -outward.z);
                other.hurtMarked = true;
                if (touchesShell && clear(spin.level, actor, spin.center, rim)) {
                    contact(actor, spin, rim);
                }
            }
        }
    }

    private static boolean enemy(LivingEntity actor, Entity other) {
        return other == null || other != actor && !actor.isAlliedTo(other) && !Faction.sameSide(actor, other)
                && !(other instanceof TamableAnimal tame && tame.isOwnedBy(actor))
                && (!(other instanceof Player target) || !target.isCreative()
                && (!(actor instanceof Player player) || player.canHarmPlayer(target)));
    }

    private static boolean clear(ServerLevel level, LivingEntity actor, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, actor))
                .getType() == HitResult.Type.MISS;
    }

    private static void contact(LivingEntity actor, Spin spin, Vec3 point) {
        if (spin.flashTick != spin.level.getGameTime()) {
            spin.flashTick = spin.level.getGameTime();
            publish(actor, spin, Step.CONTACT, point);
            tone(spin, point, true);
        }
    }

    private static void tone(Spin spin, Vec3 point, boolean hit) {
        var sound = hit ? NarutoSounds.KAITEN_CONTACT.get() : NarutoSounds.KAITEN_SPIN.get();
        spin.level.playSound(null, point.x, point.y, point.z, sound, SoundSource.PLAYERS, hit ? 0.76f : 0.87f, 1);
    }

    private static void publish(LivingEntity actor, Spin spin, Step step, Vec3 point) {
        PacketHandler.sendNear(new KaitenPacket(spin.level.dimension().location(), actor.getId(), step, spin.center, point),
                spin.level, spin.center, 38);
    }

    private static final class Spin {
        private final ServerLevel level;
        private final Vec3 center;
        private final long start;
        private final float damage;
        private final int duration;
        private final Set<UUID> touched = new HashSet<>();
        private long heartbeat;
        private long flashTick = Long.MIN_VALUE;

        private Spin(ServerLevel level, Vec3 center, float damage, int duration) {
            this.level = level;
            this.center = center;
            this.damage = damage;
            this.duration = duration;
            this.start = level.getGameTime();
            this.heartbeat = this.start;
        }

        private long age() { return this.level.getGameTime() - this.start; }
    }
}
