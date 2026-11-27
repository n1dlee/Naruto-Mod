package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.ShockArcEntity;
import com.sekwah.narutomod.util.ShockArc;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class LightningShockAbility extends Ability implements Ability.Cooldown {
    private static final float CHAKRA_COST = 12f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int castPoseTicks() {
        return ShockArc.PREPARE + ShockArc.FADE + 1;
    }

    @Override
    public long defaultCombo() {
        return 222;
    }

    @Override
    public String element() {
        return "lightning";
    }

    @Override
    public int elementLevelRequired() {
        return 1;
    }

    @Override
    public float elementXpReward() {
        return 12f;
    }

    @Override
    public int getCooldown() {
        return 3 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (data.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        data.useChakra(CHAKRA_COST, 15);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        var aim = ShockArc.endpoint(player.getEyePosition(), player.getLookAngle());
        ShockArcEntity.cast(player, aim, 4 * data.getRankDamageMultiplier());
    }
}
