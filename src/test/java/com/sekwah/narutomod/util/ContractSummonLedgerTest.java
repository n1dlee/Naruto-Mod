package com.sekwah.narutomod.util;

import com.sekwah.narutomod.entity.SummonBeastVariant;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractSummonLedgerTest {
    private static final ResourceLocation OVERWORLD = new ResourceLocation("minecraft", "overworld");
    private static final ResourceLocation NETHER = new ResourceLocation("minecraft", "the_nether");

    @Test
    void persistedContractRemembersAnExactEntityAndWorldThroughReload() {
        var store = new ContractSummonLedger();
        UUID owner = new UUID(5, 7);
        UUID beast = new UUID(5, 8);
        assertTrue(store.claim(owner, beast, OVERWORLD, 10));
        assertFalse(store.claim(owner, new UUID(5, 9), NETHER, 12));
        var restored = ContractSummonLedger.read(store.save(new CompoundTag()));
        assertTrue(restored.owns(owner, beast, OVERWORLD, 90));
        assertFalse(restored.owns(owner, beast, NETHER, 90));
        assertNull(restored.active(owner, 10 + ContractSummonLedger.LIFETIME));
    }

    @Test
    void oldBeastCannotReleaseTheReplacementLease() {
        var store = new ContractSummonLedger();
        UUID owner = new UUID(7, 7);
        UUID old = new UUID(7, 8);
        UUID next = new UUID(7, 9);
        assertTrue(store.claim(owner, old, OVERWORLD, 1));
        store.release(owner, old);
        assertTrue(store.claim(owner, next, NETHER, 2));
        store.release(owner, old);
        assertTrue(store.owns(owner, next, NETHER, 3));
    }

    @Test
    void signedContractOverridesItsFallbackAndSpeciesUseDistinctSeals() {
        assertEquals(SummonBeastVariant.MANDA, SummonBeastVariant.forNinja("manda", "uzumaki"));
        assertEquals(SummonBeastVariant.GAMABUNTA, SummonBeastVariant.forNinja("", "uzumaki"));
        assertEquals(SummonBeastVariant.KATSUYU, SummonBeastVariant.forNinja("katsuyu", "uchiha"));
        assertEquals(SummoningCircle.Style.MONKEY, SummonBeastVariant.ENMA.sealStyle());
        assertEquals(4, java.util.Arrays.stream(SummonBeastVariant.values()).map(SummonBeastVariant::sealStyle).distinct().count());
    }
}
