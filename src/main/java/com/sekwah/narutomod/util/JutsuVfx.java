package com.sekwah.narutomod.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Procedural effects that are generated rather than authored - a spinning point cloud for the
 * Rasengan, a random-walk polyline for Chidori's arcs.
 *
 * These run entirely on the client. Nothing here is spawned with sendParticles, so density
 * costs no bandwidth and the effects can be as thick as they need to be to read properly; the
 * state that drives them ({@code isChidoriActive}, {@code isRasenganHeld}) is already synced
 * for every player in view, so other people's jutsu light up too.
 *
 * The old Rasengan orbited the player's own centre on a flat horizontal ring, which is why it
 * looked like a halo at the waist rather than a sphere in the hand.
 */
public final class JutsuVfx {

    private JutsuVfx() {
    }

    /**
     * The golden angle. Successive multiples of it never repeat a longitude, which is what
     * spreads a point cloud evenly over a sphere instead of banding it into stripes.
     */
    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    // --- Rasengan ---

    private static final int SPHERE_POINTS = 34;
    private static final int SHELL_POINTS = 14;
    private static final int SPARK_POINTS = 8;
    private static final double SPIN_RATE = 0.95;

    /**
     * A thin ring of sparks thrown clear of the Rasengan, and nothing else.
     *
     * Since the sphere became real geometry the particle cloud stopped being the effect and
     * started being the thing hiding it. This is the leftover: eight points on a shell wider
     * than the sphere, drifting outward, so the ball reads as shedding chakra rather than
     * being wrapped in fog.
     */
    public static void rasenganSparks(Level level, Vec3 centre, Vec3 axis, double radius,
                                      float age, ParticleOptions spark) {
        Vec3 spin = axis.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : axis.normalize();
        Vec3 u = perpendicular(spin);
        Vec3 v = spin.cross(u);
        double theta = age * SPIN_RATE * 0.5;

        for (int i = 0; i < SPARK_POINTS; i++) {
            double y = 1.0 - 2.0 * i / (double) (SPARK_POINTS - 1);
            double ring = Math.sqrt(Math.max(0.0, 1.0 - y * y));
            double phi = i * GOLDEN_ANGLE + theta;
            Vec3 point = spin.scale(y).add(u.scale(Math.cos(phi) * ring))
                    .add(v.scale(Math.sin(phi) * ring)).scale(radius);
            Vec3 at = centre.add(point);
            Vec3 drift = point.normalize().scale(0.03);
            level.addParticle(spark, at.x, at.y, at.z, drift.x, drift.y, drift.z);
        }
    }

    /**
     * A sphere of chakra spinning in the hand.
     *
     * The points are a Fibonacci sphere - evenly spread by construction - and the entire cloud
     * is then rotated rigidly about the spin axis. Rotating the whole set together is what sells
     * it as one solid spinning object; per-particle random motion, which is the obvious way to
     * write this, reads as a cloud of sparks instead.
     *
     * @param radius sphere radius in blocks; the held charge scales this
     * @param age    a continuously rising time in ticks, carrying the partial tick
     */
    public static void rasengan(Level level, Vec3 centre, Vec3 axis, double radius, float age,
                                ParticleOptions core, ParticleOptions shell) {
        Vec3 spin = axis.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : axis.normalize();
        Vec3 u = perpendicular(spin);
        Vec3 v = spin.cross(u);

        // The rigid rotation is folded into each point's longitude below rather than applied
        // as a matrix: for a cloud whose longitudes are already being computed, adding theta
        // to phi IS the rotation about the spin axis, and it costs nothing.
        double theta = age * SPIN_RATE;

        for (int i = 0; i < SPHERE_POINTS; i++) {
            // Fibonacci sphere: latitude marches linearly, longitude by the golden angle.
            double y = 1.0 - 2.0 * i / (double) (SPHERE_POINTS - 1);
            double ring = Math.sqrt(Math.max(0.0, 1.0 - y * y));
            double phi = i * GOLDEN_ANGLE + theta;

            // Local frame: y along the spin axis, ring sweeping the u/v plane.
            double a = Math.cos(phi) * ring;
            double b = Math.sin(phi) * ring;

            Vec3 point = spin.scale(y).add(u.scale(a)).add(v.scale(b)).scale(radius);
            Vec3 at = centre.add(point);
            level.addParticle(core, at.x, at.y, at.z, 0, 0, 0);
        }

        // Outer turbulence: a looser, slightly larger shell running the other way, so the two
        // layers shear against each other rather than turning as one rigid ball.
        double shellRadius = radius * (1.18 + Math.sin(age * 0.4) * 0.06);
        for (int i = 0; i < SHELL_POINTS; i++) {
            double y = 1.0 - 2.0 * i / (double) (SHELL_POINTS - 1);
            double ring = Math.sqrt(Math.max(0.0, 1.0 - y * y));
            double phi = i * GOLDEN_ANGLE - theta * 1.6;
            double a = Math.cos(phi) * ring;
            double b = Math.sin(phi) * ring;
            Vec3 point = spin.scale(y).add(u.scale(a)).add(v.scale(b)).scale(shellRadius);
            Vec3 at = centre.add(point);
            // A little outward drift so the shell frays at the edges.
            Vec3 drift = point.normalize().scale(0.012);
            level.addParticle(shell, at.x, at.y, at.z, drift.x, drift.y, drift.z);
        }
    }

    // --- Chidori ---

    private static final int ARCS = 4;
    private static final int SEGMENTS = 5;
    private static final double ARC_REACH = 0.55;
    private static final double ARC_JITTER = 0.16;

