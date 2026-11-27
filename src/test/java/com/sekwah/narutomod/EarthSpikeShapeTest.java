package com.sekwah.narutomod;

import com.google.gson.JsonParser;
import com.sekwah.narutomod.util.EarthSpikeShape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EarthSpikeShapeTest {
    @Test
    void warningPropagatesOutwardAndTipFallsBeforeTheRoot() {
        long root = EarthSpikeShape.riseAt(15, 3, 0);
        long tip = EarthSpikeShape.riseAt(15, 3, 2);
        assertTrue(root < tip);
        assertTrue(EarthSpikeShape.riseAt(15, 13, 0) > root);
        assertTrue(EarthSpikeShape.endAt(root, 0) > EarthSpikeShape.endAt(tip, 2));
    }

    @Test
    void damageUsesTheActualNarrowingVolumeRatherThanTheEntireColumn() {
        BlockPos origin = new BlockPos(-12, 63, 44);
        AABB outer = new AABB(-11.84, 63.12, 44.13, -11.75, 63.28, 44.27);
        AABB point = new AABB(-11.56, 63.65, 44.44, -11.45, 63.91, 44.56);
        assertTrue(EarthSpikeShape.touches(origin, 0, outer));
        assertFalse(EarthSpikeShape.touches(origin, 2, outer));
        assertTrue(EarthSpikeShape.touches(origin, 2, point));
    }

    @Test
    void bothStoneSlabsMatchTheirServerVolumesForEveryModel() throws Exception {
        for (int row = 0; row < EarthSpikeShape.SEGMENTS; row++) {
            String path = "/assets/narutomod/models/block/earth_spike_" + row + ".json";
            try (var asset = getClass().getResourceAsStream(path)) {
                assertNotNull(asset, path);
                var json = JsonParser.parseReader(new InputStreamReader(asset, StandardCharsets.UTF_8)).getAsJsonObject();
                var layers = json.getAsJsonArray("elements");
                assertEquals(2, layers.size());
                for (int i = 0; i < layers.size(); i++) {
                    var geometry = layers.get(i).getAsJsonObject();
                    var from = geometry.getAsJsonArray("from");
                    var to = geometry.getAsJsonArray("to");
                    var slab = EarthSpikeShape.slab(BlockPos.ZERO, row, i == 1);
                    assertEquals(slab.minX, from.get(0).getAsDouble() / 16);
                    assertEquals(slab.minY, from.get(1).getAsDouble() / 16);
                    assertEquals(slab.minZ, from.get(2).getAsDouble() / 16);
                    assertEquals(slab.maxX, to.get(0).getAsDouble() / 16);
                    assertEquals(slab.maxY, to.get(1).getAsDouble() / 16);
                    assertEquals(slab.maxZ, to.get(2).getAsDouble() / 16);
                }
            }
        }
    }
}
