package com.sekwah.narutomod.events;

import com.sekwah.narutomod.block.IceSpikeBlock;
import com.sekwah.narutomod.block.IceSpikeBlockEntity;
import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.ElementalVfx;
import com.sekwah.narutomod.util.IceSpikeFormation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.UUID;

public final class IceSpikeCast {
    private IceSpikeCast() {
    }

    public static boolean start(Player owner, int mastery, float power) {
        if (!(owner.level() instanceof ServerLevel level) || !owner.onGround() || !owner.isAlive()
                || owner.isSpectator() || owner.noPhysics) {
            return false;
        }
        var field = new Field(level, UUID.randomUUID(), level.getGameTime(), power);
        int accepted = 0;
        for (BlockPos candidate : IceSpikeFormation.roots(owner.position(), owner.getYRot(), mastery)) {
            BlockPos root = findRoot(owner, field, candidate);
            if (root != null && column(owner, field, root)) {
                accepted++;
            }
        }
        if (accepted == 0) {
            return false;
        }
        ElementalVfx.frostDendrite(level, owner.position(), 2.15);
        level.playSound(null, owner.blockPosition(), NarutoSounds.ICE_FIELD_WARN.get(), SoundSource.BLOCKS, 0.82f, 1.0f);
        return true;
    }

    private static @Nullable BlockPos findRoot(Player owner, Field field, BlockPos candidate) {
        if (!field.level().hasChunkAt(candidate)) {
            return null;
        }
        for (int y : new int[]{0, 1, -1}) {
            BlockPos at = candidate.above(y);
            Vec3 stop = Vec3.atBottomCenterOf(at).add(0, 0.28, 0);
            if (empty(field.level(), owner, at) && IceSpikeBlockEntity.supported(field.level(), at, 0, field.id())
                    && field.level().clip(new ClipContext(owner.getEyePosition(), stop,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner)).getType() == HitResult.Type.MISS) {
                return at;
            }
        }
        return null;
    }

    private static boolean column(Player owner, Field field, BlockPos root) {
        ServerLevel level = field.level();
        long warning = field.start() + IceSpikeFormation.delay(owner.position(), root);
        int limit = warning - field.start() > 13 ? 3 : 4;
        int height = 0;
        while (height < limit && empty(level, owner, root.above(height))) {
            height++;
        }
        if (height == 0) {
            return false;
        }
        var shapes = IceSpikeFormation.layers(height);
        BlockState dormant = NarutoBlocks.ICE_SPIKE.get().defaultBlockState().setValue(IceSpikeBlock.MANAGED, true)
                .setValue(IceSpikeBlock.RAISED, false);
        for (int row = 0; row < height; row++) {
            var pos = root.above(row);
            var lease = new IceSpikeBlockEntity.Lease(warning + row * 2L, warning + IceSpikeFormation.LIFESPAN,
                    row, field.power(), owner.getUUID(), field.id());
            if (!place(level, owner, pos, dormant.setValue(IceSpikeBlock.THICKNESS, shapes.get(row)), lease)) {
                withdraw(level, root, row, field.id());
                return false;
            }
        }
        return true;
    }

    private static boolean place(ServerLevel level, Player owner, BlockPos pos, BlockState state, IceSpikeBlockEntity.Lease lease) {
        if (!empty(level, owner, pos)) {
            return false;
        }
        var snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (!level.setBlock(pos, state, Block.UPDATE_ALL)) {
            return false;
        }
        var inserted = level.getBlockEntity(pos);
        if (ForgeEventFactory.onBlockPlace(owner, snapshot, Direction.UP) || !(inserted instanceof IceSpikeBlockEntity spike)) {
            if (level.getBlockEntity(pos) == inserted && level.getBlockState(pos) == state) {
                snapshot.restore(true, false);
            }
            return false;
        }
        if (level.getBlockEntity(pos) != spike) {
            return false;
        }
        spike.bind(lease);
        return true;
    }

    private static void withdraw(ServerLevel level, BlockPos root, int placed, UUID id) {
        for (int row = placed - 1; row >= 0; row--) {
            BlockPos pos = root.above(row);
            if (level.getBlockEntity(pos) instanceof IceSpikeBlockEntity spike && spike.belongs(id)) {
                level.removeBlock(pos, false);
            }
        }
    }

    private static boolean empty(ServerLevel level, Player owner, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
                && level.mayInteract(owner, pos) && level.isEmptyBlock(pos);
    }

    private record Field(ServerLevel level, UUID id, long start, float power) {
    }
}
