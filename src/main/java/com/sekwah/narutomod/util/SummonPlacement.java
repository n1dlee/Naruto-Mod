package com.sekwah.narutomod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Somewhere a large summon actually fits.
 *
 * Summons were placed blind: a point a few blocks ahead of the caster, at the caster's own Y.
 * That is fine on a plain and wrong everywhere else - called on a slope, Gamabunta appeared
 * with his legs inside the hill; called at a cliff edge, he appeared in mid-air over the drop;
 * called in a cave, inside the ceiling. Something eight blocks wide that spawns intersecting
 * terrain does not push itself free, it simply stays there for the whole summon.
 */
public final class SummonPlacement {

    private SummonPlacement() {
    }

    /** How far up and down from the requested spot to look before giving up. */
    private static final int SEARCH_UP = 4;
    private static final int SEARCH_DOWN = 8;

    /**
     * Finds a standing position near {@code desired} with room for a body of this size.
     *
     * Searches downward first, because the overwhelmingly common case is a caster standing
     * above the ground the summon should land on - a slope, a ledge, a doorway. Only then
     * upward, for the cave-floor case where the requested spot is inside rock.
     *
     * @return a position whose box is clear, or null when there is nowhere to put it
     */
    @Nullable
    public static Vec3 findClear(ServerLevel level, Vec3 desired, double startY,
                                 float width, float height) {
        for (int offset = 0; offset <= SEARCH_DOWN; offset++) {
            Vec3 candidate = new Vec3(desired.x, startY - offset, desired.z);
            if (fits(level, candidate, width, height) && supported(level, candidate, width)) {
                return candidate;
            }
        }
        for (int offset = 1; offset <= SEARCH_UP; offset++) {
            Vec3 candidate = new Vec3(desired.x, startY + offset, desired.z);
            if (fits(level, candidate, width, height) && supported(level, candidate, width)) {
                return candidate;
            }
        }
        return null;
    }

    /** Nothing solid inside the body's own volume. */
    private static boolean fits(ServerLevel level, Vec3 at, float width, float height) {
        double half = width / 2.0;
        AABB box = new AABB(at.x - half, at.y, at.z - half, at.x + half, at.y + height, at.z + half);
        return level.noCollision(box);
    }

    /**
     * Something to stand on.
     *
     * Checked at the centre only. Testing the whole footprint would refuse any summon at the
     * edge of a platform, which is a legitimate place to put one - and anything that does step
     * off will simply fall, which is survivable, unlike being encased.
     */
    private static boolean supported(ServerLevel level, Vec3 at, float width) {
        BlockPos below = BlockPos.containing(at.x, at.y - 0.5, at.z);
        return !level.getBlockState(below).isAir();
    }
}
