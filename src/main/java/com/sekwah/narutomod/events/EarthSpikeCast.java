package com.sekwah.narutomod.events;

import com.sekwah.narutomod.block.EarthSpikeBlock;
import com.sekwah.narutomod.block.EarthSpikeBlockEntity;
import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.util.EarthSpikeShape;
import com.sekwah.narutomod.util.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.UUID;

public final class EarthSpikeCast {
    private static final double GOLDEN_ANGLE = Math.PI * (3 - Math.sqrt(5));

    private EarthSpikeCast() {
    }

    public static boolean disc(Player caster, double radius, int count, float power) {
        if (!(caster.level() instanceof ServerLevel level) || !caster.onGround() || caster.isSpectator()) {
            return false;
        }
        Field field = new Field(level, caster, level.getGameTime(), UUID.randomUUID(), power);
        var roots = new LinkedHashSet<BlockPos>();
        var targets = level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(radius, 3, radius),
                target -> target != caster && target.isAlive() && !target.isSpectator()
                        && !Faction.sameSide(caster, target) && target.distanceToSqr(caster) <= radius * radius);
        targets.sort(Comparator.comparingDouble(caster::distanceToSqr));
        for (LivingEntity target : targets) {
            if (roots.size() >= count) {
                break;
            }
            addRoot(field, roots, target.getX(), target.getZ(), radius);
        }
        for (int i = 0; i < count && roots.size() < count; i++) {
            double spread = Math.max(1.5, radius * Math.sqrt((i + 0.5) / count));
            addRoot(field, roots, caster.getX() + Math.cos(i * GOLDEN_ANGLE) * spread,
                    caster.getZ() + Math.sin(i * GOLDEN_ANGLE) * spread, radius);
        }
        boolean built = false;
        for (BlockPos root : roots) {
            built |= field.column(root);
        }
        return built;
    }

    public static boolean line(LivingEntity owner, Vec3 direction, int count, float power) {
        if (!(owner.level() instanceof ServerLevel level) || !ForgeEventFactory.getMobGriefingEvent(level, owner)) {
            return false;
        }
        Field field = new Field(level, owner, level.getGameTime(), UUID.randomUUID(), power);
        boolean built = false;
        for (int i = 1; i <= count; i++) {
            Vec3 loc = owner.position().add(direction.scale(i * 2.0));
            BlockPos root = field.ground(loc.x, loc.z);
            if (root != null) {
                built |= field.column(root);
            }
        }
        return built;
    }

    private static void addRoot(Field field, LinkedHashSet<BlockPos> roots, double x, double z, double radius) {
        BlockPos root = field.ground(x, z);
        if (root != null) {
            Vec3 center = Vec3.atBottomCenterOf(root);
            double horizontal = center.subtract(field.owner().position()).horizontalDistanceSqr();
            if (horizontal > 1.4 * 1.4 && horizontal <= radius * radius) {
                roots.add(root);
            }
        }
    }

    private record Field(ServerLevel level, LivingEntity owner, long time, UUID id, float power) {
        @Nullable
        private BlockPos ground(double x, double z) {
            BlockPos probe = BlockPos.containing(x, this.owner.getY() + 1.0, z);
            if (!this.level.hasChunkAt(probe) || !this.level.getWorldBorder().isWithinBounds(probe)) {
                return null;
            }
            for (int step = 0; step <= 4; step++) {
                BlockPos pos = probe.below(step);
                var below = this.level.getBlockState(pos.below());
                if (this.level.isEmptyBlock(pos) && below.getFluidState().isEmpty()
                        && !below.is(NarutoBlocks.EARTH_SPIKE.get()) && !below.is(NarutoBlocks.DORYUHEKI.get())
                        && below.isFaceSturdy(this.level, pos.below(), Direction.UP, SupportType.CENTER)) {
                    return pos;
                }
            }
            return null;
        }

        private boolean column(BlockPos root) {
            double distance = Vec3.atBottomCenterOf(root).subtract(this.owner.position()).horizontalDistance();
            for (int row = 0; row < EarthSpikeShape.SEGMENTS; row++) {
                if (!this.reserve(root.above(row), row, distance)) {
                    return row > 0;
                }
            }
            return true;
        }

        private boolean reserve(BlockPos pos, int row, double distance) {
            Player builder = this.owner instanceof Player player ? player
                    : this.owner instanceof ShadowCloneEntity clone ? clone.playerOwner() : null;
            if (this.level.isOutsideBuildHeight(pos) || !this.level.isEmptyBlock(pos)
                    || (builder != null && !this.level.mayInteract(builder, pos))) {
                return false;
            }
            var snapshot = BlockSnapshot.create(this.level.dimension(), this.level, pos);
            var state = NarutoBlocks.EARTH_SPIKE.get().defaultBlockState().setValue(EarthSpikeBlock.ROW, row);
            if (!this.level.setBlock(pos, state, Block.UPDATE_CLIENTS)) {
                return false;
            }
            if (ForgeEventFactory.onBlockPlace(builder == null ? this.owner : builder, snapshot, Direction.UP)
                    || !(this.level.getBlockEntity(pos) instanceof EarthSpikeBlockEntity spike)) {
                if (this.level.getBlockState(pos).is(NarutoBlocks.EARTH_SPIKE.get())) {
                    snapshot.restore(true, false);
                }
                return false;
            }
            long rise = EarthSpikeShape.riseAt(this.time, distance, row);
            spike.bind(new EarthSpikeBlockEntity.Lease(rise, EarthSpikeShape.endAt(rise, row),
                    this.owner.getUUID(), this.id, this.power));
            return true;
        }
    }
}
