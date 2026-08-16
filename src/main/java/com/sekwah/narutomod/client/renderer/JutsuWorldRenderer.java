package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.util.JutsuVfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Draws the jutsu that have a shape: the Rasengan as an actual sphere, Chidori as actual bolts.
 *
 * The particle versions stay - they are the spray around the edges - but the object itself is
 * geometry now. A ball of dust sprites never had a silhouette, so the Rasengan read as a blue
 * smudge beside the hand instead of the sphere it is; the same was true of Chidori, which is
 * lightning and so is defined entirely by having length and a jagged path.
 *
 * Runs after translucent blocks so the additive glow lands on top of water and glass rather
 * than being clipped by them.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JutsuWorldRenderer {

    private static final double RENDER_RANGE_SQR = 48.0 * 48.0;

    /** Sphere tessellation. 10x14 is round enough at arm's length and cheap enough to nest. */
    private static final int SPHERE_RINGS = 10;
    private static final int SPHERE_SECTORS = 14;

    private JutsuWorldRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW);
        float partialTick = event.getPartialTick();

        poseStack.pushPose();
        // The level pose stack is camera-relative; shifting by -camera puts us in world space
        // so every position below can be a plain world coordinate.
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        boolean drewAnything = false;
        for (Player player : minecraft.level.players()) {
            if (player.isSpectator() || player.position().distanceToSqr(camera) > RENDER_RANGE_SQR) {
                continue;
            }
            var capability = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve();
            if (capability.isEmpty() || !capability.get().isNinjaModeEnabled()) {
                continue;
            }
            var ninjaData = capability.get();
            float age = player.tickCount + partialTick;

            if (ninjaData.isRasenganHeld()) {
                Vec3 hand = JutsuVfx.handPosition(player, 1.0, partialTick);
                float charge = Math.max(0, Math.min(ninjaData.getRasenganCharge() - 20, 40)) / 40.0f;
                drawRasengan(poseStack, consumer, hand, 0.24 + charge * 0.26, age);
                drewAnything = true;
            }
            if (ninjaData.isChidoriActive()) {
                Vec3 hand = JutsuVfx.handPosition(player, 1.0, partialTick);
                drawChidori(poseStack, consumer, hand, player.getViewVector(partialTick),
                        camera, player.tickCount);
                drewAnything = true;
            }
            int gates = ninjaData.getGatesOpen();
            if (gates > 0 && !isOwnFirstPersonBody(minecraft, player)) {
                drawGateCloak(poseStack, consumer, player, gates, partialTick, age);
                drewAnything = true;
            }
        }

        poseStack.popPose();
        if (drewAnything) {
            buffers.endBatch(NarutoRenderTypes.CHAKRA_GLOW);
        }
    }

    // --- Rasengan ---

    /**
     * Two nested spheres turning against each other.
     *
     * The surface radius is perturbed by a pair of sines in latitude and longitude that also
     * advance with time, so the shell churns rather than sitting still - a perfectly smooth
     * sphere reads as a marble, not as compressed chakra fighting to escape.
     */
    private static void drawRasengan(PoseStack poseStack, VertexConsumer consumer,
                                     Vec3 centre, double radius, float age) {
        Matrix4f matrix = poseStack.last().pose();

        // Inner core: bright, near-white, spinning fast.
        sphere(matrix, consumer, centre, radius * 0.72, age * 0.22f, 0.75f, 0.92f, 1.0f, 0.85f,
                age, 0.045);
        // Outer shell: the blue skin, larger, counter-rotating, fainter and rougher.
        sphere(matrix, consumer, centre, radius, -age * 0.13f, 0.30f, 0.62f, 1.0f, 0.42f,
                age * 1.6f, 0.085);
    }

    private static void sphere(Matrix4f matrix, VertexConsumer consumer, Vec3 centre, double radius,
                               float spin, float red, float green, float blue, float alpha,
                               float churnTime, double churn) {
        for (int ring = 0; ring < SPHERE_RINGS; ring++) {
            double lat0 = Math.PI * ring / SPHERE_RINGS;
            double lat1 = Math.PI * (ring + 1) / SPHERE_RINGS;

            for (int sector = 0; sector < SPHERE_SECTORS; sector++) {
                double lon0 = 2 * Math.PI * sector / SPHERE_SECTORS + spin;
                double lon1 = 2 * Math.PI * (sector + 1) / SPHERE_SECTORS + spin;

                // Quad wound so both faces exist; NO_CULL means winding does not matter.
                vertex(matrix, consumer, centre, radius, lat0, lon0, churnTime, churn, red, green, blue, alpha);
                vertex(matrix, consumer, centre, radius, lat1, lon0, churnTime, churn, red, green, blue, alpha);
                vertex(matrix, consumer, centre, radius, lat1, lon1, churnTime, churn, red, green, blue, alpha);
                vertex(matrix, consumer, centre, radius, lat0, lon1, churnTime, churn, red, green, blue, alpha);
            }
        }
    }

    private static void vertex(Matrix4f matrix, VertexConsumer consumer, Vec3 centre, double radius,
                               double lat, double lon, float time, double churn,
                               float red, float green, float blue, float alpha) {
        // Two incommensurate frequencies so the churn never settles into a visible pattern.
        double wobble = 1.0
                + Math.sin(lat * 4.0 + time * 0.31) * churn
                + Math.sin(lon * 3.0 - time * 0.47) * churn;
        double r = radius * wobble;

        double x = centre.x + Math.sin(lat) * Math.cos(lon) * r;
        double y = centre.y + Math.cos(lat) * r;
        double z = centre.z + Math.sin(lat) * Math.sin(lon) * r;
        consumer.vertex(matrix, (float) x, (float) y, (float) z).color(red, green, blue, alpha).endVertex();
    }

    // --- Eight Gates cloak ---

    /** Rings up the body, and points around each ring. Enough to read as a skin of fire. */
    private static final int CLOAK_RINGS = 9;
    private static final int CLOAK_POINTS = 10;

    /**
     * The shroud of chakra a body with the Gates open is burning off.
     *
     * This is a surface wrapped around the wearer rather than a cloud around them, which is
     * the whole difference the user asked for: a particle aura is fog you stand inside and it
     * blinds you at exactly the range this technique is fought at. A shell has an inside and
     * an outside, so from the wearer's own eyes there is nothing in the way, and from anyone
     * else's it is unmistakable.
     *
     * The silhouette is a flame: widest at the hips, drawn in to the shoulders and torn into
     * tongues above the head. Each ring's radius is modulated by two out-of-phase sines so the
     * surface boils instead of sitting there as a smooth cylinder.
     */
    private static void drawGateCloak(PoseStack poseStack, VertexConsumer consumer, Player player,
                                      int gates, float partialTick, float age) {
        // Gates 1-3 green, 4-7 blue, the eighth a deep red that is a warning rather than a
        // power-up colour. This is the only readout of how far past safe the wearer is.
        float red;
        float green;
        float blue;
        if (gates <= 3) {
            red = 0.30f; green = 0.95f; blue = 0.35f;
        } else if (gates <= 7) {
            red = 0.35f; green = 0.62f; blue = 1.00f;
        } else {
            red = 0.85f; green = 0.08f; blue = 0.10f;
        }
        float intensity = Math.min(1.0f, (gates - 1) / 7.0f);
        float alpha = 0.16f + intensity * 0.26f;

        double x = Mth.lerp(partialTick, player.xo, player.getX());
        double y = Mth.lerp(partialTick, player.yo, player.getY());
        double z = Mth.lerp(partialTick, player.zo, player.getZ());
        double height = player.getBbHeight() * (1.18 + intensity * 0.22);
        double waist = 0.42 + intensity * 0.22;

        Matrix4f matrix = poseStack.last().pose();
        double[] prev = null;
        for (int ring = 0; ring <= CLOAK_RINGS; ring++) {
            double t = ring / (double) CLOAK_RINGS;
            // Flame profile: full at the hips, pinched at the shoulders, guttering out above.
            double profile = Math.sin(Math.min(1.0, t * 1.15) * Math.PI * 0.92);
            double radius = waist * profile * (1.0 - t * 0.25);
            double[] points = cloakRing(x, y + height * t, z, radius, t, age, intensity);
            if (prev != null) {
                for (int i = 0; i < CLOAK_POINTS; i++) {
                    int j = (i + 1) % CLOAK_POINTS;
                    // Fades out toward the tips so the tongues dissolve rather than end flat.
                    float fadeLow = alpha * (float) (1.0 - (t - 1.0 / CLOAK_RINGS) * 0.8);
                    float fadeHigh = alpha * (float) (1.0 - t * 0.8);
                    put(matrix, consumer, prev[i * 3], prev[i * 3 + 1], prev[i * 3 + 2], red, green, blue, fadeLow);
                    put(matrix, consumer, prev[j * 3], prev[j * 3 + 1], prev[j * 3 + 2], red, green, blue, fadeLow);
                    put(matrix, consumer, points[j * 3], points[j * 3 + 1], points[j * 3 + 2], red, green, blue, fadeHigh);
                    put(matrix, consumer, points[i * 3], points[i * 3 + 1], points[i * 3 + 2], red, green, blue, fadeHigh);
                }
            }
            prev = points;
        }
    }

    private static double[] cloakRing(double x, double y, double z, double radius,
                                      double t, float age, float intensity) {
        double[] out = new double[CLOAK_POINTS * 3];
        for (int i = 0; i < CLOAK_POINTS; i++) {
            double angle = Math.PI * 2 * i / CLOAK_POINTS;
            // Two incommensurate frequencies, one running up the body and one around it, so
            // the boil never settles into a pattern you can see repeating.
            double boil = 1.0
                    + Math.sin(angle * 3.0 + age * (0.22 + intensity * 0.3)) * 0.16
                    + Math.sin(t * 7.0 - age * 0.17) * 0.12;
            double r = radius * boil;
            out[i * 3] = x + Math.cos(angle) * r;
            out[i * 3 + 1] = y;
            out[i * 3 + 2] = z + Math.sin(angle) * r;
        }
        return out;
    }

    /**
     * True when this is the local player's own body in first person.
     *
     * Drawing the shroud there would put it directly over the camera - the exact complaint
     * the particle version earned. In third person the player wants to see their own cloak,
     * so only the first-person case is skipped.
     */
    private static boolean isOwnFirstPersonBody(Minecraft minecraft, Player player) {
        return player == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
    }

    private static void put(Matrix4f matrix, VertexConsumer consumer, double x, double y, double z,
                            float red, float green, float blue, float alpha) {
        consumer.vertex(matrix, (float) x, (float) y, (float) z)
                .color(red, green, blue, Math.max(0f, alpha)).endVertex();
    }

    // --- Chidori ---

    private static final int BOLTS = 6;
    private static final int BOLT_SEGMENTS = 6;
    private static final double BOLT_REACH = 0.75;
    private static final double BOLT_WIDTH = 0.028;

    /**
     * Bolts drawn as camera-facing ribbons.
     *
     * Each bolt is the same random walk the particle version uses, but instead of dotting the
     * path it builds a quad per segment, widened along the axis perpendicular to both the
     * segment and the view direction. That perpendicular is what keeps a flat ribbon edge-on
     * to the camera from disappearing, which is the usual way hand-rolled beams fail.
     *
     * The walk is reseeded from the tick, so the bolt snaps to a new shape twenty times a
     * second instead of sliding smoothly between shapes.
     */
    private static void drawChidori(PoseStack poseStack, VertexConsumer consumer, Vec3 hand,
                                    Vec3 facing, Vec3 camera, int tick) {
        Matrix4f matrix = poseStack.last().pose();
        Vec3 forward = facing.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : facing.normalize();
        Random random = new Random(tick * 2654435761L);

        for (int bolt = 0; bolt < BOLTS; bolt++) {
            Vec3 at = hand;
            Vec3 heading = forward.scale(0.45).add(randomUnit(random).scale(0.95)).normalize();

            for (int segment = 0; segment < BOLT_SEGMENTS; segment++) {
                double taper = (segment + 1) / (double) BOLT_SEGMENTS;
                heading = heading.add(randomUnit(random).scale(0.22 * taper)).normalize();
                Vec3 next = at.add(heading.scale(BOLT_REACH / BOLT_SEGMENTS));

                // Thins toward the tip so a bolt looks like it is fraying out, not stopping.
                double widthNear = BOLT_WIDTH * (1.15 - taper * 0.6);
                double widthFar = BOLT_WIDTH * (1.15 - (taper + 0.15) * 0.6);
                ribbon(matrix, consumer, at, next, camera, widthNear, Math.max(0.004, widthFar));
                at = next;
            }
        }
    }

    private static void ribbon(Matrix4f matrix, VertexConsumer consumer, Vec3 from, Vec3 to,
                               Vec3 camera, double widthFrom, double widthTo) {
        Vec3 along = to.subtract(from);
        if (along.lengthSqr() < 1.0E-9) {
            return;
        }
        Vec3 toCamera = camera.subtract(from);
        Vec3 side = along.cross(toCamera);
        if (side.lengthSqr() < 1.0E-9) {
            // Segment points straight at the camera: any perpendicular will do.
            side = along.cross(new Vec3(0, 1, 0));
            if (side.lengthSqr() < 1.0E-9) {
                return;
            }
        }
        side = side.normalize();

        Vec3 a = from.add(side.scale(widthFrom));
        Vec3 b = from.subtract(side.scale(widthFrom));
        Vec3 c = to.subtract(side.scale(widthTo));
        Vec3 d = to.add(side.scale(widthTo));

        // Near-white core with a cyan bias, brightest at the root of the segment.
        put(matrix, consumer, a, 0.80f, 0.95f, 1.0f, 0.95f);
        put(matrix, consumer, b, 0.80f, 0.95f, 1.0f, 0.95f);
        put(matrix, consumer, c, 0.35f, 0.75f, 1.0f, 0.55f);
        put(matrix, consumer, d, 0.35f, 0.75f, 1.0f, 0.55f);
    }

    private static void put(Matrix4f matrix, VertexConsumer consumer, Vec3 at,
                            float red, float green, float blue, float alpha) {
        consumer.vertex(matrix, (float) at.x, (float) at.y, (float) at.z)
                .color(red, green, blue, alpha).endVertex();
    }

    private static Vec3 randomUnit(Random random) {
        double z = random.nextDouble() * 2.0 - 1.0;
        double a = random.nextDouble() * Math.PI * 2.0;
        double r = Math.sqrt(Math.max(0.0, 1.0 - z * z));
        return new Vec3(Math.cos(a) * r, z, Math.sin(a) * r);
    }
}
