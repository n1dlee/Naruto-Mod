package com.sekwah.narutomod;

import com.sekwah.narutomod.entity.BossChakraReserve;
import com.sekwah.narutomod.entity.BossSpawnProgression;
import com.sekwah.narutomod.entity.MangekyoBossVariant;
import com.sekwah.narutomod.entity.TailedBeastVariant;
import com.sekwah.narutomod.world.NinjaPopulation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossEncounterTest {
    @Test
    void aNinjaCanSpendDownItsReserveDespiteLongCastCooldowns() {
        var chakra = new BossChakraReserve(248, BossChakraReserve.NINJA_CAST_FLOOR);
        assertTrue(chakra.spend(70));
        tick(chakra, true, 240);
        assertEquals(178, chakra.remaining());
        assertTrue(chakra.spend(70));
        assertTrue(chakra.spend(45));
        assertTrue(chakra.exhausted());
        assertFalse(chakra.spend(8));
        assertEquals(63, chakra.remaining());
    }

    @Test
    void exhaustionAndRecoveryPauseSurviveAChunkReload() {
        var chakra = new BossChakraReserve(240, BossChakraReserve.BEAST_CAST_FLOOR);
        assertTrue(chakra.spend(105));
        assertTrue(chakra.spend(105));
        assertTrue(chakra.spend(30));
        tick(chakra, true, 40);
        var tag = new CompoundTag();
        chakra.save(tag);
        var restored = new BossChakraReserve(240, BossChakraReserve.BEAST_CAST_FLOOR);
        restored.load(tag);
        tick(restored, true, 60);
        assertEquals(0, restored.remaining());
        assertFalse(restored.canSpend(30));
        tick(restored, true, 269);
        assertTrue(restored.exhausted());
        tick(restored, true, 1);
        assertTrue(restored.canSpend(105));
    }

    @Test
    void restRecoversChakraWithoutRefillingPastTheVariantLimit() {
        var chakra = new BossChakraReserve(248, BossChakraReserve.NINJA_CAST_FLOOR);
        assertFalse(chakra.spend(Float.NaN));
        assertFalse(chakra.spend(-35));
        assertFalse(chakra.spend(249));
        assertTrue(chakra.spend(35));
        tick(chakra, false, 120);
        assertEquals(233, chakra.remaining());
        tick(chakra, false, 10_000);
        assertEquals(248, chakra.remaining());
        var legacy = new CompoundTag();
        legacy.putFloat("BossChakra", 400);
        chakra.load(legacy);
        assertEquals(248, chakra.remaining());
    }

    @Test
    void eligibleNinjaPoolCannotRollAHighRankBossEarly() {
        var random = RandomSource.create(982L);
        assertTrue(BossSpawnProgression.rollNinja(random, 3).isEmpty());
        assertEquals(MangekyoBossVariant.IRUKA, BossSpawnProgression.rollNinja(random, 4).orElseThrow());
        Set<MangekyoBossVariant> seen = new HashSet<>();
        for (int rank = 5; rank <= 13; rank++) {
            for (int roll = 0; roll < 1_000; roll++) {
                var variant = BossSpawnProgression.rollNinja(random, rank).orElseThrow();
                assertTrue(BossSpawnProgression.minimumRank(variant) <= rank);
                if (rank == 12) {
                    seen.add(variant);
                }
            }
        }
        assertEquals(Set.of(MangekyoBossVariant.values()), seen);
    }

    @Test
    void beastSelectionUsesKageTiersAndManualSpawnsKeepTheirOwnRules() {
        var random = RandomSource.create(843L);
        assertTrue(BossSpawnProgression.rollBeast(random, 9).isEmpty());
        for (int i = 0; i < 100; i++) {
            assertTrue(BossSpawnProgression.rollBeast(random, 10).orElseThrow().getTails() <= 2);
            assertTrue(BossSpawnProgression.rollBeast(random, 11).orElseThrow().getTails() <= 4);
        }
        assertEquals(12, BossSpawnProgression.minimumRank(TailedBeastVariant.GYUKI));
        assertTrue(BossSpawnProgression.isAmbient(MobSpawnType.NATURAL));
        assertFalse(BossSpawnProgression.isAmbient(MobSpawnType.COMMAND));
        assertFalse(BossSpawnProgression.isAmbient(MobSpawnType.SPAWN_EGG));
    }

    @Test
    void populationPolicyLeavesPassivesAndNinjaAloneAndHasAnOptOut() {
        assertTrue(NinjaPopulation.blocks(false, "minecraft", true));
        assertTrue(NinjaPopulation.blocks(false, "some_dimension_mod", true));
        assertFalse(NinjaPopulation.blocks(false, "minecraft", false));
        assertFalse(NinjaPopulation.blocks(false, "narutomod", true));
        assertFalse(NinjaPopulation.blocks(true, "minecraft", true));
    }

    private static void tick(BossChakraReserve chakra, boolean fighting, int ticks) {
        for (int i = 0; i < ticks; i++) {
            chakra.tick(fighting);
        }
    }
}
