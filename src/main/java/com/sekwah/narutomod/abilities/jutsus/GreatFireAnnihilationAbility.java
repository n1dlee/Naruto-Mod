package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.FireWallEntity;
import com.sekwah.narutomod.util.FireFront;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class GreatFireAnnihilationAbility extends Ability implements Ability.Cooldown {
    private static final float CHAKRA_COST = 85f;
    private static final float DAMAGE = 5f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int castPoseTicks() {
        return FireFront.WINDUP + 18;
    }

    @Override
    public long defaultCombo() {
        return 3113;
    }

    @Override
    public int getCooldown() {
        return 35 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public String element() {
        return "fire";
    }

    @Override
    public int elementLevelRequired() {
        return 14;
    }

    @Override
    public float elementXpReward() {
        return 45f;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 60);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        float clanScale = "uchiha".equals(ninjaData.getClanId()) ? 1.25f : 1.0f;
        FireWallEntity.start(player, player.getLookAngle(), DAMAGE * clanScale * ninjaData.getRankDamageMultiplier(), 3);
    }
}
