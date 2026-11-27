package com.sekwah.narutomod;

import com.sekwah.narutomod.util.BaikaScale;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaikaScaleTest {
    private static final EntityDimensions STANDING = EntityDimensions.scalable(0.6f, 1.8f);

    @Test
    void growthIsMonotoneAndDoesNotSnapToFullSize() {
        assertEquals(1f, BaikaScale.growth(0));
        for (int t = 1; t < BaikaScale.GROW_TICKS; t++) {
            assertTrue(BaikaScale.growth(t) > BaikaScale.growth(t - 1));
            assertTrue(BaikaScale.growth(t) < BaikaScale.FULL);
        }
        assertEquals(BaikaScale.FULL, BaikaScale.growth(BaikaScale.GROW_TICKS));
    }

    @Test
    void doorwayAndCeilingTestTheActualStandingWidthAndHeight() {
        Vec3 feet = new Vec3(0.5, 64, 0.5);
        AABB baseline = BaikaScale.bounds(STANDING, 1, 1, feet);
        AABB enlarged = BaikaScale.bounds(STANDING, 1, BaikaScale.FULL, feet);
        AABB doorwaySide = new AABB(1, 64, 0, 2, 66, 1);
        AABB ceiling = new AABB(0, 67, 0, 1, 68, 1);
        assertFalse(baseline.intersects(doorwaySide));
        assertFalse(baseline.intersects(ceiling));
        assertTrue(enlarged.intersects(doorwaySide));
        assertTrue(enlarged.intersects(ceiling));
        assertEquals(feet.y, enlarged.minY);
    }

    @Test
    void alreadyScaledDimensionsAreNotMultipliedTwice() {
        float partial = BaikaScale.growth(6);
        AABB expected = BaikaScale.bounds(STANDING, 1, BaikaScale.FULL, Vec3.ZERO);
        AABB fromGrowth = BaikaScale.bounds(STANDING.scale(partial), partial, BaikaScale.FULL, Vec3.ZERO);
        assertEquals(expected.getXsize(), fromGrowth.getXsize(), 1.0e-6);
        assertEquals(expected.getYsize(), fromGrowth.getYsize(), 1.0e-6);
        assertEquals(1f, BaikaScale.shrink(partial, BaikaScale.SHRINK_TICKS));
    }
}
