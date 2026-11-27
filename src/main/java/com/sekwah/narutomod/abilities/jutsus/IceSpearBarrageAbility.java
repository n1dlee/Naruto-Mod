package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.IceSpearEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class IceSpearBarrageAbility extends Ability implements Ability.Cooldown {
    private static final float CHAKRA_COST = 50f;

    @Override
    public ActivationType activationType() { return ActivationType.INSTANT; }

    @Override
    public int castPoseTicks() { return 14; }

    @Override
    public long defaultCombo() { return 2312; }

    @Override
    public String element() { return "water"; }

    @Override
    public int elementLevelRequired() { return 8; }

    @Override
    public String secondaryElement() { return "wind"; }

    @Override
    public int secondaryElementLevelRequired() { return 8; }

    @Override
    public int getCooldown() { return 8 * 20; }

    @Override
    public SoundEvent castingSound() { return null; }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (data.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (!IceSpearEntity.launch(player, data.getRankDamageMultiplier())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.ice_spears.space"), true);
            return false;
        }
        data.useChakra(CHAKRA_COST, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
    }
}
