package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.entity.jutsuprojectile.ChakraDragonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Draws the dragon: a coiling serpent built out of rings, not a model file and not particles.
 *
 * It is generated rather than authored because the body has to move like a body. A rigged
 * model would need a skeleton and a walk cycle to do what two sine waves do here for free,
 * and the particle version it replaces had no silhouette at all - a cloud of blue dots
 * travelling toward you is not a dragon, whatever the technique is called.
 *
 * The spine is a travelling wave along the flight axis. Each spine point gets a ring of
 * vertices, consecutive rings are stitched into quads, and the ring radius follows a profile
 * that swells behind the head and tapers to nothing at the tail. Horns and a jaw are drawn as
 * a few extra tapered spurs so the leading end reads as a head rather than a blunt tube.
 */
public class ChakraDragonRenderer extends EntityRenderer<ChakraDragonEntity> {

    /** Spine samples along the body. Enough to curve smoothly without a vertex explosion. */
    private static final int SEGMENTS = 26;
    /** Vertices around each ring. Six reads as round at the size these are seen from. */
    private static final int RING = 6;

    private static final float BODY_LENGTH = 7.5f;
    private static final float MAX_GIRTH = 0.62f;

    /** How hard the body snakes, and how fast the wave runs down it. */
    private static final float WAVE_AMPLITUDE = 0.85f;
    private static final float WAVE_FREQUENCY = 1.7f;
    private static final float WAVE_SPEED = 0.35f;

    /** The head is the real thing: 1.12.2's ModelDragonHead, teeth, horns and whiskers. */
    private final com.sekwah.narutomod.client.model.entity.DragonHeadModel headModel;

    private static final ResourceLocation WATER_SKIN =
            new ResourceLocation(com.sekwah.narutomod.NarutoMod.MOD_ID, "textures/dragon_blue.png");
    private static final ResourceLocation LIGHTNING_SKIN =
            new ResourceLocation(com.sekwah.narutomod.NarutoMod.MOD_ID, "textures/dragon_lightning.png");

    /**
     * Head size. The imported model is a little over three blocks from nose to skull at scale
     * one, which is about right for a serpent this long - scaled down it stops reading as a
     * head and just becomes a lump on the front of the body.
     */
    private static final float HEAD_SCALE = 0.55f;

