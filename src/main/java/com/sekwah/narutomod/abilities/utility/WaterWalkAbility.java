package com.sekwah.narutomod.abilities.utility;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class WaterWalkAbility extends Ability implements Ability.Toggled {

    /** Exempt from the free-hands gate: this is a movement mode, not a hand-cast technique. */
    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 3;
    }
    private final int CHARKA_COOLDOWN = 15;
    private static final float WALL_WALK_COST = 0.18F;
    private static final float WALL_WALK_STAMINA_COST = 0.3F;
    private static final double WALL_WALK_RUN_SPEED = 0.21D;
    private static final double WALL_WALK_SNEAK_SPEED = 0.08D;
    private static final double WALL_WALK_DESCEND_MULTIPLIER = 0.75D;
    private static final double WALL_GRIP_PUSH = 0.035D;
    private static final DustParticleOptions WALL_WALK_PARTICLE = new DustParticleOptions(new Vector3f(0.3F, 0.8F, 1.0F), 0.85F);

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        WaterChecks checks = this.checkSteadyNormalFastPush(player, ninjaData);

        if(player.getVehicle() != null) {
            player.displayClientMessage(Component.translatable("jutsu.fail.riding", Component.translatable("jutsu.waterwalk").withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }

        float fullCost = 0;
        float staminaCost = 0;
        if (checks.pushUpFast) {
            fullCost += 1f;
        } else if (checks.steadyCheck) {
            fullCost += 0.12F;
        } else if (checks.wallCheck) {
            fullCost += WALL_WALK_COST;
            staminaCost += WALL_WALK_STAMINA_COST;
        }
        if(ninjaData.getChakra() < fullCost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra", Component.translatable("jutsu.waterwalk").withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if(ninjaData.getStamina() < staminaCost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughstamina", Component.translatable("jutsu.waterwalk").withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(fullCost, CHARKA_COOLDOWN);
        if (staminaCost > 0) {
            ninjaData.useStamina(WALL_WALK_STAMINA_COST, CHARKA_COOLDOWN);
        }
        return true;
    }


    public SoundEvent castingSound() {
        return null;
    }

    public record WaterChecks(boolean steadyCheck, boolean pushUpFast, boolean pushUpNormal, boolean wallCheck, Direction wallDirection) {}

    public WaterChecks checkSteadyNormalFastPush(Player player, INinjaData ninjaData) {


        int blockX = (int)Math.floor(player.getX());
        int blockZ = (int)Math.floor(player.getZ());
        final int block1 = (int) Math.round(player.getY() - 0.56f);
        boolean steadyCheck = triggerWaterWalk(player.level(), new BlockPos(blockX, block1, blockZ));

        int block2 = (int) Math.round(player.getY());
        int beforeBlock2 = (int) Math.round(player.yo);
        boolean pushUpFast = triggerWaterWalk(player.level(), new BlockPos(blockX, block2, blockZ));
        if(player.level().isClientSide() && player.yo > player.getY()) {
            boolean beforeYCheck = triggerWaterWalk(player.level(), new BlockPos(blockX, beforeBlock2, blockZ));
            if(!beforeYCheck && steadyCheck && player.yo - player.getY() < 0.9f) {
                Vec3 vec = player.getDeltaMovement();
                player.lerpMotion(vec.x(), 0, vec.z());
                player.setPos(player.getX(), block2 + 0.05f, player.getZ());
            } else {
                steadyCheck = false;
            }
        }

        int block3 = (int) Math.round(player.getY() - 0.47f);
        boolean pushUpNormal = triggerWaterWalk(player.level(), new BlockPos(blockX, block3, blockZ));
        Direction wallDirection = findWallWalkDirection(player, ninjaData);
        boolean wallCheck = wallDirection != null;

        return new WaterChecks(steadyCheck, pushUpFast, pushUpNormal, wallCheck, wallDirection);
    }

    private void updatePlayerMovement(Player player, INinjaData ninjaData) {

        // TODO rewrite as this is the old way of doing it ported over
        // TODO also check if the block is waterlogged and non solid
        WaterChecks checks = this.checkSteadyNormalFastPush(player, ninjaData);


        // TODO sort offset
        Vec3 vec = player.getDeltaMovement();
        double resultingYSpeed = vec.y();
        if (checks.pushUpFast) {
            resultingYSpeed += 0.2D;
            if (resultingYSpeed > 0.6D) {
                resultingYSpeed = 0.6D;
            }
        }
        else if (checks.pushUpNormal) {
            resultingYSpeed += 0.1D;
            if (resultingYSpeed > 0.2D) {
                resultingYSpeed = 0.2D;
            }
        } else if (checks.steadyCheck && resultingYSpeed < 0.0D) {
            resultingYSpeed = 0.0D;
            player.resetFallDistance();
            player.setOnGround(true);
            ninjaData.getDoubleJumpData().canDoubleJumpServer = true;
            if(player.isFallFlying()) {
                player.stopFallFlying();
            }
            // This adds the hand bobbing back to the player
            float f = (float)Math.min(0.1D, player.getDeltaMovement().horizontalDistance());
            player.bob += (f - player.bob) * 0.4F;
        } else if (checks.wallCheck) {
            ninjaData.setWallWalkDirection(checks.wallDirection);
            ninjaData.setWallWalkAttached(true);
            applyWallPlaneMovement(player, ninjaData, checks.wallDirection);
            spawnWallWalkParticles(player, checks.wallDirection);
            playWallWalkStep(player, checks.wallDirection);
            return;
        } else {
            if (ninjaData.isWallWalkAttached()) {
                // Just detached — restore gravity
                player.setNoGravity(false);
            }
            ninjaData.setWallWalkAttached(false);
        }
        player.lerpMotion(vec.x(), resultingYSpeed, vec.z());

    }

    public boolean triggerWaterWalk(Level level, BlockPos blockPos) {
        FluidState fluidState = level.getFluidState(blockPos);
        BlockState blockState = level.getBlockState(blockPos);
        return (fluidState.is(Fluids.WATER) || fluidState.is(Fluids.FLOWING_WATER)) && !blockState.blocksMotion();
    }

    public boolean triggerWallWalk(Player player) {
        return findWallWalkDirection(player) != null;
    }

    public Direction findWallWalkDirection(Player player) {
        return findWallWalkDirection(player, null);
    }

    public Direction findWallWalkDirection(Player player, INinjaData ninjaData) {
        if (player.isInWaterOrBubble() || player.isFallFlying()) {
            return null;
        }
        if (ninjaData != null && ninjaData.getWallWalkDetachTicks() > 0) {
            return null;
        }

        Level level = player.level();
        BlockPos base = player.blockPosition();

        // If already attached — stay on the wall as long as the block exists, regardless of input
        if (ninjaData != null && ninjaData.isWallWalkAttached()) {
            Direction current = ninjaData.getWallWalkDirection();
            if (current != null && hasWallBlock(level, base, current)) {
                return current;
            }
            // Block disappeared (e.g. destroyed) — detach
            return null;
        }

        // Not yet attached — only trigger if moving toward a wall (not standing still on ground)
        if (player.onGround() && player.zza <= 0.0F) {
            return null;
        }
        // Whatever they are actually facing wins, so running at a specific wall in a corner
        // attaches to that one rather than to whichever of the four the enum happens to list
        // first. Only then fall back to scanning.
        Direction faced = Direction.getNearest(
                player.getLookAngle().x, player.getLookAngle().y, player.getLookAngle().z);
        if (isClingable(faced) && hasWallBlock(level, base, faced)) {
            return faced;
        }
        for (Direction direction : CLINGABLE) {
            if (hasWallBlock(level, base, direction)) {
                return direction;
            }
        }
        return player.horizontalCollision ? Direction.fromYRot(player.getYRot()) : null;
    }

    /**
     * Faces a ninja can stand on.
     *
     * The four walls and the ceiling. DOWN is left out because a surface underneath you is
     * just the floor, and clinging to it is what walking already does.
     *
     * Only the horizontals were ever considered, which is why the technique was a ladder: you
     * could go up a wall and that was the whole of it. Chakra control in the source is used to
     * stand on ceilings and under branches at least as often as it is used to climb.
     */
    private static final Direction[] CLINGABLE = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP
    };

    private static boolean isClingable(Direction direction) {
        for (Direction candidate : CLINGABLE) {
            if (candidate == direction) {
                return true;
            }
        }
        return false;
    }

    private boolean hasWallBlock(Level level, BlockPos base, Direction direction) {
        BlockPos lower = base.relative(direction);
        BlockPos upper = lower.above();
        return isWallBlock(level, lower) || isWallBlock(level, upper);
    }

    private boolean isWallBlock(Level level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        return !blockState.isAir() && blockState.blocksMotion();
    }

    private void applyWallPlaneMovement(Player player, INinjaData ninjaData, Direction wallDirection) {
        Vec3 normal = Vec3.atLowerCornerOf(wallDirection.getNormal());
        Vec3 wallMovement = planeMovement(player, normal);

        // Running into another face turns the corner onto it, rather than grinding to a halt
        // against it. This is what makes a wall and the ceiling above it one continuous
        // surface instead of two unrelated features - go up far enough and you keep going,
        // upside down, which is the thing the technique is for.
        //
        // Resolved once and only once. The first version of this called itself with the new
        // face, and in a wall-and-ceiling corner that never terminated: the wall handed off to
        // the ceiling, the ceiling looked forward, found the wall and handed straight back. It
        // crashed the game with a StackOverflowError the moment anybody tried to walk onto a
        // ceiling, which is the one thing the change existed to allow.
        Direction turn = cornerTurn(player, wallDirection, wallMovement.subtract(normal.scale(WALL_GRIP_PUSH)));
        if (turn != null) {
            wallDirection = turn;
            normal = Vec3.atLowerCornerOf(turn.getNormal());
            ninjaData.setWallWalkDirection(turn);
            wallMovement = planeMovement(player, normal);
        }

        Vec3 travel = wallMovement.subtract(normal.scale(WALL_GRIP_PUSH));

        // Landing. Only when they are genuinely on top of a floor and heading into it, rather
        // than any time a solid block existed somewhere below: the old test looked a whole
        // block down, which at the foot of any wall is the ground, so pressing back detached
        // instantly and walking DOWN a wall was impossible from the moment you set off.
        if (travel.y < -0.001D && wallDirection.getAxis().isHorizontal()
                && isWallBlock(player.level(), net.minecraft.core.BlockPos.containing(
                        player.getX(), player.getY() - 0.3D, player.getZ()))) {
            ninjaData.setWallWalkAttached(false);
            player.setNoGravity(false);
            player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            return;
        }

        // Defensive clamp — final safety net so no single tick can ever spike the player's
        // vertical velocity beyond what wall-climbing should produce, regardless of edge cases.
        double clampedY = Mth.clamp(wallMovement.y, -0.35D, 0.35D);

        // Cancel gravity — this flag is synced to client, so gravity is cancelled on both sides
        player.setNoGravity(true);

        player.resetFallDistance();
        player.setOnGround(true);
        ninjaData.getDoubleJumpData().canDoubleJumpServer = true;
        player.lerpMotion(wallMovement.x, clampedY, wallMovement.z);
        float bobTarget = (float) Math.min(0.18D,
                Math.abs(player.zza) * 0.12D + Math.abs(player.xxa) * 0.08D);
        player.bob += (bobTarget - player.bob) * 0.45F;
    }

    /** Where the player's input takes them along one surface, plus the grip into it. */
    private Vec3 planeMovement(Player player, Vec3 normal) {
        Vec3 forward = getWallPlaneForward(player, normal);
        Vec3 right = normal.cross(forward).normalize();
        double speed = player.isShiftKeyDown() ? WALL_WALK_SNEAK_SPEED : WALL_WALK_RUN_SPEED;
        if (Math.abs(player.zza) < 0.01D && Math.abs(player.xxa) < 0.01D) {
            return normal.scale(WALL_GRIP_PUSH);
        }
        return forward.scale(player.zza * speed)
                .add(right.scale(-player.xxa * speed))
                .add(normal.scale(WALL_GRIP_PUSH));
    }

    /**
     * The face the player is about to run into, if it is one they could stand on instead.
     *
     * Null when there is nothing to turn onto, which is the normal case. Never returns the
     * face they are already on, nor the one directly behind them.
     */
    private Direction cornerTurn(Player player, Direction current, Vec3 travel) {
        if (travel.lengthSqr() <= 1.0E-6) {
            return null;
        }
        Direction ahead = Direction.getNearest(travel.x, travel.y, travel.z);
        if (ahead == current || ahead == current.getOpposite() || !isClingable(ahead)) {
            return null;
        }
        return hasWallBlock(player.level(), player.blockPosition(), ahead) ? ahead : null;
    }

    /**
     * Which way "forward" points along the surface: wherever the player is looking.
     *
     * This used to be world-up projected onto the wall, fixed, whatever the camera was doing —
     * so W always climbed and S always descended and there was no such thing as running along
     * a wall or turning on it. The surface was a ladder with two rungs.
     *
     * It was written that way to kill a real bug: deriving forward from the look angle made the
     * climb direction flip as the mouse moved, and combined with a speed that scaled off the
     * same vector, W could spike the player's velocity. The direction is not the part that was
     * dangerous — the magnitude was. Forward is normalised here and the speed is a constant, so
     * where the camera points changes only where you go, never how fast.
     *
     * The fallbacks matter: looking straight into a wall leaves nothing to project, and a zero
     * vector normalises to NaN, which Math.min and Math.max propagate rather than clamp. World
     * up on a wall, and the body's own facing on a ceiling, are both always perpendicular to
     * the surface they are used on.
     */
    private Vec3 getWallPlaneForward(Player player, Vec3 wallNormal) {
        Vec3 looking = projectOntoWallPlane(player.getLookAngle(), wallNormal);
        if (looking.lengthSqr() > 0.0025D) {
            return looking.normalize();
        }
        Vec3 up = projectOntoWallPlane(new Vec3(0.0D, 1.0D, 0.0D), wallNormal);
        if (up.lengthSqr() > 0.0001D) {
            return up.normalize();
        }
        // On a ceiling, world up IS the normal, so fall back to which way the body faces.
        Vec3 facing = Vec3.directionFromRotation(0.0F, player.getYRot());
        Vec3 flattened = projectOntoWallPlane(facing, wallNormal);
        return flattened.lengthSqr() > 0.0001D ? flattened.normalize() : new Vec3(0.0D, 0.0D, -1.0D);
    }

    private Vec3 projectOntoWallPlane(Vec3 vector, Vec3 wallNormal) {
        return vector.subtract(wallNormal.scale(vector.dot(wallNormal)));
    }

    private void spawnWallWalkParticles(Player player, Direction wallDirection) {
        if (!(player.level() instanceof ServerLevel serverLevel) || wallDirection == null || player.tickCount % 3 != 0) {
            return;
        }
        Vec3 normal = Vec3.atLowerCornerOf(wallDirection.getNormal());
        Vec3 contact = player.position().add(normal.scale(0.48D)).add(0.0D, 0.12D, 0.0D);
        serverLevel.sendParticles(WALL_WALK_PARTICLE, contact.x, contact.y, contact.z, 2, 0.08D, 0.1D, 0.08D, 0.01D);
        serverLevel.sendParticles(ParticleTypes.END_ROD, contact.x, contact.y, contact.z, 1, 0.05D, 0.08D, 0.05D, 0.005D);
    }

    private void playWallWalkStep(Player player, Direction wallDirection) {
        if (player.level().isClientSide() || wallDirection == null || (Math.abs(player.zza) < 0.01F && Math.abs(player.xxa) < 0.01F) || player.tickCount % 10 != 0) {
            return;
        }
        // The step sound comes from whatever is actually underfoot - or rather, under hand:
        // the block the player is clinging to. A flat STONE_STEP meant running up a glass
        // tower, a tree or a sand dune all sounded like scrambling on rock.
        net.minecraft.core.BlockPos wallPos = player.blockPosition().relative(wallDirection);
        net.minecraft.world.level.block.state.BlockState wall = player.level().getBlockState(wallPos);
        net.minecraft.world.level.block.SoundType soundType = wall.isAir()
                ? net.minecraft.world.level.block.SoundType.STONE
                : wall.getSoundType(player.level(), wallPos, player);
        player.level().playSound(null, player.blockPosition(), soundType.getStepSound(),
                SoundSource.PLAYERS, 0.28F, 1.45F);
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        updatePlayerMovement(player, ninjaData);
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        updatePlayerMovement(player, ninjaData);
    }
}
