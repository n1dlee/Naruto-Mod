package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.ChakraDragonEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class WaterDragonAbility extends Ability implements Ability.Cooldown {
    private static final float CHAKRA_COST = 55f;
    private static final float MAIN_DAMAGE = 18f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int castPoseTicks() {
        return ChakraDragonEntity.WATER_WINDUP;
    }

    @Override
    public long defaultCombo() {
        return 1312;
    }

    @Override
    public int getCooldown() {
        return 12 * 20;
    }

    @Override
    public String element() {
        return "water";
    }

    @Override
    public int elementLevelRequired() {
        return 8;
    }

    @Override
    public float elementXpReward() {
        return 30f;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
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
        ChakraDragonEntity.launchWaterAlongLook(player, MAIN_DAMAGE * ninjaData.getRankDamageMultiplier());
    }
}