    /**
     * Lightning crawling off the hand.
     *
     * Each arc is a random walk: it heads generally outward, and every segment adds a
     * perpendicular kick that grows along the length, so the arc starts tight at the hand and
     * thrashes at the tip. The walk is reseeded every tick from the tick number, which is what
     * makes it flicker to a new shape rather than writhe smoothly - real electrical discharge
     * does not tween.
     *
     * @param seed a value that changes once per tick; the arc shape is a pure function of it
     */
    public static void chidoriArcs(Level level, Vec3 hand, Vec3 facing, int seed,
                                   ParticleOptions spark) {
        Vec3 forward = facing.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : facing.normalize();
        java.util.Random random = new java.util.Random(seed * 31L);

        for (int arc = 0; arc < ARCS; arc++) {
            Vec3 at = hand;
            // Each arc leaves the hand in its own direction, biased forward.
            Vec3 heading = forward.scale(0.5)
                    .add(randomUnit(random).scale(0.9))
                    .normalize();

            for (int segment = 0; segment < SEGMENTS; segment++) {
                double taper = (segment + 1) / (double) SEGMENTS;
                Vec3 kick = randomUnit(random).scale(ARC_JITTER * taper);
                heading = heading.add(kick).normalize();
                Vec3 next = at.add(heading.scale(ARC_REACH / SEGMENTS));

                // Fill the segment so the arc is a line rather than a row of dots.
                for (int step = 0; step < 2; step++) {
                    double t = step / 2.0;
                    Vec3 point = at.add(next.subtract(at).scale(t));
                    level.addParticle(spark, point.x, point.y, point.z, 0, 0, 0);
                }
                at = next;
            }
        }
    }

    // --- Eight Gates aura ---

    private static final int AURA_POINTS = 26;

    /**
     * The shell of chakra boiling off a body with the Gates open.
     *
     * Points ride a vertical cylinder around the player and climb, which is the direction the
     * chakra visibly moves in the source - up and off the shoulders, not outward. Each point's
     * height is derived from its index so the column is evenly filled at any instant, and the
     * whole ring counter-rotates against the climb so it spirals.
     *
     * @param intensity 0..1, driven by the gate count; scales radius, speed and spread
     */
    public static void gateAura(Level level, Vec3 feet, double height, float age,
                                float intensity, ParticleOptions particle) {
        double radius = 0.42 + intensity * 0.38;
        double spin = age * (0.12 + intensity * 0.22);

        for (int i = 0; i < AURA_POINTS; i++) {
            double along = (i / (double) AURA_POINTS + age * 0.035) % 1.0;
            double phi = i * GOLDEN_ANGLE + spin;
            // Narrower at the crown: the column tapers into a flame rather than a pillar.
            double taper = 1.0 - along * 0.45;
            double x = Math.cos(phi) * radius * taper;
            double z = Math.sin(phi) * radius * taper;
            Vec3 at = feet.add(x, along * height, z);
            level.addParticle(particle, at.x, at.y, at.z, 0, 0.04 + intensity * 0.05, 0);
        }
    }

    // --- helpers ---

    /** Any unit vector at right angles to the given one, chosen to stay well conditioned. */
    private static Vec3 perpendicular(Vec3 axis) {
        Vec3 seed = Math.abs(axis.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        return axis.cross(seed).normalize();
    }

    private static Vec3 randomUnit(java.util.Random random) {
        // Rejection-free: pick a latitude uniformly by z, then a longitude. Sampling three
        // gaussians and normalising would also work but costs three calls instead of two.
        double z = random.nextDouble() * 2.0 - 1.0;
        double a = random.nextDouble() * Math.PI * 2.0;
        double r = Math.sqrt(Math.max(0.0, 1.0 - z * z));
        return new Vec3(Math.cos(a) * r, z, Math.sin(a) * r);
    }

    /**
     * Roughly where a player's hand is, following their body yaw and pitch.
     *
     * Not read off the model: the model only exists on the render thread mid-frame, and the
     * effects are spawned on the client tick. This tracks closely enough that the sphere sits
     * in the hand rather than beside it.
     *
     * @param side -1 for the left hand, +1 for the right
     */
    public static Vec3 handPosition(Player player, double side, float partialTick) {
        // Built from the BODY's yaw, not from the view vector.
        //
        // The old version flattened the look direction and normalised it. Looking straight up
        // or down leaves that flattened vector at zero length, so it fell back to a fixed
        // world-Z heading - and the Rasengan jumped to whatever direction south happened to
        // be, regardless of where the player was facing. Body yaw has no such singularity: it
        // is an angle, and an angle pointing at the sky is still a well-defined angle.
        float bodyYaw = net.minecraft.util.Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        double radians = Math.toRadians(bodyYaw);
        Vec3 forward = new Vec3(-Math.sin(radians), 0, Math.cos(radians));
        Vec3 right = new Vec3(Math.cos(radians), 0, Math.sin(radians));

        // Pitch still tilts the arm, but only as a lift on the hand rather than by rotating
        // the whole frame - the shoulder does not move when you look up.
        double pitch = Math.toRadians(net.minecraft.util.Mth.rotLerp(
                partialTick, player.xRotO, player.getXRot()));
        double lift = -Math.sin(pitch) * 0.35;

        return new Vec3(
                Mth_lerp(partialTick, player.xo, player.getX()),
                Mth_lerp(partialTick, player.yo, player.getY()),
                Mth_lerp(partialTick, player.zo, player.getZ()))
                .add(0, player.getBbHeight() * 0.72 + lift, 0)
                .add(right.scale(0.42 * side))
                .add(forward.scale(0.34));
    }

    private static double Mth_lerp(float t, double from, double to) {
        return from + (to - from) * t;
    }
}
