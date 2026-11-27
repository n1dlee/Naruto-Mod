package com.sekwah.narutomod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class BossEncounterLedger extends SavedData {
    public static final int EXCLUSION_RADIUS = 800;
    private final Map<UUID, BlockPos> encounters = new HashMap<>();

    public static BossEncounterLedger of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(BossEncounterLedger::read, BossEncounterLedger::new,
                "narutomod_boss_encounters");
    }

    public boolean available(BlockPos candidate, BlockPos player, Function<UUID, BlockPos> livePosition) {
        for (var entry : encounters.entrySet()) {
            BlockPos live = livePosition.apply(entry.getKey());
            BlockPos boss = live == null ? entry.getValue() : live;
            if (inRadius(boss, candidate) || inRadius(boss, player)) {
                return false;
            }
        }
        return true;
    }

    private static boolean inRadius(BlockPos boss, BlockPos center) {
        double dx = (double) boss.getX() - center.getX();
        double dz = (double) boss.getZ() - center.getZ();
        return dx * dx + dz * dz <= EXCLUSION_RADIUS * EXCLUSION_RADIUS;
    }

    public void record(UUID id, BlockPos pos) {
        if (!pos.equals(encounters.put(id, pos.immutable()))) {
            setDirty();
        }
    }

    public void release(UUID id) {
        if (encounters.remove(id) != null) {
            setDirty();
        }
    }

    public static BossEncounterLedger read(CompoundTag tag) {
        var ledger = new BossEncounterLedger();
        ListTag records = tag.getList("Bosses", Tag.TAG_COMPOUND);
        for (int i = 0; i < records.size(); i++) {
            CompoundTag row = records.getCompound(i);
            if (row.hasUUID("Id") && row.contains("Position", Tag.TAG_LONG)) {
                ledger.encounters.put(row.getUUID("Id"), BlockPos.of(row.getLong("Position")));
            }
        }
        return ledger;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        var records = new ListTag();
        encounters.forEach((id, pos) -> {
            var row = new CompoundTag();
            row.putUUID("Id", id);
            row.putLong("Position", pos.asLong());
            records.add(row);
        });
        tag.put("Bosses", records);
        return tag;
    }
}
