package com.sekwah.narutomod.util;

import com.sekwah.narutomod.entity.ShadowCloneEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Where a field of spikes comes up, shared by the earth and ice versions.
 *
 * Both techniques used to fire down a line in front of the caster, which made them aiming
 * problems rather than the area denial they are meant to be - step to one side and the whole
 * technique missed. They now erupt in a disc centred on the caster, so being surrounded is the
 * situation they answer.
 *
 * The disc grows with mastery of the element, to a hard ceiling of {@link #MAX_RADIUS}. One
 * shared implementation because two copies of "where do the spikes go" would not stay
 * identical for long, and the two techniques differing in reach for no stated reason is worse
 * than either number being wrong.
 */
public final class SpikeField {

    private SpikeField() {
    }

    /** Reach at the lowest mastery, in blocks. Below this the technique cannot fend anyone off. */
    public static final double MIN_RADIUS = 4.0;
    /** The ceiling the user set: far enough to clear a crowd, close enough to stay a technique. */
    public static final double MAX_RADIUS = 20.0;

    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    /** Element mastery runs 0-20, so the radius simply tracks it above a usable floor. */
    public static double radiusFor(int elementLevel) {
        return Math.min(MAX_RADIUS, Math.max(MIN_RADIUS, MIN_RADIUS + elementLevel));
    }

    /**
     * How many spikes come up. Roughly one per square metre of the disc, capped.
     *
     * The first version topped out at twenty, which spread across a twenty-block circle is one
     * spike every sixty square metres - a scattering, not a field, and you could stroll
     * between them. Deriving the count from the AREA instead means the density stays the same
     * as the technique grows, so a mastered cast really does carpet the ground.
     *
     * {@link #MAX_SPIKES} is a performance ceiling rather than a design one: every spike is
     * four block writes plus three scheduled callbacks, and an unbounded disc at radius twenty
     * would ask for over twelve hundred of them in a single tick.
     */
    public static int countFor(int elementLevel) {
        double radius = radiusFor(elementLevel);
        int byArea = (int) Math.round(Math.PI * radius * radius * SPIKE_DENSITY);
        return Math.max(8, Math.min(MAX_SPIKES, byArea));
    }

    /** Spikes per square block of the disc. Just under one, so they read as a thicket. */
    private static final double SPIKE_DENSITY = 0.55;
    /** Hard ceiling; see {@link #countFor}. */
    public static final int MAX_SPIKES = 150;

    /**
     * Picks the columns to erupt under.
     *
     * Anything alive inside the disc is speared first, nearest outward, each led by its own
     * velocity - the eruption is deliberately staggered for the wave effect, and without the
     * lead that stagger is exactly what lets a running target walk out of it. Whatever budget
     * is left fills the disc itself.
     *
     * The filler uses a sunflower distribution: radius grows as the square root of the index
     * while the angle advances by the golden angle, which spreads points evenly over the AREA.
     * Stepping the radius linearly instead - the obvious way - crowds everything into the
     * middle and leaves the outer ring, the part that actually keeps enemies off you, bare.
     */
    public static List<BlockPos> roots(Level level, Player caster, double radius, int maxSpikes) {
        List<BlockPos> roots = new ArrayList<>();
        double radiusSqr = radius * radius;

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(radius),
                entity -> entity != caster
                        && entity.isAlive()
                        && !entity.isSpectator()
                        // Your own clones stand exactly where you are fighting; spearing them
                        // is never what anyone meant by casting this.
                        && !(entity instanceof ShadowCloneEntity)
                        && entity.distanceToSqr(caster) <= radiusSqr);
        targets.sort(Comparator.comparingDouble(caster::distanceToSqr));

        for (LivingEntity target : targets) {
            if (roots.size() >= maxSpikes) {
                break;
            }
            Vec3 lead = target.getDeltaMovement().scale((2 + roots.size() * 2) * 0.5);
            roots.add(groundUnder(level, target.getX() + lead.x, target.getY() + 1, target.getZ() + lead.z));
        }

        int filler = maxSpikes - roots.size();
        for (int i = 0; i < filler; i++) {
            double t = (i + 0.5) / filler;
            double r = radius * Math.sqrt(t);
            // Never underfoot: a spike in the caster's own square is a self-inflicted wall.
            if (r < 1.5) {
                r = 1.5;
            }
            double angle = i * GOLDEN_ANGLE;
            roots.add(groundUnder(level,
                    caster.getX() + Math.cos(angle) * r,
                    caster.getY(),
                    caster.getZ() + Math.sin(angle) * r));
        }
        return roots;
    }

    /** First open block above the ground at this column, searched down from startY. */
    public static BlockPos groundUnder(Level level, double x, double startY, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int by = (int) Math.floor(startY) + 1;
        while (by > level.getMinBuildHeight() && level.getBlockState(new BlockPos(bx, by, bz)).isAir()) {
            by--;
        }
        return new BlockPos(bx, by + 1, bz);
    }
}
