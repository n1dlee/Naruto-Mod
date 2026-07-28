package com.sekwah.narutomod.abilities.utility;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.world.entity.player.Player;

/**
 * More of a slight speed boost than an actual dash
 */
public class ChakraChargeAbility extends Ability implements Ability.Channeled {

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
     * Rates are x4 the original. At Kage rank the pool is ~15000, and the old 1/tick meant
     * standing still for over ten minutes to refill from empty - long enough that charging
     * stopped being a decision and became a chore.
     */
    private static final float CHARGE_STILL = 4f;
    private static final float CHARGE_MOVING = 0.8f;

    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
       if(player.isSprinting() || !player.onGround()) {
           ninjaData.addChakra(CHARGE_MOVING);
       } else {
           ninjaData.addChakra(CHARGE_STILL);
       }
    }

    @Override
    public boolean canActivateBelowMinCharge() {
        return false;
    }
}
