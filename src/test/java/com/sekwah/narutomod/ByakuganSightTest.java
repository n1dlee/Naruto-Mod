package com.sekwah.narutomod;

import com.sekwah.narutomod.util.ByakuganSight;
import com.sekwah.narutomod.util.DojutsuVisual;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ByakuganSightTest {
    @Test
    void nativeFocusKeepsRankRangeAndTransplantHasASeparateReducedRange() {
        assertEquals(0, ByakuganSight.range(3, true, false));
        assertEquals(600, ByakuganSight.range(999, true, true));
        assertEquals(0, ByakuganSight.range(0, false, true));
        assertEquals(20, ByakuganSight.range(1, false, false));
        assertEquals(50, ByakuganSight.range(1, false, true));
    }

    @Test
    void onlyNarrowSeamDirectlyBehindTheBodyIsBlind() {
        Vec3 body = new Vec3(0, 0, 1);
        assertFalse(ByakuganSight.outsideBlindSpur(body, new Vec3(0.1, 2, -10)));
        assertTrue(ByakuganSight.outsideBlindSpur(body, new Vec3(2, 2, -10)));
        assertTrue(ByakuganSight.outsideBlindSpur(body, new Vec3(0, 5, 0)));
        assertTrue(ByakuganSight.outsideBlindSpur(body, new Vec3(0, -5, 2)));
    }

    @Test
    void passiveEyeHasNoVeinsUntilThatEyeIsFocused() {
        var passive = new DojutsuVisual.State(false, false, 0, false, false, false, false, true);
        var combat = new DojutsuVisual.State(false, false, 0, false, false, true, false, true);
        assertEquals(DojutsuVisual.Eye.BYAKUGAN, DojutsuVisual.eye(DojutsuVisual.resolve(passive), false));
        assertEquals(DojutsuVisual.Eye.BYAKUGAN_FOCUS, DojutsuVisual.eye(DojutsuVisual.resolve(combat), false));
        assertEquals(DojutsuVisual.Eye.NONE, DojutsuVisual.eye(DojutsuVisual.resolve(combat), true));
    }

    @Test
    void protectionAndPerceptionBudgetsNeverMakeTheUserImmuneOrFloodTheHud() {
        assertEquals(1f, ByakuganSight.shielding(1));
        assertEquals(0.8f, ByakuganSight.shielding(2));
        assertTrue(ByakuganSight.shielding(4) > 0);
        assertEquals(0, ByakuganSight.reveal(0));
        assertEquals(1, ByakuganSight.reveal(5));
        assertTrue(ByakuganSight.LOW_LIMIT < ByakuganSight.VISIBLE_LIMIT);
    }
}
