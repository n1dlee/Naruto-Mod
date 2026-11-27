package com.sekwah.narutomod.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MagatamaCluster extends SavedData {
    private static final int MAX_VOLLEYS = 256;
    private static final int MAX_TARGETS = 128;
    private final Map<UUID, Impact> impacts = new HashMap<>();

    public static MagatamaCluster of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(MagatamaCluster::read, MagatamaCluster::new, "narutomod_magatama_hits");
    }

    public boolean claim(UUID volley, UUID target, long now, long until) {
        if (impacts.values().removeIf(impact -> impact.expiry() <= now)) {
            this.setDirty();
        }
        if (until <= now || impacts.size() >= MAX_VOLLEYS && !impacts.containsKey(volley)) {
            return false;
        }
        Impact impact = impacts.computeIfAbsent(volley, id -> new Impact(until, new HashSet<>()));
        if (impact.victims().size() >= MAX_TARGETS || !impact.victims().add(target)) {
            return false;
        }
        this.setDirty();
        return true;
    }

    public static MagatamaCluster read(CompoundTag tag) {
        var result = new MagatamaCluster();
        for (Tag value : tag.getList("Volleys", Tag.TAG_COMPOUND)) {
            if (value instanceof CompoundTag entry && entry.hasUUID("Id") && result.impacts.size() < MAX_VOLLEYS) {
                Set<UUID> victims = new HashSet<>();
                for (Tag item : entry.getList("Targets", Tag.TAG_COMPOUND)) {
                    if (item instanceof CompoundTag target && target.hasUUID("Id") && victims.size() < MAX_TARGETS) {
                        victims.add(target.getUUID("Id"));
                    }
                }
                result.impacts.put(entry.getUUID("Id"), new Impact(entry.getLong("Expiry"), victims));
            }
        }
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        var list = new ListTag();
        impacts.forEach((id, impact) -> {
            var entry = new CompoundTag();
            entry.putUUID("Id", id);
            entry.putLong("Expiry", impact.expiry());
            var victims = new ListTag();
            for (UUID uuid : impact.victims()) {
                var target = new CompoundTag();
                target.putUUID("Id", uuid);
                victims.add(target);
            }
            entry.put("Targets", victims);
            list.add(entry);
        });
        tag.put("Volleys", list);
        return tag;
    }

    private record Impact(long expiry, Set<UUID> victims) {
    }
}
