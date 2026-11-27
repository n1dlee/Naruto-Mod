package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.util.ShadowCloneFormation.Profile;

public class MultipleShadowCloneJutsuAbility extends ShadowCloneJutsuAbility {
    @Override
    public int castPoseTicks() {
        return 12;
    }

    @Override
    public long defaultCombo() {
        return 1222;
    }

    @Override
    public int getCooldown() {
        return 600;
    }

    @Override
    protected Profile profile() {
        return Profile.MASS;
    }
}
