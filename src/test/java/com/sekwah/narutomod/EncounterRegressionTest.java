package com.sekwah.narutomod;

import com.sekwah.narutomod.abilities.jutsus.WaterDragonAbility;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.entity.BossEncounterLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncounterRegressionTest {
    @Test
    void sharinganKeepsMultipleTechniquesWithoutSkippingElementTraining() {
        var data = new NinjaData(true);
        data.setClanId("uchiha");
        var dragon = new WaterDragonAbility();
        data.grantElement("water");
        data.setElementLevel("water", dragon.elementLevelRequired() - 1);
        data.setCopiedJutsu("water_dragon");
        data.setCopiedJutsu("fireball");

        var restored = new NinjaData(true);
        restored.deserializeNBT(data.serializeNBT());
        assertTrue(restored.isJutsuLearned("water_dragon"));
        assertTrue(restored.isJutsuLearned("fireball"));
        assertEquals("fireball", restored.getCopiedJutsu());
        assertFalse(dragon.hasElementAccess(restored));
        restored.setElementLevel("water", dragon.elementLevelRequired());
        assertTrue(dragon.hasElementAccess(restored));
        restored.setElementLevel("water", dragon.elementLevelRequired() + 1);
        assertTrue(dragon.hasElementAccess(restored));
    }

    @Test
    void migratesAnUnspentCopyInAnOlderSave() {
        var data = new NinjaData(true);
        var tag = (CompoundTag) data.serializeNBT();
        tag.putString("copiedJutsu", "water_dragon");
        data.deserializeNBT(tag);
        assertTrue(data.isJutsuLearned("water_dragon"));
        assertFalse(new WaterDragonAbility().hasElementAccess(data));
    }

    @Test
    void unloadedBossBlocksBothTheSpawnPointAndThePlayerArea() {
        var ledger = new BossEncounterLedger();
        var boss = UUID.randomUUID();
        ledger.record(boss, new BlockPos(450, -30, 0));
        var restored = BossEncounterLedger.read(ledger.save(new CompoundTag()));
        assertFalse(restored.available(new BlockPos(-450, 90, 0), BlockPos.ZERO, id -> null));
        assertFalse(restored.available(new BlockPos(1250, 90, 0), new BlockPos(1500, 0, 0), id -> null));
        assertTrue(restored.available(new BlockPos(1251, 90, 0), new BlockPos(1500, 0, 0), id -> null));
        assertTrue(restored.available(BlockPos.ZERO, BlockPos.ZERO, id -> new BlockPos(812, 0, 0)));
        restored.release(boss);
        assertTrue(restored.available(BlockPos.ZERO, BlockPos.ZERO, id -> null));
    }
}
