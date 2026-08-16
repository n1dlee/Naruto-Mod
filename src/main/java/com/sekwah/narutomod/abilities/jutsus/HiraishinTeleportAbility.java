package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.projectile.HiraishinKunaiEntity;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.UUID;

/**
 * Flying Thunder God — the jump itself. Bound to its own key (H) rather than a hand-seal
 * combo, because in a fight you snap between seals faster than you could ever type one.
 *
 * Where you land is decided by what is actually useful, nearest-first:
 *   1. a thrown Hiraishin kunai you are looking at
 *   2. the closest Hiraishin kunai in range
 *   3. the creature you branded
 *   4. the ground seal you laid
 *
 * The cooldown is deliberately half a second — the technique's whole identity is that it
 * is effectively instantaneous and can be chained.
 */
public class HiraishinTeleportAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 20f;
    private static final double KUNAI_SEARCH_RADIUS = 128.0D;
    private static final double LOOK_TOLERANCE = 0.985D; // dot product — a fairly tight aim cone

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** Same as Flying Thunder God - gone before the stance registers. */
    @Override
    public int castPoseTicks() {
        return 3;
    }

    @Override
    public long defaultCombo() {
        return -1; // key-bound only
    }

    @Override
    public int getCooldown() {
        return 10; // 0.5s
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.ENDERMAN_TELEPORT;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (findDestination(player, ninjaData) == null) {
            player.displayClientMessage(
                    Component.translatable("hiraishin.fail.nomark").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 10);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        Vec3 destination = findDestination(player, ninjaData);
        if (destination == null) {
            return;
        }
        flash(player, player.position().add(0, 1.0, 0));

        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.level().playSound(null, destination.x, destination.y, destination.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.6f);
        flash(player, destination.add(0, 1.0, 0));
    }

    /** Resolves the best seal to jump to, or null when the player has none. */
    private static Vec3 findDestination(Player player, INinjaData ninjaData) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        HiraishinKunaiEntity best = null;
        HiraishinKunaiEntity aimed = null;
        double bestDistance = Double.MAX_VALUE;
        double aimedDistance = Double.MAX_VALUE;

        for (HiraishinKunaiEntity kunai : player.level().getEntitiesOfClass(HiraishinKunaiEntity.class,
                player.getBoundingBox().inflate(KUNAI_SEARCH_RADIUS))) {
            // Your own marks only.
            //
            // The search took every Hiraishin kunai within 128 blocks regardless of who threw
            // it, so two users of the technique shared one network: either could jump to the
            // other's marks, including one planted in the middle of the other's base. The
            // formula is the wielder's own, and stealing one is a different jutsu entirely.
            //
            // AbstractArrow already tracks and persists its shooter, so no new field is
            // needed - the ownership was there all along and simply was not consulted.
            if (kunai.getOwner() != player) {
                continue;
            }
            double distance = kunai.distanceToSqr(player);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = kunai;
            }
            // Looking straight at one always wins, so you can pick a specific kunai.
            Vec3 toKunai = kunai.position().subtract(eye).normalize();
            if (toKunai.dot(look) >= LOOK_TOLERANCE && distance < aimedDistance) {
                aimedDistance = distance;
                aimed = kunai;
            }
        }
        HiraishinKunaiEntity chosen = aimed != null ? aimed : best;
        if (chosen != null) {
            return new Vec3(chosen.getX(), chosen.getY(), chosen.getZ());
        }

        // A branded creature — jump to its side rather than inside it.
        String markedId = ninjaData.getHiraishinEntityMark();
        if (markedId != null && !markedId.isEmpty() && player.level() instanceof ServerLevel serverLevel) {
            try {
                Entity marked = serverLevel.getEntity(UUID.fromString(markedId));
                if (marked != null && marked.isAlive()) {
                    Vec3 behind = marked.position().subtract(marked.getLookAngle().scale(1.2));
                    return new Vec3(behind.x, marked.getY(), behind.z);
                }
            } catch (IllegalArgumentException ignored) {
                // stored id was malformed — fall through to the position mark
            }
        }

        BlockPos mark = ninjaData.getThunderGodMark();
        return mark == null ? null : new Vec3(mark.getX() + 0.5, mark.getY(), mark.getZ() + 0.5);
    }

    private static void flash(Player player, Vec3 at) {
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(NarutoParticles.TELEPORT_GOLD,
                    at.x, at.y, at.z, 25, 0.4, 0.7, 0.4, 0.08);
        }
    }
}
