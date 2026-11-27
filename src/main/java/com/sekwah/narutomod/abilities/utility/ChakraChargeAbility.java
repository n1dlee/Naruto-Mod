package com.sekwah.narutomod.abilities.utility;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChannelCompletion;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class ChakraChargeAbility extends Ability implements Ability.Channeled {

    /** Exempt from the free-hands gate: this is gathering chakra, not shaping a technique. */
    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public long defaultCombo() {
        return 1;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
    }

    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        boolean moving = player.isSprinting() || !player.onGround();
        float before = ninjaData.getChakra();
        ninjaData.addChakra(ninjaData.getChakraChargePerTick(moving));
        var pulse = ninjaData.chakraChargeFeedback().accept(before, ninjaData.getChakra(),
                ninjaData.getMaxChakra(), ninjaData.getChakraChargePerTick(false));
        ninjaData.setChargeIntensity(pulse.strength());
        if (pulse.completed()) {
            player.level().playSound(null, player, NarutoSounds.CHAKRA_CHARGE_FULL.get(), SoundSource.PLAYERS, 0.64f, 1.0f);
        }
        if (ninjaData.getChakra() >= ninjaData.getMaxChakra()) {
            ChannelCompletion.finish(player, ninjaData, this, ticksChanneled + 1, ChannelCompletion.Reason.FULL);
        }
    }

    @Override
    public boolean channelCommittedAt(INinjaData data, int ticksChanneled) {
        return data.chakraChargeFeedback().earned(data.getMaxChakra());
    }

    @Override
    public boolean canActivateBelowMinCharge() {
        return false;
    }
}
