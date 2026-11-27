package com.sekwah.narutomod.block;

import com.sekwah.narutomod.util.EarthSpikeShape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.stream.IntStream;

public class EarthSpikeBlock extends BaseEntityBlock {
    public static final BooleanProperty RAISED = BooleanProperty.create("raised");
    public static final BooleanProperty CRACK = BooleanProperty.create("crack");
    public static final IntegerProperty ROW = IntegerProperty.create("row", 0, EarthSpikeShape.SEGMENTS - 1);
    private static final VoxelShape[] SHAPES = IntStream.range(0, EarthSpikeShape.SEGMENTS)
            .mapToObj(row -> Shapes.or(Shapes.create(EarthSpikeShape.slab(BlockPos.ZERO, row, false)),
                    Shapes.create(EarthSpikeShape.slab(BlockPos.ZERO, row, true))))
            .toArray(VoxelShape[]::new);

    public EarthSpikeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ROW, 0).setValue(RAISED, false).setValue(CRACK, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROW, RAISED, CRACK);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(RAISED) || state.getValue(CRACK) ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(RAISED) ? SHAPES[state.getValue(ROW)] : Shapes.empty();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EarthSpikeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, NarutoBlocks.EARTH_SPIKE_ENTITY.get(), EarthSpikeBlockEntity::tick);
    }
}
