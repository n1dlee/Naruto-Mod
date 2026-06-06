package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Earth Style — Earth Spikes (combo 313).
 * Raises 5 dirt pillars in a line directly in front of the player (2-block spacing).
 * Each pillar is 3 blocks tall and lasts 10 seconds.
 * Mobs standing on the pillar positions take 8 damage + upward knockback.
 */
public class EarthSpikesAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 30f;
    private static final int SPIKE_COUNT = 5;
    private static final int SPIKE_HEIGHT = 3;
    private static final double SPIKE_SPACING = 2.0;
    private static final int LIFESPAN_TICKS = 10 * 20;
    private static final float SPIKE_DAMAGE = 8.0f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 313;
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
        ninjaData.useChakra(CHAKRA_COST, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        final float spikeDamage = SPIKE_DAMAGE * ninjaData.getRankDamageMultiplier();
        double yawRad = Math.toRadians(Math.round(player.getYRot() / 45.0) * 45.0);
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);

        // Build list of spike base positions
        List<BlockPos> spikeRoots = new ArrayList<>();
        for (int i = 1; i <= SPIKE_COUNT; i++) {
            double dist = i * SPIKE_SPACING + 1.0;
            int bx = (int) Math.round(player.getX() + fwdX * dist);
            int bz = (int) Math.round(player.getZ() + fwdZ * dist);
            // Find ground level at this xz
            int by = player.blockPosition().getY();
            while (by > player.level().getMinBuildHeight() &&
                    player.level().getBlockState(new BlockPos(bx, by, bz)).isAir()) {
                by--;
            }
            spikeRoots.add(new BlockPos(bx, by + 1, bz));
        }

        // For each spike: delay placement so they erupt in sequence
        for (int i = 0; i < spikeRoots.size(); i++) {
            final BlockPos root = spikeRoots.get(i);
            final int delay = 2 + i * 3;

            // Eruption event
            ninjaData.scheduleDelayedTickEvent((p) -> {
                if (!(p.level() instanceof ServerLevel serverLevel)) return;

                // Damage mobs at this position before raising
                AABB hitBox = new AABB(root).inflate(0.6, SPIKE_HEIGHT, 0.6);
                List<LivingEntity> targets = p.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                        e -> e != p && e.isAlive());
                for (LivingEntity e : targets) {
                    e.hurt(p.damageSources().playerAttack(p), spikeDamage);
                    Vec3 vel = e.getDeltaMovement();
                    e.setDeltaMovement(vel.x * 0.3, Math.min(vel.y + 0.8, 1.4), vel.z * 0.3);
                }

                // Raise the pillar: 3 blocks of dirt
                for (int h = 0; h < SPIKE_HEIGHT; h++) {
                    BlockPos pos = root.above(h);
                    if (serverLevel.getBlockState(pos).isAir()) {
                        serverLevel.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                    }
                }

                serverLevel.playSound(null, root, SoundEvents.GRAVEL_BREAK,
                        SoundSource.BLOCKS, 1.2f, 0.7f + (float) Math.random() * 0.3f);
            }, delay);

            // Removal event: scheduled separately via the same ninjaData reference
            ninjaData.scheduleDelayedTickEvent((p) -> {
                if (!(p.level() instanceof ServerLevel serverLevel)) return;
                for (int h = SPIKE_HEIGHT - 1; h >= 0; h--) {
                    BlockPos pos = root.above(h);
                    if (serverLevel.getBlockState(pos).getBlock() == Blocks.DIRT) {
                        serverLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                }
            }, delay + LIFESPAN_TICKS);
        }
    }
}
