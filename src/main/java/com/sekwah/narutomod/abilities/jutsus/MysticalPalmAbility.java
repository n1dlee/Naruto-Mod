package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.MedicalPalmEvents;
import com.sekwah.narutomod.util.MedicalPalm;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class MysticalPalmAbility extends Ability implements Ability.Channeled {
    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public long defaultCombo() {
        return 131;
    }

    @Override
    public boolean canActivateBelowMinCharge() {
        return false;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int age) {
        if (!"haruno".equals(ninjaData.getClanId())) {
            if (age == 0) {
                player.displayClientMessage(Component.translatable("jutsu.fail.haruno",
                        Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            }
            return false;
        }
        return ninjaData.getChakra() >= MedicalPalm.COST && !player.isSpectator() && player.isAlive();
    }

    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int age) {
        MedicalPalmEvents.tickChannel(player, ninjaData, age);
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int age) {
        MedicalPalmEvents.end(player);
    }

    @Override
    public boolean channelCommittedAt(INinjaData data, int age) {
        return MedicalPalmEvents.takeHealCredit(data);
    }
}
