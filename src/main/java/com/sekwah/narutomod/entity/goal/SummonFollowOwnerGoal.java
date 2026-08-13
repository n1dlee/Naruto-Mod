package com.sekwah.narutomod.entity.goal;

import com.sekwah.narutomod.entity.SummonBeastEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Keeps a contract beast with the ninja who called it.
 *
 * Without this the summons hold whatever ground they were called onto and then wander off on
 * the ordinary stroll goal, so ninety seconds of chakra buys a beast standing in a field
 * somewhere behind you. They only follow while they have nothing to fight, so this never
 * pulls one out of a fight it is already in.
 *
 * Support contracts shadow the summoner much more closely than the bruisers do - Katsuyu
 * heals in a radius around whoever she is covering, so drifting ten blocks off makes her
 * technique miss the one person it exists for.
 */
public class SummonFollowOwnerGoal extends Goal {

    private static final double SUPPORT_FOLLOW_DISTANCE = 5.0;
    private static final double FOLLOW_DISTANCE = 11.0;
    /** Close enough. Re-pathing every tick at the owner's heels just jitters. */
    private static final double ARRIVED_DISTANCE = 3.0;
    /** Somewhere behind a wall, or the owner changed dimension: step across instead. */
    private static final double TELEPORT_DISTANCE = 34.0;

    private final SummonBeastEntity beast;
    private Player owner;
    private int repathCooldown;

    public SummonFollowOwnerGoal(SummonBeastEntity beast) {
        this.beast = beast;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.beast.getTarget() != null && this.beast.getTarget().isAlive()) {
            return false;
        }
        Player candidate = this.beast.getOwner();
        if (candidate == null || candidate.isSpectator()) {
            return false;
        }
        if (this.beast.distanceToSqr(candidate) < this.followDistance() * this.followDistance()) {
            return false;
        }
        this.owner = candidate;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.owner == null || !this.owner.isAlive()) {
            return false;
        }
        if (this.beast.getTarget() != null && this.beast.getTarget().isAlive()) {
            return false;
        }
        return this.beast.distanceToSqr(this.owner) > ARRIVED_DISTANCE * ARRIVED_DISTANCE;
    }

    private double followDistance() {
        return this.beast.getVariant().isSupport() ? SUPPORT_FOLLOW_DISTANCE : FOLLOW_DISTANCE;
    }

    @Override
    public void start() {
        this.repathCooldown = 0;
    }

    @Override
    public void stop() {
        this.owner = null;
        this.beast.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.owner == null) {
            return;
        }
        this.beast.getLookControl().setLookAt(this.owner, 10.0F, this.beast.getMaxHeadXRot());
        if (this.beast.distanceToSqr(this.owner) > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
            // Left behind entirely. A summon that cannot keep up is not a summon.
            this.beast.moveTo(this.owner.getX(), this.owner.getY(), this.owner.getZ(),
                    this.beast.getYRot(), this.beast.getXRot());
            this.beast.getNavigation().stop();
            return;
        }
        if (--this.repathCooldown <= 0) {
            this.repathCooldown = 10;
            this.beast.getNavigation().moveTo(this.owner, 1.15D);
        }
    }
}
