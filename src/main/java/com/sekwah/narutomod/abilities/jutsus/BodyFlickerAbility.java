package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Body Flicker — Shunshin no Jutsu (combo 21).
 * Teleports the player 15 blocks in their look direction.
 * Stops at the first block encountered.
 * Leaves a CLOUD particle poof at the origin; appears with a CLOUD poof at the destination.
 * Costs 20 chakra, 8 second cooldown.
 */
public class BodyFlickerAbility extends Ability implements Ability.Cooldown {

    /** Exempt from the free-hands gate: this is a movement burst, not a hand-cast technique. */
    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    private static final float CHAKRA_COST = 15f;
    private static final float STAMINA_COST = 30f;
    private static final double[] RANK_RANGES = {8.0D, 12.0D, 15.0D, 25.0D, 40.0D};

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** A body flicker that reads as a pose has already failed to be one. */
    @Override
    public int castPoseTicks() {
        return 3;
    }

    @Override
    public long defaultCombo() {
        return 21;
    }

    @Override
    public int getCooldown() {
        return 8 * 20;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getStamina() < STAMINA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughstamina",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 20);
        ninjaData.useStamina(STAMINA_COST, 20);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double maxRange = RANK_RANGES[Math.min(Math.max(ninjaData.getNinjaRank(), 0), 4)];
        Vec3 destination = eye.add(look.scale(maxRange));

        // Block collision check — stop just before first block
        BlockHitResult blockHit = player.level().clip(
                new ClipContext(eye, destination, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        Vec3 target;
        if (blockHit.getType() != HitResult.Type.MISS) {
            // Stop 0.5 blocks before the block hit
            Vec3 hitDir = blockHit.getLocation().subtract(eye);
            double dist = Math.max(0, hitDir.length() - 0.5);
            target = eye.add(look.scale(dist));
        } else {
            target = destination;
        }

        // Particle poof at origin
        Vec3 origin = player.position();
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    origin.x, origin.y + player.getBbHeight() * 0.5, origin.z,
                    20, 0.3, 0.5, 0.3, 0.1);
            serverLevel.sendParticles(ParticleTypes.POOF,
                    origin.x, origin.y + player.getBbHeight() * 0.5, origin.z,
                    10, 0.2, 0.4, 0.2, 0.05);
        }

        // Teleport
        player.teleportTo(target.x, target.y - player.getBbHeight(), target.z);

        // Particle poof at destination
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    target.x, target.y - player.getBbHeight() + player.getBbHeight() * 0.5, target.z,
                    20, 0.3, 0.5, 0.3, 0.1);
            serverLevel.sendParticles(ParticleTypes.POOF,
                    target.x, target.y - player.getBbHeight() + player.getBbHeight() * 0.5, target.z,
                    10, 0.2, 0.4, 0.2, 0.05);
        }
    }
}
