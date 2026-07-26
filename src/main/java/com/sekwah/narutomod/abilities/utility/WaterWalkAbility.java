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
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (hasWallBlock(level, base, direction)) {
                return direction;
            }
        }
        return player.horizontalCollision ? Direction.fromYRot(player.getYRot()) : null;
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
        Vec3 wallForward = getWallPlaneForward(normal);
        Vec3 wallRight = normal.cross(wallForward).normalize();
        double baseSpeed = player.isShiftKeyDown() ? WALL_WALK_SNEAK_SPEED : WALL_WALK_RUN_SPEED;
        double verticalInput = player.zza;
        double horizontalInput = player.xxa;
        double verticalSpeed = verticalInput < 0.0D ? baseSpeed * WALL_WALK_DESCEND_MULTIPLIER : baseSpeed;

        // Ceiling guard — don't let climbing push the player's head into a solid block above
        if (verticalInput > 0.0D && hasCeilingAbove(player.level(), player.blockPosition())) {
            verticalInput = 0.0D;
        }

        Vec3 wallMovement = wallForward.scale(verticalInput * verticalSpeed)
                .add(wallRight.scale(-horizontalInput * baseSpeed))
                .add(normal.scale(WALL_GRIP_PUSH));
        if (Math.abs(verticalInput) < 0.01D && Math.abs(horizontalInput) < 0.01D) {
            wallMovement = normal.scale(WALL_GRIP_PUSH);
        }

        // Descending onto solid ground — detach smoothly instead of fighting collision at the base of the wall
        if (verticalInput < 0.0D && hasSolidGroundBelow(player.level(), player.blockPosition())) {
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
        float bobTarget = (float) Math.min(0.18D, Math.abs(verticalInput) * 0.12D + Math.abs(horizontalInput) * 0.08D);
        player.bob += (bobTarget - player.bob) * 0.45F;
    }

    /**
     * "Up the wall" is always world-up projected onto the wall plane — deliberately NOT derived
     * from the player's look angle. Using look angle here was the root cause of the wall-walk
     * launch bug: glancing up/down with the mouse changed the climb direction each tick, so W/S
     * could spike vertical velocity unpredictably. This keeps climbing direction fixed regardless
     * of where the camera is pointed, matching literal "walk straight up the wall" chakra control.
     */
    private Vec3 getWallPlaneForward(Vec3 wallNormal) {
        Vec3 up = projectOntoWallPlane(new Vec3(0.0D, 1.0D, 0.0D), wallNormal);
        if (up.lengthSqr() < 0.0001D) {
            return new Vec3(0.0D, 1.0D, 0.0D);
        }
        return up.normalize();
    }

    private boolean hasCeilingAbove(Level level, BlockPos base) {
        BlockState state = level.getBlockState(base.above(2));
        return !state.isAir() && state.blocksMotion();
    }

    private boolean hasSolidGroundBelow(Level level, BlockPos base) {
        BlockState state = level.getBlockState(base.below());
        return !state.isAir() && state.blocksMotion();
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
        player.level().playSound(null, player.blockPosition(), SoundEvents.STONE_STEP, SoundSource.PLAYERS, 0.28F, 1.45F);
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
