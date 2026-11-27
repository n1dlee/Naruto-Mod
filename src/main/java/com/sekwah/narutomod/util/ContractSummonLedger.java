package com.sekwah.narutomod.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ContractSummonLedger extends SavedData {
    public static final int LIFETIME = 1800;
    private final Map<UUID, Lease> bonds = new HashMap<>();

    public static ContractSummonLedger of(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                ContractSummonLedger::read, ContractSummonLedger::new, "narutomod_contract_summons");
    }

    public static long time(ServerLevel level) { return level.getServer().overworld().getGameTime(); }

    public @Nullable Lease active(UUID owner, long now) {
        Lease lease = this.bonds.get(owner);
        if (lease != null && now >= lease.expires()) {
            this.bonds.remove(owner);
            this.setDirty();
            return null;
        }
        return lease;
    }

    public boolean claim(UUID owner, UUID entity, ResourceLocation dimension, long now) {
        if (active(owner, now) != null) {
            return false;
        }
        this.bonds.put(owner, new Lease(entity, dimension, now + LIFETIME));
        this.setDirty();
        return true;
    }

    public boolean owns(UUID owner, UUID entity, ResourceLocation dimension, long now) {
        var lease = this.active(owner, now);
        return lease != null && lease.entity().equals(entity) && lease.dimension().equals(dimension);
    }

    public void release(UUID owner, UUID onlyEntity) {
        var lease = this.bonds.get(owner);
        if (lease != null && lease.entity().equals(onlyEntity)) {
            this.bonds.remove(owner);
            this.setDirty();
        }
    }

    public static ContractSummonLedger read(CompoundTag tag) {
        var book = new ContractSummonLedger();
        var entries = tag.getList("Contracts", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            var row = entries.getCompound(i);
            var world = ResourceLocation.tryParse(row.getString("World"));
            if (world != null && row.hasUUID("Owner") && row.hasUUID("Beast") && row.getLong("End") > 0) {
                book.bonds.put(row.getUUID("Owner"), new Lease(row.getUUID("Beast"), world, row.getLong("End")));
            }
        }
        return book;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        var entries = new ListTag();
        this.bonds.forEach((owner, lease) -> {
            var row = new CompoundTag();
            row.putUUID("Owner", owner);
            row.putUUID("Beast", lease.entity());
            row.putString("World", lease.dimension().toString());
            row.putLong("End", lease.expires());
            entries.add(row);
        });
        tag.put("Contracts", entries);
        return tag;
    }

    public record Lease(UUID entity, ResourceLocation dimension, long expires) {
    }
}
