package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Water Style — Water Dragon Bullet (combo 1312).
 * Fires a powerful torrent of water in the look direction (25 blocks).
 * On the first target hit: 18 damage + Slowness II (4s) + heavy knockback.
 * Also hits all entities in a 3-block splash radius around the impact point.
 * Extinguishes fire in a 5-block radius at impact.
 */
public class WaterDragonAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 55f;
    private static final double RANGE = 25.0;
    private static final double HIT_RADIUS = 1.2;
    private static final double SPLASH_RADIUS = 3.0;
    private static final float MAIN_DAMAGE = 18.0f;
    private static final float SPLASH_DAMAGE = 8.0f;
    private static final double KNOCKBACK = 4.0;
    private static final int WINDUP_TICKS = 9;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** A long seal chain in canon - the longest of the water techniques here. */
    @Override
    public int castPoseTicks() {
        return 14;
    }

    @Override
    public long defaultCombo() {
        return 1312;
    }

    @Override
    public int getCooldown() {
        return 12 * 20;
    }
    // --- Phase 15: Nature Release ---
    @Override
    public String element() {
        return "water";
    }

    @Override
    public int elementLevelRequired() {
        return 8;
    }

    @Override
    public float elementXpReward() {
        return 30f;
    }


    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 40);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        // Aim is locked in at cast time — the dragon coalesces at this point over the next
        // few ticks (WINDUP_TICKS) before striking, instead of an instant hit-scan.
        Vec3 end = eye.add(look.scale(RANGE));
        BlockHitResult blockHit = player.level().clip(
                new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 impactPoint = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        LivingEntity mainTarget = findFirstEntity(player, eye, look, eye.distanceTo(impactPoint));
        Vec3 coalescePoint = mainTarget != null
                ? mainTarget.position().add(0, mainTarget.getBbHeight() * 0.5, 0)
                : impactPoint;

        // The dragon itself. It used to be a spiral of particles at the destination and a
        // hit-scan - the technique announced a dragon and never put one on the field.
        com.sekwah.narutomod.entity.jutsuprojectile.ChakraDragonEntity dragon =
                new com.sekwah.narutomod.entity.jutsuprojectile.ChakraDragonEntity(
                        player, eye.add(look.scale(1.5)).add(0, -0.4, 0), coalescePoint,
                        com.sekwah.narutomod.entity.jutsuprojectile.ChakraDragonEntity.Kind.WATER)
                        .speed(1.15)
                        .damage(MAIN_DAMAGE * ninjaData.getRankDamageMultiplier(), SPLASH_RADIUS);
        player.level().addFreshEntity(dragon);

        for (int i = 0; i < WINDUP_TICKS; i++) {
            final double shrink = 1.6 - i * 0.14;
            ninjaData.scheduleDelayedTickEvent(p -> {
                if (p.level() instanceof ServerLevel serverLevel) {
                    NarutoParticles.spawnSpiral(serverLevel, coalescePoint, Math.max(shrink, 0.2), 0.15, 6, NarutoParticles.WATER_BLUE);
                    serverLevel.sendParticles(ParticleTypes.SPLASH,
                            coalescePoint.x, coalescePoint.y, coalescePoint.z, 4, 0.5, 0.3, 0.5, 0.02);
                }
            }, i + 1);
        }

        // The dragon IS the attack. There used to be a second, independent strike scheduled
        // here: the entity flew to a point computed at cast time while this hit whatever the
        // target had moved to, so one jutsu produced two unsynchronised hits - and the one
        // that mattered was the invisible one.
    }

    private void strike(Player player, INinjaData ninjaData, Vec3 eye, Vec3 impactPoint, LivingEntity mainTarget) {
        DamageSource source = NarutoDamageTypes.getDamageSource(
                player.level(), NarutoDamageTypes.WATER_BULLET, player, player);
        float damageMultiplier = ninjaData.getRankDamageMultiplier();

        boolean targetAlive = mainTarget != null && mainTarget.isAlive();
        LivingEntity hitTarget = targetAlive ? mainTarget : null;
        Vec3 splashCenter;
        if (targetAlive) {
            applyHit(mainTarget, player, source, MAIN_DAMAGE * damageMultiplier);
            splashCenter = mainTarget.position().add(0, mainTarget.getBbHeight() * 0.5, 0);
        } else {
            splashCenter = impactPoint;
        }

        // Splash damage around impact
        AABB splashBox = new AABB(
                splashCenter.x - SPLASH_RADIUS, splashCenter.y - SPLASH_RADIUS, splashCenter.z - SPLASH_RADIUS,
                splashCenter.x + SPLASH_RADIUS, splashCenter.y + SPLASH_RADIUS, splashCenter.z + SPLASH_RADIUS);
        List<LivingEntity> splashTargets = player.level().getEntitiesOfClass(LivingEntity.class, splashBox,
                e -> e != player && e != hitTarget && e.isAlive());
        for (LivingEntity target : splashTargets) {
            if (target.position().distanceTo(splashCenter) <= SPLASH_RADIUS) {
                target.hurt(source, SPLASH_DAMAGE * damageMultiplier);
                target.clearFire();
            }
        }

        // Extinguish fire in splash radius
        if (player.level() instanceof ServerLevel serverLevel) {
            int r = (int) SPLASH_RADIUS + 1;
            BlockPos center = BlockPos.containing(splashCenter);
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (serverLevel.getBlockState(pos).getBlock() == Blocks.FIRE) {
                            serverLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                        }
                    }
                }
            }
        }

        // Visual: draw water ray + splash particles
        spawnRayParticles(player, eye, splashCenter);
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    splashCenter.x, splashCenter.y, splashCenter.z,
                    60, SPLASH_RADIUS * 0.5, SPLASH_RADIUS * 0.5, SPLASH_RADIUS * 0.5, 0.3);
            serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                    splashCenter.x, splashCenter.y, splashCenter.z,
                    20, SPLASH_RADIUS * 0.4, SPLASH_RADIUS * 0.4, SPLASH_RADIUS * 0.4, 0.1);
        }
    }

    private void applyHit(LivingEntity target, Player player, DamageSource source, float damage) {
        target.hurt(source, damage);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 4 * 20, 1, false, true));
        target.clearFire();
        // Knockback away from player
        Vec3 dir = target.position().subtract(player.position()).normalize();
        target.knockback(KNOCKBACK, -dir.x, -dir.z);
    }

    private LivingEntity findFirstEntity(Player player, Vec3 origin, Vec3 dir, double maxDist) {
        Vec3 end = origin.add(dir.scale(maxDist));
        AABB box = new AABB(
                Math.min(origin.x, end.x) - HIT_RADIUS, origin.y - 2, Math.min(origin.z, end.z) - HIT_RADIUS,
                Math.max(origin.x, end.x) + HIT_RADIUS, origin.y + 4, Math.max(origin.z, end.z) + HIT_RADIUS);
        return player.level().getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != player && e.isAlive()).stream()
                .filter(e -> {
                    Vec3 toE = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(origin);
                    double proj = toE.dot(dir);
                    if (proj < 0 || proj > maxDist) return false;
                    Vec3 closest = origin.add(dir.scale(proj));
                    return e.position().add(0, e.getBbHeight() * 0.5, 0).distanceTo(closest) <= HIT_RADIUS;
                })
                .min(java.util.Comparator.comparingDouble(e -> e.position().distanceTo(origin)))
                .orElse(null);
    }

    private void spawnRayParticles(Player player, Vec3 from, Vec3 to) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        Vec3 diff = to.subtract(from);
        int steps = (int)(diff.length() * 4);
        for (int i = 0; i <= steps; i++) {
            Vec3 pos = from.add(diff.scale(i / (double) Math.max(steps, 1)));
            serverLevel.sendParticles(NarutoParticles.WATER_BLUE, pos.x, pos.y, pos.z, 2, 0.15, 0.15, 0.15, 0.0);
            serverLevel.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y, pos.z, 1, 0.1, 0.1, 0.1, 0.05);
        }
    }
}
