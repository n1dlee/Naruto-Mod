package com.sekwah.narutomod;

import com.sekwah.narutomod.util.GianLine;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GianLineTest {
    private static final Vec3 START = new Vec3(0, 1, 0);
    private static final Vec3 END = new Vec3(20, 1, 0);

    @Test
    void beamPiercesTwoOnAxisAndRejectsAThirdBeyondAThinWall() {
        var wall = GianLine.contact(START, END, new AABB(9, 0, -0.6, 9.08, 2, 0.6));
        var first = GianLine.contact(START, END, new AABB(4, 0, -0.25, 4.6, 2, 0.25));
        var second = GianLine.contact(START, END, new AABB(7, 0, -0.25, 7.6, 2, 0.25));
        var hidden = GianLine.contact(START, END, new AABB(10, 0, -0.25, 10.6, 2, 0.25));
        assertNotNull(wall);
        assertNotNull(first);
        assertNotNull(second);
        assertNotNull(hidden);
        assertTrue(first.fraction() < second.fraction());
        assertTrue(GianLine.precedesWall(first, wall));
        assertTrue(GianLine.precedesWall(second, wall));
        assertFalse(GianLine.precedesWall(hidden, wall));
    }

    @Test
    void decorationDoesNotExtendDamageReachOffAxis() {
        var side = new AABB(6, 0, 0.24, 6.6, 2, 0.62);
        assertNull(GianLine.contact(START, END, side));
    }

    @Test
    void simultaneousWallWinsAndCancelsTheFirstContact() {
        var same = GianLine.contact(START, END, new AABB(6, 0, -0.25, 6.6, 2, 0.25));
        assertNotNull(same);
        assertFalse(GianLine.precedesWall(same, same));
    }

    @Test
    void cancelledDamageDoesNotConsumePiercingPower() {
        assertEquals(15, GianLine.nextDamage(15, false));
        assertEquals(9.75f, GianLine.nextDamage(15, true), 1e-5);
    }
}
