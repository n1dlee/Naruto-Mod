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
                // The Gates aura is NOT particles any more. A shell of dust sprites around
                // your own body is a fog bank you are standing inside: it hid everything
                // within a couple of blocks, which in a technique meant for close combat is
                // the worst possible place to lose visibility. It is drawn as geometry by
                // JutsuWorldRenderer instead, which can be seen by other people without
                // filling the wearer's screen.
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


    /** Arcs reseeded each tick, plus vanilla sparks for the bright flecks between them. */
    private static void renderChidori(Player player) {
        // Distance LOD. Forty particles a tick per user is eight hundred a second, and in a
        // fight with several Chidori up at once that alone can outweigh everything else on
        // screen - for arcs that are a few pixels wide at range and were never readable
        // anyway. Close up nothing changes; past twelve blocks it halves, past twenty-four it
        // is only the bright core spark.
        double distance = Minecraft.getInstance().player == null
                ? 0 : Minecraft.getInstance().player.distanceTo(player);
        boolean self = player == Minecraft.getInstance().player;

        if (!self && distance > CHIDORI_HALF_RATE_RANGE && player.tickCount % 2 != 0) {
            return;
        }

        Vec3 hand = JutsuVfx.handPosition(player, 1.0, 1.0f);
        if (!self && distance > CHIDORI_SPARK_ONLY_RANGE) {
            player.level().addParticle(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, 0, 0, 0);
            return;
        }
        Vec3 facing = player.getViewVector(1.0f);

        // The seed carries the player's id as well as the tick, so two people holding a
        // Chidori side by side do not produce the same bolt shape in lockstep.
        JutsuVfx.chidoriArcs(player.level(), hand, facing,
                player.tickCount * 31 + player.getId(), NarutoParticles.CHIDORI_CYAN);
        player.level().addParticle(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, 0, 0, 0);
    }

    /** Past this only the core spark is drawn; past the half-rate range, every other tick. */
    private static final double CHIDORI_SPARK_ONLY_RANGE = 24.0;
    private static final double CHIDORI_HALF_RATE_RANGE = 12.0;
}
