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
 * Hiruko, imported from the 1.12.2 mod. Box coordinates and pivots are the originals, so
 * this shares their +Y-downward authoring convention.
 *
 * The body is a straight machine conversion. The tail is not: the original builds it in a
 * loop over a ModelRenderer[30][2], which the converter stops at rather than guess about, so
 * the thirty segments are transcribed here from the same bytecode. Each link carries a fin
 * and two diamond spikes; the numbers are the originals.
 */
public class HirukoModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(NarutoMod.MOD_ID, "puppet_hiruko"), "main");

    /** The original allocates thirty links, at four units each: a seven-block tail. */
    private static final int TAIL_SEGMENTS = 30;
    private static final float SEGMENT_LENGTH = 4.0f;

    private final ModelPart root;
    private final ModelPart[] tail = new ModelPart[TAIL_SEGMENTS];

    public HirukoModel(ModelPart root) {
        super(net.minecraft.client.renderer.RenderType::entityCutoutNoCull);
        this.root = root;
        ModelPart link = root.getChild("tail0");
        this.tail[0] = link;
        for (int i = 1; i < TAIL_SEGMENTS; i++) {
            link = link.getChild("tail" + i);
            this.tail[i] = link;
        }
    }

    /**
     * Rolls a wave down the tail so the stinger is always moving.
     *
     * Set every frame by the renderer: one model instance serves every Hiruko on screen, and
     * the parts keep whatever rotation the last one left on them.
     */
    public void lashTail(float ageInTicks) {
        float wave = ageInTicks * 0.08f;
        for (int i = 1; i < TAIL_SEGMENTS; i++) {
            this.tail[i].xRot = Mth.sin(wave - i * 0.22f) * 0.055f;
            this.tail[i].yRot = Mth.cos(wave * 0.7f - i * 0.18f) * 0.035f;
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partdefinition = mesh.getRoot();
        PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9000f, 15.0000f, 0.0000f));
        PartDefinition rightThigh = right_leg.addOrReplaceChild("rightThigh", CubeListBuilder.create().texOffs(0, 54).addBox(-2.0000f, 0.0000f, -2.0000f, 4.0000f, 6.0000f, 4.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, -0.7854f, 0.6545f, 0.0000f));
        PartDefinition calfRight = rightThigh.addOrReplaceChild("calfRight", CubeListBuilder.create().texOffs(52, 6).addBox(-2.0000f, 0.0000f, 0.0000f, 4.0000f, 6.0000f, 4.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 6.0000f, -2.0000f, 0.7854f, 0.0000f, 0.0000f));
        PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9000f, 15.0000f, 0.0000f));
        PartDefinition leftThigh = left_leg.addOrReplaceChild("leftThigh", CubeListBuilder.create().mirror().texOffs(0, 54).addBox(-2.0000f, 0.0000f, -2.0000f, 4.0000f, 6.0000f, 4.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, -0.7854f, -0.6545f, 0.0000f));
        PartDefinition calfLeft = leftThigh.addOrReplaceChild("calfLeft", CubeListBuilder.create().mirror().texOffs(52, 6).addBox(-2.0000f, 0.0000f, 0.0000f, 4.0000f, 6.0000f, 4.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 6.0000f, -2.0000f, 0.7854f, 0.0000f, 0.0000f));
        PartDefinition tail0 = partdefinition.addOrReplaceChild("tail0", CubeListBuilder.create(), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition tail00 = partdefinition.addOrReplaceChild("tail00", CubeListBuilder.create().texOffs(32, 56).addBox(-2.0000f, -0.5000f, 0.0000f, 4.0000f, 1.0000f, 4.0000f, new CubeDeformation(0.0000f)), PartPose.offset(0.0000f, 15.0000f, 0.0000f));

        // The tail, transcribed from the loop the converter stops at. Link 0 hangs off the
        // root at the base of the shell; every later link hangs off the one in front, so a
        // rotation anywhere along it carries the rest of the tail with it.
        PartDefinition link = partdefinition.addOrReplaceChild("tail0",
                CubeListBuilder.create().texOffs(32, 56)
                        .addBox(-2.0f, -0.5f, 0.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 15.0f, 0.0f));
        for (int i = 1; i < TAIL_SEGMENTS; i++) {
            link = link.addOrReplaceChild("tail" + i,
                    CubeListBuilder.create().texOffs(32, 56)
                            .addBox(-2.0f, -0.5f, 0.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.0f)),
                    PartPose.offset(0.0f, 0.0f, SEGMENT_LENGTH));

            // The fin sitting on top of each link, and the two diamond spikes on it.
            PartDefinition fin = link.addOrReplaceChild("fin" + i,
                    CubeListBuilder.create().texOffs(58, 58)
                            .addBox(-2.0f, -0.5f, 0.0f, 4.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)),
                    PartPose.offsetAndRotation(0.0f, 0.0f, SEGMENT_LENGTH, 0.2618f, 0.0f, 0.0f));
            fin.addOrReplaceChild("spikeUpper" + i, CubeListBuilder.create(),
                            PartPose.offsetAndRotation(0.0f, 0.5f, 2.0f, 0.2618f, 0.0f, 0.0f))
                    .addOrReplaceChild("spikeUpperTip" + i,
                            CubeListBuilder.create().texOffs(56, 50)
                                    .addBox(-1.5f, 0.0f, -1.5f, 3.0f, 1.0f, 3.0f, new CubeDeformation(0.0f)),
                            PartPose.offsetAndRotation(0.0f, -1.0f, 0.0f, 0.0f, 0.7854f, 0.0f));
            fin.addOrReplaceChild("spikeLower" + i, CubeListBuilder.create(),
                            PartPose.offsetAndRotation(0.0f, -0.5f, 2.0f, -0.2618f, 0.0f, 0.0f))
                    .addOrReplaceChild("spikeLowerTip" + i,
                            CubeListBuilder.create().texOffs(60, 54)
                                    .addBox(-1.5f, -1.0f, -1.5f, 3.0f, 1.0f, 3.0f, new CubeDeformation(0.0f)),
                            PartPose.offsetAndRotation(0.0f, 1.0f, 0.0f, 0.0f, 0.7854f, 0.0f));
        }

        return LayerDefinition.create(mesh, 128, 128);
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
