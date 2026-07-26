package com.sekwah.narutomod.util;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
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
}
