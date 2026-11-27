package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.GustFrontEntity;
import com.sekwah.narutomod.util.GustFront;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class GreatBreakthroughAbility extends Ability implements Ability.Cooldown {
    private static final float CHAKRA_COST = 35f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int castPoseTicks() {
        return GustFront.PREPARE + 5;
    }

    @Override
    public long defaultCombo() {
        return 231;
    }

    @Override
    public int getCooldown() {
        return 2 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public String element() {
        return "wind";
    }

    @Override
    public int elementLevelRequired() {
        return 1;
    }

    @Override
    public float elementXpReward() {
        return 15f;
    }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (data.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        data.useChakra(CHAKRA_COST, 20);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        GustFrontEntity.launch(player, player.getLookAngle(), data.getRankDamageMultiplier());
    }
}
