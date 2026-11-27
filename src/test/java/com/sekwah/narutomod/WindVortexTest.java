package com.sekwah.narutomod;

import com.sekwah.narutomod.util.WindVortex;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WindVortexTest {
    @Test
    void expandsMonotonicallyWithSameRadiusForServerAndVisuals() {
        assertEquals(0, WindVortex.radius(0));
        for (int tick = 1; tick <= WindVortex.EXPANSION_TICKS; tick++) {
            assertTrue(WindVortex.radius(tick) > WindVortex.radius(tick - 1));
        }
        assertEquals(WindVortex.RADIUS, WindVortex.radius(WindVortex.EXPANSION_TICKS));
        assertEquals(WindVortex.RADIUS, WindVortex.radius(WindVortex.LIFETIME));
    }

    @Test
    void onlyFirstContactWithEachVictimSpendsDamageMarker() {
        WindVortex vortex = new WindVortex();
        UUID first = new UUID(0, 1);
        UUID second = new UUID(0, 2);
        assertTrue(vortex.mark(first));
        assertFalse(vortex.mark(first));
        assertTrue(vortex.mark(second));
        assertEquals(2, vortex.marks().size());
    }

    @Test
    void savedMarksStillRejectThatVictimAfterReload() {
        WindVortex before = new WindVortex();
        UUID victim = new UUID(0, 1);
        before.mark(victim);
        WindVortex restored = new WindVortex();
        before.marks().forEach(restored::mark);
        assertFalse(restored.mark(victim));
    }

    @Test
    void marksAreBoundedWithoutEvictingOldVictims() {
        WindVortex vortex = new WindVortex();
        for (int i = 0; i < 256; i++) {
            assertTrue(vortex.mark(new UUID(0, i)));
        }
        assertFalse(vortex.mark(new UUID(0, 256)));
        assertFalse(vortex.mark(new UUID(0, 0)));
    }

    @Test
    void speedRampsThenStaysBelowTwoBlocksPerTick() {
        Vec3 direction = new Vec3(1, 0, 0);
        assertEquals(0.9, WindVortex.velocity(direction, 0).x, 1.0e-6);
        assertEquals(1.65, WindVortex.velocity(direction, 4).x, 1.0e-6);
        assertEquals(1.65, WindVortex.velocity(direction, 30).x, 1.0e-6);
    }
}
