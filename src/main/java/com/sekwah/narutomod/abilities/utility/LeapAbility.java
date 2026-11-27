package com.sekwah.narutomod.abilities.utility;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.LeapEvents;
import com.sekwah.narutomod.util.LeapMotion;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class LeapAbility extends Ability {
    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int castPoseTicks() {
        return 0;
    }

    @Override
    public boolean logInChat() {
        return false;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public SoundEvent castingFailSound() {
        return null;
    }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (!LeapEvents.canStart(player)) {
            player.displayClientMessage(Component.translatable("jutsu.leap.no_clearance"), true);
            return false;
        }
        if (data.getStamina() < LeapEvents.STAMINA) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughstamina",
                    Component.translatable("jutsu.leap").withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        LeapEvents.start(player, LeapMotion.impulse(data.getNinjaRank(), player.getLookAngle(), player.getYRot(),
                data.getMoveStrafe(), data.getMoveForward()));
    }
}
