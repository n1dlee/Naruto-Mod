package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class SharinganAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck {

    private static final float CHAKRA_COST = 1.0F;
    private static final int CHAKRA_COOLDOWN = 15;

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return -1;
    }

    @Override
    public boolean canStartToggle(Player player, INinjaData ninjaData) {
        return validateSharinganAccess(player, ninjaData) && validateChakra(player, ninjaData);
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!validateSharinganAccess(player, ninjaData) || !validateChakra(player, ninjaData)) {
            return false;
        }
        ninjaData.useChakra(toggleCost(ninjaData), CHAKRA_COOLDOWN);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        int level = ninjaData.getSharinganLevel();
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false));
        if (level >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
        }
        if (level >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false));
        }

        // Canon: the Sharingan also perceives chakra — chakra vision at 2+ tomoe, same
        // glowing-outline translation as the Byakugan but with a much smaller radius
        // (the Byakugan is THE long-range sensory dojutsu; the Sharingan reads what's near).
        if (level >= 2 && player.tickCount % 20 == 0) {
            double visionRadius = 8 + level * 4; // 16 -> 24 blocks
            for (LivingEntity seen : player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(visionRadius), e -> e != player && e.isAlive())) {
                seen.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
            }
        }
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        if (player.tickCount % 8 == 0) {
            player.level().addParticle(NarutoParticles.SHARINGAN_RED,
                    player.getX(), player.getEyeY() - 0.15D, player.getZ(),
                    0.0D, 0.0D, 0.0D);
        }
        // Slow expanding "eye glint" ring, pulses roughly once a second
        if (player.tickCount % 20 == 0) {
            double eyeY = player.getEyeY() - 0.1D;
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                double radius = 0.15D;
                player.level().addParticle(NarutoParticles.SHARINGAN_RED,
                        player.getX() + Math.cos(angle) * radius, eyeY, player.getZ() + Math.sin(angle) * radius,
                        0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    public net.minecraft.sounds.SoundEvent castingSound() {
        return NarutoSounds.SHARINGAN_ACTIVATE.get();
    }

    /**
     * Gated on owning the eye rather than on the clan, so a transplanted Sharingan works
     * for a non-Uchiha (Kakashi's case) without a second code path.
     */
    private boolean validateSharinganAccess(Player player, INinjaData ninjaData) {
        if (!ninjaData.hasSharinganEye()) {
            player.displayClientMessage(Component.translatable("jutsu.fail.uchiha",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getSharinganLevel() < 1) {
            player.displayClientMessage(Component.translatable("jutsu.fail.rank.genin",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }

    /**
     * A foreign eye is far more expensive to actively drive — the host has no Uchiha body
     * to run it efficiently. This is on TOP of the passive per-second drain the transplant
     * already charges in NinjaData.
     */
    private static float toggleCost(INinjaData ninjaData) {
        return ninjaData.isTransplantedSharingan() ? CHAKRA_COST * 3f : CHAKRA_COST;
    }

    private boolean validateChakra(Player player, INinjaData ninjaData) {
        if (ninjaData.getChakra() < toggleCost(ninjaData)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }
}
