package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Nara Clan — Shadow Strangle Technique / Kubishibari (combo 333).
 * A combat extension of Shadow Imitation: the shadow already holding the victim crawls up
 * their body and takes physical form as a hand around their throat, inflicting direct
 * damage — canonically it can only be applied to a target the shadow has already caught.
 *
 * Requires an active Shadow Possession target (combo 331 first). Deals a choking damage-
 * over-time to that target: 4 waves over ~4 seconds + Weakness while being strangled.
 */
public class ShadowStrangleAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 30f;
    private static final int CHOKE_WAVES = 4;
    private static final int TICKS_BETWEEN_WAVES = 20;
    private static final float DAMAGE_PER_WAVE = 3.0f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** The shadow has to reach before it can close. */
    @Override
    public int castPoseTicks() {
        return 14;
    }

    @Override
    public long defaultCombo() {
        return 333;
    }

    @Override
    public int getCooldown() {
        return 10 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.WARDEN_ATTACK_IMPACT;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!"nara".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.nara",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        // Canon: the strangle is formed FROM the shadow already binding the victim
        if (!ninjaData.hasShadowTarget()) {
            player.displayClientMessage(Component.literal("No target caught by your shadow - use Shadow Possession first!")
                    .withStyle(ChatFormatting.RED), true);
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
        UUID targetId = ninjaData.getShadowPossessedTargetUUID();
        if (targetId == null) {
            return;
        }
        float damageMultiplier = ninjaData.getRankDamageMultiplier();

        chokeWave(player, targetId, damageMultiplier);
        for (int wave = 1; wave < CHOKE_WAVES; wave++) {
            ninjaData.scheduleDelayedTickEvent(
                    p -> chokeWave(p, targetId, damageMultiplier),
                    wave * TICKS_BETWEEN_WAVES);
        }
    }

    private void chokeWave(Player player, UUID targetId, float damageMultiplier) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity entity = serverLevel.getEntity(targetId);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
            return;
        }
        target.hurt(player.damageSources().magic(), DAMAGE_PER_WAVE * damageMultiplier);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, TICKS_BETWEEN_WAVES + 10, 1, false, true));

        // Shadow hand closing around the throat
        NarutoParticles.spawnRing(serverLevel,
                target.position().add(0, target.getBbHeight() * 0.85, 0), 0.4, 10, NarutoParticles.SHADOW_PURPLE);
    }
}
