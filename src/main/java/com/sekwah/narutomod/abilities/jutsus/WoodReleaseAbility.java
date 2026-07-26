package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Wood Release (Mokuton) — combo 3312, INSTANT.
 * Creates a 3x3x3 cube of Oak Logs around target entity (10 block raycast).
 * Requires Senju clan. Cost: 50 chakra. Cooldown: 15 seconds.
 */
public class WoodReleaseAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 60f;
    private static final double RANGE = 10.0;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 3312;
    }

    @Override
    public int getCooldown() {
        return 15 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.AZALEA_PLACE;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!"senju".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.senju",
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
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        // Find target entity via raycast
        AABB searchBox = new AABB(eye, eye.add(look.scale(RANGE))).inflate(1.0);
        List<Entity> entities = level.getEntities(player, searchBox,
                e -> e instanceof LivingEntity && e.isAlive() && e != player);

        LivingEntity target = null;
        double closestDist = RANGE + 1;

        for (Entity e : entities) {
            Vec3 toEntity = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            double dot = toEntity.dot(look);
            if (dot > 0 && dot < RANGE) {
                Vec3 proj = eye.add(look.scale(dot));
                double perpDist = proj.distanceTo(e.position().add(0, e.getBbHeight() / 2, 0));
                if (perpDist < 1.5 && dot < closestDist) {
                    closestDist = dot;
                    target = (LivingEntity) e;
                }
            }
        }

        if (target == null) {
            player.displayClientMessage(Component.literal("No target found!")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        // Create 3x3x3 wood cage around target
        BlockPos center = target.blockPosition();
        List<BlockPos> placedBlocks = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    // Only place on the edges (shell), not fill the inside
                    if (Math.abs(dx) == 1 || Math.abs(dz) == 1 || dy == 0 || dy == 2) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (level.getBlockState(pos).isAir() || level.getBlockState(pos).canBeReplaced()) {
                            level.setBlock(pos, Blocks.OAK_LOG.defaultBlockState(), 3);
                            placedBlocks.add(pos.immutable());
                        }
                    }
                }
            }
        }

        // Green leaf particles + a rising growth spiral around the cage shell
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.COMPOSTER,
                    center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5,
                    30, 1.5, 1.5, 1.5, 0.05);
            Vec3 base = Vec3.atCenterOf(center).subtract(0, 0.5, 0);
            NarutoParticles.spawnSpiral(serverLevel, base, 1.8, 0.3, 12, NarutoParticles.LOG_BROWN);
        }

        // Schedule wood removal after 10 seconds
        ninjaData.scheduleDelayedTickEvent(p -> {
            for (BlockPos pos : placedBlocks) {
                if (p.level().getBlockState(pos).is(Blocks.OAK_LOG)) {
                    p.level().destroyBlock(pos, false);
                }
            }
        }, 10 * 20);
    }
}
