package com.sekwah.narutomod;

import com.sekwah.narutomod.util.LightningCoat;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightningCoatTest {
    @Test
    void auraBuildsFromLegsToTorsoAndCrown() {
        assertEquals(0, LightningCoat.layer(0, 0));
        assertTrue(LightningCoat.layer(3, 0) > LightningCoat.layer(3, 0.32f));
        assertEquals(0, LightningCoat.layer(3, 0.56f));
        assertEquals(1, LightningCoat.layer(10, 0.56f));
    }

    @Test
    void decorativeTrailNeedsMeasuredSprintAndRejectsTeleportsAndJumpsInPlace() {
        assertFalse(LightningCoat.leavesTrail(new Vec3(0.12, 0, 0), true));
        assertFalse(LightningCoat.leavesTrail(new Vec3(0, 0.7, 0), true));
        assertFalse(LightningCoat.leavesTrail(new Vec3(0.5, 0, 0), false));
        assertFalse(LightningCoat.leavesTrail(new Vec3(2.2, 0, 0), true));
        assertTrue(LightningCoat.leavesTrail(new Vec3(0.43, 0.02, 0), true));
    }

    @Test
    void costumeIsExclusiveWithTransformationsButNeitherBuffMakesPlayerImmune() {
        assertTrue(LightningCoat.compatible(0, 0, 0, false));
        assertFalse(LightningCoat.compatible(2, 0, 0, false));
        assertFalse(LightningCoat.compatible(0, 1, 0, false));
        assertFalse(LightningCoat.compatible(0, 0, 2, false));
        assertFalse(LightningCoat.compatible(0, 0, 0, true));
        assertEquals(8, LightningCoat.protectedDamage(10));
        assertEquals(0.68f, LightningCoat.cushioning(1), 1e-6);
    }
}
