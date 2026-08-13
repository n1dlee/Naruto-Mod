package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Inuzuka Clan — Fang Over Fang / Gatsuga (combo 223, INSTANT).
 * The Inuzuka spin into a ferocious drilling cyclone and bore through the enemy.
 * Any of the summoner's tamed wolves standing nearby join the charge, launched along
 * the same vector — the canonical man-and-beast twin drill.
 */
public class GatsugaAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 35f;
    private static final double DRILL_DISTANCE = 10.0;
    private static final double HIT_RADIUS = 1.4;
    private static final float DAMAGE = 10.0f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** The spin does the talking. */
    @Override
    public int castPoseTicks() {
        return 10;
    }

    @Override
    public long defaultCombo() {
        return 223;
    }

    @Override
    public int getCooldown() {
        return 12 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.WOLF_GROWL;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!"inuzuka".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.inuzuka",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 20);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        Vec3 look = player.getLookAngle().normalize();
        float damage = DAMAGE * ninjaData.getRankDamageMultiplier();

        // Drill damage along the charge path
        Vec3 start = player.position().add(0, player.getBbHeight() * 0.5, 0);
        AABB drillBox = player.getBoundingBox().expandTowards(look.scale(DRILL_DISTANCE)).inflate(HIT_RADIUS);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, drillBox,
                e -> e != player && e.isAlive() && !(e instanceof Wolf wolf && wolf.isOwnedBy(player)));
        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(start);
            double along = toTarget.dot(look);
            if (along < 0 || along > DRILL_DISTANCE) continue;
            if (toTarget.subtract(look.scale(along)).length() > HIT_RADIUS) continue;
            target.hurt(player.damageSources().playerAttack(player), damage);
            target.knockback(1.3, -look.x, -look.z);
        }

        // Launch the spin — and any nearby ninken join the twin drill
        player.setDeltaMovement(look.x * 2.3, Math.max(look.y * 0.6, 0.1), look.z * 2.3);
        player.hurtMarked = true;
        for (Wolf wolf : player.level().getEntitiesOfClass(Wolf.class,
                player.getBoundingBox().inflate(6), w -> w.isOwnedBy(player) && w.isAlive())) {
            wolf.setDeltaMovement(look.x * 2.0, 0.25, look.z * 2.0);
            wolf.hurtMarked = true;
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i <= 14; i++) {
                Vec3 pos = start.add(look.scale(DRILL_DISTANCE * i / 14.0));
                double angle = i * 1.3;
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        pos.x + Math.cos(angle) * 0.7, pos.y + Math.sin(angle) * 0.5, pos.z + Math.sin(angle) * 0.7,
                        1, 0.05, 0.05, 0.05, 0.02);
                serverLevel.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 2, 0.3, 0.3, 0.3, 0.05);
            }
        }
    }
}
