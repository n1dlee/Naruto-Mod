package com.sekwah.narutomod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IceSpikeFormationTest {
    @Test
    void frostGrowsOutwardWithACappedNumberOfDistinctColumns() {
        var feet = new Vec3(0.5, 5, 0.5);
        var field = IceSpikeFormation.roots(feet, 0, 20);
        assertFalse(field.contains(new BlockPos(0, 5, 0)));
        assertEquals(field.size(), new HashSet<>(field).size());
        assertTrue(field.size() <= 48);
        assertTrue(field.size() > IceSpikeFormation.roots(feet, 0, 8).size());
        assertTrue(IceSpikeFormation.delay(feet, field.get(0)) < IceSpikeFormation.delay(feet, field.get(field.size() - 1)));
    }

    @Test
    void crampedColumnsStillEndInATip() {
        for (int height = 1; height <= 4; height++) {
            var sections = IceSpikeFormation.layers(height);
            assertEquals(height, sections.size());
            assertEquals(DripstoneThickness.TIP, sections.get(height - 1));
        }
        assertTrue(IceSpikeFormation.radius(DripstoneThickness.BASE) > IceSpikeFormation.radius(DripstoneThickness.TIP));
        assertTrue(IceSpikeFormation.height(DripstoneThickness.TIP) < 1);
    }

    @Test
    void freezeRespectsImmunityAndNeverLowersAnExistingMeter() {
        assertEquals(6, IceSpikeFormation.chill(6, 140, false));
        assertEquals(37, IceSpikeFormation.chill(6, 140, true));
        assertEquals(144, IceSpikeFormation.chill(136, 140, true));
        assertEquals(165, IceSpikeFormation.chill(165, 140, true));
    }
}
