package com.sekwah.narutomod.block;

import com.sekwah.narutomod.util.AmaterasuFlames;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Amaterasu's black flame as a world block, so the technique actually spreads instead of
 * burning at the single point it was aimed at.
 *
 * The 1.12.2 mod got this by subclassing vanilla BlockFire and inheriting its whole
 * flammability table. That gives genuine fire behaviour but also its worst property: an
 * unbreakable fire that eats terrain and never stops. Two deliberate changes here:
 *
 *  - Spread is generational, not probabilistic. AGE doubles as the wavefront counter: a
 *    flame only seeds neighbours while its age is below SPREAD_AGE_LIMIT, and each child
 *    starts older than its parent. The burn therefore has a hard, predictable radius.
 *  - It does not consume blocks. Canon says Amaterasu burns through anything, but an
 *    indestructible self-spreading fire that also deletes terrain would take a survival
 *    world apart. It spreads through open air and burns what is standing in it.
 *
 * Unbreakable while it lasts, like the real thing, but it does burn out - that is what
 * keeps it from being a permanent scar on the map.
 */
public class AmaterasuFlameBlock extends BaseFireBlock {

    public static final IntegerProperty AGE = BlockStateProperties.AGE_15;

    /** Ticks between age steps. 15 steps at this rate is roughly eleven seconds alight. */
    private static final int TICK_RATE = 15;
    /** Past this age a flame stops seeding new ones, capping the burn radius. */
    private static final int SPREAD_AGE_LIMIT = 6;
    private static final int MAX_AGE = 15;

    public AmaterasuFlameBlock(Properties properties) {
        super(properties, 2.0F);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(AGE);
    }

    /**
     * Nothing is "flammable" to it in the vanilla sense, because it does not consume blocks
     * at all - it spreads through open air under its own rules in {@link #spread}.
     */
    @Override
    protected boolean canBurn(BlockState state) {
        return false;
    }

    /**
     * Clings to anything, including thin air. Real fire needs a surface; Amaterasu is
     * chakra-fed and hangs wherever it was cast.
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        level.scheduleTick(pos, this, TICK_RATE);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);

        if (age < SPREAD_AGE_LIMIT) {
            spread(level, pos, age, random);
        }
        if (age >= MAX_AGE) {
            level.removeBlock(pos, false);
            return;
        }
        level.setBlock(pos, state.setValue(AGE, age + 1), 4);
        level.scheduleTick(pos, this, TICK_RATE);
    }

    /**
     * Seeds the six neighbours. Each child is two steps older, so the wave slows and dies
     * out on its own rather than needing a separate radius check.
     */
    private void spread(ServerLevel level, BlockPos pos, int age, RandomSource random) {
        for (Direction direction : Direction.values()) {
            if (random.nextInt(3) == 0) {
                continue; // ragged edge, so the burn does not look like a cube
            }
            BlockPos neighbour = pos.relative(direction);
            if (!level.getBlockState(neighbour).is(Blocks.AIR)) {
                continue;
            }
            level.setBlock(neighbour, this.defaultBlockState().setValue(AGE, Math.min(age + 2, MAX_AGE)), 3);
        }
    }

    /**
     * Standing in it sets the black flame on you, which is the part that actually kills -
     * see AmaterasuFlames, where the burn continues after you have run out of the fire and
     * ignores being doused.
     */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living) || entity.fireImmune()) {
            return;
        }
        AmaterasuFlames.ignite(living, AmaterasuFlames.DEFAULT_DURATION, 0);
    }
}
