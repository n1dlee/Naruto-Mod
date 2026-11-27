package com.sekwah.narutomod.block;

import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.DoryuhekiLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.phys.AABB;

public class DoryuhekiBlockEntity extends BlockEntity {
    private long rises;
    private long retracts;

    public DoryuhekiBlockEntity(BlockPos pos, BlockState state) {
        super(NarutoBlocks.DORYUHEKI_ENTITY.get(), pos, state);
    }

    public void plan(long rises, long retracts) {
        this.rises = rises;
        this.retracts = retracts;
        this.setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DoryuhekiBlockEntity wall) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        long now = level.getGameTime();
        int height = state.getValue(DoryuhekiBlock.STAGE);
        if (wall.retracts <= wall.rises || (now >= wall.retracts && height == 0)) {
            level.removeBlock(pos, false);
            return;
        }
        if (now < wall.rises) {
            return;
        }
        int requested = DoryuhekiLayout.height(now, wall.rises, wall.retracts);
        if (now >= wall.retracts) {
            requested = Math.min(requested, height);
        }
        if (requested == height) {
            if (height == DoryuhekiLayout.SOLID && !supported(level, pos)) {
                level.removeBlock(pos, false);
            }
            return;
        }
        if (requested > height && !canRaise(server, pos, requested)) {
            if (now >= wall.rises + 12) {
                if (height == 0 || !supported(level, pos)) {
                    level.removeBlock(pos, false);
                } else {
                    wall.plan(wall.rises, now);
                }
            }
            return;
        }
        if (requested == 0) {
            level.removeBlock(pos, false);
        } else {
            level.setBlock(pos, state.setValue(DoryuhekiBlock.STAGE, requested), Block.UPDATE_ALL);
        }
        if (height == 0 || height == DoryuhekiLayout.SOLID || requested == DoryuhekiLayout.SOLID || requested == 0) {
            gravel(server, pos, Math.max(height, requested), requested > height);
        }
    }

    public static boolean supported(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState state = level.getBlockState(below);
        if (state.is(NarutoBlocks.DORYUHEKI.get())) {
            return state.getValue(DoryuhekiBlock.STAGE) == DoryuhekiLayout.SOLID;
        }
        return state.getFluidState().isEmpty() && state.isFaceSturdy(level, below, Direction.UP, SupportType.FULL);
    }

    private static boolean canRaise(ServerLevel level, BlockPos pos, int height) {
        AABB body = DoryuhekiLayout.bounds(pos, height).deflate(0.0001);
        return supported(level, pos)
                && level.getEntitiesOfClass(LivingEntity.class, body, entity -> entity.isAlive() && !entity.isSpectator()).isEmpty();
    }

    private static void gravel(ServerLevel level, BlockPos pos, int height, boolean rising) {
        var debris = new BlockParticleOption(ParticleTypes.BLOCK, rising ? Blocks.TUFF.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState());
        level.sendParticles(debris, pos.getX() + 0.5, pos.getY() + height / 4.0, pos.getZ() + 0.5,
                rising ? 7 : 5, 0.39, 0.08, 0.39, 0.03);
        if (rising && height == 1 && !level.getBlockState(pos.below()).is(NarutoBlocks.DORYUHEKI.get())) {
            level.playSound(null, pos, NarutoSounds.DORYUHEKI_RISE.get(), SoundSource.BLOCKS, 0.7f, 0.92f);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Rises", this.rises);
        tag.putLong("Retracts", this.retracts);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.rises = tag.getLong("Rises");
        this.retracts = tag.getLong("Retracts");
    }
}
