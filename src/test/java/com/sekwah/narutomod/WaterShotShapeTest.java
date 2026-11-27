package com.sekwah.narutomod;

import com.sekwah.narutomod.util.WaterDragonFlight;
import com.sekwah.narutomod.util.WaterShotShape;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterShotShapeTest {
    @Test
    void heavyRoundIsWiderAndSlowerAtCruise() {
        assertTrue(WaterShotShape.radius(true) > WaterShotShape.radius(false));
        assertTrue(WaterShotShape.velocity(10, true) < WaterShotShape.velocity(10, false));
        assertEquals(WaterShotShape.velocity(10, true), WaterShotShape.velocity(29, true));
    }

    @Test
    void splashIsBoundedAndScalesWithDistanceFromContact() {
        assertEquals(4, WaterShotShape.splashDamage(0, false));
        assertEquals(0, WaterShotShape.splashDamage(2.1, false));
        assertTrue(WaterShotShape.splashDamage(2.1, true) > 0);
        assertEquals(0, WaterShotShape.splashDamage(4, true));
    }

    @Test
    void glancingContactReflectsOutAlongTheBlockFace() {
        Vec3 heading = new Vec3(0.7, -0.2, 0).normalize();
        Vec3 reflected = WaterShotShape.reflect(heading, new Vec3(0, 1, 0));
        assertEquals(heading.x, reflected.x, 1e-7);
        assertEquals(-heading.y, reflected.y, 1e-7);
    }

    @Test
    void narrowWallFaceWinsBeforeTheTargetBehindIt() {
        Vec3 from = new Vec3(0, 0.5, 0);
        Vec3 to = new Vec3(2, 0.5, 0);
        var wall = WaterDragonFlight.sweep(from, to, new AABB(1, 0, -0.5, 1.06, 1, 0.5),
                WaterShotShape.radius(false));
        var target = WaterDragonFlight.sweep(from, to, new AABB(1.38, 0, -0.2, 1.55, 1, 0.2),
                WaterShotShape.radius(false));
        assertNotNull(wall);
        assertNotNull(target);
        assertEquals(Direction.WEST, wall.normal());
        assertFalse(target.precedes(wall));
        assertEquals(1, wall.point().x, 1e-7);
    }

    @Test
    void aHeavyRoundCanCatchAThinTargetTheLighterRoundMisses() {
        var bounds = new AABB(0.9, 0, 0.21, 1.1, 1, 0.24);
        var to = new Vec3(1.4, 0.5, 0);
        var from = new Vec3(0, 0.5, 0);
        assertNull(WaterDragonFlight.sweep(from, to, bounds, WaterShotShape.radius(false)));
        assertNotNull(WaterDragonFlight.sweep(from, to, bounds, WaterShotShape.radius(true)));
    }
}