    public ChakraDragonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.headModel = new com.sekwah.narutomod.client.model.entity.DragonHeadModel(
                context.bakeLayer(com.sekwah.narutomod.client.model.entity.DragonHeadModel.LAYER_LOCATION));
    }

    @Override
    public ResourceLocation getTextureLocation(ChakraDragonEntity entity) {
        return entity.getKind() == ChakraDragonEntity.Kind.LIGHTNING ? LIGHTNING_SKIN : WATER_SKIN;
    }

    @Override
    public void render(ChakraDragonEntity dragon, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float age = dragon.tickCount + partialTick;
        boolean lightning = dragon.getKind() == ChakraDragonEntity.Kind.LIGHTNING;

        poseStack.pushPose();
        // Point the body down its flight path; the spine is then laid out along local -Z.
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-entityYaw));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(
                Mth.lerp(partialTick, dragon.xRotO, dragon.getXRot())));
        // Kirin and a Water Dragon are not the same animal - see ChakraDragonEntity#scale.
        float scale = dragon.getScale();
        poseStack.scale(scale, scale, scale);

        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.CHAKRA_GLOW);
        Matrix4f matrix = poseStack.last().pose();

        // Water is a body of moving liquid, lightning is a discharge - so the water dragon
        // undulates smoothly and the lightning one snaps in tighter, faster kinks.
        float amplitude = lightning ? WAVE_AMPLITUDE * 0.55f : WAVE_AMPLITUDE;
        float frequency = lightning ? WAVE_FREQUENCY * 2.1f : WAVE_FREQUENCY;
        float speed = lightning ? WAVE_SPEED * 3.0f : WAVE_SPEED;

        float[] prev = null;
        for (int i = 0; i <= SEGMENTS; i++) {
            float t = i / (float) SEGMENTS;
            float[] ring = ringAt(t, age, amplitude, frequency, speed);
            if (prev != null) {
                stitch(matrix, consumer, prev, ring, t, lightning);
            }
            prev = ring;
        }

        // The imported skull goes on the front of the procedural body. The original 1.12.2
        // renderer drew ONLY this head and trailed a gas billboard behind it - there is no
        // body model in that mod to port - so the coiling spine above is what gives the
        // technique a length, and this is what gives it a face.
        poseStack.pushPose();
        // 1.12.2 authoring convention: +Y runs downward, so it needs the vanilla flip. The
        // model looks along +Z and the body runs down -Z, so it also turns to face forward.
        poseStack.translate(0.0, 0.15, -0.35);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f));
        poseStack.scale(-HEAD_SCALE, -HEAD_SCALE, HEAD_SCALE);
        // A slow open-and-close on the jaw line, so the head is not a frozen prop.
        this.headModel.renderToBuffer(poseStack,
                bufferSource.getBuffer(net.minecraft.client.renderer.RenderType
                        .entityTranslucent(getTextureLocation(dragon))),
                packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                1.0f, 1.0f, 1.0f, 0.95f);
        poseStack.popPose();

        poseStack.popPose();
    }

    /**
     * One ring of vertices at position t along the spine, as flat x/y/z triples.
     *
     * t = 0 is the head. The spine offset is two waves at different rates so the coil never
     * repeats on an obvious beat, and both fade toward the head - a dragon leads with its
     * skull and whips with its tail, not the other way round.
     */
    private static float[] ringAt(float t, float age, float amplitude, float frequency, float speed) {
        float z = -t * BODY_LENGTH;
        float sway = amplitude * t * Mth.sin(t * frequency * Mth.PI * 2f - age * speed);
        float rise = amplitude * 0.6f * t * Mth.cos(t * frequency * Mth.PI * 1.4f - age * speed * 0.8f);

        // Girth profile: thin nose, thick shoulders just behind the head, taper to a point.
        float girth = MAX_GIRTH * Mth.sin(Mth.clamp(t * 1.25f, 0f, 1f) * Mth.PI) * (1f - t * 0.35f);
        girth = Math.max(girth, 0.02f);

        float[] out = new float[RING * 3];
        for (int i = 0; i < RING; i++) {
            double angle = Math.PI * 2 * i / RING;
            out[i * 3] = sway + (float) Math.cos(angle) * girth;
            out[i * 3 + 1] = rise + (float) Math.sin(angle) * girth;
            out[i * 3 + 2] = z;
        }
        return out;
    }

    /** Joins two consecutive rings into a band of quads. */
    private static void stitch(Matrix4f matrix, VertexConsumer consumer,
                               float[] a, float[] b, float t, boolean lightning) {
        // Brightest at the head, fading out along the body so the tail dissolves.
        float fade = (1f - t) * (1f - t);
        float red = lightning ? 0.75f : 0.25f;
        float green = lightning ? 0.90f : 0.62f;
        float blue = 1.0f;
        float alpha = (lightning ? 0.85f : 0.72f) * Math.max(0.05f, fade);

        for (int i = 0; i < RING; i++) {
            int j = (i + 1) % RING;
            vertex(matrix, consumer, a, i, red, green, blue, alpha);
            vertex(matrix, consumer, a, j, red, green, blue, alpha);
            vertex(matrix, consumer, b, j, red, green, blue, alpha);
            vertex(matrix, consumer, b, i, red, green, blue, alpha);
        }
    }

    private static void vertex(Matrix4f matrix, VertexConsumer consumer, float[] ring, int index,
                               float red, float green, float blue, float alpha) {
        consumer.vertex(matrix, ring[index * 3], ring[index * 3 + 1], ring[index * 3 + 2])
                .color(red, green, blue, alpha).endVertex();
    }

}
