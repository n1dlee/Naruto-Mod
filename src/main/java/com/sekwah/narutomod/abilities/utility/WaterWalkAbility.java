package com.sekwah.narutomod.abilities.utility;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.SurfaceStride;
import com.sekwah.narutomod.util.SurfaceSupport;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public class WaterWalkAbility extends Ability implements Ability.Toggled, Ability.HandleEnded {
    private static final int CHAKRA_COOLDOWN = 15;
    private static final float WALL_CHAKRA = 0.18f;
    private static final float WALL_STAMINA = 0.3f;
    private static final double GRIP_PUSH = 0.035;
    private static final double CORNER_REACH = 0.07;
    private static final Direction[] CLINGABLE = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP
    };
    private static final DustParticleOptions FOOT_SPARK =
            new DustParticleOptions(new Vector3f(0.38f, 0.84f, 1.0f), 0.72f);

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

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (player.isPassenger()) {
            return reject(player, "riding");
        }
        WaterContact water = findWater(player);
        boolean clinging = water == null && findWall(player, data) != null;
        float chakra = clinging ? WALL_CHAKRA : water == null ? 0f : water.submerged() ? 1f : 0.12f;
        if (data.getChakra() < chakra) {
            return reject(player, "notenoughchakra");
        }
        if (clinging && data.getStamina() < WALL_STAMINA) {
            return reject(player, "notenoughstamina");
        }
        data.useChakra(chakra, CHAKRA_COOLDOWN);
        if (clinging) {
            data.useStamina(WALL_STAMINA, CHAKRA_COOLDOWN);
        }
        return true;
    }

    private static boolean reject(Player player, String reason) {
        player.displayClientMessage(Component.translatable("jutsu.fail." + reason,
                Component.translatable("jutsu.waterwalk").withStyle(ChatFormatting.YELLOW)), true);
        return false;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Nullable
    private static WaterContact findWater(Player player) {
        if (player.isFallFlying() || player.isSwimming() || player.isPassenger()) {
            return null;
        }
        double feet = player.getY();
        BlockPos pos = BlockPos.containing(player.getX(), feet, player.getZ());
        var level = player.level();
        for (int i = 0; i <= 1; i++) {
            BlockPos test = i == 0 ? pos : pos.below();
            var water = level.getFluidState(test);
            if (!water.is(FluidTags.WATER) || level.getBlockState(test).blocksMotion()) {
                continue;
            }
            double height = test.getY() + water.getHeight(level, test);
            if (height - feet >= -0.18) {
                return new WaterContact(height, height - feet > 0.16);
            }
        }
        return null;
    }

    @Nullable
    private static BlockHitResult findWall(Player player, INinjaData data) {
        if (player.isInWaterOrBubble() || player.isFallFlying() || player.isPassenger()
                || data.getWallWalkDetachTicks() > 0) {
            return null;
        }
        Direction current = data.getWallWalkDirection();
        if (data.isWallWalkAttached() && current != null) {
            return SurfaceSupport.find(player, current, SurfaceSupport.GRIP_REACH);
        }
        if (player.onGround() && player.zza <= 0.0f) {
            return null;
        }
        Direction facing = Direction.getNearest(player.getLookAngle().x, player.getLookAngle().y, player.getLookAngle().z);
        if (facing != Direction.DOWN) {
            BlockHitResult aimed = SurfaceSupport.find(player, facing, SurfaceSupport.GRIP_REACH);
            if (aimed != null) {
                return aimed;
            }
        }
        for (Direction face : CLINGABLE) {
            if (face == facing) {
                continue;
            }
            BlockHitResult support = SurfaceSupport.find(player, face, SurfaceSupport.GRIP_REACH);
            if (support != null) {
                return support;
            }
        }
        return null;
    }

    private static void updateMovement(Player player, INinjaData data) {
        WaterContact water = findWater(player);
        if (water != null) {
            detach(player, data);
            walkOverWater(player, water);
            return;
        }
        BlockHitResult support = findWall(player, data);
        if (support == null) {
            detach(player, data);
            return;
        }
        walkOnFace(player, data, support);
    }

    private static void walkOverWater(Player player, WaterContact water) {
        Vec3 velocity = player.getDeltaMovement();
        if (water.submerged()) {
            player.lerpMotion(velocity.x, Math.min(0.42, velocity.y + 0.20), velocity.z);
        } else if (velocity.y <= 0.0) {
            player.setPos(player.getX(), water.height() + 0.016, player.getZ());
            player.lerpMotion(velocity.x, 0, velocity.z);
            player.resetFallDistance();
            player.setOnGround(true);
            player.bob += ((float) Math.min(0.1, velocity.horizontalDistance()) - player.bob) * 0.4f;
        }
    }

    private static void walkOnFace(Player player, INinjaData data, BlockHitResult support) {
        Direction face = support.getDirection().getOpposite();
        Vec3 travel = travel(player, face);
        if (travel.y < -0.001 && face.getAxis().isHorizontal()
                && SurfaceSupport.find(player, Direction.DOWN, CORNER_REACH) != null) {
            detach(player, data);
            player.lerpMotion(0, 0, 0);
            return;
        }
        if (travel.lengthSqr() > 1.0e-6 && data.getWallWalkTurnTicks() <= 0) {
            Direction ahead = Direction.getNearest(travel.x, travel.y, travel.z);
            if (ahead != Direction.DOWN && ahead.getAxis() != face.getAxis()) {
                BlockHitResult corner = SurfaceSupport.find(player, ahead, CORNER_REACH);
                if (corner != null) {
                    data.beginWallWalkTurn(face);
                    support = corner;
                    face = ahead;
                    travel = travel(player, face);
                }
            }
        }
        Direction previousFace = data.getWallWalkTurnFrom();
        if (data.getWallWalkTurnTicks() > 0 && previousFace != null && player.zza > 0.01f) {
            Vec3 awayFromCorner = SurfaceStride.normal(previousFace).reverse();
            if (travel.dot(awayFromCorner) <= 0) {
                travel = awayFromCorner.scale(player.isShiftKeyDown() ? 0.08 : 0.21);
            }
        }
        data.setWallWalkDirection(face);
        data.setWallWalkAttached(true);
        Vec3 velocity = travel.add(SurfaceStride.normal(face).scale(GRIP_PUSH));
        player.setNoGravity(true);
        player.resetFallDistance();
        player.setOnGround(true);
        data.getDoubleJumpData().canDoubleJumpServer = true;
        player.lerpMotion(velocity.x, velocity.y, velocity.z);
        player.bob += ((float) Math.min(0.11, travel.length() * 0.54) - player.bob) * 0.45f;
        if (player.level() instanceof ServerLevel level) {
            step(level, player, data.getSurfaceStride(), support);
        }
    }

    private static Vec3 travel(Player player, Direction face) {
        Vec3 normal = SurfaceStride.normal(face);
        Vec3 forward = SurfaceStride.project(player.getLookAngle(), normal);
        if (forward.lengthSqr() < 0.0025) {
            forward = face.getAxis().isVertical()
                    ? Vec3.directionFromRotation(0f, player.getYRot()) : new Vec3(0, 1, 0);
        }
        forward = forward.normalize();
        Vec3 right = normal.cross(forward);
        Vec3 tangent = forward.scale(player.zza).add(right.scale(-player.xxa));
        double speed = player.isShiftKeyDown() ? 0.08 : 0.21;
        return tangent.scale(speed / Math.max(1.0, tangent.length()));
    }

    private static void step(ServerLevel level, Player player, SurfaceStride stride, BlockHitResult support) {
        Direction out = support.getDirection();
        int foot = stride.advance(player.position(), out.getOpposite());
        if (foot == 0) {
            return;
        }
        Vec3 normal = SurfaceStride.normal(out);
        Vec3 lateral = normal.cross(stride.forward()).scale(0.13 * foot);
        Vec3 mark = player.position().add(SurfaceSupport.pivot(player, out.getOpposite(), support))
                .add(lateral).add(normal.scale(0.035));
        var state = level.getBlockState(support.getBlockPos());
        level.playSound(null, mark.x, mark.y, mark.z, state.getSoundType(level, support.getBlockPos(), player)
                .getStepSound(), SoundSource.PLAYERS, 0.38f, 1.25f);
        level.sendParticles(FOOT_SPARK, mark.x, mark.y, mark.z, 3, 0.045, 0.045, 0.045, 0.012);
    }

    private static void detach(Player player, INinjaData data) {
        if (data.isWallWalkAttached()) {
            data.setWallWalkAttached(false);
            player.setNoGravity(false);
        }
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        updateMovement(player, data);
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData data, int ticksActive) {
        detach(player, data);
    }

    @Override
    public void performToggleClient(Player player, INinjaData data) {
        if (player.isLocalPlayer()) {
            updateMovement(player, data);
        }
    }

    private record WaterContact(double height, boolean submerged) {
    }
}
