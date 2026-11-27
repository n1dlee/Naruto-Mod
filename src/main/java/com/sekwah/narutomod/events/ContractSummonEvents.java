package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.entity.SummonBeastEntity;
import com.sekwah.narutomod.util.ContractSummonLedger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ContractSummonEvents {
    private ContractSummonEvents() {
    }

    public static boolean occupied(ServerPlayer owner) {
        ServerLevel level = owner.serverLevel();
        var registry = ContractSummonLedger.of(level);
        return registry.active(owner.getUUID(), ContractSummonLedger.time(level)) != null
                || !level.getEntitiesOfClass(SummonBeastEntity.class, owner.getBoundingBox().inflate(50),
                summon -> summon.isAlive() && !summon.isDismissing()
                        && summon.getOwnerUUID().filter(owner.getUUID()::equals).isPresent()).isEmpty();
    }

    public static boolean admit(ServerPlayer owner, SummonBeastEntity beast, ServerLevel level) {
        var book = ContractSummonLedger.of(level);
        if (!book.claim(owner.getUUID(), beast.getUUID(), level.dimension().location(), ContractSummonLedger.time(level))) {
            return false;
        }
        beast.beginContract();
        return true;
    }

    public static boolean recall(ServerPlayer owner) {
        var book = ContractSummonLedger.of(owner.serverLevel());
        var lease = book.active(owner.getUUID(), ContractSummonLedger.time(owner.serverLevel()));
        if (lease == null) {
            return false;
        }
        book.release(owner.getUUID(), lease.entity());
        var old = owner.serverLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, lease.dimension()));
        if (old != null && old.getEntity(lease.entity()) instanceof SummonBeastEntity beast) {
            beast.dispel();
        }
        return true;
    }

    @SubscribeEvent
    public static void worldChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer owner) {
            recall(owner);
        }
    }
}
