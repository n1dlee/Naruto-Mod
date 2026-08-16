package com.sekwah.narutomod.client.renderer;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.util.JutsuVfx;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Draws the procedural jutsu effects for every player in view, once per client tick.
 *
 * Doing this from one client-side place rather than from each ability has two consequences
 * that matter. Effects appear on OTHER players, because the state they key off is synced and
 * this loops the whole level rather than only the local player; and they appear in first
 * person, because nothing here goes through the player model, which is the thing first-person
 * rendering skips.
 *
 * Everything is spawned with {@code level.addParticle}, so none of it touches the network.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JutsuVfxHandler {

    /** Past this the effects are a few pixels wide and not worth the particle budget. */
    private static final double RENDER_RANGE = 48.0;
    private static final double RENDER_RANGE_SQR = RENDER_RANGE * RENDER_RANGE;

    private JutsuVfxHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused() || minecraft.player == null) {
            return;
        }
        Vec3 camera = minecraft.player.position();

        for (Player player : minecraft.level.players()) {
            if (player.isSpectator() || player.position().distanceToSqr(camera) > RENDER_RANGE_SQR) {
                continue;
            }
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                if (!ninjaData.isNinjaModeEnabled()) {
                    return;
                }
                if (ninjaData.isRasenganHeld()) {
                    renderRasengan(player, ninjaData.getRasenganCharge());
                }
                if (ninjaData.isChidoriActive()) {
                    renderChidori(player);
                }
                if (ninjaData.getGatesOpen() > 0) {
                    renderGateAura(player, ninjaData.getGatesOpen());
                }
            });
        }
    }

    /**
     * The sphere sits in the right hand and spins about the player's own look direction, so
     * turning the camera turns the ball with it rather than leaving it hanging in place.
     */
    private static void renderRasengan(Player player, int charge) {
        // The sphere is geometry now (JutsuWorldRenderer). These are only the few sparks
        // thrown clear of it - a dense cloud around a solid ball just hides the ball, which
        // is exactly what it was doing. Every fourth tick, well outside the surface, and the
        // core layer dropped entirely.
        if (player.tickCount % 4 != 0) {
            return;
        }
        float t = Math.max(0, Math.min(charge - 20, 40)) / 40.0f;
        double radius = (0.24 + t * 0.26) * 1.45;

        Vec3 hand = JutsuVfx.handPosition(player, 1.0, 1.0f);
        Vec3 axis = player.getViewVector(1.0f);
        float age = player.tickCount + 1.0f;

        JutsuVfx.rasenganSparks(player.level(), hand, axis, radius, age,
                NarutoParticles.ROTATION_WHITE);
    }

    /**
     * The Gates aura, escalating in colour as they open.
     *
     * Gates 1-3 burn green - the ordinary chakra a body can survive releasing. From the
     * fourth the output turns blue as the technique stops being sustainable, and the Gate of
     * Death burns a deep red that is meant to read as a warning, not as power. It was one flat
     * green at every gate before, which threw away the only visual the technique had for
     * telling you how far past safe you were.
     */
    private static void renderGateAura(Player player, int gates) {
        net.minecraft.core.particles.ParticleOptions colour;
        if (gates <= 3) {
            colour = NarutoParticles.GATE_GREEN;
        } else if (gates <= 7) {
            colour = GATE_BLUE;
        } else {
            colour = GATE_DEEP_RED;
        }

        float intensity = Math.min(1.0f, (gates - 1) / 7.0f);
        Vec3 feet = player.position();
        JutsuVfx.gateAura(player.level(), feet, player.getBbHeight() * 1.15,
                player.tickCount + 1.0f, intensity, colour);

        // From the fifth gate the shell starts shedding embers outward as well - the body is
        // now losing chakra faster than it can shape it.
        if (gates >= 5 && player.tickCount % 2 == 0) {
            JutsuVfx.gateAura(player.level(), feet.add(0, 0.2, 0), player.getBbHeight() * 0.9,
                    (player.tickCount + 1.0f) * 1.7f, intensity, colour);
        }
    }

    /** Gates 4-7: the output has gone past what green chakra represents. */
    private static final net.minecraft.core.particles.DustParticleOptions GATE_BLUE =
            new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(0.35f, 0.6f, 1.0f), 1.2f);
    /** The Gate of Death. Deliberately dark - this is not a power-up colour. */
    private static final net.minecraft.core.particles.DustParticleOptions GATE_DEEP_RED =
            new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(0.62f, 0.04f, 0.06f), 1.5f);

    /** Arcs reseeded each tick, plus vanilla sparks for the bright flecks between them. */
    private static void renderChidori(Player player) {
        Vec3 hand = JutsuVfx.handPosition(player, 1.0, 1.0f);
        Vec3 facing = player.getViewVector(1.0f);

        JutsuVfx.chidoriArcs(player.level(), hand, facing, player.tickCount,
                NarutoParticles.CHIDORI_CYAN);
        player.level().addParticle(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, 0, 0, 0);
    }
}
