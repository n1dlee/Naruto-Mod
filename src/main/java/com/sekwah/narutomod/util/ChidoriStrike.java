package com.sekwah.narutomod.util;

import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.ChidoriImpactPacket;
import com.sekwah.narutomod.network.s2c.ChidoriImpactPacket.Kind;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

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
        if (!ninjaData.isChidoriActive() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 from = player.getEyePosition();
        Vec3 aim = player.getLookAngle();
        Vec3 end = from.add(aim.scale(REACH));
        var block = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        boolean blocked = block.getType() == HitResult.Type.BLOCK;
        Vec3 limit = blocked ? block.getLocation() : end;
        Contact contact = firstTarget(player, from, limit);

        ninjaData.setChidoriTicks(0);
        if (contact != null) {
            Kind kind = damage(player, ninjaData, contact.target) ? Kind.ENTITY : Kind.MISS;
            impact(level, contact.point, aim, kind);
        } else {
            Vec3 direction = blocked ? Vec3.atLowerCornerOf(block.getDirection().getNormal()) : aim;
            impact(level, limit, direction, blocked ? Kind.BLOCK : Kind.MISS);
        }
        recover(player);
    }

    private static Contact firstTarget(Player player, Vec3 from, Vec3 limit) {
        AABB sweep = new AABB(from, limit).inflate(0.1);
        Contact nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, sweep,
                living -> living != player && living.isAlive() && living.isPickable()
                        && !Faction.sameSide(player, living))) {
            var point = firstContact(candidate.getBoundingBox(), from, limit);
            if (point.isPresent() && point.get().distanceToSqr(from) < nearestDistance) {
                nearestDistance = point.get().distanceToSqr(from);
                nearest = new Contact(candidate, point.get());
            }
        }
        return nearest;
    }

    public static Optional<Vec3> firstContact(AABB box, Vec3 from, Vec3 limit) {
        if (from.distanceToSqr(limit) < 1.0E-9) {
            return Optional.empty();
        }
        return box.contains(from) ? Optional.of(from) : box.clip(from, limit);
    }

    private static boolean damage(Player attacker, INinjaData ninjaData, LivingEntity target) {
        float damage = (target instanceof Player ? 16.0F : 20.0F)
                * ninjaData.getRankDamageMultiplier() * ninjaData.getClanLightningDamageMultiplier();
        if (target instanceof Player && ninjaData.getNinjaRank() < 4) {
            damage = Math.min(damage, target.getHealth() - 1.0F);
        }
        return damage > 0 && target.hurt(NarutoDamageTypes.getDamageSource(
                attacker.level(), NarutoDamageTypes.CHIDORI, attacker, attacker), damage);
    }

    private static void impact(ServerLevel level, Vec3 point, Vec3 direction, Kind kind) {
        var packet = new ChidoriImpactPacket(level.dimension().location(), point, direction, kind,
                level.random.nextInt());
        PacketHandler.sendNear(packet, level, point, 48.0);
        var sound = kind == Kind.BLOCK ? SoundEvents.LIGHTNING_BOLT_IMPACT : NarutoSounds.CHIDORI.get();
        level.playSound(null, point.x, point.y, point.z, sound, SoundSource.PLAYERS,
                kind == Kind.MISS ? 0.45F : 0.9F, kind == Kind.MISS ? 0.72F : 1.2F);
    }

    /** The arm is committed and the user is open, for about a second. */
    private static void recover(Player player) {
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, RECOVERY_TICKS, 1, false, false));
        player.addEffect(new MobEffectInstance(
                MobEffects.DIG_SLOWDOWN, RECOVERY_TICKS, 1, false, false));
    }

    private record Contact(LivingEntity target, Vec3 point) {
    }
}
