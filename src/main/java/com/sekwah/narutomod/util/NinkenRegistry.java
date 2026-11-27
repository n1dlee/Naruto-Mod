package com.sekwah.narutomod.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NinkenRegistry extends SavedData {
    public static final int LIFETIME = 2400;
    private final Map<UUID, Entry> owners = new HashMap<>();

    public static NinkenRegistry of(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(NinkenRegistry::read, NinkenRegistry::new,
                "narutomod_ninken");
    }

    public static long time(ServerLevel level) {
        return level.getServer().overworld().getGameTime();
    }

    public @Nullable UUID active(UUID owner, long now) {
        Entry entry = this.owners.get(owner);
        if (entry == null) {
            return null;
        }
        if (now >= entry.expires()) {
            this.owners.remove(owner);
            this.setDirty();
            return null;
        }
        return entry.partner();
    }

    public boolean claim(UUID owner, UUID partner, long now) {
        if (this.active(owner, now) != null) {
            return false;
        }
        this.owners.put(owner, new Entry(partner, now + LIFETIME));
        this.setDirty();
        return true;
    }

    public void release(UUID owner, UUID partner) {
        Entry entry = this.owners.get(owner);
        if (entry != null && entry.partner().equals(partner)) {
            this.owners.remove(owner);
            this.setDirty();
        }
    }

    public static NinkenRegistry read(CompoundTag root) {
        var data = new NinkenRegistry();
        ListTag rows = root.getList("Pairs", Tag.TAG_COMPOUND);
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.getCompound(i);
            if (row.hasUUID("Owner") && row.hasUUID("Partner")) {
                data.owners.put(row.getUUID("Owner"), new Entry(row.getUUID("Partner"), row.getLong("Expires")));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        var rows = new ListTag();
        this.owners.forEach((owner, entry) -> {
            var row = new CompoundTag();
            row.putUUID("Owner", owner);
            row.putUUID("Partner", entry.partner());
            row.putLong("Expires", entry.expires());
            rows.add(row);
        });
        root.put("Pairs", rows);
        return root;
    }

    private record Entry(UUID partner, long expires) {
    }
}
