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
 * Geometry imported from the 1.12.2 mod's SixTails.
 * Machine-converted from bytecode: box coordinates and pivots are the originals,
 * so this model shares their +Y-downward authoring convention.
 */
public class SixTailsModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(NarutoMod.MOD_ID, "six_tails"), "main");

    private final ModelPart root;

    public SixTailsModel(ModelPart root) {
        super(net.minecraft.client.renderer.RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partdefinition = mesh.getRoot();
        PartDefinition rand = partdefinition.addOrReplaceChild("rand", CubeListBuilder.create(), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition field_78115_e = partdefinition.addOrReplaceChild("field_78115_e", CubeListBuilder.create().texOffs(17, 16).addBox(-3.0000f, -6.7456f, -4.9001f, 6.0000f, 4.0000f, 5.0000f, new CubeDeformation(0.0000f)), PartPose.offset(0.0000f, 22.7500f, 3.0000f));
        PartDefinition cube_r1 = field_78115_e.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5000f, -2.5000f, -3.6000f, 7.0000f, 5.0000f, 6.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -1.2500f, -2.4000f, -0.0873f, 0.0000f, 0.0000f));
        PartDefinition cube_r2 = field_78115_e.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 11).addBox(-3.0000f, -3.6000f, -2.5000f, 6.0000f, 5.0000f, 5.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(0.0000f, -6.7456f, -2.4001f, 0.1309f, 0.0000f, 0.0000f));
        PartDefinition field_78116_c = field_78115_e.addOrReplaceChild("field_78116_c", CubeListBuilder.create().texOffs(0, 22).addBox(-3.0000f, -4.8000f, -2.1000f, 6.0000f, 5.0000f, 3.0000f, new CubeDeformation(0.0000f)).texOffs(20, 0).addBox(-3.0000f, -4.8000f, -3.1000f, 6.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)).texOffs(17, 11).addBox(-3.0000f, -3.4000f, -4.6000f, 6.0000f, 3.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offset(0.0000f, -8.7500f, -2.0000f));
        PartDefinition cube_r3 = field_78116_c.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(31, 9).addBox(-3.0000f, -0.9000f, -0.4000f, 6.0000f, 2.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -3.8499f, -3.4747f, -0.8290f, 0.0000f, 0.0000f));
        PartDefinition cube_r4 = field_78116_c.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(26, 6).addBox(-3.0000f, -1.0000f, -0.7000f, 6.0000f, 2.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -0.4907f, -3.3866f, 1.0472f, 0.0000f, 0.0000f));
        PartDefinition cube_r5 = field_78116_c.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(18, 25).addBox(-3.0000f, -3.7000f, -0.3000f, 6.0000f, 5.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -1.0169f, 0.9617f, 0.1745f, 0.0000f, 0.0000f));
        PartDefinition horn00 = field_78116_c.addOrReplaceChild("horn00", CubeListBuilder.create().texOffs(0, 0).addBox(0.0000f, -0.4500f, -0.5000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(-2.5000f, -4.7500f, -2.5000f, 0.0000f, 0.0000f, -0.4363f));
        PartDefinition horn01 = horn00.addOrReplaceChild("horn01", CubeListBuilder.create().texOffs(0, 0).addBox(0.0000f, -0.7500f, -0.5000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.0500f)), PartPose.offsetAndRotation(0.0000f, -0.2500f, 0.0000f, 0.0000f, 0.0000f, 0.0873f));
        PartDefinition horn02 = horn01.addOrReplaceChild("horn02", CubeListBuilder.create().texOffs(0, 0).addBox(0.0000f, -0.7500f, -0.5000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.1000f)), PartPose.offsetAndRotation(0.0000f, -0.5000f, 0.0000f, 0.0873f, 0.0000f, 0.0000f));
        PartDefinition horn03 = horn02.addOrReplaceChild("horn03", CubeListBuilder.create().texOffs(0, 0).addBox(0.0000f, -0.7500f, -0.5000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.1500f)), PartPose.offsetAndRotation(0.0000f, -0.5000f, 0.0000f, 0.0873f, 0.0000f, 0.0000f));
        PartDefinition horn04 = horn03.addOrReplaceChild("horn04", CubeListBuilder.create().texOffs(0, 2).addBox(0.0000f, -0.7500f, -0.5000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.0500f)), PartPose.offsetAndRotation(0.0000f, -0.6499f, 0.0253f, 0.0000f, 0.0000f, -0.0873f));
        PartDefinition horn10 = field_78116_c.addOrReplaceChild("horn10", CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-1.0000f, -0.4500f, -0.5000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(2.5000f, -4.7500f, -2.5000f, 0.0000f, 0.0000f, 0.4363f));
        PartDefinition horn11 = horn10.addOrReplaceChild("horn11", CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-1.0000f, -0.7500f, -0.5000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.0500f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -0.2500f, 0.0000f, 0.0000f, 0.0000f, -0.0873f));
        PartDefinition horn12 = horn11.addOrReplaceChild("horn12", CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-1.0000f, -0.7500f, -0.5000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.1000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -0.5000f, 0.0000f, -0.0873f, 0.0000f, 0.0000f));
        PartDefinition horn13 = horn12.addOrReplaceChild("horn13", CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-1.0000f, -0.7500f, -0.5000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.1500f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -0.5000f, 0.0000f, -0.0873f, 0.0000f, 0.0000f));
        PartDefinition horn14 = horn13.addOrReplaceChild("horn14", CubeListBuilder.create().mirror().texOffs(0, 2).addBox(-1.0000f, -0.7500f, -0.5000f, 1.0000f, 1.0000f, 1.0000f, new CubeDeformation(-0.0500f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -0.6499f, 0.0253f, 0.0000f, 0.0000f, 0.0873f));
        PartDefinition field_178723_h = field_78115_e.addOrReplaceChild("field_178723_h", CubeListBuilder.create(), PartPose.offset(-3.0000f, -6.0000f, -1.5000f));
        PartDefinition bone = field_178723_h.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.3491f, 0.0000f));
        PartDefinition cube_r6 = bone.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(12, 31).addBox(-1.0000f, -0.9000f, -1.5000f, 2.0000f, 2.0000f, 3.0000f, new CubeDeformation(-0.4000f)), PartPose.offsetAndRotation(-0.2870f, 0.2867f, -1.7153f, 0.2618f, -0.5672f, 0.0000f));
        PartDefinition cube_r7 = bone.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(30, 35).addBox(-0.8000f, -1.2000f, -1.2000f, 2.0000f, 2.0000f, 2.0000f, new CubeDeformation(-0.4000f)), PartPose.offsetAndRotation(-0.9000f, 0.2500f, -0.3000f, 0.1309f, 0.0000f, 0.0000f));
        PartDefinition cube_r8 = bone.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(34, 16).addBox(-1.1000f, -1.0000f, -1.5000f, 2.0000f, 2.0000f, 2.0000f, new CubeDeformation(-0.4000f)), PartPose.offsetAndRotation(-0.5869f, -0.0397f, 0.4300f, 0.1309f, -0.5672f, -0.0873f));
        PartDefinition field_178724_i = field_78115_e.addOrReplaceChild("field_178724_i", CubeListBuilder.create(), PartPose.offset(3.0000f, -6.0000f, -1.5000f));
        PartDefinition bone5 = field_178724_i.addOrReplaceChild("bone5", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.3491f, 0.0000f));
        PartDefinition cube_r9 = bone5.addOrReplaceChild("cube_r9", CubeListBuilder.create().mirror().texOffs(12, 31).addBox(-1.0000f, -0.9000f, -1.5000f, 2.0000f, 2.0000f, 3.0000f, new CubeDeformation(-0.4000f)).mirror(false), PartPose.offsetAndRotation(0.2870f, 0.2867f, -1.7153f, 0.2618f, 0.5672f, 0.0000f));
        PartDefinition cube_r10 = bone5.addOrReplaceChild("cube_r10", CubeListBuilder.create().mirror().texOffs(30, 35).addBox(-1.2000f, -1.2000f, -1.2000f, 2.0000f, 2.0000f, 2.0000f, new CubeDeformation(-0.4000f)).mirror(false), PartPose.offsetAndRotation(0.9000f, 0.2500f, -0.3000f, 0.1309f, 0.0000f, 0.0000f));
        PartDefinition cube_r11 = bone5.addOrReplaceChild("cube_r11", CubeListBuilder.create().mirror().texOffs(34, 16).addBox(-0.9000f, -1.0000f, -1.5000f, 2.0000f, 2.0000f, 2.0000f, new CubeDeformation(-0.4000f)).mirror(false), PartPose.offsetAndRotation(0.5869f, -0.0397f, 0.4300f, 0.1309f, 0.5672f, 0.0873f));
        PartDefinition field_178721_j = field_78115_e.addOrReplaceChild("field_178721_j", CubeListBuilder.create(), PartPose.offset(-2.7500f, -3.7500f, -3.0000f));
        PartDefinition cube_r12 = field_178721_j.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(22, 32).addBox(-1.1000f, -0.5000f, -1.5000f, 2.0000f, 1.0000f, 3.0000f, new CubeDeformation(-0.1000f)), PartPose.offsetAndRotation(-3.0464f, 4.5000f, -0.8128f, 0.0000f, -0.2618f, 0.0000f));
        PartDefinition cube_r13 = field_178721_j.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(33, 12).addBox(-0.5000f, -0.5000f, -1.5000f, 1.0000f, 1.0000f, 3.0000f, new CubeDeformation(-0.1000f)), PartPose.offsetAndRotation(-3.1083f, 4.1510f, -0.8684f, -0.1745f, -0.1745f, 0.7418f));
        PartDefinition RightLeg1 = field_178721_j.addOrReplaceChild("RightLeg1", CubeListBuilder.create(), PartPose.offsetAndRotation(0.5000f, 3.6667f, 2.5333f, 0.4800f, -0.2618f, 0.0436f));
        PartDefinition cube_r14 = RightLeg1.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(29, 28).addBox(-1.0000f, -2.5000f, -1.4000f, 3.0000f, 4.0000f, 3.0000f, new CubeDeformation(-0.1000f)), PartPose.offsetAndRotation(-2.7866f, -3.3303f, -0.9362f, -0.3927f, 0.3491f, 0.6545f));
        PartDefinition cube_r15 = RightLeg1.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(0, 30).addBox(-1.5000f, -1.7000f, -2.0000f, 3.0000f, 3.0000f, 3.0000f, new CubeDeformation(-0.1000f)), PartPose.offsetAndRotation(-3.0000f, -1.2667f, -1.4333f, -0.5236f, 0.0000f, 0.0000f));
        PartDefinition Legdetail = RightLeg1.addOrReplaceChild("Legdetail", CubeListBuilder.create(), PartPose.offset(-3.0000f, -1.2667f, -1.4333f));
        PartDefinition field_178722_k = field_78115_e.addOrReplaceChild("field_178722_k", CubeListBuilder.create(), PartPose.offset(2.7500f, -3.7500f, -3.0000f));
        PartDefinition cube_r16 = field_178722_k.addOrReplaceChild("cube_r16", CubeListBuilder.create().mirror().texOffs(22, 32).addBox(-0.9000f, -0.5000f, -1.5000f, 2.0000f, 1.0000f, 3.0000f, new CubeDeformation(-0.1000f)).mirror(false), PartPose.offsetAndRotation(3.0464f, 4.5000f, -0.8128f, 0.0000f, 0.2618f, 0.0000f));
        PartDefinition cube_r17 = field_178722_k.addOrReplaceChild("cube_r17", CubeListBuilder.create().mirror().texOffs(33, 12).addBox(-0.5000f, -0.5000f, -1.5000f, 1.0000f, 1.0000f, 3.0000f, new CubeDeformation(-0.1000f)).mirror(false), PartPose.offsetAndRotation(3.1083f, 4.1510f, -0.8684f, -0.1745f, 0.1745f, -0.7418f));
        PartDefinition RightLeg4 = field_178722_k.addOrReplaceChild("RightLeg4", CubeListBuilder.create(), PartPose.offsetAndRotation(-0.5000f, 3.6667f, 2.5333f, 0.4800f, 0.2618f, -0.0436f));
        PartDefinition cube_r18 = RightLeg4.addOrReplaceChild("cube_r18", CubeListBuilder.create().mirror().texOffs(29, 28).addBox(-2.0000f, -2.5000f, -1.4000f, 3.0000f, 4.0000f, 3.0000f, new CubeDeformation(-0.1000f)).mirror(false), PartPose.offsetAndRotation(2.7866f, -3.3303f, -0.9362f, -0.3927f, -0.3491f, -0.6545f));
        PartDefinition cube_r19 = RightLeg4.addOrReplaceChild("cube_r19", CubeListBuilder.create().mirror().texOffs(0, 30).addBox(-1.5000f, -1.7000f, -2.0000f, 3.0000f, 3.0000f, 3.0000f, new CubeDeformation(-0.1000f)).mirror(false), PartPose.offsetAndRotation(3.0000f, -1.2667f, -1.4333f, -0.5236f, 0.0000f, 0.0000f));
        PartDefinition Legdetail2 = RightLeg4.addOrReplaceChild("Legdetail2", CubeListBuilder.create(), PartPose.offset(3.0000f, -1.2667f, -1.4333f));
        PartDefinition horn0 = partdefinition.addOrReplaceChild("horn0", CubeListBuilder.create(), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition horn1 = partdefinition.addOrReplaceChild("horn1", CubeListBuilder.create(), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition tails = partdefinition.addOrReplaceChild("tails", CubeListBuilder.create(), PartPose.offset(0.0000f, 22.7500f, 3.0000f));
        PartDefinition tail00 = tails.addOrReplaceChild("tail00", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.2000f)), PartPose.offsetAndRotation(1.2500f, 0.0000f, 0.0000f, -1.2217f, 1.3090f, 0.0000f));
        PartDefinition tail01 = tail00.addOrReplaceChild("tail01", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.1500f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail02 = tail01.addOrReplaceChild("tail02", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail03 = tail02.addOrReplaceChild("tail03", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0500f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail04 = tail03.addOrReplaceChild("tail04", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail05 = tail04.addOrReplaceChild("tail05", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail10 = tails.addOrReplaceChild("tail10", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.2000f)), PartPose.offsetAndRotation(0.7500f, 0.0000f, 0.0000f, -0.7854f, 0.7854f, 0.0000f));
        PartDefinition tail11 = tail10.addOrReplaceChild("tail11", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.1500f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail12 = tail11.addOrReplaceChild("tail12", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail13 = tail12.addOrReplaceChild("tail13", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0500f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail14 = tail13.addOrReplaceChild("tail14", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, -0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail15 = tail14.addOrReplaceChild("tail15", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, -0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail20 = tails.addOrReplaceChild("tail20", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.2000f)), PartPose.offsetAndRotation(0.2500f, 0.0000f, 0.0000f, -1.0472f, 0.2618f, 0.0000f));
        PartDefinition tail21 = tail20.addOrReplaceChild("tail21", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.1500f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail22 = tail21.addOrReplaceChild("tail22", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail23 = tail22.addOrReplaceChild("tail23", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0500f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail24 = tail23.addOrReplaceChild("tail24", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail25 = tail24.addOrReplaceChild("tail25", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, -0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail30 = tails.addOrReplaceChild("tail30", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.2000f)), PartPose.offsetAndRotation(-0.2500f, 0.0000f, 0.0000f, -0.7854f, -0.2618f, 0.0000f));
        PartDefinition tail31 = tail30.addOrReplaceChild("tail31", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.1500f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, -0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail32 = tail31.addOrReplaceChild("tail32", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail33 = tail32.addOrReplaceChild("tail33", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0500f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail34 = tail33.addOrReplaceChild("tail34", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail35 = tail34.addOrReplaceChild("tail35", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, -0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail40 = tails.addOrReplaceChild("tail40", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.2000f)), PartPose.offsetAndRotation(-0.7500f, 0.0000f, 0.0000f, -0.8727f, -0.7854f, 0.0000f));
        PartDefinition tail41 = tail40.addOrReplaceChild("tail41", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.1500f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail42 = tail41.addOrReplaceChild("tail42", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail43 = tail42.addOrReplaceChild("tail43", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0500f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, -0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail44 = tail43.addOrReplaceChild("tail44", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, -0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail45 = tail44.addOrReplaceChild("tail45", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, -0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail50 = tails.addOrReplaceChild("tail50", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.2000f)), PartPose.offsetAndRotation(-1.2500f, 0.0000f, 0.0000f, -1.2217f, -1.3090f, 0.0000f));
        PartDefinition tail51 = tail50.addOrReplaceChild("tail51", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.1500f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail52 = tail51.addOrReplaceChild("tail52", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail53 = tail52.addOrReplaceChild("tail53", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0500f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail54 = tail53.addOrReplaceChild("tail54", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, -0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail55 = tail54.addOrReplaceChild("tail55", CubeListBuilder.create().texOffs(34, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -3.0000f, 0.0000f, -0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail0 = partdefinition.addOrReplaceChild("tail0", CubeListBuilder.create(), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition tail1 = partdefinition.addOrReplaceChild("tail1", CubeListBuilder.create(), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition tail2 = partdefinition.addOrReplaceChild("tail2", CubeListBuilder.create(), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition tail3 = partdefinition.addOrReplaceChild("tail3", CubeListBuilder.create(), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition tail4 = partdefinition.addOrReplaceChild("tail4", CubeListBuilder.create(), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition tail5 = partdefinition.addOrReplaceChild("tail5", CubeListBuilder.create(), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
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
