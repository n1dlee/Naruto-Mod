package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.EyeTargeting;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Uchiha Gunbai: Wind Fan — Madara's signature Mangekyo technique (combo 1122).
 * A single sweep of the war fan hurls everything in a forward cone away with crushing
 * force. Madara's differentiator is raw battlefield control rather than genjutsu.
 */
public class GunbaiWindAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 70f;
    private static final double RANGE = 12.0;
    private static final float DAMAGE = 16f;
    private static final double KNOCKBACK = 2.6;
    /** Cone half-angle as a dot-product threshold — same shape check Air Palm uses. */
    private static final double CONE_DOT = 0.55;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** A full fan sweep. */
    @Override
    public int castPoseTicks() {
        return 12;
    }

    @Override
    public long defaultCombo() {
        return 1122;
    }

    @Override
    public String requiredEye() {
        return "sharingan_ems";
    }

    @Override
    public String requiredEyeForm() {
        return "madara";
    }

    @Override
    public int getCooldown() {
        return 20 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.PHANTOM_SWOOP;
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
        Vec3 look = player.getLookAngle().normalize();
        float multiplier = ninjaData.getRankDamageMultiplier();

        for (LivingEntity target : EyeTargeting.livingAround(player, RANGE)) {
            Vec3 toTarget = target.position().subtract(player.position()).normalize();
            if (toTarget.dot(look) < CONE_DOT) {
                continue;
            }
            target.hurt(player.damageSources().playerAttack(player), DAMAGE * multiplier);
            Vec3 push = look.scale(KNOCKBACK).add(0, 0.45, 0);
            target.setDeltaMovement(target.getDeltaMovement().add(push));
            target.hurtMarked = true;
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            // The sweep itself — a widening wall of displaced air along the look vector
            Vec3 origin = player.getEyePosition();
            for (int step = 1; step <= 12; step++) {
                Vec3 point = origin.add(look.scale(step));
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        point.x, point.y, point.z, 6, step * 0.08, step * 0.08, step * 0.08, 0.02);
            }
            NarutoParticles.spawnBurst(serverLevel, origin.add(look.scale(1.5)), 25, 1.0, ParticleTypes.CLOUD);
        }
    }
}
