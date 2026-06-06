package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

public class SharinganAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck {

    private static final float CHAKRA_COST = 1.0F;
    private static final int CHAKRA_COOLDOWN = 15;
    private static final DustParticleOptions SHARINGAN_PARTICLE = new DustParticleOptions(new Vector3f(0.9F, 0.0F, 0.0F), 0.7F);

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
        ninjaData.useChakra(CHAKRA_COST, CHAKRA_COOLDOWN);
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
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        if (player.tickCount % 8 == 0) {
            player.level().addParticle(SHARINGAN_PARTICLE,
                    player.getX(), player.getEyeY() - 0.15D, player.getZ(),
                    0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public net.minecraft.sounds.SoundEvent castingSound() {
        return NarutoSounds.SHARINGAN_ACTIVATE.get();
    }

    private boolean validateSharinganAccess(Player player, INinjaData ninjaData) {
        if (!"uchiha".equals(ninjaData.getClanId())) {
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

    private boolean validateChakra(Player player, INinjaData ninjaData) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }
}
