package com.sekwah.narutomod.util;

import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Chidori as a thing with a body, rather than a damage bonus on your next sword swing.
 *
 * Activating it used to set a three-second flag and nothing else. The lightning was drawn in
 * the hand, but the hand was not a weapon: you closed the distance, hit the attack key, and
 * vanilla melee decided whether anything happened — with vanilla's reach, vanilla's target
 * pick and vanilla's timing. The technique had no presence in the world, which is why it read
 * as a buff. The ability's own comment already described what it was supposed to be — "you
 * raise it, you go, and if you miss you have spent it" — and none of that was true.
 *
 * So the arm now resolves on its own terms:
 *
 *  - a {@link #thrust} traced from the eyes along the look vector, at the reach of an extended
 *    arm with a fistful of lightning on the end of it, rather than at whatever the vanilla
 *    attack raycast happened to pick;
 *  - a {@link #discharge} when that thrust finds nothing, because a committed run at someone
 *    that misses has to cost something or it is not a commitment;
 *  - a {@link #recover} window on either outcome, since the arm is buried and the user is
 *    open. That is the half of the technique the anime spends the most time on.
 *
 * Landing it still also works through an ordinary melee hit, so the technique did not get
 * harder to use — {@link #land} is the single place that knows what a Chidori hit is, and both
 * paths go through it.
 */
public final class ChidoriStrike {

    /**
     * How far the lit hand reaches, from the eyes.
     *
     * A little past vanilla's three blocks: the arm is extended and there is a foot of
     * lightning past the fist. Not far enough to be a ranged attack — the whole point of the
     * technique is that you had to get there.
     */
    public static final double REACH = 3.6;

    /**
     * How long the user is committed after the arm goes in, hit or miss.
     *
     * Short, but real. Without it the correct way to play a Chidori was to spam it into a
     * crowd, because a miss cost nothing but chakra and a hit cost nothing at all.
     */
    public static final int RECOVERY_TICKS = 20;

    private ChidoriStrike() {
    }

    /**
     * Resolves the arm along the user's look vector.
     *
     * <p>Called on the swing rather than on a landed vanilla hit, so that a thrust into empty
     * air is a thing that happens and can be answered by dodging.
     */
    public static void thrust(Player player, INinjaData ninjaData) {
        if (!ninjaData.isChidoriActive()) {
            return;
        }
        LivingEntity victim = findVictim(player);
        if (victim == null) {
            discharge(player, ninjaData);
            return;
        }
        land(player, ninjaData, victim);
    }

    /**
     * The first thing the arm passes through.
     *
     * Nearest along the look vector rather than the first found, because a swing that clips
     * the edge of something behind the target should not pick the far one.
     */
    @Nullable
    private static LivingEntity findVictim(Player player) {
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(REACH));
        AABB sweep = new AABB(from, to).inflate(0.55);

        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Entity candidate : player.level().getEntities(player, sweep,
                entity -> entity instanceof LivingEntity living
                        && living.isAlive()
                        && living.isPickable()
                        && !Faction.sameSide(player, living))) {
            // Distance to the line of the thrust, not to the player: a target off to one side
            // at arm's length is not in front of the fist.
            Vec3 nearestOnLine = nearestPointOnSegment(from, to, candidate.getBoundingBox().getCenter());
            if (candidate.getBoundingBox().inflate(0.35).contains(nearestOnLine)) {
                double distance = nearestOnLine.distanceToSqr(from);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = (LivingEntity) candidate;
                }
            }
        }
        return nearest;
    }

    /**
     * Closest point to {@code point} on the segment from {@code a} to {@code b}.
     *
     * Clamped to the segment rather than the infinite line, which is the whole difference
     * between "in front of the fist" and "somewhere along that heading, possibly behind you".
     */
    public static Vec3 nearestPointOnSegment(Vec3 a, Vec3 b, Vec3 point) {
        Vec3 segment = b.subtract(a);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 1.0E-6) {
            return a;
        }
        double t = Math.max(0.0, Math.min(1.0, point.subtract(a).dot(segment) / lengthSqr));
        return a.add(segment.scale(t));
    }

    /**
     * The hit itself: the one place that knows what a Chidori does to somebody.
     *
     * Both the thrust above and the ordinary melee path in PlayerEvents call this, so the
     * technique cannot drift into two different attacks depending on how it connected.
     */
    public static void land(Player attacker, INinjaData ninjaData, LivingEntity target) {
        DamageSource source = NarutoDamageTypes.getDamageSource(
                attacker.level(), NarutoDamageTypes.CHIDORI, attacker, attacker);
        // Cleared BEFORE hurt(): hurt() fires LivingHurtEvent synchronously, and the melee
        // handler that also lands this technique reads the same flag. Leaving it set here is
        // an infinite recursion, which is exactly how the Rasengan version of this method
        // crashed the game before it was fixed.
        ninjaData.setChidoriTicks(0);

        float damageMultiplier = ninjaData.getRankDamageMultiplier()
                * ninjaData.getClanLightningDamageMultiplier();
        if (target instanceof Player targetPlayer) {
            float damage = 16.0F * damageMultiplier;
            if (ninjaData.getNinjaRank() < 4) {
                damage = Math.min(damage, targetPlayer.getHealth() - 1.0F);
            }
            if (damage > 0.0F) {
                target.hurt(source, damage);
            }
        } else {
            target.hurt(source, 20.0F * damageMultiplier);
        }

        attacker.level().playSound(null, attacker, NarutoSounds.CHIDORI.get(),
                SoundSource.PLAYERS, 1.0F, 1.15F);
        if (attacker.level() instanceof ServerLevel serverLevel) {
            Vec3 pos = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
            serverLevel.sendParticles(NarutoParticles.CHIDORI_CYAN, pos.x, pos.y, pos.z,
                    14, 0.3D, 0.35D, 0.3D, 0.04D);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z,
                    18, 0.35D, 0.4D, 0.35D, 0.08D);
        }
        recover(attacker);
    }

    /** A thrust that found nothing: the charge earths itself and the technique is spent. */
    public static void discharge(Player player, INinjaData ninjaData) {
        ninjaData.setChidoriTicks(0);
        Vec3 tip = player.getEyePosition().add(player.getLookAngle().scale(REACH * 0.7));
        player.level().playSound(null, player, NarutoSounds.CHIDORI.get(),
                SoundSource.PLAYERS, 0.7F, 0.7F);
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, tip.x, tip.y, tip.z,
                    22, 0.25D, 0.25D, 0.25D, 0.35D);
            serverLevel.sendParticles(NarutoParticles.CHIDORI_CYAN, tip.x, tip.y, tip.z,
                    10, 0.3D, 0.3D, 0.3D, 0.05D);
        }
        recover(player);
    }

    /** The arm is committed and the user is open, for about a second. */
    private static void recover(Player player) {
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, RECOVERY_TICKS, 1, false, false));
        player.addEffect(new MobEffectInstance(
                MobEffects.DIG_SLOWDOWN, RECOVERY_TICKS, 1, false, false));
    }
}
