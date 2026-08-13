package com.sekwah.narutomod.entity.goal;

import com.sekwah.narutomod.entity.TailedBeastEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Chomei flies. Nothing else here does.
 *
 * Not implemented by swapping in a flying navigation: the variant is unknown when the mob is
 * constructed, so the navigation and move control are already fixed by the time anyone knows
 * this is the Seven Tails. A goal can ask, so the flight lives in a goal - hold a height band
 * above whatever ground is below, and drift toward the target.
 *
 * The result is a hover rather than true pathfinding, which for a ten-block beetle is the
 * same thing in practice: it goes over the trees instead of through them.
 */
public class BijuHoverGoal extends Goal {

    private static final double MIN_ALTITUDE = 4.0;
    private static final double MAX_ALTITUDE = 8.0;
    private static final double DRIFT_SPEED = 0.16;
    /** Stop closing once overhead, so it hovers above the fight instead of landing in it. */
    private static final double HOLD_DISTANCE = 6.0;

    private final TailedBeastEntity beast;

    public BijuHoverGoal(TailedBeastEntity beast) {
        this.beast = beast;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!this.beast.getVariant().isFlyer()) {
            return false;
        }
        LivingEntity target = this.beast.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        this.beast.setNoGravity(true);
        this.beast.getNavigation().stop();
    }

    @Override
    public void stop() {
        // Always handed back: a beetle that keeps noGravity after losing its target would
        // drift off into the sky and never come down.
        this.beast.setNoGravity(false);
    }

    @Override
    public void tick() {
        LivingEntity target = this.beast.getTarget();
        if (target == null) {
            return;
        }
        this.beast.getLookControl().setLookAt(target, 30f, 30f);

        double altitude = this.beast.getY() - this.groundBelow();
        double climb;
        if (altitude < MIN_ALTITUDE) {
            climb = 0.08;
        } else if (altitude > MAX_ALTITUDE) {
            climb = -0.06;
        } else {
            climb = Math.sin(this.beast.tickCount * 0.08) * 0.01; // idle bob
        }

        Vec3 toTarget = new Vec3(target.getX() - this.beast.getX(), 0, target.getZ() - this.beast.getZ());
        Vec3 drift = toTarget.horizontalDistance() > HOLD_DISTANCE
                ? toTarget.normalize().scale(DRIFT_SPEED)
                : Vec3.ZERO;

        Vec3 movement = this.beast.getDeltaMovement();
        this.beast.setDeltaMovement(
                movement.x * 0.8 + drift.x,
                movement.y * 0.6 + climb,
                movement.z * 0.8 + drift.z);
    }

    /** Y of the first solid block under the beast, or the world floor if there is none. */
    private double groundBelow() {
        Vec3 from = this.beast.position();
        Vec3 to = from.subtract(0, MAX_ALTITUDE + 24, 0);
        BlockHitResult hit = this.beast.level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.beast));
        return hit.getType() == BlockHitResult.Type.MISS
                ? this.beast.level().getMinBuildHeight()
                : hit.getLocation().y;
    }
}
