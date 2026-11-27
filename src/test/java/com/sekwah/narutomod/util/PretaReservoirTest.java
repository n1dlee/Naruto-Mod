package com.sekwah.narutomod.util;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PretaReservoirTest {
    @Test
    void tooLargeAProjectileDoesNotSpendAGuardChargeOrVanish() {
        var guard = new PretaReservoir();
        assertEquals(0, guard.absorb(PretaReservoir.CAPACITY + 1, true).absorbed());
        assertFalse(guard.exhausted());
        assertEquals(PretaReservoir.CAPACITY, guard.absorb(PretaReservoir.CAPACITY, true).absorbed());
        assertTrue(guard.exhausted());
    }

    @Test
    void partialAbsorptionCannotOverReduceOrPrintChakra() {
        var guard = new PretaReservoir();
        assertEquals(7.8, guard.absorb(10, false).absorbed(), 1.0e-6);
        float credited = 0;
        guard = new PretaReservoir();
        for (int i = 0; i < 24; i++) {
            credited += guard.absorb(6, false).chakra();
        }
        assertTrue(guard.exhausted());
        assertEquals(8, credited, 1.0e-6);
        assertEquals(0, guard.absorb(Float.NaN, false).chakra());
    }

    @Test
    void packagedDamageAllowlistDoesNotContainVanillaMagicOrGentleFist() throws Exception {
        try (var resource = Objects.requireNonNull(getClass().getResourceAsStream("/data/narutomod/tags/damage_type/preta_absorbable.json"));
             var contents = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            var names = new HashSet<String>();
            JsonParser.parseReader(contents).getAsJsonObject().getAsJsonArray("values")
                    .forEach(value -> names.add(value.getAsString()));
            assertTrue(names.contains("narutomod:rasengan"));
            assertFalse(names.contains("narutomod:trigram_palms"));
            assertFalse(names.contains("minecraft:magic"));
            assertFalse(names.contains("minecraft:indirect_magic"));
        }
    }
}
