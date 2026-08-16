package com.sekwah.narutomod.abilities.utility;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.world.entity.player.Player;

/**
 * More of a slight speed boost than an actual dash
 */
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
        // This jutsu can always be cast
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Add particle effects n stuff
    }


    /**
     * The rate is no longer a constant. It used to be a flat 4 per tick at every rank, which
     * filled an Academy pool in a second and a quarter and a Kage pool in three minutes of
     * standing perfectly still - the same technique being pointless at one end of the game
     * and unusable at the other.
     *
     * INinjaData derives it from the pool and shortens the fill time as rank climbs, so a
     * full charge is a comparable commitment at every rank and a Kage genuinely channels
     * faster rather than merely channelling more.
     */
    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        boolean moving = player.isSprinting() || !player.onGround();
        ninjaData.addChakra(ninjaData.getChakraChargePerTick(moving));
    }

    @Override
    public boolean canActivateBelowMinCharge() {
        return false;
    }
}
