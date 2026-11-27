package com.sekwah.narutomod.block;

import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.util.EarthSpikeShape;
import com.sekwah.narutomod.util.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public class EarthSpikeBlockEntity extends BlockEntity {
    private long riseAt;
    private long endAt;
    private UUID source;
    private UUID field;
    private float power;
    private boolean attempted;

    public EarthSpikeBlockEntity(BlockPos pos, BlockState state) {
        super(NarutoBlocks.EARTH_SPIKE_ENTITY.get(), pos, state);
    }

    public void bind(Lease lease) {
        this.riseAt = lease.rise();
        this.endAt = lease.end();
        this.source = lease.owner();
        this.field = lease.field();
        this.power = lease.power();
        this.setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EarthSpikeBlockEntity spike) {
        if (level instanceof ServerLevel server) {
            spike.advance(server, pos, state);
        }
    }

    private void advance(ServerLevel level, BlockPos pos, BlockState state) {
        long now = level.getGameTime();
        boolean raised = state.getValue(EarthSpikeBlock.RAISED);
        int row = state.getValue(EarthSpikeBlock.ROW);
        if (this.field == null || now >= this.endAt) {
            this.withdraw(level, pos, row, raised);
            return;
        }
        if (now < this.riseAt) {
            if (row == 0 && now >= this.riseAt - 4 && !state.getValue(EarthSpikeBlock.CRACK)) {
                level.setBlock(pos, state.setValue(EarthSpikeBlock.CRACK, true), Block.UPDATE_ALL);
            }
            return;
        }
        if (raised && Math.floorMod(now + pos.getX() + pos.getZ(), 4) != 0) {
            return;
        }
        boolean supported = this.supported(level, pos, row);
        if (raised || !supported) {
            if (!supported && now >= this.riseAt + EarthSpikeShape.WAIT_LIMIT) {
                this.withdraw(level, pos, row, raised);
            }
            return;
        }
        var obstructing = this.occupants(level, pos, row);
        if (!this.attempted) {
            this.attempted = true;
            this.setChanged();
            this.strike(level, pos, obstructing);
        }
        if (obstructing.isEmpty()) {
            level.setBlock(pos, state.setValue(EarthSpikeBlock.RAISED, true).setValue(EarthSpikeBlock.CRACK, false), Block.UPDATE_ALL);
            this.grit(level, pos, row, true);
        } else if (now >= this.riseAt + EarthSpikeShape.WAIT_LIMIT) {
            this.withdraw(level, pos, row, false);
        }
    }

    private List<LivingEntity> occupants(ServerLevel level, BlockPos pos, int row) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(pos),
                entity -> entity.isAlive() && !entity.isSpectator()
                        && EarthSpikeShape.touches(pos, row, entity.getBoundingBox()));
    }

    private void strike(ServerLevel level, BlockPos pos, List<LivingEntity> targets) {
        if (this.source == null || targets.isEmpty() || !(level.getEntity(this.source) instanceof LivingEntity owner)
                || !owner.isAlive()) {
            return;
        }
        var damage = NarutoDamageTypes.getDamageSource(level, NarutoDamageTypes.EARTH_SPIKE, owner, owner, Vec3.atCenterOf(pos));
        for (LivingEntity target : targets) {
            if (!opponent(owner, target) || !target.hurt(damage, this.power)) {
                continue;
            }
            Vec3 v = target.getDeltaMovement();
            target.setDeltaMovement(v.x * 0.5, Math.max(v.y, 0.63), v.z * 0.5);
            target.hurtMarked = true;
        }
    }

    private static boolean opponent(Entity owner, LivingEntity target) {
        if (owner == target || owner.isAlliedTo(target) || Faction.sameSide(owner, target)) {
            return false;
        }
        return !(target instanceof Player player)
                || (!player.isCreative() && (!(owner instanceof Player caster) || caster.canHarmPlayer(player)));
    }

    private boolean supported(ServerLevel level, BlockPos pos, int row) {
        BlockPos below = pos.below();
        BlockState floor = level.getBlockState(below);
        if (row == 0) {
            return floor.getFluidState().isEmpty() && floor.isFaceSturdy(level, below, Direction.UP, SupportType.CENTER);
        }
        return floor.is(NarutoBlocks.EARTH_SPIKE.get()) && floor.getValue(EarthSpikeBlock.RAISED)
                && level.getBlockEntity(below) instanceof EarthSpikeBlockEntity base && this.field.equals(base.field);
    }

    private void withdraw(ServerLevel level, BlockPos pos, int row, boolean raised) {
        level.removeBlock(pos, false);
        if (raised) {
            this.grit(level, pos, row, false);
        }
    }

    private void grit(ServerLevel level, BlockPos pos, int row, boolean rising) {
        BlockState soil = level.getBlockState(pos.below(row + 1));
        var debris = new BlockParticleOption(ParticleTypes.BLOCK, soil.isAir() ? Blocks.TUFF.defaultBlockState() : soil);
        level.sendParticles(debris, pos.getX() + 0.5, pos.getY() + (rising ? 0.5 : 0.12), pos.getZ() + 0.5,
                row == 0 ? 8 : 4, 0.25, 0.12, 0.25, rising ? 0.055 : 0.02);
        if (row == 0 && rising && Math.floorMod(31 * pos.getX() + pos.getZ(), 4) == 0) {
            level.playSound(null, pos, SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.BLOCKS, 0.56f, 0.87f);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Rise", this.riseAt);
        tag.putLong("End", this.endAt);
        tag.putFloat("Power", this.power);
        tag.putBoolean("Attempted", this.attempted);
        if (this.field != null && this.source != null) {
            tag.putUUID("Field", this.field);
            tag.putUUID("Source", this.source);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.riseAt = tag.getLong("Rise");
        this.endAt = tag.getLong("End");
        this.power = tag.getFloat("Power");
        this.attempted = tag.getBoolean("Attempted");
        this.field = tag.hasUUID("Field") ? tag.getUUID("Field") : null;
        this.source = tag.hasUUID("Source") ? tag.getUUID("Source") : null;
    }

    public record Lease(long rise, long end, UUID owner, UUID field, float power) {
    }
}
