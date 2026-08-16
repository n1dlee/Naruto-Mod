package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Lightning Style — False Darkness (combo 232).
 * Fires an instant lightning ray forward (20 blocks).
 * On hitting the first target: deals 15 damage and SPLITS into two side rays (±45°),
 * each dealing 8 damage to any targets hit.
 * All hit calculated as instant raycasts (no projectile entity).
 */
public class FalseDarknessAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 45f;
    private static final double RANGE = 20.0;
    private static final double SPLIT_RANGE = 10.0;
    private static final float MAIN_DAMAGE = 15.0f;
    private static final float SPLIT_DAMAGE = 8.0f;
    private static final double HIT_RADIUS = 1.0;
    private static final DustParticleOptions LIGHTNING_PARTICLE =
            new DustParticleOptions(new Vector3f(0.9f, 0.95f, 0.3f), 1.0f);

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** A sustained exhale, not a flick. */
    @Override
    public int castPoseTicks() {
        return 12;
    }

    @Override
    public long defaultCombo() {
        return 232;
    }

    @Override
    public int getCooldown() {
        return 12 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.LIGHTNING_BOLT_IMPACT;
    }
    // --- Phase 15: Nature Release ---
    @Override
    public String element() {
        return "lightning";
    }

    @Override
    public int elementLevelRequired() {
        return 3;
    }

    @Override
    public float elementXpReward() {
        return 20f;
    }


    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // A bolt that wanders but is pulled back to the aim line, and forks as it goes.
        if (player.level() instanceof net.minecraft.server.level.ServerLevel vfxLevel) {
            com.sekwah.narutomod.util.ElementalVfx.lightningBeam(vfxLevel,
                    player.getEyePosition(), player.getLookAngle(), 14.0, player.tickCount,
                    com.sekwah.narutomod.util.NarutoParticles.CHIDORI_CYAN);
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        DamageSource source = NarutoDamageTypes.getDamageSource(player.level(), NarutoDamageTypes.CHIDORI, player, player);
        float damageMultiplier = ninjaData.getRankDamageMultiplier() * ninjaData.getClanLightningDamageMultiplier();

        // Find main ray endpoint (respects blocks)
        Vec3 mainEnd = findRayEnd(player, eye, look, RANGE);

        // Draw main ray particles
        spawnRayParticles(player, eye, mainEnd);

        // Find first entity on main ray
        LivingEntity mainTarget = findFirstEntity(player, eye, look, eye.distanceTo(mainEnd));

        // One lance, and it pierces.
        //
        // This used to fork into two side rays at forty-five degrees, which is a different
        // technique: Gian is a single concentrated spear of lightning, and its whole identity
        // is that it goes THROUGH what it hits rather than spreading around it. The forking
        // version also quietly tripled the damage of a "single beam".
        //
        // The energy that went into the forks is now spent going further: everything standing
        // in the line takes the hit, with each body it passes through bleeding some of it.
        float remaining = MAIN_DAMAGE * damageMultiplier;
        for (LivingEntity pierced : findEntitiesAlongRay(player, eye, look, eye.distanceTo(mainEnd))) {
            if (remaining < 1.0f) {
                break;
            }
            pierced.hurt(source, remaining);
            spawnHitParticles(player, pierced.position().add(0, pierced.getBbHeight() * 0.5, 0));
            remaining *= PIERCE_FALLOFF;
        }
    }

    /** How much of the lance survives each body it passes through. */
    private static final float PIERCE_FALLOFF = 0.65f;

    /** Everything standing in the lance's line, nearest first. */
    private java.util.List<LivingEntity> findEntitiesAlongRay(Player player, Vec3 origin, Vec3 dir,
                                                              double range) {
        Vec3 end = origin.add(dir.scale(range));
        java.util.List<LivingEntity> hit = player.level().getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(origin, end).inflate(1.0),
                candidate -> candidate != player && candidate.isAlive()
                        && candidate.getBoundingBox().inflate(0.3)
                                .clip(origin, end).isPresent());
        hit.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(origin)));
        return hit;
    }

    private Vec3 findRayEnd(Player player, Vec3 origin, Vec3 dir, double range) {
        Vec3 end = origin.add(dir.scale(range));
        BlockHitResult blockHit = player.level().clip(
                new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
    }

    private LivingEntity findFirstEntity(Player player, Vec3 origin, Vec3 dir, double maxDist) {
        return findFirstEntityAlongRay(player, origin, dir, maxDist);
    }

    private LivingEntity findFirstEntityAlongRay(Player player, Vec3 origin, Vec3 dir, double range) {
        Vec3 end = origin.add(dir.scale(range));
        AABB box = new AABB(
                Math.min(origin.x, end.x) - HIT_RADIUS, origin.y - 1.5, Math.min(origin.z, end.z) - HIT_RADIUS,
                Math.max(origin.x, end.x) + HIT_RADIUS, origin.y + 3, Math.max(origin.z, end.z) + HIT_RADIUS);

        return player.level().getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != player && e.isAlive()).stream()
                .filter(e -> {
                    Vec3 toE = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(origin);
                    double proj = toE.dot(dir);
                    if (proj < 0 || proj > range) return false;
                    Vec3 closest = origin.add(dir.scale(proj));
                    return e.position().add(0, e.getBbHeight() * 0.5, 0).distanceTo(closest) <= HIT_RADIUS;
                })
                .min(java.util.Comparator.comparingDouble(e -> e.position().distanceTo(origin)))
                .orElse(null);
    }

    private void spawnRayParticles(Player player, Vec3 from, Vec3 to) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        Vec3 diff = to.subtract(from);
        int steps = (int)(diff.length() * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 pos = from.add(diff.scale(i / (double) Math.max(steps, 1)));
            serverLevel.sendParticles(LIGHTNING_PARTICLE, pos.x, pos.y, pos.z, 1, 0.04, 0.04, 0.04, 0.0);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 1, 0.06, 0.06, 0.06, 0.04);
        }
    }

    private void spawnHitParticles(Player player, Vec3 pos) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 20, 0.3, 0.3, 0.3, 0.1);
        serverLevel.sendParticles(LIGHTNING_PARTICLE, pos.x, pos.y, pos.z, 12, 0.2, 0.2, 0.2, 0.05);
    }

    private Vec3 rotateY(Vec3 v, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(v.x * cos - v.z * sin, 0, v.x * sin + v.z * cos).normalize();
    }
}
