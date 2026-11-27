package com.sekwah.narutomod;

import com.google.gson.JsonParser;
import com.sekwah.narutomod.util.DoryuhekiLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoryuhekiLayoutTest {
    @Test
    void columnsStayAcrossLookDirectionAndNeverAtCasterFeet() {
        var origin = new BlockPos(-1, 75, -6);
        var line = DoryuhekiLayout.columns(origin, Direction.SOUTH);
        assertEquals(new BlockPos(-1, 75, -4), line.get(0).base());
        assertTrue(line.stream().allMatch(column -> column.base().getZ() == -4));
        assertEquals(3, line.stream().map(DoryuhekiLayout.Column::base).distinct().count());
    }

    @Test
    void quarterHeightsRiseFromTheCenterBeforeTheUpperRows() {
        var center = DoryuhekiLayout.columns(BlockPos.ZERO, Direction.NORTH).get(0);
        long start = DoryuhekiLayout.riseAt(20, center, 0);
        long end = DoryuhekiLayout.retractAt(20, center, 0);
        assertEquals(0, DoryuhekiLayout.height(start - 1, start, end));
        assertEquals(1, DoryuhekiLayout.height(start, start, end));
        assertEquals(4, DoryuhekiLayout.height(start + 6, start, end));
        assertTrue(DoryuhekiLayout.riseAt(20, center, 1) > start + 6);
    }

    @Test
    void retractionStartsAtTheTopAndCannotOutliveItsWorldTimestamp() {
        var center = DoryuhekiLayout.columns(BlockPos.ZERO, Direction.WEST).get(0);
        long start = DoryuhekiLayout.riseAt(0, center, 2);
        long end = DoryuhekiLayout.retractAt(0, center, 2);
        assertEquals(4, DoryuhekiLayout.height(end - 1, start, end));
        assertEquals(3, DoryuhekiLayout.height(end, start, end));
        assertEquals(0, DoryuhekiLayout.height(end + 6, start, end));
        assertTrue(DoryuhekiLayout.retractAt(0, center, 1) > end + 6);
    }

    @Test
    void eachSolidModelHasTheSameHeightAsItsOccupancyCheck() throws Exception {
        for (int stage = 1; stage <= DoryuhekiLayout.SOLID; stage++) {
            String path = "/assets/narutomod/models/block/doryuheki_" + stage + ".json";
            try (var asset = getClass().getResourceAsStream(path)) {
                assertNotNull(asset, path);
                var json = JsonParser.parseReader(new InputStreamReader(asset, StandardCharsets.UTF_8)).getAsJsonObject();
                var faces = json.getAsJsonArray("elements");
                assertFalse(faces.isEmpty());
                var top = faces.get(faces.size() - 1).getAsJsonObject().getAsJsonArray("to");
                assertEquals(DoryuhekiLayout.bounds(BlockPos.ZERO, stage).maxY, top.get(1).getAsDouble() / 16);
            }
        }
    }
}
