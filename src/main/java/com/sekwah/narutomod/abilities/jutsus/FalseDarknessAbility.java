package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.GianBeamEntity;
import com.sekwah.narutomod.util.GianLine;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class FalseDarknessAbility extends Ability implements Ability.Cooldown {
    private static final float CHAKRA_COST = 45f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int castPoseTicks() {
        return GianLine.PREPARE + GianLine.SHOCK + 2;
    }

    @Override
    public long defaultCombo() {
        return 232;
    }

    @Override
    public int getCooldown() {
        return 12 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public String element() {
        return "lightning";
    }

    @Override
    public int elementLevelRequired() {
        return 3;
    }

    @Override
    public float elementXpReward() {
        return 20f;
    }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (data.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        data.useChakra(CHAKRA_COST, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        var aim = player.getEyePosition().add(player.getLookAngle().scale(GianLine.RANGE));
        GianBeamEntity.launch(player, aim, 15 * data.getRankDamageMultiplier() * data.getClanLightningDamageMultiplier(), false);
    }
}
