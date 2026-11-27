package com.sekwah.narutomod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The ice spear raised by Ice Release, shaped like pointed dripstone and coloured for ice.
 *
 * It is not {@link net.minecraft.world.level.block.PointedDripstoneBlock}. Subclassing that
 * would drag in everything the vanilla block does on its own initiative - growing downward,
 * dripping, falling when unsupported, breaking when the block above changes - all of which
 * fights a spike that is supposed to stand for eight seconds and then be removed on cue.
 * What is borrowed is only the silhouette: the same four thickness stages, so the spear
 * tapers from a wide base to an actual point instead of being a stack of cubes.
 */
public class IceSpikeBlock extends BaseEntityBlock {

    public static final EnumProperty<DripstoneThickness> THICKNESS =
            EnumProperty.create("thickness", DripstoneThickness.class);
    public static final BooleanProperty MANAGED = BooleanProperty.create("managed");
    public static final BooleanProperty RAISED = BooleanProperty.create("raised");

    /** Narrowing collision boxes, one per stage, so the taper is solid as well as visible. */
    private static final VoxelShape SHAPE_BASE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
    private static final VoxelShape SHAPE_FRUSTUM = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    private static final VoxelShape SHAPE_MIDDLE = Block.box(5.0, 0.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape SHAPE_TIP = Block.box(6.0, 0.0, 6.0, 10.0, 11.0, 10.0);

    public IceSpikeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(THICKNESS, DripstoneThickness.TIP).setValue(MANAGED, false).setValue(RAISED, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(THICKNESS, MANAGED, RAISED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return isDormant(state) ? Shapes.empty() : shape(state.getValue(THICKNESS));
    }

    public static VoxelShape shape(DripstoneThickness thickness) {
        return switch (thickness) {
            case BASE -> SHAPE_BASE;
            case FRUSTUM -> SHAPE_FRUSTUM;
            case MIDDLE -> SHAPE_MIDDLE;
            default -> SHAPE_TIP;
        };
    }

    public static boolean isDormant(BlockState state) { return state.getValue(MANAGED) && !state.getValue(RAISED); }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(MANAGED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(MANAGED) ? new IceSpikeBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || !state.getValue(MANAGED) ? null
                : createTickerHelper(type, NarutoBlocks.ICE_SPIKE_ENTITY.get(), IceSpikeBlockEntity::tick);
    }

    /**
     * Never culls a neighbour's face. The spike is narrower than a full block at every stage,
     * so letting it hide the faces behind it would punch holes in the world around it.
     */
    @Override
    public boolean skipRendering(BlockState state, BlockState neighbour, Direction direction) {
        return false;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }
}
