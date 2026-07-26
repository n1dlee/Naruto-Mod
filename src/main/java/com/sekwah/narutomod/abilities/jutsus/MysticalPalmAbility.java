package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

/**
 * Medical Ninjutsu — Mystical Palm Technique (combo 131, CHANNELED, Haruno).
 * The iconic green-glow healing hands: channel to knit wounds back together.
 * Healing an ALLY takes priority — if a damaged living target is right in front of the
 * caster (within 4 blocks of their look direction), the palm heals them; otherwise the
 * chakra flows into the caster's own body. Canon requires precise chakra control, which
 * is the Haruno clan's whole identity — clan-gated accordingly.
 */
public class MysticalPalmAbility extends Ability implements Ability.Channeled {

    private static final float CHAKRA_PER_PULSE = 6f;
    private static final int TICKS_PER_PULSE = 10;
    private static final float SELF_HEAL = 1.0f;
    private static final float ALLY_HEAL = 1.5f;
    private static final double ALLY_RANGE = 4.0;

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public long defaultCombo() {
        return 131;
    }

    @Override
    public boolean canActivateBelowMinCharge() {
        return false;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!"haruno".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.haruno",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_PER_PULSE / TICKS_PER_PULSE) {
            return false;
        }
        return true;
    }

    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        if (ticksChanneled % TICKS_PER_PULSE != 0) {
            return;
        }
        if (ninjaData.getChakra() < CHAKRA_PER_PULSE) {
            return;
        }

        LivingEntity ally = findAllyInFront(player);
        LivingEntity healTarget = ally != null ? ally : player;
        if (healTarget.getHealth() >= healTarget.getMaxHealth()) {
            return; // Nothing to knit — don't burn chakra pointlessly
        }

        ninjaData.useChakra(CHAKRA_PER_PULSE, 5);
        healTarget.heal(healTarget == player ? SELF_HEAL : ALLY_HEAL);

        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 pos = healTarget.position().add(0, healTarget.getBbHeight() * 0.6, 0);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 4, 0.3, 0.4, 0.3, 0.0);
            serverLevel.sendParticles(ParticleTypes.COMPOSTER, pos.x, pos.y, pos.z, 6, 0.35, 0.45, 0.35, 0.01);
        }
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // All the work happens per-pulse in handleChannelling
    }

    /** A hurt living target the caster is looking at within palm reach — allies first. */
    private LivingEntity findAllyInFront(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        return player.level().getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(ALLY_RANGE),
                        e -> e != player && e.isAlive() && e.getHealth() < e.getMaxHealth()).stream()
                .filter(e -> {
                    Vec3 toE = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
                    return toE.length() <= ALLY_RANGE && toE.normalize().dot(look) >= 0.7;
                })
                .min(Comparator.comparingDouble(e -> e.position().distanceTo(eye)))
                .orElse(null);
    }
}
