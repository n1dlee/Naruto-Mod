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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Hyuga Clan — Eight Trigrams: Air Palm (combo 111).
 * Fires a concentrated burst of chakra in a cone (8 blocks, 30° half-angle).
 * All mobs in the cone take 8 damage and are knocked back strongly.
 * With Byakugan active: full 8-block range. Without: 4-block range.
 */
public class AirPalmAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 25f;
    private static final double FULL_RANGE = 8.0;
    private static final double HALF_RANGE = 4.0;
    private static final double HALF_ANGLE_COS = Math.cos(Math.toRadians(30));
    private static final float DAMAGE = 8.0f;
    private static final double KNOCKBACK = 3.5;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 111;
    }

    @Override
    public int getCooldown() {
        return 5 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.GUARDIAN_ATTACK;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!"hyuga".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.hyuga",
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
        boolean byakuganActive = ninjaData.isByakuganActive();
        double range = byakuganActive ? FULL_RANGE : HALF_RANGE;
        float damage = DAMAGE * ninjaData.getRankDamageMultiplier();

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(range));

        // Search box along the look direction
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(range * 0.5);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(eye).normalize();
            // Check cone angle
            if (toTarget.dot(look) >= HALF_ANGLE_COS) {
                double dist = eye.distanceTo(target.position());
                if (dist <= range) {
                    target.hurt(player.damageSources().playerAttack(player), damage);
                    // Knock target back and slightly upward
                    Vec3 pushDir = look.normalize();
                    target.knockback(KNOCKBACK, -pushDir.x, -pushDir.z);
                    Vec3 vel = target.getDeltaMovement();
                    target.setDeltaMovement(vel.x, Math.min(vel.y + 0.4, 0.8), vel.z);
                }
            }
        }

        // Visual: white cloud particles in a fan in front of player
        if (player.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 20; i++) {
                double t = i / 20.0;
                Vec3 pos = eye.add(look.scale(t * range));
                // Add slight spread
                double spread = t * range * 0.3;
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        pos.x, pos.y, pos.z,
                        1, spread * 0.4, spread * 0.2, spread * 0.4, 0.05);
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        pos.x, pos.y, pos.z,
                        1, spread * 0.3, 0.1, spread * 0.3, 0.02);
            }
        }
    }
}
