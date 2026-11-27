package com.sekwah.narutomod;

import com.sekwah.narutomod.util.MirrorRing;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MirrorRingTest {
    @Test
    void panesFinishRisingOverThirteenTicksInOrder() {
        assertEquals(0, MirrorRing.delay(0));
        assertEquals(13, MirrorRing.delay(MirrorRing.SECTIONS - 1) + MirrorRing.RISE_TICKS);
        assertEquals(0, MirrorRing.rise(-1));
        assertEquals(0.5, MirrorRing.rise(4));
        assertEquals(1, MirrorRing.rise(8));
    }

    @Test
    void frontCrossingClipsAtThePaneNotAtCenter() {
        Vec3 pane = new Vec3(4, 0, 0);
        var clip = MirrorRing.clipOutward(new Vec3(3, 0, 0.5), new Vec3(4.5, 0, 0.5),
                pane, MirrorRing.outward(0), 0.25, 2.5);
        assertNotNull(clip);
        assertEquals(3.725, clip.x, 1.0e-6);
        assertEquals(0.5, clip.z, 1.0e-6);
    }

    @Test
    void doesNotPushOutsidersBackOrBlockEntryIntoRing() {
        Vec3 pane = new Vec3(4, 0, 0);
        Vec3 east = MirrorRing.outward(0);
        assertNull(MirrorRing.clipOutward(new Vec3(4.5, 0, 0), new Vec3(5, 0, 0), pane, east, 0.24, 2.5));
        assertNull(MirrorRing.clipOutward(new Vec3(4.5, 0, 0), new Vec3(3.6, 0, 0), pane, east, 0.24, 2.5));
    }

    @Test
    void noInvisibleCollisionAboveOrBesideTheVisibleSection() {
        Vec3 pane = new Vec3(4, 0, 0);
        assertNull(MirrorRing.clipOutward(new Vec3(3.6, 2.61, 0), new Vec3(4.6, 2.61, 0),
                pane, MirrorRing.outward(0), 0.2, 2.5));
        assertNull(MirrorRing.clipOutward(new Vec3(3.6, 0, 2.8), new Vec3(4.6, 0, 2.8),
                pane, MirrorRing.outward(0), 0.2, 2.5));
    }

    @Test
    void oppositePanesFaceDifferentDirectionsAndTouchAdjacentEdges() {
        Vec3 east = MirrorRing.outward(0);
        Vec3 west = MirrorRing.outward(4);
        assertEquals(-1.0, east.dot(west), 1.0e-8);
        assertTrue(MirrorRing.WIDTH >= 2 * MirrorRing.RADIUS * Math.tan(Math.PI / MirrorRing.SECTIONS));
    }
}
