package com.sekwah.narutomod;

import com.sekwah.narutomod.util.ChakraFlow;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChakraFlowTest {
    @Test
    void knownWeaponsHaveTheirOwnModelAxisAndWidth() {
        var sword = ChakraFlow.edge(new ResourceLocation("minecraft", "iron_sword"));
        var katana = ChakraFlow.edge(new ResourceLocation("narutomod", "katana"));
        var kunai = ChakraFlow.edge(new ResourceLocation("narutomod", "kunai"));
        assertNotNull(sword);
        assertTrue(sword.tip().y > sword.base().y);
        assertTrue(katana.tip().x > katana.base().x);
        assertTrue(kunai.tip().z < kunai.base().z);
        assertTrue(sword.offset().z > 0.5 / 16);
        assertTrue(kunai.offset().x > 1.0 / 16);
        assertNotEquals(katana.width(), kunai.width());
    }

    @Test
    void unprofiledItemsCannotConsumeChakraOrDrawAnInventedBlade() {
        assertNull(ChakraFlow.edge((ResourceLocation) null));
        assertNull(ChakraFlow.edge(new ResourceLocation("minecraft", "iron_pickaxe")));
        assertNull(ChakraFlow.edge(new ResourceLocation("minecraft", "bow")));
        assertNull(ChakraFlow.edge(new ResourceLocation("another_mod", "iron_sword")));
    }

    @Test
    void coatingFillsInFourTicksAndSwingEndsWithoutResidue() {
        assertEquals(0, ChakraFlow.fill(-1));
        assertEquals(0.5f, ChakraFlow.fill(ChakraFlow.COAT_TICKS / 2.0f));
        assertEquals(1, ChakraFlow.fill(ChakraFlow.COAT_TICKS + 1));
        assertEquals(0, ChakraFlow.swing(0));
        assertEquals(1, ChakraFlow.swing(0.5f), 1e-4);
        assertEquals(0, ChakraFlow.swing(1));
    }

    @Test
    void rankBonusRemainsCapped() {
        assertEquals(5, ChakraFlow.bonus(1));
        assertEquals(12.5f, ChakraFlow.bonus(4.2f));
    }
}
