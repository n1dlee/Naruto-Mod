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
 * Geometry imported from the 1.12.2 mod's Bijudama.
 * Machine-converted from bytecode: box coordinates and pivots are the originals,
 * so this model shares their +Y-downward authoring convention.
 */
public class BijudamaModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(NarutoMod.MOD_ID, "bijudama"), "main");

    private final ModelPart root;

    public BijudamaModel(ModelPart root) {
        super(net.minecraft.client.renderer.RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partdefinition = mesh.getRoot();
        PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create(), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition hexadecagon = bb_main.addOrReplaceChild("hexadecagon", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition hexadecagon_r1 = hexadecagon.addOrReplaceChild("hexadecagon_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.9635f));
        PartDefinition hexadecagon_r2 = hexadecagon.addOrReplaceChild("hexadecagon_r2", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.5708f));
        PartDefinition hexadecagon_r3 = hexadecagon.addOrReplaceChild("hexadecagon_r3", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.1781f));
        PartDefinition hexadecagon_r4 = hexadecagon.addOrReplaceChild("hexadecagon_r4", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.7854f));
        PartDefinition hexadecagon_r5 = hexadecagon.addOrReplaceChild("hexadecagon_r5", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.3927f));
        PartDefinition hexadecagon_r6 = hexadecagon.addOrReplaceChild("hexadecagon_r6", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.3927f));
        PartDefinition hexadecagon_r7 = hexadecagon.addOrReplaceChild("hexadecagon_r7", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.7854f));
        PartDefinition hexadecagon2 = bb_main.addOrReplaceChild("hexadecagon2", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.3927f, 0.0000f));
        PartDefinition hexadecagon_r8 = hexadecagon2.addOrReplaceChild("hexadecagon_r8", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.9635f));
        PartDefinition hexadecagon_r9 = hexadecagon2.addOrReplaceChild("hexadecagon_r9", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.5708f));
        PartDefinition hexadecagon_r10 = hexadecagon2.addOrReplaceChild("hexadecagon_r10", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.1781f));
        PartDefinition hexadecagon_r11 = hexadecagon2.addOrReplaceChild("hexadecagon_r11", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.7854f));
        PartDefinition hexadecagon_r12 = hexadecagon2.addOrReplaceChild("hexadecagon_r12", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.3927f));
        PartDefinition hexadecagon_r13 = hexadecagon2.addOrReplaceChild("hexadecagon_r13", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.3927f));
        PartDefinition hexadecagon_r14 = hexadecagon2.addOrReplaceChild("hexadecagon_r14", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.7854f));
        PartDefinition hexadecagon3 = bb_main.addOrReplaceChild("hexadecagon3", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.7854f, 0.0000f));
        PartDefinition hexadecagon_r15 = hexadecagon3.addOrReplaceChild("hexadecagon_r15", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.9635f));
        PartDefinition hexadecagon_r16 = hexadecagon3.addOrReplaceChild("hexadecagon_r16", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.5708f));
        PartDefinition hexadecagon_r17 = hexadecagon3.addOrReplaceChild("hexadecagon_r17", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.1781f));
        PartDefinition hexadecagon_r18 = hexadecagon3.addOrReplaceChild("hexadecagon_r18", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.7854f));
        PartDefinition hexadecagon_r19 = hexadecagon3.addOrReplaceChild("hexadecagon_r19", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.3927f));
        PartDefinition hexadecagon_r20 = hexadecagon3.addOrReplaceChild("hexadecagon_r20", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.3927f));
        PartDefinition hexadecagon_r21 = hexadecagon3.addOrReplaceChild("hexadecagon_r21", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.7854f));
        PartDefinition hexadecagon4 = bb_main.addOrReplaceChild("hexadecagon4", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.1781f, 0.0000f));
        PartDefinition hexadecagon_r22 = hexadecagon4.addOrReplaceChild("hexadecagon_r22", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.9635f));
        PartDefinition hexadecagon_r23 = hexadecagon4.addOrReplaceChild("hexadecagon_r23", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.5708f));
        PartDefinition hexadecagon_r24 = hexadecagon4.addOrReplaceChild("hexadecagon_r24", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.1781f));
        PartDefinition hexadecagon_r25 = hexadecagon4.addOrReplaceChild("hexadecagon_r25", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.7854f));
        PartDefinition hexadecagon_r26 = hexadecagon4.addOrReplaceChild("hexadecagon_r26", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.3927f));
        PartDefinition hexadecagon_r27 = hexadecagon4.addOrReplaceChild("hexadecagon_r27", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.3927f));
        PartDefinition hexadecagon_r28 = hexadecagon4.addOrReplaceChild("hexadecagon_r28", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.7854f));
        PartDefinition hexadecagon5 = bb_main.addOrReplaceChild("hexadecagon5", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.5708f, 0.0000f));
        PartDefinition hexadecagon_r29 = hexadecagon5.addOrReplaceChild("hexadecagon_r29", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.9635f));
        PartDefinition hexadecagon_r30 = hexadecagon5.addOrReplaceChild("hexadecagon_r30", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.5708f));
        PartDefinition hexadecagon_r31 = hexadecagon5.addOrReplaceChild("hexadecagon_r31", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.1781f));
        PartDefinition hexadecagon_r32 = hexadecagon5.addOrReplaceChild("hexadecagon_r32", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.7854f));
        PartDefinition hexadecagon_r33 = hexadecagon5.addOrReplaceChild("hexadecagon_r33", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.3927f));
        PartDefinition hexadecagon_r34 = hexadecagon5.addOrReplaceChild("hexadecagon_r34", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.3927f));
        PartDefinition hexadecagon_r35 = hexadecagon5.addOrReplaceChild("hexadecagon_r35", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.7854f));
        PartDefinition hexadecagon6 = bb_main.addOrReplaceChild("hexadecagon6", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, -1.9635f, 0.0000f));
        PartDefinition hexadecagon_r36 = hexadecagon6.addOrReplaceChild("hexadecagon_r36", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.9635f));
        PartDefinition hexadecagon_r37 = hexadecagon6.addOrReplaceChild("hexadecagon_r37", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.5708f));
        PartDefinition hexadecagon_r38 = hexadecagon6.addOrReplaceChild("hexadecagon_r38", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.1781f));
        PartDefinition hexadecagon_r39 = hexadecagon6.addOrReplaceChild("hexadecagon_r39", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.7854f));
        PartDefinition hexadecagon_r40 = hexadecagon6.addOrReplaceChild("hexadecagon_r40", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.3927f));
        PartDefinition hexadecagon_r41 = hexadecagon6.addOrReplaceChild("hexadecagon_r41", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.3927f));
        PartDefinition hexadecagon_r42 = hexadecagon6.addOrReplaceChild("hexadecagon_r42", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.7854f));
        PartDefinition hexadecagon7 = bb_main.addOrReplaceChild("hexadecagon7", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, -2.3562f, 0.0000f));
        PartDefinition hexadecagon_r43 = hexadecagon7.addOrReplaceChild("hexadecagon_r43", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.9635f));
        PartDefinition hexadecagon_r44 = hexadecagon7.addOrReplaceChild("hexadecagon_r44", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.5708f));
        PartDefinition hexadecagon_r45 = hexadecagon7.addOrReplaceChild("hexadecagon_r45", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.1781f));
        PartDefinition hexadecagon_r46 = hexadecagon7.addOrReplaceChild("hexadecagon_r46", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.7854f));
        PartDefinition hexadecagon_r47 = hexadecagon7.addOrReplaceChild("hexadecagon_r47", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.3927f));
        PartDefinition hexadecagon_r48 = hexadecagon7.addOrReplaceChild("hexadecagon_r48", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.3927f));
        PartDefinition hexadecagon_r49 = hexadecagon7.addOrReplaceChild("hexadecagon_r49", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.7854f));
        PartDefinition hexadecagon8 = bb_main.addOrReplaceChild("hexadecagon8", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, -2.7489f, 0.0000f));
        PartDefinition hexadecagon_r50 = hexadecagon8.addOrReplaceChild("hexadecagon_r50", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.9635f));
        PartDefinition hexadecagon_r51 = hexadecagon8.addOrReplaceChild("hexadecagon_r51", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.5708f));
        PartDefinition hexadecagon_r52 = hexadecagon8.addOrReplaceChild("hexadecagon_r52", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 1.1781f));
        PartDefinition hexadecagon_r53 = hexadecagon8.addOrReplaceChild("hexadecagon_r53", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.7854f));
        PartDefinition hexadecagon_r54 = hexadecagon8.addOrReplaceChild("hexadecagon_r54", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.3927f));
        PartDefinition hexadecagon_r55 = hexadecagon8.addOrReplaceChild("hexadecagon_r55", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.3927f));
        PartDefinition hexadecagon_r56 = hexadecagon8.addOrReplaceChild("hexadecagon_r56", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5027f, -2.5000f, -0.5000f, 1.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.7854f));
        return LayerDefinition.create(mesh, 4, 4);
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
