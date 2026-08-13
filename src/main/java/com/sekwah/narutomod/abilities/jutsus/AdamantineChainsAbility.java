package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Uzumaki Clan — Adamantine Sealing Chains (combo 1311).
 * Fires 4 chakra chains in a cross pattern (forward, back-left, back-right, behind).
 * Each chain travels 10 blocks; first entity hit per chain is bound.
 * Bound players: Slowness V + Jump disabled for 5s (not killed).
 * Bound mobs: complete freeze (Slowness V) for 4s.
 */
public class AdamantineChainsAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 70f;
    private static final double CHAIN_RANGE = 10.0;
    private static final double CHAIN_WIDTH = 1.0;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** Four chains unfold; the arms stay out while they do. */
    @Override
    public int castPoseTicks() {
        return 16;
    }

    @Override
    public long defaultCombo() {
        return 1311;
    }

    @Override
    public int getCooldown() {
        return 25 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.CHAIN_PLACE;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!"uzumaki".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.uzumaki",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
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
        Vec3 look = player.getLookAngle();
        int playerBindTicks = Math.round(5 * 20 * ninjaData.getRankDamageMultiplier());
        int mobBindTicks = Math.round(4 * 20 * ninjaData.getRankDamageMultiplier());
        // Ignore vertical — chains go horizontal only
        Vec3 forward = new Vec3(look.x, 0, look.z).normalize();
        Vec3 right = new Vec3(-forward.z, 0, forward.x); // 90° right

        // 4 chain directions: forward, forward+right, right, forward-right (fan-like)
        Vec3[] chainDirs = {
                forward,
                forward.add(right).normalize(),
                right,
                forward.subtract(right).normalize()
        };

        Set<LivingEntity> alreadyHit = new HashSet<>();

        for (Vec3 dir : chainDirs) {
            Vec3 origin = player.position().add(0, player.getBbHeight() * 0.5, 0);
            Vec3 end = origin.add(dir.scale(CHAIN_RANGE));

            // Draw chain particles
            if (player.level() instanceof ServerLevel serverLevel) {
                int steps = (int)(CHAIN_RANGE * 4);
                for (int i = 0; i <= steps; i++) {
                    Vec3 pos = origin.add(dir.scale(i / 4.0));
                    serverLevel.sendParticles(NarutoParticles.CHAIN_GOLD, pos.x, pos.y, pos.z,
                            1, 0.05, 0.05, 0.05, 0.01);
                }
            }

            // Find first entity on this chain
            AABB searchBox = new AABB(
                    Math.min(origin.x, end.x) - CHAIN_WIDTH, origin.y - 1, Math.min(origin.z, end.z) - CHAIN_WIDTH,
                    Math.max(origin.x, end.x) + CHAIN_WIDTH, origin.y + 2, Math.max(origin.z, end.z) + CHAIN_WIDTH);

            List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                    e -> e != player && e.isAlive() && !alreadyHit.contains(e));

            candidates.stream()
                    .filter(e -> {
                        Vec3 toE = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(origin);
                        double proj = toE.dot(dir);
                        if (proj < 0 || proj > CHAIN_RANGE) return false;
                        Vec3 closest = origin.add(dir.scale(proj));
                        return e.position().add(0, e.getBbHeight() * 0.5, 0).distanceTo(closest) <= CHAIN_WIDTH;
                    })
                    .findFirst()
                    .ifPresent(target -> {
                        alreadyHit.add(target);
                        if (target instanceof Player targetPlayer) {
                            // Players: slowed heavily, not killed
                            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, playerBindTicks, 4, false, true));
                            target.addEffect(new MobEffectInstance(MobEffects.JUMP, playerBindTicks, -10, false, false));
                            // Canon: these are SEALING chains — they suppress the victim's
                            // chakra, not just their movement (strong enough to seal bijuu)
                            targetPlayer.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                                    .ifPresent(targetData -> targetData.useChakra(150f, playerBindTicks));
                        } else {
                            // Mobs: full freeze
                            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, mobBindTicks, 10, false, true));
                        }
                        // Chakra suppression weakens whatever the chains bind
                        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                                target instanceof Player ? playerBindTicks : mobBindTicks, 1, false, true));
                        // Particle burst on target
                        if (player.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(NarutoParticles.CHAIN_GOLD,
                                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                                    15, 0.3, 0.4, 0.3, 0.05);
                        }
                    });
        }
    }
}
