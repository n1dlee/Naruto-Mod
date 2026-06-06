package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Flying Thunder God (Hiraishin) — combo 1213, INSTANT.
 * First use: marks current position. Second use: teleports to mark.
 * Requires Uzumaki clan OR Kage rank.
 */
public class FlyingThunderGodAbility extends Ability implements Ability.Cooldown {

    private static final float MARK_COST = 50f;
    private static final float TELEPORT_COST = 80f;
    private static final double MARK_RANGE = 64.0D;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 1213;
    }

    @Override
    public int getCooldown() {
        return 10 * 20;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        // Check clan/rank requirement
        String clan = ninjaData.getClanId();
        int rank = ninjaData.getNinjaRank();
        if (!"uzumaki".equals(clan) && rank < 4) { // 4 = Kage
            player.displayClientMessage(Component.literal("You must be Uzumaki clan or Kage rank!")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        BlockPos mark = ninjaData.getThunderGodMark();

        if (mark == null) {
            // Phase 1: Mark — no cooldown
            if (ninjaData.getChakra() < MARK_COST) {
                player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                        Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
                return false;
            }
            ninjaData.useChakra(MARK_COST, 10);
            BlockPos markPos = findLookMark(player);
            ninjaData.setThunderGodMark(markPos);
            player.displayClientMessage(Component.literal("Position marked!")
                    .withStyle(ChatFormatting.YELLOW), true);
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.1f), 1.5f),
                        markPos.getX() + 0.5D, markPos.getY() + 1.0D, markPos.getZ() + 0.5D,
                        10, 0.3, 0.5, 0.3, 0.05);
            }
            return false; // No cooldown for marking
        }

        // Phase 2: Teleport
        if (ninjaData.getChakra() < TELEPORT_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(TELEPORT_COST, 20);
        return true; // Cooldown on teleport
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        BlockPos mark = ninjaData.getThunderGodMark();
        if (mark == null) return; // Shouldn't happen — marking is handled in handleCost

        // Yellow flash particles at source
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.1f), 2.0f),
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    20, 0.5, 1.0, 0.5, 0.1);
        }

        // Teleport
        player.teleportTo(mark.getX() + 0.5, mark.getY(), mark.getZ() + 0.5);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.level().playSound(null, mark.getX(), mark.getY(), mark.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.5f);

        // Yellow flash particles at destination
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.1f), 2.0f),
                    mark.getX() + 0.5, mark.getY() + 1.0, mark.getZ() + 0.5,
                    20, 0.5, 1.0, 0.5, 0.1);
        }

        // Clear mark after teleport
        ninjaData.setThunderGodMark(null);
    }

    private BlockPos findLookMark(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 target = eye.add(player.getLookAngle().scale(MARK_RANGE));
        HitResult hit = player.level().clip(new ClipContext(
                eye,
                target,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player));

        if (hit instanceof BlockHitResult blockHit && hit.getType() != HitResult.Type.MISS) {
            return blockHit.getBlockPos().relative(blockHit.getDirection());
        }
        return player.blockPosition();
    }
}
