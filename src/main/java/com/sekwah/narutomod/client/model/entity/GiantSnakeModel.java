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
import net.minecraft.util.Mth;

/**
 * Manda, imported from the 1.12.2 mod's snake model. Box coordinates and pivots are the
 * originals, so this shares their +Y-downward authoring convention.
 *
 * The head is a straight machine conversion. The spine is not: the original builds it in a
 * loop and then re-places every segment each frame from the entity's own position history,
 * which needs a movement system this mod does not have. Here the segments are a plain
 * parent-child chain given a travelling sine wave, which reads as a slither and costs
 * nothing. The taper is the original's: full width to segment 11, then shrinking by 0.2 a
 * segment to the tail tip.
 */
public class GiantSnakeModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(NarutoMod.MOD_ID, "giant_snake"), "main");

    /** The original allocates 21; past about a dozen the tail is only ever off-screen. */
    private static final int SEGMENTS = 14;
    /** Length of one spine box along Z, in model units. */
    private static final float SEGMENT_LENGTH = 6.0f;

    private final ModelPart root;
    private final ModelPart[] spine = new ModelPart[SEGMENTS];

    public GiantSnakeModel(ModelPart root) {
        super(net.minecraft.client.renderer.RenderType::entityCutoutNoCull);
        this.root = root;
        ModelPart parent = root.getChild("segment0");
        this.spine[0] = parent;
        for (int i = 1; i < SEGMENTS; i++) {
            parent = parent.getChild("segment" + i);
            this.spine[i] = parent;
        }
    }

    /**
     * Rolls a travelling wave down the spine. Amplitude grows toward the tail so the head
     * stays pointed where the entity is actually facing.
     */
    public void slither(float ageInTicks, float speed) {
        float wave = ageInTicks * 0.15f;
        for (int i = 1; i < SEGMENTS; i++) {
            float amplitude = 0.10f + 0.012f * i;
            this.spine[i].yRot = Mth.sin(wave - i * 0.55f) * amplitude * (0.4f + speed);
            this.spine[i].xRot = Mth.cos(wave - i * 0.55f) * 0.03f;
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partdefinition = mesh.getRoot();
        PartDefinition headNeck = partdefinition.addOrReplaceChild("headNeck", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5000f, -2.0000f, -5.0000f, 5.0000f, 4.0000f, 6.0000f, new CubeDeformation(0.0000f)), PartPose.offset(0.0000f, 22.0000f, 0.0000f));
        PartDefinition head = headNeck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(16, 0).addBox(-2.5000f, -2.0000f, 0.0000f, 5.0000f, 4.0000f, 1.0000f, new CubeDeformation(0.1000f)), PartPose.offset(0.0000f, 0.0000f, -5.0000f));
        PartDefinition bone2 = head.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(17, 22).addBox(-0.5000f, -0.5000f, 0.0000f, 1.0000f, 1.0000f, 3.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(1.4000f, -0.7000f, -5.3500f, 0.7854f, 0.0000f, 0.6109f));
        PartDefinition bone3 = bone2.addOrReplaceChild("bone3", CubeListBuilder.create().texOffs(22, 5).addBox(-0.5000f, 0.0000f, 0.0000f, 1.0000f, 1.0000f, 3.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -0.5000f, 3.0000f, -0.9599f, 0.0000f, 0.0000f));
        PartDefinition bone4 = head.addOrReplaceChild("bone4", CubeListBuilder.create().mirror().texOffs(17, 22).addBox(-0.5000f, -0.5000f, 0.0000f, 1.0000f, 1.0000f, 3.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(-1.4000f, -0.7000f, -5.3500f, 0.7854f, 0.0000f, -0.6109f));
        PartDefinition bone5 = bone4.addOrReplaceChild("bone5", CubeListBuilder.create().mirror().texOffs(22, 5).addBox(-0.5000f, 0.0000f, 0.0000f, 1.0000f, 1.0000f, 3.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -0.5000f, 3.0000f, -0.9599f, 0.0000f, 0.0000f));
        PartDefinition bone6 = head.addOrReplaceChild("bone6", CubeListBuilder.create().texOffs(13, 10).addBox(-0.0076f, -1.5000f, -3.8257f, 3.0000f, 3.0000f, 4.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -1.0000f, 0.0000f, 0.0436f, 0.0873f, 0.0000f));
        PartDefinition bone7 = head.addOrReplaceChild("bone7", CubeListBuilder.create().mirror().texOffs(13, 10).addBox(-2.9924f, -1.5000f, -3.8257f, 3.0000f, 3.0000f, 4.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -1.0000f, 0.0000f, 0.0436f, -0.0873f, 0.0000f));
        PartDefinition bone8 = head.addOrReplaceChild("bone8", CubeListBuilder.create().texOffs(17, 17).addBox(-0.0500f, -1.5000f, -3.0757f, 3.0000f, 2.0000f, 3.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(-0.1500f, -1.1000f, -2.5000f, 0.5236f, 0.2618f, 0.0000f));
        PartDefinition bone9 = head.addOrReplaceChild("bone9", CubeListBuilder.create().mirror().texOffs(17, 17).addBox(-2.9500f, -1.5000f, -3.0757f, 3.0000f, 2.0000f, 3.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.1500f, -1.1000f, -2.5000f, 0.5236f, -0.2618f, 0.0000f));
        PartDefinition bone11 = head.addOrReplaceChild("bone11", CubeListBuilder.create().texOffs(10, 19).addBox(-2.0000f, -1.0000f, -2.7500f, 2.0000f, 1.0000f, 3.0000f, new CubeDeformation(0.0000f)).texOffs(0, 19).addBox(-2.0000f, -0.4000f, -2.7500f, 2.0000f, 1.0000f, 3.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(2.6000f, 0.1000f, -3.9500f, 0.0000f, 0.2618f, 0.0000f));
        PartDefinition bone19 = head.addOrReplaceChild("bone19", CubeListBuilder.create().mirror().texOffs(10, 19).addBox(0.0500f, -1.0000f, -2.7500f, 2.0000f, 1.0000f, 3.0000f, new CubeDeformation(0.0000f)).mirror(false).mirror().texOffs(0, 19).addBox(0.0500f, -0.4000f, -2.7500f, 2.0000f, 1.0000f, 3.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(-2.6500f, 0.1000f, -3.9500f, 0.0000f, -0.2618f, 0.0000f));
        PartDefinition bone20 = head.addOrReplaceChild("bone20", CubeListBuilder.create().texOffs(0, 1).addBox(-0.2000f, -1.0000f, 0.0000f, 0.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.1000f)).mirror().texOffs(0, 1).addBox(-3.0000f, -1.0000f, 0.0000f, 0.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.1000f)).mirror(false), PartPose.offset(1.6000f, 1.8000f, -5.9500f));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create(), PartPose.offset(0.0000f, 0.5000f, 0.0000f));
        PartDefinition bone21 = jaw.addOrReplaceChild("bone21", CubeListBuilder.create().texOffs(0, 10).addBox(-3.0000f, -1.0000f, -6.7000f, 3.0000f, 2.0000f, 7.0000f, new CubeDeformation(-0.1000f)), PartPose.offsetAndRotation(3.0000f, 0.9000f, 0.0000f, 0.0000f, 0.2182f, 0.0000f));
        PartDefinition bone22 = jaw.addOrReplaceChild("bone22", CubeListBuilder.create().mirror().texOffs(0, 10).addBox(0.0000f, -1.0000f, -6.7000f, 3.0000f, 2.0000f, 7.0000f, new CubeDeformation(-0.1000f)).mirror(false), PartPose.offsetAndRotation(-3.0000f, 0.9000f, 0.0000f, 0.0000f, -0.2182f, 0.0000f));
        PartDefinition bone23 = jaw.addOrReplaceChild("bone23", CubeListBuilder.create().texOffs(0, 1).addBox(1.2000f, -0.5000f, -0.5000f, 0.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.1000f)).mirror().texOffs(0, 1).addBox(-1.2000f, -0.5000f, -0.5000f, 0.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.1000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -0.2000f, -5.5000f, 3.1416f, 3.1416f, 0.0000f));
        PartDefinition horns = head.addOrReplaceChild("horns", CubeListBuilder.create(), PartPose.offset(0.0000f, 0.6000f, 0.0000f));
        PartDefinition bone24 = horns.addOrReplaceChild("bone24", CubeListBuilder.create().texOffs(28, 0).addBox(-0.5000f, -0.5000f, 0.0000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.1500f)).texOffs(28, 0).addBox(-0.5000f, -0.5000f, 1.0000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.1000f)).texOffs(28, 0).addBox(-0.5000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)).texOffs(28, 0).addBox(-0.5000f, -0.5000f, 2.9000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.1000f)).texOffs(28, 0).addBox(-0.5000f, -0.5000f, 3.6000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)).texOffs(28, 0).addBox(-0.5000f, -0.5000f, 4.1000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.3000f)), PartPose.offsetAndRotation(-2.3000f, -2.5000f, -1.6000f, 0.2618f, -0.5236f, 0.0000f));
        PartDefinition bone25 = horns.addOrReplaceChild("bone25", CubeListBuilder.create().texOffs(28, 0).addBox(-0.5000f, -0.5000f, 0.0000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.1000f)).texOffs(28, 0).addBox(-0.5000f, -0.5000f, 0.9000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.0500f)).texOffs(28, 0).addBox(-0.5000f, -0.5000f, 1.6000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)).texOffs(28, 0).addBox(-0.5000f, -0.5000f, 2.1000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.3000f)), PartPose.offsetAndRotation(-1.2000f, -2.5000f, -1.2000f, 0.5236f, -0.3491f, 0.0000f));
        PartDefinition bone26 = horns.addOrReplaceChild("bone26", CubeListBuilder.create().mirror().texOffs(28, 0).addBox(-0.5000f, -0.5000f, 0.0000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.1000f)).mirror(false).mirror().texOffs(28, 0).addBox(-0.5000f, -0.5000f, 0.9000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.0500f)).mirror(false).mirror().texOffs(28, 0).addBox(-0.5000f, -0.5000f, 1.6000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)).mirror(false).mirror().texOffs(28, 0).addBox(-0.5000f, -0.5000f, 2.1000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.3000f)).mirror(false), PartPose.offsetAndRotation(1.2000f, -2.5000f, -1.2000f, 0.5236f, 0.3491f, 0.0000f));
        PartDefinition bone37 = horns.addOrReplaceChild("bone37", CubeListBuilder.create().mirror().texOffs(28, 0).addBox(-0.5000f, -0.5000f, 0.0000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.1500f)).mirror(false).mirror().texOffs(28, 0).addBox(-0.5000f, -0.5000f, 1.0000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.1000f)).mirror(false).mirror().texOffs(28, 0).addBox(-0.5000f, -0.5000f, 2.0000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false).mirror().texOffs(28, 0).addBox(-0.5000f, -0.5000f, 2.9000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.1000f)).mirror(false).mirror().texOffs(28, 0).addBox(-0.5000f, -0.5000f, 3.6000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.2000f)).mirror(false).mirror().texOffs(28, 0).addBox(-0.5000f, -0.5000f, 4.1000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.3000f)).mirror(false), PartPose.offsetAndRotation(2.3000f, -2.5000f, -1.6000f, 0.2618f, 0.5236f, 0.0000f));

        // The spine, transcribed from the loop the converter stops at. Each segment hangs off
        // the one in front so a rotation anywhere along it carries the whole tail with it.
        PartDefinition previous = partdefinition;
        for (int i = 0; i < SEGMENTS; i++) {
            float taper = i < 12 ? 0.0f : (11 - i) * 0.2f;
            previous = previous.addOrReplaceChild("segment" + i,
                    CubeListBuilder.create().texOffs(0, 0)
                            .addBox(-2.5f, -2.0f, -1.0f, 5.0f, 4.0f, 6.0f, new CubeDeformation(taper)),
                    i == 0 ? PartPose.offset(0.0f, 22.0f, 1.0f)
                           : PartPose.offset(0.0f, 0.0f, SEGMENT_LENGTH));
        }

        return LayerDefinition.create(mesh, 32, 32);
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
