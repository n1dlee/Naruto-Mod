package com.sekwah.narutomod.events;

import com.sekwah.narutomod.util.WoodHouseLayout.Site;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SupportType;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.ArrayList;
import java.util.List;

final class WoodHousePlacement {
    private WoodHousePlacement() {
    }

    static boolean clear(ServerLevel level, Player owner, WoodHouseBlueprint plan) {
        Site site = plan.site();
        BlockPos end = site.corner().offset(site.size()).offset(-1, -1, -1);
        if (!level.getEntities(owner, site.box(), entity -> entity.isAlive() && !entity.isSpectator() && entity.isPickable()).isEmpty()
                || owner.getBoundingBox().intersects(site.box())) {
            return false;
        }
        for (BlockPos pos : BlockPos.betweenClosed(site.corner(), end)) {
            if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.hasChunkAt(pos) || !level.mayInteract(owner, pos) || !level.isEmptyBlock(pos)) {
                return false;
            }
        }
        int floor = site.corner().getY();
        for (var cell : plan.cells()) {
            if (cell.pos().getY() == floor) {
                BlockPos support = cell.pos().below();
                var below = level.getBlockState(support);
                if (!below.getFluidState().isEmpty() || !below.isFaceSturdy(level, support, Direction.UP, SupportType.FULL)) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean place(ServerLevel level, Player owner, WoodHouseBlueprint plan) {
        if (level.captureBlockSnapshots || level.restoringBlockSnapshots || !level.capturedBlockSnapshots.isEmpty()
                || !clear(level, owner, plan)) {
            return false;
        }
        boolean approved = false;
        var snapshots = new ArrayList<BlockSnapshot>(plan.cells().size());
        try {
            boolean placed = capture(level, plan, snapshots);
            approved = placed && !snapshots.isEmpty() && !ForgeEventFactory.onMultiBlockPlace(owner, List.copyOf(snapshots), Direction.UP);
        } finally {
            if (!approved) {
                restore(level, snapshots);
            }
        }
        if (approved) {
            for (var snapshot : snapshots) {
                BlockPos pos = snapshot.getPos();
                level.markAndNotifyBlock(pos, null, snapshot.getReplacedBlock(), level.getBlockState(pos), Block.UPDATE_ALL, 128);
            }
        }
        return approved;
    }

    private static boolean capture(ServerLevel level, WoodHouseBlueprint plan, List<BlockSnapshot> snapshots) {
        level.captureBlockSnapshots = true;
        try {
            for (var cell : plan.cells()) {
                if (!level.setBlock(cell.pos(), cell.state(), Block.UPDATE_ALL)) {
                    return false;
                }
            }
            return true;
        } finally {
            level.captureBlockSnapshots = false;
            snapshots.addAll(level.capturedBlockSnapshots);
            level.capturedBlockSnapshots.clear();
        }
    }

    private static void restore(ServerLevel level, List<BlockSnapshot> snapshots) {
        level.restoringBlockSnapshots = true;
        try {
            for (int i = snapshots.size() - 1; i >= 0; i--) {
                snapshots.get(i).restore(true, false);
            }
        } finally {
            level.restoringBlockSnapshots = false;
        }
    }
}
