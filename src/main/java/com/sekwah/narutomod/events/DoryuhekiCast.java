package com.sekwah.narutomod.events;

import com.sekwah.narutomod.block.DoryuhekiBlockEntity;
import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.util.DoryuhekiLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

public final class DoryuhekiCast {
    private DoryuhekiCast() {
    }

    public static boolean reserve(ServerLevel level, Player player) {
        if (!player.onGround() || player.isSpectator()) {
            return false;
        }
        int count = 0;
        long now = level.getGameTime();
        for (DoryuhekiLayout.Column column : DoryuhekiLayout.columns(player.blockPosition(), player.getDirection())) {
            if (placeColumn(level, player, column, now)) {
                count++;
            }
        }
        return count > 0;
    }

    private static boolean placeColumn(ServerLevel level, Player player, DoryuhekiLayout.Column column, long time) {
        BlockPos base = column.base();
        if (!level.hasChunkAt(base) || !DoryuhekiBlockEntity.supported(level, base)
                || level.getBlockState(base.below()).is(NarutoBlocks.DORYUHEKI.get())) {
            return false;
        }
        int rows = 0;
        for (int row = 0; row < DoryuhekiLayout.HEIGHT; row++) {
            BlockPos pos = base.above(row);
            if (!candidate(level, player, pos) || !place(level, player, pos,
                    DoryuhekiLayout.riseAt(time, column, row), DoryuhekiLayout.retractAt(time, column, row))) {
                break;
            }
            rows++;
        }
        if (rows == 0) {
            return false;
        }
        var soil = level.getBlockState(base.below());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, soil),
                base.getX() + 0.5, base.getY() + 0.06, base.getZ() + 0.5, 9, 0.35, 0.02, 0.35, 0.02);
        if (column.delay() == 0) {
            level.playSound(null, base, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 0.8f, 0.78f);
        }
        return true;
    }

    private static boolean candidate(ServerLevel level, Player player, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.mayInteract(player, pos) && level.isEmptyBlock(pos)
                && level.getEntitiesOfClass(LivingEntity.class, DoryuhekiLayout.bounds(pos, DoryuhekiLayout.SOLID),
                entity -> entity.isAlive() && !entity.isSpectator()).isEmpty();
    }

    private static boolean place(ServerLevel level, Player player, BlockPos pos, long rises, long retracts) {
        var snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (!level.setBlock(pos, NarutoBlocks.DORYUHEKI.get().defaultBlockState(), Block.UPDATE_ALL)) {
            return false;
        }
        if (ForgeEventFactory.onBlockPlace(player, snapshot, Direction.UP)
                || !(level.getBlockEntity(pos) instanceof DoryuhekiBlockEntity segment)) {
            if (level.getBlockState(pos).is(NarutoBlocks.DORYUHEKI.get())) {
                snapshot.restore(true, false);
            }
            return false;
        }
        segment.plan(rises, retracts);
        return true;
    }
}
