package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.entity.MangekyoBossEntity;
import com.sekwah.narutomod.entity.MangekyoBossVariant;
import com.sekwah.narutomod.entity.NarutoEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Phase 16: puts the five Mangekyo wielders into the world.
 *
 * Nothing in this mod spawns naturally — every other entity is summoned by an ability —
 * so rather than wiring up biome modifiers and vanilla spawn rules for a single mob, this
 * rolls the dice on a timer and places the boss deliberately: near a player who has
 * something to gain from the fight, far enough out that they have to go find it.
 */
@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID)
public class MangekyoBossSpawner {

    /** Ring the boss appears in, relative to the chosen player. */
    private static final int MIN_DISTANCE = 40;
    private static final int MAX_DISTANCE = 80;
    private static final int PLACEMENT_ATTEMPTS = 12;

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !NarutoConfig.mangekyoBossSpawnEnabled
                || !(event.level instanceof ServerLevel serverLevel)) {
            return;
        }
        // Overworld only — these are missing-nin walking the countryside, not Nether spawns.
        if (serverLevel.dimension() != Level.OVERWORLD) {
            return;
        }
        if (++tickCounter < NarutoConfig.mangekyoBossSpawnInterval) {
            return;
        }
        tickCounter = 0;

        if (serverLevel.random.nextDouble() > NarutoConfig.mangekyoBossSpawnChance) {
            return;
        }
        if (countExistingBosses(serverLevel) >= NarutoConfig.mangekyoBossMaxPerWorld) {
            return;
        }

        ServerPlayer host = pickCandidatePlayer(serverLevel);
        if (host == null) {
            return;
        }
        MangekyoBossVariant variant = MangekyoBossVariant.values()[
                serverLevel.random.nextInt(MangekyoBossVariant.values().length)];
        BlockPos spawnPos = findSpawnPos(serverLevel, host.blockPosition());
        if (spawnPos == null) {
            return;
        }
        spawnBoss(serverLevel, spawnPos, variant);
    }

    private static int countExistingBosses(ServerLevel level) {
        return level.getEntities(NarutoEntities.MANGEKYO_BOSS.get(), entity -> true).size();
    }

    /**
     * Prefers a player who has already awakened a Mangekyo — the fight is their path to
     * the Eternal Mangekyo, so there is no point dropping an S-rank boss on an academy
     * student. Falls back to any ninja if nobody qualifies.
     */
    private static ServerPlayer pickCandidatePlayer(ServerLevel level) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return null;
        }
        ServerPlayer fallback = null;
        for (ServerPlayer player : players) {
            var data = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve();
            if (data.isEmpty() || !data.get().isNinjaModeEnabled()) {
                continue;
            }
            if (data.get().isMangekyoAwakened()) {
                return player;
            }
            fallback = player;
        }
        return fallback;
    }

    private static BlockPos findSpawnPos(ServerLevel level, BlockPos origin) {
        for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            int distance = MIN_DISTANCE + level.random.nextInt(MAX_DISTANCE - MIN_DISTANCE);
            int x = origin.getX() + (int) (Math.cos(angle) * distance);
            int z = origin.getZ() + (int) (Math.sin(angle) * distance);
            if (!level.isLoaded(new BlockPos(x, level.getMinBuildHeight() + 1, z))) {
                continue;
            }
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            // Needs headroom and solid footing, or the boss suffocates on arrival
            if (level.getBlockState(ground).isAir() && level.getBlockState(ground.above()).isAir()
                    && !level.getBlockState(ground.below()).isAir()) {
                return ground;
            }
        }
        return null;
    }

    private static void spawnBoss(ServerLevel level, BlockPos pos, MangekyoBossVariant variant) {
        MangekyoBossEntity boss = NarutoEntities.MANGEKYO_BOSS.get().create(level);
        if (boss == null) {
            return;
        }
        boss.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
        boss.applyVariant(variant);
        level.addFreshEntity(boss);

        // Everyone nearby should know an S-rank just walked into the region.
        Component announcement = Component.translatable("mangekyo.boss.sighted",
                        Component.translatable(variant.translationKey()).withStyle(ChatFormatting.RED))
                .withStyle(ChatFormatting.DARK_RED);
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().closerThan(pos, 160)) {
                player.displayClientMessage(announcement, false);
            }
        }
    }
}
