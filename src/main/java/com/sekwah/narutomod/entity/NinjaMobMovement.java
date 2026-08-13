package com.sekwah.narutomod.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

/**
 * The two things every ninja in this mod can do that an ordinary mob cannot: stand on water,
 * and cross a gap in one bound.
 *
 * Shared rather than copied into each entity. Water walking is the player's very first
 * technique, so a rogue ninja who drowns crossing a river reads as a zombie in a headband -
 * and a boss that gives up the chase at the shoreline is worse still.
 */
public final class NinjaMobMovement {

    private NinjaMobMovement() {
    }

    /**
     * Call once from the mob's constructor.
     *
     * Zero malus tells the pathfinder that water is ordinary ground, so routes are planned
     * straight across a lake instead of the long way round. Without it the mob would be
     * physically able to run on water and would still never choose to.
     */
    public static void enableWaterWalking(Mob mob) {
        mob.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        mob.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
    }

    /**
     * Call every server tick. Holds the mob on the surface of any water it walks onto.
     *
     * Implemented by cancelling the sink rather than by adding a collision shape: fluids have
     * no collision box, so there is nothing for the ordinary movement code to stand on. The
     * mob is only held up while its head is clear - stepping into deep water from the side
     * still lets it swim, and being dragged under still works, so this is a technique rather
     * than an immunity.
     */
    public static void tickWaterWalk(Mob mob) {
        if (!mob.isInWater() || mob.isUnderWater()) {
            return;
        }
        Vec3 movement = mob.getDeltaMovement();
        if (movement.y < 0.0D) {
            mob.setDeltaMovement(movement.x, 0.0D, movement.z);
        }
        // Standing on the surface counts as standing: this is what lets it jump, stops fall
        // damage accruing, and keeps the walk animation running instead of the swim one.
        mob.setOnGround(true);
        mob.fallDistance = 0.0F;
    }
}
