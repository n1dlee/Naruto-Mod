package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import org.joml.Vector3f;

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

    private static final DustParticleOptions WATER_PARTICLE =
            new DustParticleOptions(new Vector3f(0.15f, 0.55f, 1.0f), 1.2f);

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 1312;
    }

    @Override
    public int getCooldown() {
        return 12 * 20;
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
        DamageSource source = NarutoDamageTypes.getDamageSource(
                player.level(), NarutoDamageTypes.WATER_BULLET, player, player);
        float damageMultiplier = ninjaData.getRankDamageMultiplier();

        // Find ray endpoint (block-aware)
        Vec3 end = eye.add(look.scale(RANGE));
        BlockHitResult blockHit = player.level().clip(
                new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 impactPoint = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

        // Find first entity on ray
        LivingEntity mainTarget = findFirstEntity(player, eye, look, eye.distanceTo(impactPoint));

        Vec3 splashCenter;
        if (mainTarget != null) {
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
                e -> e != player && e != mainTarget && e.isAlive());
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
            serverLevel.sendParticles(WATER_PARTICLE, pos.x, pos.y, pos.z, 2, 0.15, 0.15, 0.15, 0.0);
            serverLevel.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y, pos.z, 1, 0.1, 0.1, 0.1, 0.05);
        }
    }
}
