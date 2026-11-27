package com.sekwah.narutomod.events;

import com.sekwah.narutomod.block.MokutonTimberBlock;
import com.sekwah.narutomod.block.MokutonTimberBlockEntity;
import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.util.WoodCagePlan.Cell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.UUID;

final class MokutonTerrain {
    private MokutonTerrain() {
    }

    static boolean free(ServerLevel level, Player owner, Cell cell) {
        BlockPos pos = cell.pos();
        return level.hasChunkAt(pos) && !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
                && level.mayInteract(owner, pos) && level.isEmptyBlock(pos)
                && level.getEntitiesOfClass(LivingEntity.class, cell.profile().shape().bounds().move(pos),
                entity -> entity.isAlive() && !entity.isSpectator()).isEmpty();
    }

    static boolean place(ServerLevel level, Player owner, Cell cell, UUID cast, long expires) {
        BlockPos pos = cell.pos();
        if (!free(level, owner, cell) || !MokutonTimberBlockEntity.anchored(level, cell.support(), cell.ground(), cast)) {
            return false;
        }
        var snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        var state = NarutoBlocks.MOKUTON_TIMBER.get().defaultBlockState().setValue(MokutonTimberBlock.PROFILE, cell.profile());
        if (!level.setBlock(pos, state, Block.UPDATE_ALL)) {
            return false;
        }
        var placed = level.getBlockEntity(pos);
        if (ForgeEventFactory.onBlockPlace(owner, snapshot, Direction.UP) || !(placed instanceof MokutonTimberBlockEntity timber)) {
            if (level.getBlockEntity(pos) == placed && level.getBlockState(pos) == state) {
                snapshot.restore(true, false);
            }
            return false;
        }
        if (level.getBlockEntity(pos) != timber) {
            return false;
        }
        timber.plan(new MokutonTimberBlockEntity.Lease(cast, cell.support(), cell.ground(), level.getGameTime(), expires));
        return true;
    }
}
