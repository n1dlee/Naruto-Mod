package com.sekwah.narutomod.abilities.utility;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.gameevents.NarutoGameEvents;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.sekclib.util.PlayerUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class LeapAbility extends Ability {
    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
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
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if(!player.onGround()) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notonground", Component.translatable("jutsu.leap").withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if(ninjaData.getStamina() < 10) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughstamina", Component.translatable("jutsu.leap").withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useStamina(10, 40);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        Vec3 lookVector = player.getLookAngle();
        float[] leapScales = {2.0f, 2.5f, 3.2f, 4.0f, 5.5f};
        int rank = Math.min(Math.max(ninjaData.getNinjaRank(), 0), 4);
        float horScale = leapScales[rank];
        float vertBoost = 0.8f + rank * 0.1f;
        PlayerUtil.setVelocity(player, lookVector.x * horScale, (lookVector.y * 0.6 + vertBoost)
                , lookVector.z * horScale, true);
        player.level().playSound(null,
                player, NarutoSounds.LEAP.get(), SoundSource.PLAYERS, 0.5f, 1.0f);
        player.level().gameEvent(player, NarutoGameEvents.LEAP.get(), player.position());
    }
}
