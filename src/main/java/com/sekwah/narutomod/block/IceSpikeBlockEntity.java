package com.sekwah.narutomod.block;

import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.IceSpikeFormation;
import com.sekwah.narutomod.util.JutsuAim;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class IceSpikeBlockEntity extends BlockEntity {
    private long rise;
    private long end;
    private long appeared;
    private int row;
    private float power;
    private @Nullable UUID owner;
    private @Nullable UUID cast;
    private boolean attempted;

    public IceSpikeBlockEntity(BlockPos pos, BlockState state) {
        super(NarutoBlocks.ICE_SPIKE_ENTITY.get(), pos, state);
    }

    public void bind(Lease lease) {
        this.rise = lease.rise();
        this.end = lease.end();
        this.row = lease.row();
        this.power = lease.power();
        this.owner = lease.owner();
        this.cast = lease.cast();
        sync();
    }

    public boolean belongs(UUID id) { return id.equals(this.cast); }

    public long rise() { return this.rise; }

    public long end() { return this.end; }

    public long appeared() { return this.appeared; }

    public int row() { return this.row; }

    static void tick(Level level, BlockPos pos, BlockState state, IceSpikeBlockEntity spike) {
        if (level instanceof ServerLevel server && !spike.isRemoved() && server.getBlockEntity(pos) == spike) {
            spike.advance(server, state);
        }
    }

    private void advance(ServerLevel level, BlockState state) {
        long now = level.getGameTime();
        boolean raised = state.getValue(IceSpikeBlock.RAISED);
        if (this.cast == null || now >= this.end) {
            melt(level, raised);
        } else if (now >= this.rise && (!raised || ((now + this.worldPosition.asLong()) & 3) == 0)) {
            if (supported(level, this.worldPosition, this.row, this.cast)) {
                if (!raised) {
                    erect(level, state, now);
                }
            } else if (now >= this.rise + IceSpikeFormation.WAIT) {
                melt(level, raised);
            }
        }
    }

    public static boolean supported(ServerLevel level, BlockPos pos, int row, UUID cast) {
        var below = pos.below();
        BlockState floor = level.getBlockState(below);
        if (row == 0) {
            return !floor.is(NarutoBlocks.ICE_SPIKE.get()) && floor.getFluidState().isEmpty()
                    && floor.isFaceSturdy(level, below, Direction.UP, SupportType.CENTER);
        }
        return floor.is(NarutoBlocks.ICE_SPIKE.get()) && floor.getValue(IceSpikeBlock.MANAGED)
                && floor.getValue(IceSpikeBlock.RAISED) && level.getBlockEntity(below) instanceof IceSpikeBlockEntity support
                && support.belongs(cast);
    }

    private void erect(ServerLevel level, BlockState state, long now) {
        var shape = IceSpikeBlock.shape(state.getValue(IceSpikeBlock.THICKNESS)).bounds().move(this.worldPosition);
        var inside = level.getEntitiesOfClass(LivingEntity.class, shape, victim -> victim.isAlive() && !victim.isSpectator() && !victim.noPhysics);
        if (inside.isEmpty()) {
            this.appeared = now;
            level.setBlock(this.worldPosition, state.setValue(IceSpikeBlock.RAISED, true), Block.UPDATE_ALL);
            sync();
            flakes(level, true);
        } else {
            if (!this.attempted && this.row == 0) {
                this.attempted = true;
                this.setChanged();
                chill(level, inside);
            }
            if (now >= this.rise + IceSpikeFormation.WAIT) {
                melt(level, false);
            }
        }
    }

    private void chill(ServerLevel level, List<LivingEntity> targets) {
        if (this.owner == null) {
            return;
        }
        var caster = level.getPlayerByUUID(this.owner);
        if (caster == null || !caster.isAlive()) {
            return;
        }
        for (LivingEntity target : targets) {
            if (!JutsuAim.attackable(caster, target) || !level.mayInteract(caster, target.blockPosition())
                    || !target.hurt(caster.damageSources().playerAttack(caster), this.power)) {
                continue;
            }
            target.setTicksFrozen(IceSpikeFormation.chill(target.getTicksFrozen(), target.getTicksRequiredToFreeze(), target.canFreeze()));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 26, 0, false, true));
        }
    }

    private void melt(ServerLevel level, boolean raised) {
        if (raised) {
            flakes(level, false);
        }
        level.removeBlock(this.worldPosition, false);
    }

    private void flakes(ServerLevel level, boolean upward) {
        BlockPos pos = this.worldPosition;
        var debris = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState());
        level.sendParticles(debris, pos.getX() + 0.5, pos.getY() + 0.33, pos.getZ() + 0.5,
                this.row == 0 ? 5 : 2, 0.15, 0.12, 0.15, upward ? 0.054 : 0.022);
        if (this.row == 0 && Math.floorMod(pos.getX() * 11 + pos.getZ(), 4) == 0) {
            var tone = upward ? NarutoSounds.ICE_FIELD_RISE.get() : NarutoSounds.ICE_FIELD_MELT.get();
            level.playSound(null, pos, tone, SoundSource.BLOCKS, 0.56f, upward ? 1.06f : 0.94f);
        }
    }

    private void sync() {
        this.setChanged();
        if (this.level != null) {
            var state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.merge(getUpdateTag());
        tag.putFloat("Power", this.power);
        tag.putBoolean("Attempted", this.attempted);
        if (this.cast != null && this.owner != null) {
            tag.putUUID("Field", this.cast);
            tag.putUUID("Source", this.owner);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.rise = tag.getLong("Rise");
        this.end = tag.getLong("End");
        this.appeared = tag.getLong("Appeared");
        this.row = tag.getInt("Row");
        this.power = tag.getFloat("Power");
        this.attempted = tag.getBoolean("Attempted");
        this.cast = tag.hasUUID("Field") ? tag.getUUID("Field") : null;
        this.owner = tag.hasUUID("Source") ? tag.getUUID("Source") : null;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public CompoundTag getUpdateTag() {
        var tag = new CompoundTag();
        tag.putLong("Rise", this.rise);
        tag.putLong("End", this.end);
        tag.putLong("Appeared", this.appeared);
        tag.putInt("Row", this.row);
        return tag;
    }

    public record Lease(long rise, long end, int row, float power, UUID owner, UUID cast) {
        public Lease {
            Objects.requireNonNull(owner);
            Objects.requireNonNull(cast);
        }
    }
}
