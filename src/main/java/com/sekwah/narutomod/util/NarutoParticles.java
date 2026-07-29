package com.sekwah.narutomod.util;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Shared jutsu VFX palette + spawn-pattern helpers, used by new/enhanced ability
 * effects so every jutsu doesn't reinvent the same ring/spiral/burst spawn code.
 */
public class NarutoParticles {

    // --- Named colors reused across multiple jutsu ---
    public static final DustParticleOptions SHARINGAN_RED = new DustParticleOptions(new Vector3f(0.9F, 0.0F, 0.0F), 0.7F);
    public static final DustParticleOptions CHIDORI_CYAN = new DustParticleOptions(new Vector3f(0.45F, 0.85F, 1.0F), 1.0F);
    public static final DustParticleOptions WATER_BLUE = new DustParticleOptions(new Vector3f(0.15F, 0.55F, 1.0F), 1.2F);
    public static final DustParticleOptions LIGHTNING_GOLD = new DustParticleOptions(new Vector3f(0.9F, 0.95F, 0.3F), 1.0F);
    public static final DustParticleOptions TELEPORT_GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.9F, 0.1F), 1.7F);
    public static final DustParticleOptions SAGE_GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.7F, 0.1F), 1.2F);
    public static final DustParticleOptions SHADOW_PURPLE = new DustParticleOptions(new Vector3f(0.05F, 0.02F, 0.12F), 1.0F);
    public static final DustParticleOptions METAL_GRAY = new DustParticleOptions(new Vector3f(0.5F, 0.5F, 0.55F), 1.0F);
    public static final DustParticleOptions ROTATION_WHITE = new DustParticleOptions(new Vector3f(0.9F, 0.95F, 1.0F), 1.2F);
    public static final DustParticleOptions WALL_WALK_CYAN = new DustParticleOptions(new Vector3f(0.3F, 0.8F, 1.0F), 0.85F);
    public static final DustParticleOptions GENJUTSU_RED = new DustParticleOptions(new Vector3f(0.85F, 0.05F, 0.05F), 1.2F);
    public static final DustParticleOptions KURAMA_ORANGE = new DustParticleOptions(new Vector3f(1.0F, 0.4F, 0.05F), 1.3F);
    public static final DustParticleOptions KURAMA_RED_CORE = new DustParticleOptions(new Vector3f(0.9F, 0.15F, 0.05F), 1.0F);
    public static final DustParticleOptions GATE_GREEN = new DustParticleOptions(new Vector3f(0.3F, 0.9F, 0.3F), 1.1F);
    public static final DustParticleOptions GATE_RED = new DustParticleOptions(new Vector3f(0.9F, 0.15F, 0.1F), 1.2F);
    public static final DustParticleOptions GATE_BLACK = new DustParticleOptions(new Vector3f(0.15F, 0.02F, 0.02F), 1.4F);
    /** Amaterasu. Not pure black - a hint of violet keeps it readable against night sky. */
    public static final DustParticleOptions AMATERASU_BLACK = new DustParticleOptions(new Vector3f(0.06F, 0.02F, 0.09F), 1.5F);
    /** Hyoton. Almost white with a cold blue cast, so it reads as ice and not as water. */
    public static final DustParticleOptions ICE_PALE = new DustParticleOptions(new Vector3f(0.72F, 0.92F, 1.0F), 1.1F);
    /** Bakuton clay, before it goes off. */
    public static final DustParticleOptions CLAY_GREY = new DustParticleOptions(new Vector3f(0.72F, 0.68F, 0.6F), 1.1F);
    public static final DustParticleOptions LOG_BROWN = new DustParticleOptions(new Vector3f(0.45F, 0.3F, 0.15F), 1.1F);
    public static final DustParticleOptions CHAIN_GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.75F, 0.15F), 1.1F);
    public static final DustParticleOptions RASENGAN_BLUE = new DustParticleOptions(new Vector3f(0.6F, 0.85F, 1.0F), 1.0F);

    /** Spawns a horizontal ring of particles around a center point. */
    public static void spawnRing(ServerLevel level, Vec3 center, double radius, int count, ParticleOptions particle) {
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            double px = center.x + radius * Math.cos(angle);
            double pz = center.z + radius * Math.sin(angle);
            level.sendParticles(particle, px, center.y, pz, 1, 0, 0, 0, 0.0);
        }
    }

    /** Spawns particles in a rising spiral from a center point. */
    public static void spawnSpiral(ServerLevel level, Vec3 center, double radius, double heightPerStep, int steps, ParticleOptions particle) {
        for (int i = 0; i < steps; i++) {
            double angle = i * 0.6;
            double px = center.x + radius * Math.cos(angle);
            double pz = center.z + radius * Math.sin(angle);
            double py = center.y + i * heightPerStep;
            level.sendParticles(particle, px, py, pz, 1, 0, 0, 0, 0.0);
        }
    }

    /** Spawns a simple scattered burst of particles at a point. */
    public static void spawnBurst(ServerLevel level, Vec3 pos, int count, double spread, ParticleOptions particle) {
        level.sendParticles(particle, pos.x, pos.y, pos.z, count, spread, spread, spread, 0.02);
    }

    /**
     * Draws a jagged, branching bolt of lightning between two points.
     *
     * A straight line of particles reads as a laser, not lightning. What sells it is the
     * classic fractal-lightning construction: repeatedly split the segment at its midpoint
     * and shove that midpoint sideways by a shrinking random amount, so the path stays
     * broadly straight while gaining detail at every scale. The 1.12.2 mod drew exactly
     * this shape as raw GL quads inside a custom entity renderer; recomputed here as
     * particle placements it needs no entity, no model and no client code at all.
     *
     * @param depth  subdivisions. Each level doubles the segment count, so 5 is ~32
     *               segments - past 6 the particle count stops being worth it.
     * @param jitter how far, in blocks, the first midpoint may be displaced. Halves with
     *               every level down, which is what keeps the bolt readable rather than
     *               turning it into noise.
     */
    public static void spawnBolt(ServerLevel level, Vec3 from, Vec3 to, int depth, double jitter,
                                 ParticleOptions particle) {
        subdivideBolt(level, from, to, depth, jitter, particle, true);
    }

    private static void subdivideBolt(ServerLevel level, Vec3 from, Vec3 to, int depth, double jitter,
                                      ParticleOptions particle, boolean mayBranch) {
        if (depth <= 0) {
            level.sendParticles(particle, to.x, to.y, to.z, 1, 0.0, 0.0, 0.0, 0.0);
            return;
        }
        RandomSource random = level.getRandom();
        Vec3 mid = from.add(to).scale(0.5).add(
                (random.nextDouble() - 0.5) * jitter,
                (random.nextDouble() - 0.5) * jitter,
                (random.nextDouble() - 0.5) * jitter);

        subdivideBolt(level, from, mid, depth - 1, jitter * 0.5, particle, mayBranch);
        subdivideBolt(level, mid, to, depth - 1, jitter * 0.5, particle, mayBranch);

        // Forks. Only from the upper levels of the recursion, so branches are chunky limbs
        // coming off the trunk rather than fuzz on every twig.
        if (mayBranch && depth >= 3 && random.nextInt(4) == 0) {
            Vec3 direction = to.subtract(from).scale(0.45 + random.nextDouble() * 0.35);
            Vec3 forkEnd = mid.add(direction).add(
                    (random.nextDouble() - 0.5) * jitter * 2.0,
                    (random.nextDouble() - 0.5) * jitter,
                    (random.nextDouble() - 0.5) * jitter * 2.0);
            subdivideBolt(level, mid, forkEnd, depth - 2, jitter * 0.5, particle, false);
        }
    }
}
