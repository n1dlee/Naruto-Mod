package com.sekwah.narutomod;

import com.sekwah.narutomod.util.KaitenDome;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KaitenDomeTest {
    @Test
    void fastProjectileFindsTheFirstBoundaryBeforeItCrossesTheWholeDome() {
        Vec3 root = new Vec3(0, 60, 0);
        Vec3 enter = KaitenDome.entry(root, new Vec3(-10, 61, 0), new Vec3(10, 61, 0));
        assertNotNull(enter);
        assertTrue(enter.x < -2.0);
        assertEquals(1, KaitenDome.scaled(enter.subtract(root)).lengthSqr(), 1.0e-8);
    }

    @Test
    void noReflectionBelowTheDomeOrFromAnAlreadyInteriorProjectile() {
        assertNull(KaitenDome.entry(Vec3.ZERO, new Vec3(-5, -0.2, 0), new Vec3(5, -0.2, 0)));
        assertNull(KaitenDome.entry(Vec3.ZERO, new Vec3(0, 1, 0), new Vec3(5, 1, 0)));
        assertNull(KaitenDome.entry(Vec3.ZERO, new Vec3(3, 1, 0), new Vec3(5, 1, 0)));
    }

    @Test
    void reflectionUsesTheSameInclinedSurfaceRenderedAtTheContact() {
        Vec3 surface = KaitenDome.surface(0.57, 0.62);
        Vec3 normal = KaitenDome.normal(surface);
        Vec3 from = surface.add(normal.scale(3));
        Vec3 to = surface.subtract(normal.scale(2));
        Vec3 entry = KaitenDome.entry(Vec3.ZERO, from, to);
        assertNotNull(entry);
        assertEquals(0, entry.distanceTo(surface), 1.0e-8);
        Vec3 rebound = KaitenDome.reflect(to.subtract(from), normal);
        assertTrue(rebound.dot(normal) > 0);
        assertEquals(to.distanceTo(from) * 0.87, rebound.length(), 1.0e-8);
    }

    @Test
    void protectionOnlyExistsBetweenWindupAndCollapse() {
        assertFalse(KaitenDome.active(KaitenDome.WINDUP - 1));
        assertTrue(KaitenDome.active(KaitenDome.WINDUP));
        assertTrue(KaitenDome.active(KaitenDome.WINDUP + KaitenDome.MAX_ACTIVE - 1));
        assertFalse(KaitenDome.active(KaitenDome.WINDUP + KaitenDome.MAX_ACTIVE));
    }
}
