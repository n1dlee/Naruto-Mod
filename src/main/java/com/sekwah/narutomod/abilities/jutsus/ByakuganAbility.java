package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

public class ByakuganAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck {

    private static final float CHAKRA_COST = 0.5F;
    private static final int CHAKRA_COOLDOWN = 15;
    private static final int[] BYAKUGAN_RANGE = new int[] {20, 50, 150, 400, 1000};
    private static final DustParticleOptions BYAKUGAN_PARTICLE = new DustParticleOptions(new Vector3f(0.85F, 0.95F, 1.0F), 0.65F);

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
        return validateByakuganAccess(player, ninjaData) && validateChakra(player, ninjaData);
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!validateByakuganAccess(player, ninjaData) || !validateChakra(player, ninjaData)) {
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, CHAKRA_COOLDOWN);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        int rank = Math.min(Math.max(ninjaData.getNinjaRank(), 0), BYAKUGAN_RANGE.length - 1);
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false));
        if (rank >= 1) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, rank >= 3 ? 1 : 0, false, false));
        }
        if (rank >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, rank >= 4 ? 1 : 0, false, false));
        }
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        if (player.tickCount % 8 == 0) {
            player.level().addParticle(BYAKUGAN_PARTICLE,
                    player.getX(), player.getEyeY() - 0.15D, player.getZ(),
                    0.0D, 0.0D, 0.0D);
        }
    }

    private boolean validateByakuganAccess(Player player, INinjaData ninjaData) {
        if (!"hyuga".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.hyuga",
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
