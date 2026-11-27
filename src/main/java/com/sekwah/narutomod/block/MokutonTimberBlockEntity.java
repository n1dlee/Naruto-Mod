package com.sekwah.narutomod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

public final class MokutonTimberBlockEntity extends BlockEntity {
    private @Nullable UUID cast;
    private @Nullable BlockPos support;
    private boolean ground;
    private long rise;
    private long expires;

    public MokutonTimberBlockEntity(BlockPos pos, BlockState state) {
        super(NarutoBlocks.MOKUTON_TIMBER_ENTITY.get(), pos, state);
    }

    public void plan(Lease lease) {
        this.cast = lease.id();
        this.support = lease.support().immutable();
        this.ground = lease.ground();
        this.rise = lease.rise();
        this.expires = lease.expires();
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public long rise() { return this.rise; }

    public boolean belongs(UUID cast) { return cast.equals(this.cast); }

    public static boolean anchored(ServerLevel level, BlockPos support, boolean ground, UUID cast) {
        if (!level.hasChunkAt(support)) {
            return false;
        }
        if (!ground) {
            return level.getBlockEntity(support) instanceof MokutonTimberBlockEntity timber && timber.belongs(cast);
        }
        BlockState state = level.getBlockState(support);
        return !state.is(NarutoBlocks.MOKUTON_TIMBER.get()) && state.getFluidState().isEmpty()
                && state.isFaceSturdy(level, support, Direction.UP, SupportType.CENTER);
    }

    static void tick(Level level, BlockPos pos, BlockState state, MokutonTimberBlockEntity timber) {
        if (!(level instanceof ServerLevel server) || timber.isRemoved() || level.getBlockEntity(pos) != timber) {
            return;
        }
        long now = server.getGameTime();
        boolean overdue = timber.cast == null || timber.support == null || now >= timber.expires;
        boolean unsupported = !overdue && ((now + pos.asLong()) & 3) == 0
                && server.hasChunkAt(timber.support) && !anchored(server, timber.support, timber.ground, timber.cast);
        if (overdue || unsupported) {
            if (timber.cast != null && (pos.asLong() & 1) == 0) {
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState()),
                        pos.getX() + 0.5, pos.getY() + 0.33, pos.getZ() + 0.5, 4, 0.18, 0.17, 0.18, 0.065);
            }
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.cast != null && this.support != null) {
            tag.putUUID("Cast", this.cast);
            tag.putLong("Support", this.support.asLong());
            tag.putBoolean("Ground", this.ground);
        }
        tag.putLong("Rise", this.rise);
        tag.putLong("Expires", this.expires);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.cast = tag.hasUUID("Cast") ? tag.getUUID("Cast") : null;
        this.support = tag.contains("Support") ? BlockPos.of(tag.getLong("Support")) : null;
        this.ground = tag.getBoolean("Ground");
        this.rise = tag.getLong("Rise");
        this.expires = tag.getLong("Expires");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Rise", this.rise);
        return tag;
    }

    public record Lease(UUID id, BlockPos support, boolean ground, long rise, long expires) {
    }
}
