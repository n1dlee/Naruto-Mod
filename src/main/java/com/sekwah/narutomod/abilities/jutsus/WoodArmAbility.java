package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.EyeTargeting;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Wood Release: Hand of Hashirama - an arm of wood shoots out, strikes, and hauls the
 * target back to the caster's feet.
 *
 * The damage is modest on purpose. What this technique is actually for is repositioning:
 * dragging an archer out of cover, or a runner back into your reach.
 */
public class WoodArmAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 45f;
    private static final double RANGE = 26.0;
    private static final float DAMAGE = 6f;
    private static final double PULL_STRENGTH = 1.5;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** The arm extends across the gap rather than arriving. */
    @Override
    public int castPoseTicks() {
        return 14;
    }

    @Override
    public long defaultCombo() {
        return 3213;
    }

    @Override
    public String requiredClan() {
        return "senju";
    }

    @Override
    public String element() {
        return "earth";
    }

    @Override
    public int elementLevelRequired() {
        return 4;
    }

    @Override
    public String secondaryElement() {
        return "water";
    }

    @Override
    public int secondaryElementLevelRequired() {
        return 4;
    }

    @Override
    public int getCooldown() {
        return 8 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.BAMBOO_PLACE;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
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
        LivingEntity target = EyeTargeting.raycastLiving(player, RANGE);
        if (target == null) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notarget",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        target.hurt(player.damageSources().playerAttack(player), DAMAGE * ninjaData.getRankDamageMultiplier());

        Vec3 pull = player.position().subtract(target.position()).normalize().scale(PULL_STRENGTH);
        target.setDeltaMovement(pull.x, pull.y + 0.3, pull.z);
        target.hurtMarked = true;

        if (player.level() instanceof ServerLevel serverLevel) {
            // The limb itself, drawn as a thick line of bark from hand to victim.
            Vec3 from = player.getEyePosition().subtract(0, 0.3, 0);
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
            int steps = (int) Math.max(8, from.distanceTo(to) * 3);
            for (int i = 0; i <= steps; i++) {
                Vec3 point = from.lerp(to, i / (double) steps);
                serverLevel.sendParticles(NarutoParticles.LOG_BROWN,
                        point.x, point.y, point.z, 2, 0.12, 0.12, 0.12, 0.0);
            }
            NarutoParticles.spawnBurst(serverLevel, to, 15, 0.4, NarutoParticles.LOG_BROWN);
        }
    }
}
