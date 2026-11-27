package com.sekwah.narutomod;

import com.sekwah.narutomod.util.FireballShape;
import com.sekwah.narutomod.util.WaterDragonFlight;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FireballShapeTest {
    @Test
    void tapAndPaidMaximumAreBoundedRatherThanInheritingPacketCounters() {
        assertEquals(0, FireballShape.charge(-1));
        assertEquals(60, FireballShape.charge(6000));
        assertEquals(60, FireballShape.charge(60));
        assertEquals(3, FireballShape.EXHALE_TICKS);
    }

    @Test
    void visualAndSweptRadiusGrowTogetherAndLevelOff() {
        assertTrue(FireballShape.radius(0, 0) < FireballShape.radius(0, 5));
        assertTrue(FireballShape.radius(0, 5) < FireballShape.radius(60, 5));
        assertEquals(FireballShape.radius(60, 5), FireballShape.radius(60, 40));
    }

    @Test
    void blastDamageDecreasesAndDoesNotExtendBeyondItsRadius() {
        assertEquals(12f, FireballShape.damage(0, 0));
        assertEquals(36f, FireballShape.damage(60, 0));
        assertTrue(FireballShape.damage(60, 0.5) < FireballShape.damage(60, 0));
        assertEquals(0f, FireballShape.damage(60, 1.0));
        assertEquals(0f, FireballShape.damage(0, 2.0));
    }

    @Test
    void chargedEdgeHitsPartialBlockBeforeItsCenterTouches() {
        var slab = new AABB(2.75, 0, -0.5, 3.65, 0.5, 0.5);
        Vec3 from = new Vec3(0, 1.4, 0);
        Vec3 to = new Vec3(2, 1.4, 0);
        assertNull(WaterDragonFlight.sweep(from, to, slab, FireballShape.radius(0, 5)));
        assertNotNull(WaterDragonFlight.sweep(from, to, slab, FireballShape.radius(60, 5)));
    }
}
