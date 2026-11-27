package com.sekwah.narutomod;

import com.sekwah.narutomod.util.ScalpelEdge;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScalpelEdgeTest {
    @Test
    void shortCastMustActuallyIntersectTheHitbox() {
        Vec3 hand = new Vec3(0.35, 1.28, 0);
        Vec3 direction = new Vec3(0, 0, 1);
        AABB close = new AABB(0.17, 0, 1.85, 0.55, 1.8, 2.30);
        Vec3 hit = ScalpelEdge.contact(hand, direction, close);
        assertNotNull(hit);
        assertEquals(1.85, hit.z, 1.0e-8);
        assertNull(ScalpelEdge.contact(hand, direction, close.move(0, 0, 1.15)));
        assertNull(ScalpelEdge.contact(hand, direction, close.move(-0.9, 0, 0)));
    }

    @Test
    void formationAndRecoveryAreBothNonDamaging() {
        assertFalse(ScalpelEdge.ready(3, 20, 0));
        assertTrue(ScalpelEdge.ready(4, 20, 0));
        assertFalse(ScalpelEdge.ready(40, 52, 53));
        assertTrue(ScalpelEdge.ready(41, 53, 53));
    }

    @Test
    void onlyTheMatchingTargetOfThisTickCanUseTheArmedCut() {
        assertTrue(ScalpelEdge.sameStrike(112, 112, true));
        assertFalse(ScalpelEdge.sameStrike(112, 113, true));
        assertFalse(ScalpelEdge.sameStrike(112, 112, false));
    }

    @Test
    void rankCannotExceedThePvpDamageBudget() {
        assertEquals(3, ScalpelEdge.bonus(18, true));
        assertEquals(4.5f, ScalpelEdge.bonus(18, false));
    }
}
