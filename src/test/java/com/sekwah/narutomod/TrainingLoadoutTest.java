package com.sekwah.narutomod;

import com.sekwah.narutomod.abilities.JutsuScrolls;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.config.NarutoConfig;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingLoadoutTest {
    @Test
    void temporarilyProvidesFiveMaxedNaturesFullChakraAndEveryScroll() {
        float oldScale = NarutoConfig.maxChakra;
        try {
            NarutoConfig.maxChakra = 100;
            var ninja = new NinjaData(true);
            ninja.setClanId("uzumaki");
            ninja.setChakra(32);
            ninja.enableTrainingLoadout();
            assertTrue(ninja.hasTrainingLoadout());
            assertEquals(120_000f, ninja.getChakraXp());
            assertEquals(12, ninja.getRankIndex());
            assertFalse(ninja.isSixPaths());
            assertTrue(ninja.getMaxChakra() > 100);
            assertEquals(ninja.getMaxChakra(), ninja.getChakra());
            assertEquals(Set.of("fire", "water", "earth", "wind", "lightning"), Set.copyOf(ninja.getUnlockedElements()));
            for (String nature : ninja.getUnlockedElements()) {
                assertEquals(20, ninja.getElementLevel(nature));
            }
            for (String scroll : JutsuScrolls.SCROLL_JUTSU) {
                assertTrue(ninja.isJutsuLearned(scroll), scroll);
            }
            assertFalse(ninja.isJutsuLearned("kamui"));
            ninja.useChakra(30, 0);
            ninja.enableTrainingLoadout();
            assertEquals(ninja.getMaxChakra(), ninja.getChakra());
            assertTrue(ninja.disableTrainingLoadout());
            assertEquals(32f, ninja.getChakra());
        } finally {
            NarutoConfig.maxChakra = oldScale;
        }
    }

    @Test
    void reloadAndRemovalDoNotGrantOrErasePermanentProgress() {
        float oldScale = NarutoConfig.maxChakra;
        try {
            NarutoConfig.maxChakra = 100;
            var ninja = new NinjaData(true);
            ninja.setClanId("uchiha");
            ninja.addChakraXp(1250f);
            ninja.setElementLevel("fire", 8);
            ninja.learnJutsu("shadow_clone");
            ninja.setChakra(47);
            ninja.enableTrainingLoadout();
            ninja.addChakraXp(80_000f);
            ninja.addElementXp("fire", 10000);
            var reloaded = new NinjaData(true);
            reloaded.deserializeNBT(ninja.serializeNBT());
            assertTrue(reloaded.hasTrainingLoadout());
            assertTrue(reloaded.isJutsuLearned("eight_gates"));
            assertTrue(reloaded.disableTrainingLoadout());
            assertFalse(reloaded.disableTrainingLoadout());
            assertEquals(47f, reloaded.getChakra());
            assertEquals(1250f, reloaded.getChakraXp());
            assertEquals(1, reloaded.getRankIndex());
            assertEquals("uchiha", reloaded.getClanId());
            assertEquals("fire", reloaded.getNatureAffinity());
            assertEquals(Set.of("fire"), Set.copyOf(reloaded.getUnlockedElements()));
            assertEquals(8, reloaded.getElementLevel("fire"));
            assertTrue(reloaded.isJutsuLearned("shadow_clone"));
            assertFalse(reloaded.isJutsuLearned("eight_gates"));
            assertFalse(((CompoundTag) reloaded.serializeNBT()).contains("trainingLoadoutPreviousChakra"));
        } finally {
            NarutoConfig.maxChakra = oldScale;
        }
    }
}
