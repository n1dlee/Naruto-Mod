package com.sekwah.narutomod.entity.jutsuprojectile;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class EarthWallEntity extends Entity {

    private static final int WALL_LIFESPAN = 300; // 15 seconds

    private int ticksAlive = 0;
    private final List<BlockPos> placedBlocks = new ArrayList<>();
    // Track the block type so we only remove blocks we placed
    private Block placedBlock = null;

    public EarthWallEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setInvisible(true);
    }

    @Override
    protected void defineSynchedData() {
        // No synced data needed — this entity is invisible and server-only logic
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (ticksAlive++ >= WALL_LIFESPAN) {
                removeWallBlocks();
                discard();
            }
        }
    }

    public void placeWall(List<BlockPos> positions, Block block) {
        this.placedBlock = block;
        placeRow(positions, block);
    }

    public void placeRow(List<BlockPos> positions, Block block) {
        if (this.placedBlock == null) this.placedBlock = block;
        BlockState state = block.defaultBlockState();
        for (BlockPos pos : positions) {
            if (level().getBlockState(pos).isAir()) {
                level().setBlockAndUpdate(pos, state);
                placedBlocks.add(pos.immutable());
            }
        }
    }

    private void removeWallBlocks() {
        if (placedBlock == null) return;
        for (BlockPos pos : placedBlocks) {
            if (level().getBlockState(pos).getBlock() == placedBlock) {
                level().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ticksAlive = tag.getInt("TicksAlive");
        placedBlocks.clear();
        ListTag blockList = tag.getList("PlacedBlocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blockList.size(); i++) {
            CompoundTag blockTag = blockList.getCompound(i);
            placedBlocks.add(new BlockPos(blockTag.getInt("X"), blockTag.getInt("Y"), blockTag.getInt("Z")));
        }
        if (tag.contains("PlacedBlock")) {
            placedBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .get(new net.minecraft.resources.ResourceLocation(tag.getString("PlacedBlock")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("TicksAlive", ticksAlive);
        ListTag blockList = new ListTag();
        for (BlockPos pos : placedBlocks) {
            CompoundTag blockTag = new CompoundTag();
            blockTag.putInt("X", pos.getX());
            blockTag.putInt("Y", pos.getY());
            blockTag.putInt("Z", pos.getZ());
            blockList.add(blockTag);
        }
        tag.put("PlacedBlocks", blockList);
        if (placedBlock != null) {
            net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(placedBlock);
            var blockKey = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(placedBlock);
            if (blockKey != null) {
                tag.putString("PlacedBlock", blockKey.toString());
            }
        }
    }
}
