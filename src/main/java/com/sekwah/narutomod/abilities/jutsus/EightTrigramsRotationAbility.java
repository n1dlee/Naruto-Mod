package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Hyuga Clan — Eight Trigrams: Rotation (combo 1111).
 * CHANNELED defensive technique: the player spins and emits a chakra sphere around them (radius 2.5 blocks).
 * - All projectiles entering the sphere are deflected (discarded).
 * - All mobs inside the sphere take 6 damage + knockback every 10 ticks.
 * - The player cannot move (slowness V applied each tick of channeling).
 * - White glowing particles spiral around the player while active.
 * - Hyuga clan only. Costs 45 chakra + 2/tick while sustained.
 */
public class EightTrigramsRotationAbility extends Ability implements Ability.Channeled, Ability.Cooldown {

    private static final float CHAKRA_BASE = 45f;
    private static final float CHAKRA_PER_TICK = 2.0f;
    private static final double RADIUS = 2.5;
    private static final float MOB_DAMAGE = 6.0f;

    private static final DustParticleOptions WHITE_PARTICLE =
            new DustParticleOptions(new Vector3f(0.9f, 0.95f, 1.0f), 1.2f);

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public long defaultCombo() {
        return 1111;
    }

    @Override
    public int getCooldown() {
        return 20 * 20;
    }

    @Override
    public boolean canActivateBelowMinCharge() {
        return false;
    }

    @Override
    public boolean useChargedMessages() {
        return false;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!"hyuga".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.hyuga",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (chargeAmount == 0) {
            // Initial activation cost
            if (ninjaData.getChakra() < CHAKRA_BASE) {
                player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                        Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
                return false;
            }
            ninjaData.useChakra(CHAKRA_BASE, 5);
        } else {
            // Sustain cost per tick
            if (ninjaData.getChakra() < CHAKRA_PER_TICK) {
                return false; // stop channeling
            }
            ninjaData.useChakra(CHAKRA_PER_TICK, 2);
        }
        return true;
    }

    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksActive) {
        // Freeze player in place
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 5, 4, false, false));
        float damage = MOB_DAMAGE * ninjaData.getRankDamageMultiplier();

        Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);

        // Deflect projectiles in sphere
        AABB sphereBox = player.getBoundingBox().inflate(RADIUS);
        List<Projectile> projectiles = player.level().getEntitiesOfClass(Projectile.class, sphereBox,
                p -> p.getOwner() != player);
        for (Projectile proj : projectiles) {
            if (proj.position().distanceTo(center) <= RADIUS) {
                proj.discard();
            }
        }

        // Damage mobs every 10 ticks
        if (ticksActive % 10 == 0) {
            List<LivingEntity> mobs = player.level().getEntitiesOfClass(LivingEntity.class, sphereBox,
                    e -> e != player && e.isAlive());
            for (LivingEntity mob : mobs) {
                if (mob.position().distanceTo(center) <= RADIUS) {
                    mob.hurt(player.damageSources().playerAttack(player), damage);
                    // Radial knockback outward
                    Vec3 dir = mob.position().subtract(player.position()).normalize();
                    mob.knockback(4.0, -dir.x, -dir.z);
                }
            }
        }

        // White spiral particles around player
        if (player.level() instanceof ServerLevel serverLevel) {
            double angle = (ticksActive * 18.0) % 360.0;
            for (int i = 0; i < 8; i++) {
                double theta = Math.toRadians(angle + i * 45.0);
                double px = center.x + RADIUS * Math.cos(theta);
                double pz = center.z + RADIUS * Math.sin(theta);
                double py = center.y + Math.sin(Math.toRadians(ticksActive * 12.0 + i * 45.0)) * 0.8;
                serverLevel.sendParticles(WHITE_PARTICLE, px, py, pz, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Nothing special on release — the channeling handled everything
    }
}
