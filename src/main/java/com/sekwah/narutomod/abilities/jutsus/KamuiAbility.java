package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Kamui (combo 1132) — Mangekyo Sharingan space-time ninjutsu: the user briefly slips
 * into Kamui's dimension, phasing THROUGH whatever is in front of them (walls included)
 * and re-emerging ~6 blocks ahead, intangible for the first moment after the shift.
 *
 * Requires an awakened Mangekyo (same gate as Susanoo/Amaterasu). The destination is
 * validated in handleCost so a fully-blocked phase fails cleanly without eating chakra.
 */
public class KamuiAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 70f;
    private static final double PHASE_DISTANCE = 6.0;
    private static final int INTANGIBLE_TICKS = 20;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** The eye has to be on the target for the space to fold. */
    @Override
    public int castPoseTicks() {
        return 14;
    }

    @Override
    public long defaultCombo() {
        return 1132;
    }

    @Override
    public int getCooldown() {
        return 25 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.ENDERMAN_TELEPORT;
    }

    /** Baseline Mangekyo technique — the phase-dash stays open to every awakened Mangekyo. */
    @Override
    public String requiredEye() {
        return "sharingan_ms";
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (findPhaseDestination(player) == null) {
            player.displayClientMessage(Component.literal("No space to re-emerge on the other side!")
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }
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
        Vec3 destination = findPhaseDestination(player);
        if (destination == null) {
            return;
        }

        // Swirl at the point of departure — the signature Kamui vortex
        if (player.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnSpiral(serverLevel,
                    player.position(), 0.9, 0.12, 18, NarutoParticles.SHADOW_PURPLE);
        }

        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        // Momentarily intangible after re-emerging — effectively immune while re-forming
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, INTANGIBLE_TICKS, 4, false, false));
        ninjaData.setInvisibleTicks(INTANGIBLE_TICKS / 2);

        player.level().playSound(null, destination.x, destination.y, destination.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.7f);
        if (player.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnSpiral(serverLevel, destination, 0.9, 0.12, 18, NarutoParticles.SHADOW_PURPLE);
        }
    }

    /**
     * Looks for 2 blocks of headroom at (or just above) the point PHASE_DISTANCE ahead —
     * walls in between are irrelevant (that's the whole point of phasing), only the exit
     * needs to be clear. Falls back through a few shorter distances before giving up.
     */
    private Vec3 findPhaseDestination(Player player) {
        Vec3 flatLook = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z).normalize();
        for (double distance = PHASE_DISTANCE; distance >= 3.0; distance -= 1.5) {
            Vec3 candidate = player.position().add(flatLook.scale(distance));
            for (int yOffset = 0; yOffset <= 2; yOffset++) {
                BlockPos feet = BlockPos.containing(candidate.x, candidate.y + yOffset, candidate.z);
                if (player.level().getBlockState(feet).isAir()
                        && player.level().getBlockState(feet.above()).isAir()) {
                    return new Vec3(candidate.x, feet.getY(), candidate.z);
                }
            }
        }
        return null;
    }
}
