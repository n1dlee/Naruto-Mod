package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.List;
import java.util.Optional;

public final class BossSpawnProgression {
    public static final int FIRST_NINJA_RANK = 4;
    public static final int FIRST_BEAST_RANK = 10;
    public static final int ENCOUNTER_RANGE = 500;
    private static final int MAX_VERTICAL_DISTANCE = 20;
    private static final List<MangekyoBossVariant> NINJA = List.of(MangekyoBossVariant.values());
    private static final List<TailedBeastVariant> BEASTS = List.of(TailedBeastVariant.values());

    private BossSpawnProgression() {
    }

    public static boolean isAmbient(MobSpawnType reason) {
        return reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION;
    }

    public static int nearbyRank(ServerLevelAccessor level, BlockPos pos) {
        return rank(nearbyPlayer(level, pos));
    }

    public static Player nearbyPlayer(ServerLevelAccessor level, BlockPos pos) {
        Player nearest = level.getNearestPlayer(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5,
                ENCOUNTER_RANGE, entity -> entity instanceof Player player && player.isAlive()
                        && !player.isSpectator() && !player.isCreative());
        return nearest != null && Math.abs(nearest.getY() - pos.getY()) <= MAX_VERTICAL_DISTANCE
                ? nearest : null;
    }

    public static int rank(LivingEntity target) {
        if (!(target instanceof Player player) || !player.isAlive() || player.isCreative() || player.isSpectator()) {
            return -1;
        }
        return player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                .map(data -> data.isNinjaModeEnabled() && !data.getClanId().isEmpty() ? data.getRankIndex() : -1)
                .orElse(-1);
    }

    public static int minimumRank(MangekyoBossVariant variant) {
        return switch (variant) {
            case IRUKA -> 4;
            case TENTEN, HAKU, WHITE_ZETSU -> 5;
            case HINATA, SHIKAMARU, KANKURO, TEMARI, ZABUZA -> 6;
            case DEIDARA, KUROTSUCHI, SAKURA -> 7;
            case HIDAN, KISAME, SASORI -> 8;
            case KAKUZU, GAARA, SHISUI, KAKASHI -> 9;
            case ITACHI, SASUKE, OBITO, MIGHT_GUY -> 10;
            case HASHIRAMA, NAGATO -> 11;
            case NARUTO, MADARA -> 12;
        };
    }

    public static int minimumRank(TailedBeastVariant variant) {
        return FIRST_BEAST_RANK + Math.min(2, (variant.getTails() - 1) / 2);
    }

    public static Optional<MangekyoBossVariant> rollNinja(RandomSource random, int rank) {
        var pool = NINJA.stream().filter(variant -> minimumRank(variant) <= rank).toList();
        int weight = pool.stream().mapToInt(MangekyoBossVariant::spawnWeight).sum();
        if (weight == 0) {
            return Optional.empty();
        }
        int ticket = random.nextInt(weight);
        for (MangekyoBossVariant variant : pool) {
            ticket -= variant.spawnWeight();
            if (ticket < 0) {
                return Optional.of(variant);
            }
        }
        throw new IllegalStateException("Boss roll fell outside its weighted pool");
    }

    public static Optional<TailedBeastVariant> rollBeast(RandomSource random, int rank) {
        var pool = BEASTS.stream().filter(variant -> minimumRank(variant) <= rank).toList();
        return pool.isEmpty() ? Optional.empty() : Optional.of(pool.get(random.nextInt(pool.size())));
    }
}
