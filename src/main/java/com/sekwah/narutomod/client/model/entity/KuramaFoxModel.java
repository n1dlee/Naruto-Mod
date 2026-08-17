package com.sekwah.narutomod.client.model.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

/**
 * Kurama himself, imported from the 1.12.2 mod's ModelNineTails.
 *
 * Machine-converted from bytecode: box coordinates and pivots are the originals, so this
 * model shares their +Y-downward authoring convention - the feet are at +{@link #FEET_OFFSET}
 * units, not at zero, and the nine tails arc up over the back into negative Y.
 *
 * This replaces the hand-built KuramaAvatarModel for the nine-tail forms. That model was
 * seven boxes with a snout and no tails at all, which is why the Full Avatar read as "a face
 * and nothing else". The real thing is 179 boxes with a proper muzzle, ears, haunches and
 * nine two-segment tails.
 *
 * The original animated its tails from a precomputed sway table (a {@code float[][]} field
 * built in a constructor loop the converter cannot follow). {@link #waveTails} replaces it
 * with a travelling sine, the same approach used for Manda's spine and Hiruko's tail.
 */
public class KuramaFoxModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(NarutoMod.MOD_ID, "kurama_fox"), "main");

    public static final int TAIL_COUNT = 9;

    /**
     * Distance from the model origin down to the soles, in blocks. Both renderers translate
     * by this after scaling so the fox stands on the ground instead of sinking to its chest.
     */
    public static final float FEET_OFFSET = 39.72f / 16f;

    /**
     * Feet to the top of the skull, in blocks, unscaled. This - not the tail tips - is what
     * gets matched to {@link com.sekwah.narutomod.util.GiantForm#HEIGHT_BLOCKS}, so the body
     * fills the hitbox you can actually hit and the tails plume above it. Scaling by the tail
     * tips instead would leave a fox two thirds the height of its own hitbox.
     */
    public static final float BODY_HEIGHT_BLOCKS = 49.57f / 16f;

    /** Feet to tail tips at rest. Informational: how much sky the silhouette really takes. */
    public static final float FULL_HEIGHT_BLOCKS = 82.72f / 16f;

    // Tail wave. Slow and heavy - these are limbs the size of a building, not whiskers.
    private static final float WAVE_SPEED = 0.045f;
    /** Phase offset between neighbouring tails, so the nine ripple instead of pumping as one. */
    private static final float WAVE_LAG = 0.7f;
    /** How far behind its base the outer segment trails. */
    private static final float TIP_LAG = 0.9f;
    private static final float SWING = 0.22f;
    private static final float CURL = 0.15f;
    private static final float TIP_GAIN = 1.5f;

    private final ModelPart root;

    /** Tail bases and tips, index 0-8, paired with the rest pose the wave is added to. */
    private final ModelPart[] tailBase = new ModelPart[TAIL_COUNT];
    private final ModelPart[] tailTip = new ModelPart[TAIL_COUNT];
    private final float[] baseRestX = new float[TAIL_COUNT];
    private final float[] baseRestZ = new float[TAIL_COUNT];
    private final float[] tipRestX = new float[TAIL_COUNT];
    private final float[] tipRestZ = new float[TAIL_COUNT];

    public KuramaFoxModel(ModelPart root) {
        super(net.minecraft.client.renderer.RenderType::entityTranslucent);
        this.root = root;
        ModelPart tails = root.getChild("body").getChild("tails");
        for (int i = 0; i < TAIL_COUNT; i++) {
            ModelPart base = tails.getChild("Tail" + i + "0");
            ModelPart tip = base.getChild("Tail" + i + "1");
            this.tailBase[i] = base;
            this.tailTip[i] = tip;
            // The nine tails are fanned by their authored rotations; the wave is a delta on
            // top of those, so the fan has to be remembered before anything writes to it.
            this.baseRestX[i] = base.xRot;
            this.baseRestZ[i] = base.zRot;
            this.tipRestX[i] = tip.xRot;
            this.tipRestZ[i] = tip.zRot;
        }
    }

    /**
     * Rolls a travelling wave down the nine tails. Called every frame by every renderer that
     * draws this model - it is a shared baked instance, so a caller that skips this inherits
     * whatever pose the previous caller left behind.
     *
     * Only xRot and zRot are touched. Each tail runs along its own local -Y, so yRot would
     * spin a segment about its own length and show nothing at all.
     */
    public void waveTails(float ageInTicks) {
        for (int i = 0; i < TAIL_COUNT; i++) {
            float phase = ageInTicks * WAVE_SPEED - i * WAVE_LAG;
            this.tailBase[i].xRot = this.baseRestX[i] + net.minecraft.util.Mth.sin(phase) * SWING;
            this.tailBase[i].zRot = this.baseRestZ[i] + net.minecraft.util.Mth.cos(phase * 0.8f) * CURL;

            float tipPhase = phase - TIP_LAG;
            this.tailTip[i].xRot = this.tipRestX[i] + net.minecraft.util.Mth.sin(tipPhase) * SWING * TIP_GAIN;
            this.tailTip[i].zRot = this.tipRestZ[i] + net.minecraft.util.Mth.cos(tipPhase * 0.8f) * CURL * TIP_GAIN;
        }
    }

    /**
     * Cracks the tails forward, on top of whatever idle wave is already there.
     *
     * Kurama fights with its tails, so a strike has to be the tails - not the whole fox
     * leaning, which is what an earlier attempt did and which reads as the animal being
     * shoved rather than attacking. Applied after {@link #waveTails} rather than instead of
     * it: the idle motion carries on underneath, and the strike is the wave being interrupted.
     *
     * The tip is evaluated slightly BEHIND the base in time, not merely at a larger angle.
     * That lag is the whole difference between a whip and a rod: the crack travels down the
     * length, arriving at the tip after the base has already begun to slow.
     *
     * @param progress 0 at the first frame of the strike through to 1 at the last. Negative
     *                 leaves the tails to their idle wave.
     */
    public void strikeTails(float progress) {
        if (progress < 0f) {
            return;
        }
        float baseSweep = sweepAt(progress);
        float tipSweep = sweepAt(progress - TIP_STRIKE_LAG) * TIP_GAIN;
        for (int i = 0; i < TAIL_COUNT; i++) {
            // Fanned: the outer tails start fractionally later, so nine tails arrive as a
            // spread rather than as one slab moving in lockstep.
            float fan = 1f - i * 0.04f;
            this.tailBase[i].xRot += baseSweep * fan;
            this.tailTip[i].xRot += tipSweep * fan;
        }
    }

    /** How far the tails have swung at this point in the strike. */
    private static float sweepAt(float progress) {
        if (progress <= 0f) {
            return 0f;
        }
        if (progress < STRIKE_WINDUP_END) {
            return STRIKE_WIND_BACK * smoothstep(progress / STRIKE_WINDUP_END);
        }
        if (progress < STRIKE_LASH_END) {
            float t = (progress - STRIKE_WINDUP_END) / (STRIKE_LASH_END - STRIKE_WINDUP_END);
            return STRIKE_WIND_BACK + (STRIKE_LASH - STRIKE_WIND_BACK) * t;
        }
        if (progress >= 1f) {
            return 0f;
        }
        float t = (progress - STRIKE_LASH_END) / (1f - STRIKE_LASH_END);
        return STRIKE_LASH * (1f - smoothstep(t));
    }

    private static float smoothstep(float t) {
        float clamped = net.minecraft.util.Mth.clamp(t, 0f, 1f);
        return clamped * clamped * (3f - 2f * clamped);
    }

    /** Gather back, then whip through. The rest of the strike is the tails settling. */
    private static final float STRIKE_WINDUP_END = 0.32f;
    private static final float STRIKE_LASH_END = 0.58f;
    /** Radians back on the gather, and forward at the end of the lash. */
    private static final float STRIKE_WIND_BACK = -0.75f;
    private static final float STRIKE_LASH = 1.45f;
    /** How far behind the base the tip runs, as a fraction of the whole strike. */
    private static final float TIP_STRIKE_LAG = 0.13f;

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partdefinition = mesh.getRoot();
        PartDefinition biped_hat = partdefinition.addOrReplaceChild("biped_hat", CubeListBuilder.create(), PartPose.offset(0.0000f, 24.0000f, 0.0000f));
        PartDefinition eyes = biped_hat.addOrReplaceChild("eyes", CubeListBuilder.create(), PartPose.offset(0.0000f, -25.5000f, -5.0000f));
        PartDefinition bone = eyes.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(48, 12).addBox(-4.0000f, -1.2000f, 0.0500f, 8.0000f, 2.0000f, 0.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -4.7500f, -5.2500f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0000f, 24.0000f, 0.0000f));
        PartDefinition biped_head = body.addOrReplaceChild("biped_head", CubeListBuilder.create().texOffs(1, 52).addBox(-3.0000f, -7.3500f, -5.0000f, 6.0000f, 8.0000f, 3.0000f, new CubeDeformation(0.0000f)), PartPose.offset(0.0000f, -25.5000f, -5.0000f));
        PartDefinition bone73 = biped_head.addOrReplaceChild("bone73", CubeListBuilder.create().texOffs(14, 51).addBox(-2.5000f, 0.0000f, 0.9000f, 5.0000f, 5.0000f, 8.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -8.3500f, 0.0000f, -1.2217f, 0.0000f, 0.0000f));
        PartDefinition bone63 = biped_head.addOrReplaceChild("bone63", CubeListBuilder.create().texOffs(32, -4).addBox(0.0000f, -3.2500f, 0.0000f, 0.0000f, 6.0000f, 4.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(-3.0000f, -2.1000f, -5.0000f, 0.0000f, -0.5236f, 0.0000f));
        PartDefinition bone64 = biped_head.addOrReplaceChild("bone64", CubeListBuilder.create().mirror().texOffs(32, -4).addBox(0.0000f, -3.2500f, 0.0000f, 0.0000f, 6.0000f, 4.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(3.0000f, -2.1000f, -5.0000f, 0.0000f, 0.5236f, 0.0000f));
        PartDefinition snout = biped_head.addOrReplaceChild("snout", CubeListBuilder.create(), PartPose.offset(0.0000f, -2.6000f, -3.7500f));
        PartDefinition cube_r9 = snout.addOrReplaceChild("cube_r9", CubeListBuilder.create().mirror().texOffs(0, 17).addBox(0.0000f, -0.4483f, -0.3093f, 2.0000f, 3.0000f, 6.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(-2.0000f, -0.7676f, -5.8181f, 0.0000f, -0.2182f, 0.0000f));
        PartDefinition cube_r2 = snout.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 17).addBox(-2.0000f, -0.4483f, -0.3093f, 2.0000f, 3.0000f, 6.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(2.0000f, -0.7676f, -5.8181f, 0.0000f, 0.2182f, 0.0000f));
        PartDefinition cube_r10 = snout.addOrReplaceChild("cube_r10", CubeListBuilder.create().mirror().texOffs(0, 9).addBox(0.0000f, -0.4824f, -0.5681f, 2.0000f, 2.0000f, 6.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(-2.0000f, -0.7676f, -5.8181f, 0.2182f, -0.1745f, 0.0000f));
        PartDefinition cube_r3 = snout.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 9).addBox(-2.0000f, -0.4824f, -0.5681f, 2.0000f, 2.0000f, 6.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(2.0000f, -0.7676f, -5.8181f, 0.2182f, 0.1745f, 0.0000f));
        PartDefinition cube_r13 = snout.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(10, 14).addBox(-1.5000f, -0.5388f, -1.5257f, 3.0000f, 3.0000f, 6.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -0.7500f, -5.2500f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition jaw = biped_head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 26).addBox(-2.0000f, 0.0000f, -4.5000f, 4.0000f, 1.0000f, 5.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -0.3500f, -5.0000f, -0.1309f, 0.0000f, 0.0000f));
        PartDefinition earRight = biped_head.addOrReplaceChild("earRight", CubeListBuilder.create().mirror().texOffs(52, 16).addBox(-1.0000f, -1.0152f, -0.4236f, 3.0000f, 3.0000f, 2.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(-2.4500f, -6.1000f, -5.0000f, 0.2618f, 0.0000f, 0.2618f));
        PartDefinition bone62 = earRight.addOrReplaceChild("bone62", CubeListBuilder.create().mirror().texOffs(29, 16).addBox(-2.0000f, -0.5000f, 0.0000f, 3.0000f, 1.0000f, 2.0000f, new CubeDeformation(0.0500f)).mirror(false), PartPose.offsetAndRotation(0.9000f, -0.4152f, -0.6736f, 0.0873f, 0.0436f, 0.0000f));
        PartDefinition cube_r5 = earRight.addOrReplaceChild("cube_r5", CubeListBuilder.create().mirror().texOffs(36, 16).addBox(-0.0151f, -3.0152f, -0.1730f, 2.0000f, 3.0000f, 3.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(-1.0000f, 2.0000f, -0.2500f, 0.0000f, -0.4363f, 0.0000f));
        PartDefinition cube_r6 = cube_r5.addOrReplaceChild("cube_r6", CubeListBuilder.create().mirror().texOffs(36, 16).addBox(-1.0151f, -1.5152f, -0.1730f, 2.0000f, 3.0000f, 3.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(1.0000f, -1.5000f, 2.8000f, -0.0873f, -0.0436f, 0.0000f));
        PartDefinition bone74 = cube_r6.addOrReplaceChild("bone74", CubeListBuilder.create().mirror().texOffs(20, 24).addBox(-1.0000f, -1.5000f, -0.5000f, 2.0000f, 3.0000f, 3.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offsetAndRotation(-0.0151f, -0.0152f, 2.9770f, -0.0873f, -0.0436f, 0.0000f));
        PartDefinition bone75 = bone74.addOrReplaceChild("bone75", CubeListBuilder.create().mirror().texOffs(30, 24).addBox(-1.0000f, -1.5000f, -0.2500f, 2.0000f, 3.0000f, 3.0000f, new CubeDeformation(-0.4000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, 2.0000f, -0.0873f, 0.0436f, 0.0000f));
        PartDefinition bone76 = bone75.addOrReplaceChild("bone76", CubeListBuilder.create().mirror().texOffs(40, 24).addBox(-1.0000f, -1.5000f, -0.2500f, 2.0000f, 3.0000f, 3.0000f, new CubeDeformation(-0.6000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, 1.9000f, 0.0873f, 0.0436f, 0.0000f));
        PartDefinition bone77 = bone76.addOrReplaceChild("bone77", CubeListBuilder.create().mirror().texOffs(50, 24).addBox(-1.0000f, -1.5000f, -0.7500f, 2.0000f, 3.0000f, 3.0000f, new CubeDeformation(-0.8000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, 2.0000f, 0.0436f, 0.0000f, 0.0000f));
        PartDefinition earLeft = biped_head.addOrReplaceChild("earLeft", CubeListBuilder.create().texOffs(52, 16).addBox(-2.0000f, -1.0152f, -0.4236f, 3.0000f, 3.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(2.4500f, -6.1000f, -5.0000f, 0.2618f, 0.0000f, -0.2618f));
        PartDefinition bone65 = earLeft.addOrReplaceChild("bone65", CubeListBuilder.create().texOffs(29, 16).addBox(-1.0000f, -0.5000f, 0.0000f, 3.0000f, 1.0000f, 2.0000f, new CubeDeformation(0.0500f)), PartPose.offsetAndRotation(-0.9000f, -0.4152f, -0.6736f, 0.0873f, -0.0436f, 0.0000f));
        PartDefinition cube_r4 = earLeft.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(36, 16).addBox(-1.9849f, -3.0152f, -0.1730f, 2.0000f, 3.0000f, 3.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(1.0000f, 2.0000f, -0.2500f, 0.0000f, 0.4363f, 0.0000f));
        PartDefinition cube_r7 = cube_r4.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(36, 16).addBox(-0.9849f, -1.5152f, -0.1730f, 2.0000f, 3.0000f, 3.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(-1.0000f, -1.5000f, 2.8000f, -0.0873f, 0.0436f, 0.0000f));
        PartDefinition bone66 = cube_r7.addOrReplaceChild("bone66", CubeListBuilder.create().texOffs(20, 24).addBox(-1.0000f, -1.5000f, -0.5000f, 2.0000f, 3.0000f, 3.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(0.0151f, -0.0152f, 2.9770f, -0.0873f, 0.0436f, 0.0000f));
        PartDefinition bone67 = bone66.addOrReplaceChild("bone67", CubeListBuilder.create().texOffs(30, 24).addBox(-1.0000f, -1.5000f, -0.2500f, 2.0000f, 3.0000f, 3.0000f, new CubeDeformation(-0.4000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 2.0000f, -0.0873f, -0.0436f, 0.0000f));
        PartDefinition bone68 = bone67.addOrReplaceChild("bone68", CubeListBuilder.create().texOffs(40, 24).addBox(-1.0000f, -1.5000f, -0.2500f, 2.0000f, 3.0000f, 3.0000f, new CubeDeformation(-0.6000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 1.9000f, 0.0873f, -0.0436f, 0.0000f));
        PartDefinition bone69 = bone68.addOrReplaceChild("bone69", CubeListBuilder.create().texOffs(50, 24).addBox(-1.0000f, -1.5000f, -0.7500f, 2.0000f, 3.0000f, 3.0000f, new CubeDeformation(-0.8000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 2.0000f, 0.0436f, 0.0000f, 0.0000f));
        PartDefinition biped_body = body.addOrReplaceChild("biped_body", CubeListBuilder.create().texOffs(44, 48).addBox(-5.0000f, -16.0000f, 2.0000f, 5.0000f, 5.0000f, 5.0000f, new CubeDeformation(0.0000f)).mirror().texOffs(44, 48).addBox(0.0000f, -16.0000f, 2.0000f, 5.0000f, 5.0000f, 5.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition waist = biped_body.addOrReplaceChild("waist", CubeListBuilder.create().texOffs(42, 53).addBox(-5.0000f, -4.6340f, -2.5000f, 6.0000f, 6.0000f, 5.0000f, new CubeDeformation(0.0000f)).mirror().texOffs(42, 53).addBox(1.0000f, -4.6340f, -2.5000f, 6.0000f, 6.0000f, 5.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(-1.0000f, -16.0000f, 4.0000f, 0.5236f, 0.0000f, 0.0000f));
        PartDefinition chest = biped_body.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(38, 0).addBox(-6.0000f, -1.2929f, -3.7071f, 7.0000f, 6.0000f, 6.0000f, new CubeDeformation(0.0000f)).mirror().texOffs(38, 0).addBox(1.0000f, -1.2929f, -3.7071f, 7.0000f, 6.0000f, 6.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(-1.0000f, -23.0000f, -1.0000f, 0.7854f, 0.0000f, 0.0000f));
        PartDefinition bone70 = chest.addOrReplaceChild("bone70", CubeListBuilder.create(), PartPose.offsetAndRotation(1.0000f, -1.0000f, 0.9000f, -0.0436f, 0.0000f, 0.0000f));
        PartDefinition bone71 = bone70.addOrReplaceChild("bone71", CubeListBuilder.create().texOffs(10, 0).addBox(0.0000f, 0.0000f, -3.0000f, 8.0000f, 6.0000f, 6.0000f, new CubeDeformation(-0.1000f)), PartPose.offsetAndRotation(-7.0000f, -0.2500f, -1.7500f, 0.0000f, 0.0000f, -0.7854f));
        PartDefinition bone72 = bone70.addOrReplaceChild("bone72", CubeListBuilder.create().mirror().texOffs(10, 0).addBox(-8.0000f, 0.0000f, -3.0000f, 8.0000f, 6.0000f, 6.0000f, new CubeDeformation(-0.1000f)).mirror(false), PartPose.offsetAndRotation(7.0000f, -0.2500f, -1.7500f, 0.0000f, 0.0000f, 0.7854f));
        PartDefinition biped_right_arm = biped_body.addOrReplaceChild("biped_right_arm", CubeListBuilder.create().texOffs(0, 32).addBox(-1.0000f, 0.0000f, 0.0000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offset(-6.0000f, -23.0000f, -3.0000f));
        PartDefinition upperArmRight = biped_right_arm.addOrReplaceChild("upperArmRight", CubeListBuilder.create().mirror().texOffs(0, 32).addBox(-3.6579f, -1.0603f, -2.0000f, 4.0000f, 12.0000f, 4.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.3491f));
        PartDefinition lowerArmRight = biped_right_arm.addOrReplaceChild("lowerArmRight", CubeListBuilder.create().mirror().texOffs(48, 32).addBox(-1.2588f, -0.1635f, -1.5170f, 3.0000f, 12.0000f, 3.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(-5.5000f, 8.2791f, -0.1201f, -0.5236f, 0.0000f, -0.2618f));
        PartDefinition rightHand = lowerArmRight.addOrReplaceChild("rightHand", CubeListBuilder.create(), PartPose.offsetAndRotation(0.7500f, 10.9709f, -0.6299f, 1.5708f, 0.5236f, 1.5708f));
        PartDefinition bone5 = rightHand.addOrReplaceChild("bone5", CubeListBuilder.create().texOffs(0, 48).addBox(0.4830f, 0.3365f, -0.7588f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition bone6 = bone5.addOrReplaceChild("bone6", CubeListBuilder.create().texOffs(0, 50).addBox(-0.2635f, -0.4670f, -0.5588f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.1000f)), PartPose.offsetAndRotation(3.2000f, 0.8000f, -0.2000f, 0.0000f, 0.0000f, 0.5236f));
        PartDefinition bone34 = bone6.addOrReplaceChild("bone34", CubeListBuilder.create().texOffs(12, 48).addBox(-0.5000f, -0.3000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(2.4865f, -0.3170f, -0.0588f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition bone11 = rightHand.addOrReplaceChild("bone11", CubeListBuilder.create().texOffs(0, 48).addBox(0.4307f, 0.3365f, -0.8388f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.2500f, 0.0000f, -0.1745f, 0.0000f));
        PartDefinition bone12 = bone11.addOrReplaceChild("bone12", CubeListBuilder.create().texOffs(0, 50).addBox(-0.2088f, -0.4909f, -0.5388f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.1000f)), PartPose.offsetAndRotation(3.2000f, 0.8000f, -0.3000f, 0.0000f, 0.0000f, 0.5236f));
        PartDefinition bone33 = bone12.addOrReplaceChild("bone33", CubeListBuilder.create().texOffs(12, 48).addBox(-0.5000f, -0.3000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(2.5412f, -0.3409f, -0.0388f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition bone14 = rightHand.addOrReplaceChild("bone14", CubeListBuilder.create().texOffs(0, 48).addBox(-0.0489f, -0.5603f, -0.5088f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.5000f, 0.9000f, -0.2500f, -1.5708f, -0.7854f, 0.5236f));
        PartDefinition bone15 = bone14.addOrReplaceChild("bone15", CubeListBuilder.create().texOffs(0, 50).addBox(-0.2712f, -0.5654f, -0.4850f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.1000f)), PartPose.offsetAndRotation(2.6622f, -0.0175f, -0.0239f, 0.0000f, 0.0000f, 0.7854f));
        PartDefinition bone32 = bone15.addOrReplaceChild("bone32", CubeListBuilder.create().texOffs(12, 48).addBox(-0.3500f, -0.3000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(2.3288f, -0.5154f, 0.0150f, 0.0000f, 0.0000f, 0.2618f));
        PartDefinition bone7 = rightHand.addOrReplaceChild("bone7", CubeListBuilder.create().texOffs(0, 48).addBox(0.5206f, 0.3365f, -0.6710f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, -0.5000f, 0.0000f, 0.1745f, 0.0000f));
        PartDefinition bone8 = bone7.addOrReplaceChild("bone8", CubeListBuilder.create().texOffs(0, 50).addBox(-0.2309f, -0.4358f, -0.5710f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.1000f)), PartPose.offsetAndRotation(3.3000f, 0.8000f, -0.1000f, 0.0000f, 0.0000f, 0.5236f));
        PartDefinition bone35 = bone8.addOrReplaceChild("bone35", CubeListBuilder.create().texOffs(12, 48).addBox(-0.5000f, -0.3000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(2.5191f, -0.2858f, -0.0710f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition bone9 = rightHand.addOrReplaceChild("bone9", CubeListBuilder.create().texOffs(0, 48).addBox(0.5424f, 0.3365f, -0.5780f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, -1.0000f, 0.0000f, 0.3491f, 0.0000f));
        PartDefinition bone10 = bone9.addOrReplaceChild("bone10", CubeListBuilder.create().texOffs(0, 50).addBox(-0.2120f, -0.4467f, -0.5780f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.1000f)), PartPose.offsetAndRotation(3.3000f, 0.8000f, 0.0000f, 0.0000f, 0.0000f, 0.5236f));
        PartDefinition bone36 = bone10.addOrReplaceChild("bone36", CubeListBuilder.create().texOffs(12, 48).addBox(-0.5000f, -0.3000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(2.5380f, -0.2967f, -0.0780f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition biped_left_arm = biped_body.addOrReplaceChild("biped_left_arm", CubeListBuilder.create().mirror().texOffs(0, 32).addBox(0.0000f, 0.0000f, 0.0000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offset(6.0000f, -23.0000f, -3.0000f));
        PartDefinition upperArmLeft = biped_left_arm.addOrReplaceChild("upperArmLeft", CubeListBuilder.create().texOffs(0, 32).addBox(-0.3421f, -1.0603f, -2.0000f, 4.0000f, 12.0000f, 4.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.3491f));
        PartDefinition lowerArmLeft = biped_left_arm.addOrReplaceChild("lowerArmLeft", CubeListBuilder.create().texOffs(48, 32).addBox(-1.7412f, -0.1635f, -1.5170f, 3.0000f, 12.0000f, 3.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(5.5000f, 8.2791f, -0.1201f, -0.5236f, 0.0000f, 0.2618f));
        PartDefinition leftHand = lowerArmLeft.addOrReplaceChild("leftHand", CubeListBuilder.create(), PartPose.offsetAndRotation(-0.7500f, 10.9709f, -0.6299f, 1.5708f, -0.5236f, -1.5708f));
        PartDefinition bone2 = leftHand.addOrReplaceChild("bone2", CubeListBuilder.create().mirror().texOffs(0, 48).addBox(-3.4830f, 0.3365f, -0.7588f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition bone3 = bone2.addOrReplaceChild("bone3", CubeListBuilder.create().mirror().texOffs(0, 50).addBox(-2.7365f, -0.4670f, -0.5588f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.1000f)).mirror(false), PartPose.offsetAndRotation(-3.2000f, 0.8000f, -0.2000f, 0.0000f, 0.0000f, -0.5236f));
        PartDefinition bone4 = bone3.addOrReplaceChild("bone4", CubeListBuilder.create().mirror().texOffs(12, 48).addBox(-1.5000f, -0.3000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offsetAndRotation(-2.4865f, -0.3170f, -0.0588f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition bone13 = leftHand.addOrReplaceChild("bone13", CubeListBuilder.create().mirror().texOffs(0, 48).addBox(-3.4307f, 0.3365f, -0.8388f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.2500f, 0.0000f, 0.1745f, 0.0000f));
        PartDefinition bone16 = bone13.addOrReplaceChild("bone16", CubeListBuilder.create().mirror().texOffs(0, 50).addBox(-2.7912f, -0.4909f, -0.5388f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.1000f)).mirror(false), PartPose.offsetAndRotation(-3.2000f, 0.8000f, -0.3000f, 0.0000f, 0.0000f, -0.5236f));
        PartDefinition bone17 = bone16.addOrReplaceChild("bone17", CubeListBuilder.create().mirror().texOffs(12, 48).addBox(-1.5000f, -0.3000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offsetAndRotation(-2.5412f, -0.3409f, -0.0388f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition bone18 = leftHand.addOrReplaceChild("bone18", CubeListBuilder.create().mirror().texOffs(0, 48).addBox(-2.9511f, -0.5603f, -0.5088f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(-0.5000f, 0.9000f, -0.2500f, -1.5708f, 0.7854f, -0.5236f));
        PartDefinition bone19 = bone18.addOrReplaceChild("bone19", CubeListBuilder.create().mirror().texOffs(0, 50).addBox(-2.7288f, -0.5654f, -0.4850f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.1000f)).mirror(false), PartPose.offsetAndRotation(-2.6622f, -0.0175f, -0.0239f, 0.0000f, 0.0000f, -0.7854f));
        PartDefinition bone20 = bone19.addOrReplaceChild("bone20", CubeListBuilder.create().mirror().texOffs(12, 48).addBox(-1.6500f, -0.3000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offsetAndRotation(-2.3288f, -0.5154f, 0.0150f, 0.0000f, 0.0000f, -0.2618f));
        PartDefinition bone21 = leftHand.addOrReplaceChild("bone21", CubeListBuilder.create().mirror().texOffs(0, 48).addBox(-3.5206f, 0.3365f, -0.6710f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, -0.5000f, 0.0000f, -0.1745f, 0.0000f));
        PartDefinition bone37 = bone21.addOrReplaceChild("bone37", CubeListBuilder.create().mirror().texOffs(0, 50).addBox(-2.7691f, -0.4358f, -0.5710f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.1000f)).mirror(false), PartPose.offsetAndRotation(-3.3000f, 0.8000f, -0.1000f, 0.0000f, 0.0000f, -0.5236f));
        PartDefinition bone38 = bone37.addOrReplaceChild("bone38", CubeListBuilder.create().mirror().texOffs(12, 48).addBox(-1.5000f, -0.3000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offsetAndRotation(-2.5191f, -0.2858f, -0.0710f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition bone39 = leftHand.addOrReplaceChild("bone39", CubeListBuilder.create().mirror().texOffs(0, 48).addBox(-3.5424f, 0.3365f, -0.5780f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, -1.0000f, 0.0000f, -0.3491f, 0.0000f));
        PartDefinition bone40 = bone39.addOrReplaceChild("bone40", CubeListBuilder.create().mirror().texOffs(0, 50).addBox(-2.7880f, -0.4467f, -0.5780f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.1000f)).mirror(false), PartPose.offsetAndRotation(-3.3000f, 0.8000f, 0.0000f, 0.0000f, 0.0000f, -0.5236f));
        PartDefinition bone41 = bone40.addOrReplaceChild("bone41", CubeListBuilder.create().mirror().texOffs(12, 48).addBox(-1.5000f, -0.3000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offsetAndRotation(-2.5380f, -0.2967f, -0.0780f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition biped_right_leg = body.addOrReplaceChild("biped_right_leg", CubeListBuilder.create(), PartPose.offset(-1.9000f, -12.0000f, 4.5000f));
        PartDefinition upperLegRight = biped_right_leg.addOrReplaceChild("upperLegRight", CubeListBuilder.create().texOffs(16, 32).addBox(-1.0000f, 0.0000f, -3.0000f, 4.0000f, 12.0000f, 4.0000f, new CubeDeformation(0.5000f)), PartPose.offsetAndRotation(-2.1000f, 0.0000f, 1.0000f, -0.5236f, 0.0000f, 1.5708f));
        PartDefinition midLegRight = upperLegRight.addOrReplaceChild("midLegRight", CubeListBuilder.create().texOffs(16, 32).addBox(-1.5000f, -0.1340f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-0.1000f)), PartPose.offsetAndRotation(0.5000f, 12.5500f, -1.0000f, 0.0000f, 0.0000f, -2.3562f));
        PartDefinition lowerLegRight = midLegRight.addOrReplaceChild("lowerLegRight", CubeListBuilder.create().texOffs(16, 32).addBox(-3.5000f, -0.3840f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-0.6000f)), PartPose.offsetAndRotation(2.0000f, 7.5500f, 0.0000f, 0.0000f, 0.0000f, 0.7854f));
        PartDefinition rightFoot = lowerLegRight.addOrReplaceChild("rightFoot", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.2500f, 6.2500f, -0.2500f, 0.0000f, -3.1416f, 0.0000f));
        PartDefinition bone22 = rightFoot.addOrReplaceChild("bone22", CubeListBuilder.create().mirror().texOffs(0, 50).addBox(0.0000f, 0.5000f, -0.5000f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offset(0.0000f, -0.5000f, 0.0000f));
        PartDefinition bone23 = bone22.addOrReplaceChild("bone23", CubeListBuilder.create().mirror().texOffs(0, 48).addBox(-0.2000f, -0.5000f, -0.5000f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.0500f)).mirror(false), PartPose.offsetAndRotation(2.8000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.5236f));
        PartDefinition bone54 = bone23.addOrReplaceChild("bone54", CubeListBuilder.create().texOffs(12, 48).addBox(-0.5000f, -0.4000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(2.5500f, -0.3000f, 0.0000f, 0.0000f, 0.0000f, 0.2618f));
        PartDefinition bone24 = rightFoot.addOrReplaceChild("bone24", CubeListBuilder.create().mirror().texOffs(0, 50).addBox(0.0000f, 0.5000f, -0.5000f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -0.5000f, 0.2500f, 0.0000f, -0.1745f, 0.0000f));
        PartDefinition bone25 = bone24.addOrReplaceChild("bone25", CubeListBuilder.create().mirror().texOffs(0, 48).addBox(-0.1000f, -0.5000f, -0.5000f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.0500f)).mirror(false), PartPose.offsetAndRotation(2.7000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.5236f));
        PartDefinition bone53 = bone25.addOrReplaceChild("bone53", CubeListBuilder.create().texOffs(12, 48).addBox(-0.5000f, -0.4000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(2.6500f, -0.3000f, 0.0000f, 0.0000f, 0.0000f, 0.2618f));
        PartDefinition bone26 = rightFoot.addOrReplaceChild("bone26", CubeListBuilder.create().mirror().texOffs(0, 50).addBox(0.0000f, 0.3660f, 0.0000f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -0.5000f, 0.7500f, -0.5236f, -0.5236f, 0.0000f));
        PartDefinition bone27 = bone26.addOrReplaceChild("bone27", CubeListBuilder.create().mirror().texOffs(0, 48).addBox(-0.1670f, -0.5500f, -0.5000f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.0500f)).mirror(false), PartPose.offsetAndRotation(2.7000f, 0.9000f, 0.5000f, 0.0000f, 0.0000f, 0.5236f));
        PartDefinition bone52 = bone27.addOrReplaceChild("bone52", CubeListBuilder.create().texOffs(12, 48).addBox(-0.5000f, -0.5000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(2.5830f, -0.3000f, 0.0000f, 0.0000f, 0.0000f, 0.2182f));
        PartDefinition bone28 = rightFoot.addOrReplaceChild("bone28", CubeListBuilder.create().mirror().texOffs(0, 50).addBox(0.0000f, 0.5000f, -0.5000f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -0.5000f, -0.5000f, 0.0000f, 0.1745f, 0.0000f));
        PartDefinition bone29 = bone28.addOrReplaceChild("bone29", CubeListBuilder.create().mirror().texOffs(0, 48).addBox(-0.2000f, -0.4500f, -0.5000f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.0500f)).mirror(false), PartPose.offsetAndRotation(2.8000f, 0.9500f, 0.0000f, 0.0000f, 0.0000f, 0.5236f));
        PartDefinition bone55 = bone29.addOrReplaceChild("bone55", CubeListBuilder.create().texOffs(12, 48).addBox(-0.5000f, -0.4000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(2.5500f, -0.2500f, 0.0000f, 0.0000f, 0.0000f, 0.2618f));
        PartDefinition bone30 = rightFoot.addOrReplaceChild("bone30", CubeListBuilder.create().mirror().texOffs(0, 50).addBox(0.0000f, 0.5000f, -0.5000f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -0.5000f, -1.0000f, 0.0000f, 0.3491f, 0.0000f));
        PartDefinition bone31 = bone30.addOrReplaceChild("bone31", CubeListBuilder.create().mirror().texOffs(0, 48).addBox(-0.2000f, -0.5000f, -0.5000f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.0500f)).mirror(false), PartPose.offsetAndRotation(2.8000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, 0.5236f));
        PartDefinition bone56 = bone31.addOrReplaceChild("bone56", CubeListBuilder.create().texOffs(12, 48).addBox(-0.5000f, -0.4000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(2.5500f, -0.3000f, 0.0000f, 0.0000f, 0.0000f, 0.2618f));
        PartDefinition biped_left_leg = body.addOrReplaceChild("biped_left_leg", CubeListBuilder.create(), PartPose.offset(1.9000f, -12.0000f, 4.5000f));
        PartDefinition upperLegLeft = biped_left_leg.addOrReplaceChild("upperLegLeft", CubeListBuilder.create().mirror().texOffs(16, 32).addBox(-3.0000f, 0.0000f, -3.0000f, 4.0000f, 12.0000f, 4.0000f, new CubeDeformation(0.5000f)).mirror(false), PartPose.offsetAndRotation(2.1000f, 0.0000f, 1.0000f, -0.5236f, 0.0000f, -1.5708f));
        PartDefinition midLegLeft = upperLegLeft.addOrReplaceChild("midLegLeft", CubeListBuilder.create().mirror().texOffs(16, 32).addBox(-2.5000f, -0.1340f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-0.1000f)).mirror(false), PartPose.offsetAndRotation(-0.5000f, 12.5500f, -1.0000f, 0.0000f, 0.0000f, 2.3562f));
        PartDefinition lowerLegLeft = midLegLeft.addOrReplaceChild("lowerLegLeft", CubeListBuilder.create().mirror().texOffs(16, 32).addBox(-0.5000f, -0.3840f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-0.6000f)).mirror(false), PartPose.offsetAndRotation(-2.0000f, 7.5500f, 0.0000f, 0.0000f, 0.0000f, -0.7854f));
        PartDefinition leftFoot = lowerLegLeft.addOrReplaceChild("leftFoot", CubeListBuilder.create(), PartPose.offsetAndRotation(1.2500f, 6.2500f, -0.2500f, 0.0000f, 3.1416f, 0.0000f));
        PartDefinition bone42 = leftFoot.addOrReplaceChild("bone42", CubeListBuilder.create().texOffs(0, 50).addBox(-3.0000f, 0.5000f, -0.5000f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offset(0.0000f, -0.5000f, 0.0000f));
        PartDefinition bone43 = bone42.addOrReplaceChild("bone43", CubeListBuilder.create().texOffs(0, 48).addBox(-2.8000f, -0.5000f, -0.5000f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.0500f)), PartPose.offsetAndRotation(-2.8000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, -0.5236f));
        PartDefinition bone44 = bone43.addOrReplaceChild("bone44", CubeListBuilder.create().mirror().texOffs(12, 48).addBox(-1.5000f, -0.4000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offsetAndRotation(-2.5500f, -0.3000f, 0.0000f, 0.0000f, 0.0000f, -0.2618f));
        PartDefinition bone45 = leftFoot.addOrReplaceChild("bone45", CubeListBuilder.create().texOffs(0, 50).addBox(-3.0000f, 0.5000f, -0.5000f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -0.5000f, 0.2500f, 0.0000f, 0.1745f, 0.0000f));
        PartDefinition bone46 = bone45.addOrReplaceChild("bone46", CubeListBuilder.create().texOffs(0, 48).addBox(-2.9000f, -0.5000f, -0.5000f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.0500f)), PartPose.offsetAndRotation(-2.7000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, -0.5236f));
        PartDefinition bone47 = bone46.addOrReplaceChild("bone47", CubeListBuilder.create().mirror().texOffs(12, 48).addBox(-1.5000f, -0.4000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offsetAndRotation(-2.6500f, -0.3000f, 0.0000f, 0.0000f, 0.0000f, -0.2618f));
        PartDefinition bone48 = leftFoot.addOrReplaceChild("bone48", CubeListBuilder.create().texOffs(0, 50).addBox(-3.0000f, 0.3660f, 0.0000f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -0.5000f, 0.7500f, -0.5236f, 0.5236f, 0.0000f));
        PartDefinition bone49 = bone48.addOrReplaceChild("bone49", CubeListBuilder.create().texOffs(0, 48).addBox(-2.8330f, -0.5500f, -0.5000f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.0500f)), PartPose.offsetAndRotation(-2.7000f, 0.9000f, 0.5000f, 0.0000f, 0.0000f, -0.5236f));
        PartDefinition bone50 = bone49.addOrReplaceChild("bone50", CubeListBuilder.create().mirror().texOffs(12, 48).addBox(-1.5000f, -0.5000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offsetAndRotation(-2.5830f, -0.3000f, 0.0000f, 0.0000f, 0.0000f, -0.2182f));
        PartDefinition bone51 = leftFoot.addOrReplaceChild("bone51", CubeListBuilder.create().texOffs(0, 50).addBox(-3.0000f, 0.5000f, -0.5000f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -0.5000f, -0.5000f, 0.0000f, -0.1745f, 0.0000f));
        PartDefinition bone57 = bone51.addOrReplaceChild("bone57", CubeListBuilder.create().texOffs(0, 48).addBox(-2.8000f, -0.4500f, -0.5000f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.0500f)), PartPose.offsetAndRotation(-2.8000f, 0.9500f, 0.0000f, 0.0000f, 0.0000f, -0.5236f));
        PartDefinition bone58 = bone57.addOrReplaceChild("bone58", CubeListBuilder.create().mirror().texOffs(12, 48).addBox(-1.5000f, -0.4000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offsetAndRotation(-2.5500f, -0.2500f, 0.0000f, 0.0000f, 0.0000f, -0.2618f));
        PartDefinition bone59 = leftFoot.addOrReplaceChild("bone59", CubeListBuilder.create().texOffs(0, 50).addBox(-3.0000f, 0.5000f, -0.5000f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -0.5000f, -1.0000f, 0.0000f, -0.3491f, 0.0000f));
        PartDefinition bone60 = bone59.addOrReplaceChild("bone60", CubeListBuilder.create().texOffs(0, 48).addBox(-2.8000f, -0.5000f, -0.5000f, 3.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.0500f)), PartPose.offsetAndRotation(-2.8000f, 1.0000f, 0.0000f, 0.0000f, 0.0000f, -0.5236f));
        PartDefinition bone61 = bone60.addOrReplaceChild("bone61", CubeListBuilder.create().mirror().texOffs(12, 48).addBox(-1.5000f, -0.4000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offsetAndRotation(-2.5500f, -0.3000f, 0.0000f, 0.0000f, 0.0000f, -0.2618f));
        PartDefinition tails = body.addOrReplaceChild("tails", CubeListBuilder.create(), PartPose.offset(0.0000f, -11.0000f, 6.0000f));
        PartDefinition Tail00 = tails.addOrReplaceChild("Tail00", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(4.0000f, 0.0000f, 0.0000f, -0.5236f, 0.0000f, 1.4835f));
        PartDefinition Tail01 = Tail00.addOrReplaceChild("Tail01", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.4000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail02 = Tail01.addOrReplaceChild("Tail02", CubeListBuilder.create().texOffs(32, 31).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.7000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail03 = Tail02.addOrReplaceChild("Tail03", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(1.0000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail04 = Tail03.addOrReplaceChild("Tail04", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.6000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail05 = Tail04.addOrReplaceChild("Tail05", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.2000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail06 = Tail05.addOrReplaceChild("Tail06", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-0.4000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail07 = Tail06.addOrReplaceChild("Tail07", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-1.0000f)), PartPose.offsetAndRotation(0.0000f, -6.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail10 = tails.addOrReplaceChild("Tail10", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(3.0000f, 0.0000f, 0.0000f, -0.7854f, 0.0000f, 1.1345f));
        PartDefinition Tail11 = Tail10.addOrReplaceChild("Tail11", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.4000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail12 = Tail11.addOrReplaceChild("Tail12", CubeListBuilder.create().texOffs(32, 31).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.7000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail13 = Tail12.addOrReplaceChild("Tail13", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(1.0000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail14 = Tail13.addOrReplaceChild("Tail14", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.6000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail15 = Tail14.addOrReplaceChild("Tail15", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.2000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail16 = Tail15.addOrReplaceChild("Tail16", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-0.4000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail17 = Tail16.addOrReplaceChild("Tail17", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-1.0000f)), PartPose.offsetAndRotation(0.0000f, -6.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail20 = tails.addOrReplaceChild("Tail20", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(2.0000f, 0.0000f, 0.0000f, -1.0472f, 0.0000f, 0.7854f));
        PartDefinition Tail21 = Tail20.addOrReplaceChild("Tail21", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.4000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail22 = Tail21.addOrReplaceChild("Tail22", CubeListBuilder.create().texOffs(32, 31).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.7000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail23 = Tail22.addOrReplaceChild("Tail23", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(1.0000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail24 = Tail23.addOrReplaceChild("Tail24", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.6000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail25 = Tail24.addOrReplaceChild("Tail25", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.2000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail26 = Tail25.addOrReplaceChild("Tail26", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-0.4000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail27 = Tail26.addOrReplaceChild("Tail27", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-1.0000f)), PartPose.offsetAndRotation(0.0000f, -6.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail30 = tails.addOrReplaceChild("Tail30", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(1.0000f, 0.0000f, 0.0000f, -1.3090f, 0.0000f, 0.4363f));
        PartDefinition Tail31 = Tail30.addOrReplaceChild("Tail31", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.4000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail32 = Tail31.addOrReplaceChild("Tail32", CubeListBuilder.create().texOffs(32, 31).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.7000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail33 = Tail32.addOrReplaceChild("Tail33", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(1.0000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail34 = Tail33.addOrReplaceChild("Tail34", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.6000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail35 = Tail34.addOrReplaceChild("Tail35", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.2000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail36 = Tail35.addOrReplaceChild("Tail36", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-0.4000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail37 = Tail36.addOrReplaceChild("Tail37", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-1.0000f)), PartPose.offsetAndRotation(0.0000f, -6.0000f, 0.0000f, 0.0000f, 0.0000f, -0.1745f));
        PartDefinition Tail40 = tails.addOrReplaceChild("Tail40", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, -1.5708f, 0.0000f, 0.0000f));
        PartDefinition Tail41 = Tail40.addOrReplaceChild("Tail41", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.4000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.1745f, 0.0000f, 0.0000f));
        PartDefinition Tail42 = Tail41.addOrReplaceChild("Tail42", CubeListBuilder.create().texOffs(32, 31).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.7000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.1745f, 0.0000f, 0.0000f));
        PartDefinition Tail43 = Tail42.addOrReplaceChild("Tail43", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(1.0000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.1745f, 0.0000f, 0.0000f));
        PartDefinition Tail44 = Tail43.addOrReplaceChild("Tail44", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.6000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.1745f, 0.0000f, 0.0000f));
        PartDefinition Tail45 = Tail44.addOrReplaceChild("Tail45", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.2000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.1745f, 0.0000f, 0.0000f));
        PartDefinition Tail46 = Tail45.addOrReplaceChild("Tail46", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-0.4000f)), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.1745f, 0.0000f, 0.0000f));
        PartDefinition Tail47 = Tail46.addOrReplaceChild("Tail47", CubeListBuilder.create().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-1.0000f)), PartPose.offsetAndRotation(0.0000f, -6.0000f, 0.0000f, 0.1745f, 0.0000f, 0.0000f));
        PartDefinition Tail50 = tails.addOrReplaceChild("Tail50", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.1000f)).mirror(false), PartPose.offsetAndRotation(-1.0000f, 0.0000f, 0.0000f, -1.3090f, 0.0000f, -0.4363f));
        PartDefinition Tail51 = Tail50.addOrReplaceChild("Tail51", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.4000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail52 = Tail51.addOrReplaceChild("Tail52", CubeListBuilder.create().mirror().texOffs(32, 31).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.7000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail53 = Tail52.addOrReplaceChild("Tail53", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(1.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail54 = Tail53.addOrReplaceChild("Tail54", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.6000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail55 = Tail54.addOrReplaceChild("Tail55", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.2000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail56 = Tail55.addOrReplaceChild("Tail56", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-0.4000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail57 = Tail56.addOrReplaceChild("Tail57", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-1.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -6.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail60 = tails.addOrReplaceChild("Tail60", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.1000f)).mirror(false), PartPose.offsetAndRotation(-2.0000f, 0.0000f, 0.0000f, -1.0472f, 0.0000f, -0.7854f));
        PartDefinition Tail61 = Tail60.addOrReplaceChild("Tail61", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.4000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail62 = Tail61.addOrReplaceChild("Tail62", CubeListBuilder.create().mirror().texOffs(32, 31).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.7000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail63 = Tail62.addOrReplaceChild("Tail63", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(1.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail64 = Tail63.addOrReplaceChild("Tail64", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.6000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail65 = Tail64.addOrReplaceChild("Tail65", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.2000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail66 = Tail65.addOrReplaceChild("Tail66", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-0.4000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail67 = Tail66.addOrReplaceChild("Tail67", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-1.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -6.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail70 = tails.addOrReplaceChild("Tail70", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.1000f)).mirror(false), PartPose.offsetAndRotation(-3.0000f, 0.0000f, 0.0000f, -0.7854f, 0.0000f, -1.1345f));
        PartDefinition Tail71 = Tail70.addOrReplaceChild("Tail71", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.4000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail72 = Tail71.addOrReplaceChild("Tail72", CubeListBuilder.create().mirror().texOffs(32, 31).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.7000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail73 = Tail72.addOrReplaceChild("Tail73", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(1.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail74 = Tail73.addOrReplaceChild("Tail74", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.6000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail75 = Tail74.addOrReplaceChild("Tail75", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.2000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail76 = Tail75.addOrReplaceChild("Tail76", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-0.4000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail77 = Tail76.addOrReplaceChild("Tail77", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-1.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -6.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail80 = tails.addOrReplaceChild("Tail80", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.1000f)).mirror(false), PartPose.offsetAndRotation(-4.0000f, 0.0000f, 0.0000f, -0.5236f, 0.0000f, -1.4835f));
        PartDefinition Tail81 = Tail80.addOrReplaceChild("Tail81", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.4000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail82 = Tail81.addOrReplaceChild("Tail82", CubeListBuilder.create().mirror().texOffs(32, 31).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.7000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail83 = Tail82.addOrReplaceChild("Tail83", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(1.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail84 = Tail83.addOrReplaceChild("Tail84", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.6000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail85 = Tail84.addOrReplaceChild("Tail85", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.2000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail86 = Tail85.addOrReplaceChild("Tail86", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-0.4000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        PartDefinition Tail87 = Tail86.addOrReplaceChild("Tail87", CubeListBuilder.create().mirror().texOffs(32, 32).addBox(-2.0000f, -8.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-1.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -6.0000f, 0.0000f, 0.0000f, 0.0000f, 0.1745f));
        // The original also kept a ModelRenderer[9] of empty stand-ins that its sway table
        // indexed. They carry no geometry and nothing here reads them, so they are dropped -
        // waveTails() drives the real Tail<i>0 segments under "tails" directly.
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public ModelPart root() {
        return this.root;
    }
}
