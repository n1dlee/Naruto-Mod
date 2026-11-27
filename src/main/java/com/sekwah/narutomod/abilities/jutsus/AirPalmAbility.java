package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.GustFrontEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
public class AirPalmAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 25f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int castPoseTicks() {
        return 10;
    }

    @Override
    public long defaultCombo() {
        return 111;
    }

    @Override
    public int getCooldown() {
        return 5 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!"hyuga".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.hyuga",
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
        GustFrontEntity.palm(player, player.getLookAngle(), ninjaData.getRankDamageMultiplier(), ninjaData.isByakuganActive());
    }
}
