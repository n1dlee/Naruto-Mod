package com.sekwah.narutomod.util;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Cast effects, one shape per technique.
 *
 * The rule here is that no two of these are the same generator with a different colour. A
 * ring of particles tinted orange and a ring tinted blue are the same effect twice, and a mod
 * whose every jutsu is that ring has one effect no matter how many elements it claims. Each
 * method below is a different piece of geometry: a rose curve, a converging helix, a
 * hexagonal dendrite, a fissure that walks. What tells you which element you are looking at
 * is the shape first and the colour second.
 *
 * All of these are one-shot muzzle effects fired from the caster on the server, so they use
 * sendParticles; the sustained per-frame effects live in {@link JutsuVfx} and run client-side.
 */
public final class ElementalVfx {

    private ElementalVfx() {
    }

    // ================================ FIRE ================================

    /**
     * Phoenix Sage Fire: a five-petal rose traced in flame, standing in the aim plane.
     *
     * r = cos(5t) is the actual five-petalled rose curve, so the volley leaves behind the
     * flower it is named after rather than a puff of fire.
     */
    public static void fireBloom(ServerLevel level, Vec3 centre, Vec3 aim, double size) {
        Vec3 forward = safeNormal(aim);
        Vec3 right = perpendicular(forward);
        Vec3 up = right.cross(forward);

        int steps = 90;
        for (int i = 0; i < steps; i++) {
            double t = Math.PI * i / steps;
            double r = Math.abs(Math.cos(5.0 * t)) * size;
            Vec3 at = centre
                    .add(right.scale(Math.cos(t * 2.0) * r))
                    .add(up.scale(Math.sin(t * 2.0) * r));
            level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 1, 0, 0, 0, 0.005);
        }
        level.sendParticles(ParticleTypes.LAVA, centre.x, centre.y, centre.z, 3, 0.1, 0.1, 0.1, 0.02);
    }

    /**
     * Great Fireball: a cone of flame widening away from the mouth, with the density falling
     * off toward the rim so the middle of the blast reads as the hot part.
     */
    public static void fireCone(ServerLevel level, Vec3 mouth, Vec3 aim, double length, double spread) {
        Vec3 forward = safeNormal(aim);
        Vec3 right = perpendicular(forward);
        Vec3 up = right.cross(forward);

        int rings = 7;
        for (int ring = 1; ring <= rings; ring++) {
            double along = length * ring / (double) rings;
            double radius = spread * (ring / (double) rings);
            int count = 4 + ring * 2;
            for (int i = 0; i < count; i++) {
                double angle = Math.PI * 2 * i / count + ring * 0.4;
                Vec3 at = mouth.add(forward.scale(along))
                        .add(right.scale(Math.cos(angle) * radius))
                        .add(up.scale(Math.sin(angle) * radius));
                Vec3 push = at.subtract(mouth).normalize().scale(0.09);
                level.sendParticles(ring > 4 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME,
                        at.x, at.y, at.z, 1, 0, 0, 0, 0.0);
                level.sendParticles(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, 1,
                        push.x, push.y, push.z, 0.03);
            }
        }
    }

    /**
     * Great Fire Annihilation: a wide vertical sheet rather than a cone - this is the wall of
     * fire technique, and a wall is flat.
     */
    public static void fireWall(ServerLevel level, Vec3 mouth, Vec3 aim, double width, double height) {
        Vec3 forward = safeNormal(aim);
        Vec3 right = perpendicular(forward);

        int columns = 16;
        for (int c = 0; c < columns; c++) {
            double across = (c / (double) (columns - 1) - 0.5) * width;
            // Sag toward the edges: the sheet is thickest and tallest at the centre.
            double columnHeight = height * (1.0 - Math.abs(across / (width * 0.5)) * 0.55);
            int rows = Math.max(2, (int) (columnHeight * 3));
            for (int r = 0; r < rows; r++) {
                double up = columnHeight * r / (double) rows;
                Vec3 at = mouth.add(right.scale(across)).add(0, up, 0).add(forward.scale(1.2));
                Vec3 push = forward.scale(0.18).add(0, 0.04, 0);
                level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 1,
                        push.x, push.y, push.z, 0.05);
            }
        }
    }

    // ================================ WATER ================================

    /**
     * Water Bullet: a helix that tightens as it approaches the muzzle, so the effect is water
     * visibly being compressed into a projectile rather than merely splashing.
     */
    public static void waterLance(ServerLevel level, Vec3 muzzle, Vec3 aim, double length) {
        Vec3 forward = safeNormal(aim);
        Vec3 right = perpendicular(forward);
        Vec3 up = right.cross(forward);

        int steps = 54;
        for (int i = 0; i < steps; i++) {
            double t = i / (double) steps;
            // Radius collapses toward the tip; three full turns over the length.
            double radius = 0.55 * (1.0 - t) * (1.0 - t);
            double angle = t * Math.PI * 6.0;
            Vec3 at = muzzle.add(forward.scale(length * t))
                    .add(right.scale(Math.cos(angle) * radius))
                    .add(up.scale(Math.sin(angle) * radius));
            Vec3 push = forward.scale(0.12);
            level.sendParticles(ParticleTypes.FALLING_WATER, at.x, at.y, at.z, 1, 0, 0, 0, 0.0);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 1,
                        push.x, push.y, push.z, 0.04);
            }
        }
    }

    // ================================ WIND ================================

    /**
     * Air Palm: a single flat crescent of compressed air, edge-on to the direction of travel.
     *
     * Deliberately two-dimensional. Wind Release from the Hyuga is a blade, and giving it any
     * thickness at all turns it back into the generic puff it used to be.
     */
    public static void windCrescent(ServerLevel level, Vec3 palm, Vec3 aim, double radius) {
        Vec3 forward = safeNormal(aim);
        Vec3 right = perpendicular(forward);
        Vec3 up = right.cross(forward);

        int steps = 34;
        for (int i = 0; i < steps; i++) {
            // Only the leading 140 degrees: a crescent, not a disc.
            double angle = Math.toRadians(-70 + 140.0 * i / (steps - 1));
            double bow = Math.cos(angle) * 0.35;
            Vec3 at = palm.add(forward.scale(0.4 + bow))
                    .add(right.scale(Math.sin(angle) * radius))
                    .add(up.scale(Math.cos(angle) * radius * 0.35));
            Vec3 push = forward.scale(0.35);
            level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 1,
                    push.x, push.y, push.z, 0.02);
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                palm.add(forward.scale(0.8)).x, palm.add(forward.scale(0.8)).y,
                palm.add(forward.scale(0.8)).z, 1, 0, 0, 0, 0);
    }

    /**
     * Great Breakthrough: four streamlines corkscrewing down the aim line and flaring out.
     *
     * Streamlines rather than a cloud, because the technique is a sustained gale and what
     * makes a gale legible is being able to see which way it is going.
     */
    public static void windTunnel(ServerLevel level, Vec3 mouth, Vec3 aim, double length) {
        Vec3 forward = safeNormal(aim);
        Vec3 right = perpendicular(forward);
        Vec3 up = right.cross(forward);

        int lines = 4;
        int steps = 26;
        for (int line = 0; line < lines; line++) {
            double phase = Math.PI * 2 * line / lines;
            for (int i = 0; i < steps; i++) {
                double t = i / (double) steps;
                double radius = 0.25 + t * 1.9;
                double angle = phase + t * Math.PI * 3.0;
                Vec3 at = mouth.add(forward.scale(length * t))
                        .add(right.scale(Math.cos(angle) * radius))
                        .add(up.scale(Math.sin(angle) * radius));
                Vec3 push = forward.scale(0.5).add(at.subtract(mouth).normalize().scale(0.1));
                level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 1,
                        push.x, push.y, push.z, 0.08);
            }
        }
    }

    // ================================ EARTH ================================

    /**
     * Earth Wall: dust bursting along the wall's footprint - a straight line across the
     * caster's facing, not a circle around them.
     */
    public static void stoneFootprint(ServerLevel level, Vec3 base, Vec3 aim, double width) {
        Vec3 forward = safeNormal(aim);
        Vec3 right = perpendicular(forward).normalize();
        BlockParticleOption debris =
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());

        int columns = 14;
        for (int c = 0; c < columns; c++) {
            double across = (c / (double) (columns - 1) - 0.5) * width;
            Vec3 at = base.add(right.scale(across)).add(forward.scale(1.5));
            // Plumes climb out of the seam, tallest in the middle.
            double lift = 0.45 * (1.0 - Math.abs(across / (width * 0.5)));
            level.sendParticles(debris, at.x, at.y, at.z, 4, 0.12, 0.05, 0.12, 0.12);
            level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.2, at.z, 2, 0.1, 0.05, 0.1, lift);
        }
    }

    /**
     * Earth Spikes: the ground splitting along a line that wanders, with debris thrown from
     * each break. The zigzag is what makes it read as a fissure instead of a tidy furrow.
     */
    public static void earthFissure(ServerLevel level, Vec3 start, Vec3 aim, double length, long seed) {
        Vec3 forward = safeNormal(new Vec3(aim.x, 0, aim.z));
        Vec3 right = perpendicular(forward).normalize();
        java.util.Random random = new java.util.Random(seed);
        BlockParticleOption debris =
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());

        int steps = 16;
        double drift = 0;
        for (int i = 0; i < steps; i++) {
            double t = i / (double) steps;
            drift += (random.nextDouble() - 0.5) * 0.35;
            Vec3 at = start.add(forward.scale(length * t)).add(right.scale(drift));
            // The break widens as it runs out from the caster.
            int count = 3 + (int) (t * 5);
            level.sendParticles(debris, at.x, at.y + 0.1, at.z, count,
                    0.15, 0.05, 0.15, 0.25 + t * 0.35);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.15, at.z, 1,
                        0.1, 0.02, 0.1, 0.05);
            }
        }
    }

    // ================================ ICE ================================

    /**
     * Ice Spikes: a hexagonal dendrite - six arms with side branches at sixty degrees, which
     * is how a real snowflake is built and why it is instantly readable as ice.
     */
    public static void frostDendrite(ServerLevel level, Vec3 centre, double size) {
        int arms = 6;
        for (int arm = 0; arm < arms; arm++) {
            double angle = Math.PI * 2 * arm / arms;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);

            int steps = 10;
            for (int i = 1; i <= steps; i++) {
                double along = size * i / steps;
                Vec3 at = centre.add(dx * along, 0.05 + i * 0.02, dz * along);
                level.sendParticles(NarutoParticles.ICE_PALE, at.x, at.y, at.z, 1, 0, 0, 0, 0.0);

                // Side branches, sixty degrees off the arm, shortening toward the tip.
                if (i % 3 == 0) {
                    double branch = size * 0.22 * (1.0 - i / (double) steps);
                    for (int side = -1; side <= 1; side += 2) {
                        double ba = angle + side * Math.PI / 3.0;
                        for (int b = 1; b <= 3; b++) {
                            double bl = branch * b / 3.0;
                            Vec3 bat = at.add(Math.cos(ba) * bl, 0.01 * b, Math.sin(ba) * bl);
                            level.sendParticles(NarutoParticles.ICE_PALE,
                                    bat.x, bat.y, bat.z, 1, 0, 0, 0, 0.0);
                        }
                    }
                }
            }
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, centre.x, centre.y + 0.3, centre.z,
                12, 0.4, 0.2, 0.4, 0.01);
    }

    /** Ice Spear Barrage: parallel needles, all pointing the same way, cold and mechanical. */
    public static void iceNeedles(ServerLevel level, Vec3 origin, Vec3 aim, int needles) {
        Vec3 forward = safeNormal(aim);
        Vec3 right = perpendicular(forward);
        Vec3 up = right.cross(forward);

        for (int n = 0; n < needles; n++) {
            double angle = Math.PI * 2 * n / needles;
            double offset = 0.6 + (n % 2) * 0.25;
            Vec3 root = origin.add(right.scale(Math.cos(angle) * offset))
                    .add(up.scale(Math.sin(angle) * offset));
            for (int i = 0; i < 8; i++) {
                Vec3 at = root.add(forward.scale(i * 0.14));
                level.sendParticles(NarutoParticles.ICE_PALE, at.x, at.y, at.z, 1, 0, 0, 0, 0.0);
            }
        }
    }

    // ================================ WOOD ================================

    /**
     * Wood Release: a double helix climbing out of the ground, the way two vines wind around
     * each other. Leaves shed from the strands as they rise.
     */
    public static void woodGrowth(ServerLevel level, Vec3 base, double height, double radius) {
        BlockParticleOption bark =
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState());

        int steps = 40;
        for (int i = 0; i < steps; i++) {
            double t = i / (double) steps;
            double angle = t * Math.PI * 4.0;
            double r = radius * (1.0 - t * 0.4);
            for (int strand = 0; strand < 2; strand++) {
                double a = angle + strand * Math.PI;
                Vec3 at = base.add(Math.cos(a) * r, height * t, Math.sin(a) * r);
                level.sendParticles(bark, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
                if (i % 4 == 0) {
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            at.x, at.y, at.z, 1, 0.08, 0.08, 0.08, 0.0);
                }
            }
        }
    }

    // ================================ LIGHTNING ================================

    /**
     * False Darkness and the other beam techniques: a jagged line along the aim, branching.
     *
     * Same random-walk idea as Chidori's arcs but stretched over a long distance and only
     * lightly deflected, so it reads as a bolt going somewhere rather than crackling in place.
     */
    public static void lightningBeam(ServerLevel level, Vec3 origin, Vec3 aim, double length,
                                     long seed, ParticleOptions spark) {
        Vec3 forward = safeNormal(aim);
        java.util.Random random = new java.util.Random(seed);
        Vec3 at = origin;
        Vec3 heading = forward;

        int steps = 28;
        for (int i = 0; i < steps; i++) {
            // Pulled back toward the aim line every step so the bolt wanders without losing
            // its target - a pure random walk over this distance would miss entirely.
            heading = heading.add(randomUnit(random).scale(0.22)).normalize();
            heading = heading.scale(0.35).add(forward.scale(0.65)).normalize();
            Vec3 next = at.add(heading.scale(length / steps));
            level.sendParticles(spark, next.x, next.y, next.z, 1, 0, 0, 0, 0.0);

            // Occasional dead-end branch, the way real discharge forks.
            if (random.nextInt(6) == 0) {
                Vec3 branch = heading.add(randomUnit(random).scale(0.9)).normalize();
                Vec3 bat = next;
                for (int b = 0; b < 4; b++) {
                    bat = bat.add(branch.scale(length / steps * 0.7));
                    level.sendParticles(spark, bat.x, bat.y, bat.z, 1, 0, 0, 0, 0.0);
                }
            }
            at = next;
        }
    }

    // ================================ helpers ================================

    private static Vec3 safeNormal(Vec3 vector) {
        return vector.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : vector.normalize();
    }

    private static Vec3 perpendicular(Vec3 axis) {
        Vec3 seed = Math.abs(axis.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        return axis.cross(seed).normalize();
    }

    private static Vec3 randomUnit(java.util.Random random) {
        double z = random.nextDouble() * 2.0 - 1.0;
        double a = random.nextDouble() * Math.PI * 2.0;
        double r = Math.sqrt(Math.max(0.0, 1.0 - z * z));
        return new Vec3(Math.cos(a) * r, z, Math.sin(a) * r);
    }
}
