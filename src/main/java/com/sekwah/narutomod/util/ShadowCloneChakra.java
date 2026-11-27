package com.sekwah.narutomod.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ShadowCloneChakra extends SavedData {
    private final Map<UUID, Share> clones = new HashMap<>();
    private final Map<UUID, Float> pending = new HashMap<>();

    public static ShadowCloneChakra of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ShadowCloneChakra::read,
                ShadowCloneChakra::new, "narutomod_shadow_clone_chakra");
    }

    public static float split(float available, int count) {
        return valid(available) && count > 0 ? available / (count + 1f) : 0;
    }

    public int count(UUID owner) {
        return (int) clones.values().stream().filter(share -> share.owner.equals(owner)).count();
    }

    public boolean allocated(UUID clone) {
        return clones.containsKey(clone);
    }

    public void allocate(UUID clone, UUID owner, float amount, long expires) {
        if (valid(amount) && clones.putIfAbsent(clone, new Share(owner, amount, expires)) == null) {
            setDirty();
        }
    }

    public boolean canSpend(UUID clone, float cost) {
        Share share = clones.get(clone);
        return valid(cost) && share != null && share.remaining >= cost;
    }

    public boolean spend(UUID clone, float cost) {
        if (!canSpend(clone, cost)) {
            return false;
        }
        Share share = clones.get(clone);
        clones.put(clone, new Share(share.owner, share.remaining - cost, share.expires));
        setDirty();
        return true;
    }

    public void release(UUID clone) {
        Share share = clones.remove(clone);
        if (share != null) {
            queue(share);
            setDirty();
        }
    }

    public void expire(long now) {
        var iterator = clones.values().iterator();
        while (iterator.hasNext()) {
            Share share = iterator.next();
            if (now >= share.expires) {
                queue(share);
                iterator.remove();
                setDirty();
            }
        }
    }

    public float claim(UUID owner, float room) {
        if (!valid(room)) {
            return 0;
        }
        float stored = pending.getOrDefault(owner, 0f);
        float paid = Math.min(stored, room);
        if (paid > 0) {
            if (paid == stored) {
                pending.remove(owner);
            } else {
                pending.put(owner, stored - paid);
            }
            setDirty();
        }
        return paid;
    }

    private void queue(Share share) {
        if (valid(share.remaining)) {
            pending.merge(share.owner, share.remaining, Float::sum);
        }
    }

    private static boolean valid(float amount) {
        return Float.isFinite(amount) && amount > 0;
    }

    public static ShadowCloneChakra read(CompoundTag tag) {
        var ledger = new ShadowCloneChakra();
        ListTag active = tag.getList("Active", Tag.TAG_COMPOUND);
        for (int i = 0; i < active.size(); i++) {
            CompoundTag row = active.getCompound(i);
            float remaining = row.getFloat("Chakra");
            if (row.hasUUID("Clone") && row.hasUUID("Owner") && Float.isFinite(remaining) && remaining >= 0) {
                ledger.clones.put(row.getUUID("Clone"), new Share(row.getUUID("Owner"), remaining, row.getLong("Until")));
            }
        }
        ListTag owed = tag.getList("Pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < owed.size(); i++) {
            CompoundTag row = owed.getCompound(i);
            float amount = row.getFloat("Chakra");
            if (row.hasUUID("Owner") && valid(amount)) {
                ledger.pending.put(row.getUUID("Owner"), amount);
            }
        }
        return ledger;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        var active = new ListTag();
        clones.forEach((id, share) -> {
            var row = new CompoundTag();
            row.putUUID("Clone", id);
            row.putUUID("Owner", share.owner);
            row.putFloat("Chakra", share.remaining);
            row.putLong("Until", share.expires);
            active.add(row);
        });
        var owed = new ListTag();
        pending.forEach((id, amount) -> {
            var row = new CompoundTag();
            row.putUUID("Owner", id);
            row.putFloat("Chakra", amount);
            owed.add(row);
        });
        tag.put("Active", active);
        tag.put("Pending", owed);
        return tag;
    }

    private record Share(UUID owner, float remaining, long expires) {
    }
}
