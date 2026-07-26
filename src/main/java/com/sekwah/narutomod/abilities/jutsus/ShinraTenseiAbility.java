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
 * Shinra Tensei — Deva Path (combo 1221).
 * Repels everything around the user with overwhelming force: heavy damage falling off
 * with distance, and a shove strong enough to clear a fight. The Rinnegan's signature.
 */
public class ShinraTenseiAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 110f;
    private static final double RADIUS = 12.0;
    private static final float MAX_DAMAGE = 20f;
    private static final double MAX_PUSH = 3.2;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 1221;
    }

    @Override
    public String requiredEye() {
        return "rinnegan_path:deva";
    }

    @Override
    public int getCooldown() {
        return 40 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.WARDEN_SONIC_BOOM;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 60);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        float multiplier = ninjaData.getRankDamageMultiplier();
        Vec3 origin = player.position();

        for (LivingEntity target : EyeTargeting.livingAround(player, RADIUS)) {
            double distance = target.position().distanceTo(origin);
            // Force falls off with range — standing next to the user is fatal, the edge is a shove
            double falloff = Math.max(0.0, 1.0 - distance / RADIUS);
            target.hurt(player.damageSources().magic(), (float) (MAX_DAMAGE * falloff * multiplier));

            Vec3 away = target.position().subtract(origin).normalize();
            if (away.lengthSqr() < 1.0E-4) {
                away = player.getLookAngle();
            }
            Vec3 push = away.scale(MAX_PUSH * falloff).add(0, 0.5 * falloff, 0);
            target.setDeltaMovement(target.getDeltaMovement().add(push));
            target.hurtMarked = true;
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            // Expanding shell of displaced air
            for (int ring = 1; ring <= 4; ring++) {
                NarutoParticles.spawnRing(serverLevel, origin.add(0, 1.0, 0),
                        RADIUS * ring / 4.0, 20 + ring * 8, ParticleTypes.CLOUD);
            }
            NarutoParticles.spawnBurst(serverLevel, origin.add(0, 1.0, 0), 40, 1.5, ParticleTypes.EXPLOSION);
        }
    }
}
