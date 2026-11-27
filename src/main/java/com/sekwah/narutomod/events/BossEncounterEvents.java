package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.entity.BossEncounterLedger;
import com.sekwah.narutomod.entity.BossSpawnProgression;
import com.sekwah.narutomod.entity.MangekyoBossEntity;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.TailedBeastEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID)
public final class BossEncounterEvents {
    private BossEncounterEvents() {
    }

    public static boolean canSpawn(ServerLevelAccessor world, BlockPos pos, int minimumRank) {
        var player = BossSpawnProgression.nearbyPlayer(world, pos);
        if (BossSpawnProgression.rank(player) < minimumRank) {
            return false;
        }
        ServerLevel level = world.getLevel();
        return BossEncounterLedger.of(level).available(pos, player.blockPosition(), id -> {
            Entity boss = level.getEntity(id);
            return boss == null ? null : boss.blockPosition();
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void join(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel level) || !isBoss(entity) || !entity.isAlive()) {
            return;
        }
        boolean admittedSpawn = entity instanceof MangekyoBossEntity ninja ? ninja.takeSpawnAdmission()
                : ((TailedBeastEntity) entity).takeSpawnAdmission();
        if (admittedSpawn) {
            int rank = entity instanceof MangekyoBossEntity ninja ? BossSpawnProgression.minimumRank(ninja.getVariant())
                    : BossSpawnProgression.minimumRank(((TailedBeastEntity) entity).getVariant());
            if (!canSpawn(level, entity.blockPosition(), rank)) {
                event.setCanceled(true);
                return;
            }
            if (entity instanceof MangekyoBossEntity ninja) {
                ninja.announceSighting(ninja.getVariant());
            }
        }
        BossEncounterLedger.of(level).record(entity.getUUID(), entity.blockPosition());
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel level) || !isBoss(entity)) {
            return;
        }
        var reason = entity.getRemovalReason();
        var ledger = BossEncounterLedger.of(level);
        if (reason == Entity.RemovalReason.UNLOADED_TO_CHUNK) {
            ledger.record(entity.getUUID(), entity.blockPosition());
        } else if (reason != null && reason != Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
            ledger.release(entity.getUUID());
        }
    }

    @SubscribeEvent
    public static void refresh(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level
                && level.getGameTime() % 20 == 0) {
            var ledger = BossEncounterLedger.of(level);
            level.getEntities(NarutoEntities.MANGEKYO_BOSS.get(), Entity::isAlive)
                    .forEach(boss -> ledger.record(boss.getUUID(), boss.blockPosition()));
            level.getEntities(NarutoEntities.TAILED_BEAST.get(), Entity::isAlive)
                    .forEach(boss -> ledger.record(boss.getUUID(), boss.blockPosition()));
        }
    }

    private static boolean isBoss(Entity entity) {
        return entity instanceof MangekyoBossEntity || entity instanceof TailedBeastEntity;
    }
}
