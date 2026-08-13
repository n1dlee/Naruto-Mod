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
 * Geometry imported from the 1.12.2 mod's OneTail.
 * Machine-converted from bytecode: box coordinates and pivots are the originals,
 * so this model shares their +Y-downward authoring convention.
 */
public class OneTailModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(NarutoMod.MOD_ID, "one_tail"), "main");

    private final ModelPart root;

    public OneTailModel(ModelPart root) {
        super(net.minecraft.client.renderer.RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partdefinition = mesh.getRoot();
        PartDefinition rand = partdefinition.addOrReplaceChild("rand", CubeListBuilder.create(), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition field_178720_f = partdefinition.addOrReplaceChild("field_178720_f", CubeListBuilder.create(), PartPose.offset(0.0000f, 21.0000f, 3.0000f));
        PartDefinition eyes = field_178720_f.addOrReplaceChild("eyes", CubeListBuilder.create().texOffs(41, 4).addBox(-1.5000f, -3.0000f, -4.7000f, 3.0000f, 1.0000f, 0.0000f, new CubeDeformation(0.0000f)), PartPose.offset(0.0000f, -9.6000f, -7.0000f));
        PartDefinition field_78115_e = partdefinition.addOrReplaceChild("field_78115_e", CubeListBuilder.create(), PartPose.offset(0.0000f, 21.0000f, 3.0000f));
        PartDefinition field_78116_c = field_78115_e.addOrReplaceChild("field_78116_c", CubeListBuilder.create().texOffs(52, 8).addBox(-1.5000f, -0.7200f, -5.4006f, 3.0000f, 0.0000f, 3.0000f, new CubeDeformation(0.0000f)), PartPose.offset(0.0000f, -9.6000f, -7.0000f));
        PartDefinition cube_r1 = field_78116_c.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(58, 2).addBox(-1.0000f, 0.5500f, -0.2921f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(0.0000f, -2.4000f, -5.5000f, 0.3054f, 0.0000f, 0.0000f));
        PartDefinition cube_r2 = field_78116_c.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(48, 0).addBox(-1.5000f, -13.9000f, -2.1000f, 3.0000f, 0.0000f, 2.0000f, new CubeDeformation(0.0000f)).texOffs(48, 0).addBox(-1.5000f, -14.9000f, -2.1000f, 3.0000f, 2.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 12.6000f, -1.5000f, 0.1309f, 0.0000f, 0.0000f));
        PartDefinition cube_r3 = field_78116_c.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 54).addBox(-2.0000f, -3.2623f, -0.9368f, 4.0000f, 4.0000f, 3.0000f, new CubeDeformation(-0.1000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, -2.5000f, -0.1745f, 0.0000f, 0.0000f));
        PartDefinition cube_r4 = field_78116_c.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(50, 36).addBox(-2.0000f, -0.0057f, 0.0000f, 4.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -4.4193f, -0.8247f, -0.3491f, 0.0000f, 0.0000f));
        PartDefinition cube_r5 = field_78116_c.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(32, 35).addBox(-2.0000f, -17.3000f, -1.8000f, 4.0000f, 4.0000f, 4.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 13.0000f, -1.5000f, 0.0873f, 0.0000f, 0.0000f));
        PartDefinition jaw = field_78116_c.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(47, 13).addBox(-1.5000f, 0.0000f, -2.9000f, 3.0000f, 1.0000f, 3.0000f, new CubeDeformation(0.2000f)).texOffs(47, 13).addBox(-1.5000f, 0.3000f, -2.9000f, 3.0000f, 0.0000f, 3.0000f, new CubeDeformation(0.2000f)), PartPose.offsetAndRotation(0.0000f, -0.7346f, -2.3706f, 0.5236f, 0.0000f, 0.0000f));
        PartDefinition bone3 = field_78116_c.addOrReplaceChild("bone3", CubeListBuilder.create().texOffs(58, 0).addBox(-1.1000f, -0.6000f, -0.8000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(-1.1000f, -3.5000f, -4.2000f, 0.3491f, -0.1745f, 0.1745f));
        PartDefinition cube_r6 = bone3.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(0, 6).addBox(-0.5000f, -0.4000f, -0.1000f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(-0.1000f)), PartPose.offsetAndRotation(-0.8000f, -0.3000f, 0.1000f, 0.0000f, -0.5236f, 0.0000f));
        PartDefinition bone4 = field_78116_c.addOrReplaceChild("bone4", CubeListBuilder.create().mirror().texOffs(58, 0).addBox(-0.9000f, -0.6000f, -0.8000f, 2.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(1.1000f, -3.5000f, -4.2000f, 0.3491f, 0.1745f, -0.1745f));
        PartDefinition cube_r7 = bone4.addOrReplaceChild("cube_r7", CubeListBuilder.create().mirror().texOffs(0, 6).addBox(-0.5000f, -0.4000f, -0.1000f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(-0.1000f)).mirror(false), PartPose.offsetAndRotation(0.8000f, -0.3000f, 0.1000f, 0.0000f, 0.5236f, 0.0000f));
        PartDefinition field_178723_h = field_78115_e.addOrReplaceChild("field_178723_h", CubeListBuilder.create(), PartPose.offset(-4.0000f, -9.0000f, -4.5000f));
        PartDefinition rightArm = field_178723_h.addOrReplaceChild("rightArm", CubeListBuilder.create(), PartPose.offsetAndRotation(4.0000f, -2.0000f, -1.0000f, -0.2182f, 0.0000f, 0.0873f));
        PartDefinition cube_r8 = rightArm.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(20, 39).addBox(-0.5000f, -2.2500f, -2.0000f, 2.0000f, 3.0000f, 4.0000f, new CubeDeformation(0.5000f)), PartPose.offsetAndRotation(-5.4741f, 1.5764f, -0.1412f, 0.1309f, 0.0000f, 0.7854f));
        PartDefinition cube_r9 = rightArm.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(10, 37).addBox(-5.9032f, 2.6821f, -1.9955f, 2.0000f, 6.0000f, 3.0000f, new CubeDeformation(0.5000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.1309f, 0.0000f, 0.2182f));
        PartDefinition cube_r10 = rightArm.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(0, 37).addBox(-7.5298f, 0.4835f, -2.5163f, 2.0000f, 7.0000f, 3.0000f, new CubeDeformation(0.6000f)), PartPose.offsetAndRotation(0.0000f, 7.0000f, 2.0000f, -0.2618f, -0.0436f, 0.0000f));
        PartDefinition bone = rightArm.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offsetAndRotation(-5.3500f, 9.2500f, -1.7500f, 0.0000f, -0.3054f, 0.0000f));
        PartDefinition cube_r11 = bone.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(0, 17).addBox(-0.0290f, -1.1225f, -3.7507f, 1.0000f, 1.0000f, 3.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(-0.6000f, 3.7500f, 0.7500f, 0.8026f, -0.3011f, -0.4101f));
        PartDefinition cube_r12 = bone.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(0, 17).addBox(-1.9419f, -1.0421f, -4.6535f, 1.0000f, 1.0000f, 3.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(-0.4000f, 3.7500f, 0.7500f, 0.6953f, 0.3893f, 0.1347f));
        PartDefinition cube_r13 = bone.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(0, 17).addBox(-0.7793f, -1.0421f, -4.4548f, 1.0000f, 1.0000f, 3.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(-0.6000f, 3.7500f, 0.7500f, 0.6525f, 0.2185f, -0.0078f));
        PartDefinition cube_r14 = bone.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(0, 17).addBox(0.3597f, -1.0421f, -4.5360f, 1.0000f, 1.0000f, 3.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(-0.6000f, 3.7500f, 0.7500f, 0.6392f, 0.1139f, -0.0876f));
        PartDefinition field_178724_i = field_78115_e.addOrReplaceChild("field_178724_i", CubeListBuilder.create(), PartPose.offset(4.0000f, -9.0000f, -4.5000f));
        PartDefinition leftArm = field_178724_i.addOrReplaceChild("leftArm", CubeListBuilder.create(), PartPose.offsetAndRotation(-4.0000f, -2.0000f, -1.0000f, -0.2182f, 0.0000f, -0.0873f));
        PartDefinition cube_r15 = leftArm.addOrReplaceChild("cube_r15", CubeListBuilder.create().mirror().texOffs(20, 39).addBox(-1.5000f, -2.2500f, -2.0000f, 2.0000f, 3.0000f, 4.0000f, new CubeDeformation(0.5000f)).mirror(false), PartPose.offsetAndRotation(5.4741f, 1.5764f, -0.1412f, 0.1309f, 0.0000f, -0.7854f));
        PartDefinition cube_r16 = leftArm.addOrReplaceChild("cube_r16", CubeListBuilder.create().mirror().texOffs(10, 37).addBox(3.9032f, 2.6821f, -1.9955f, 2.0000f, 6.0000f, 3.0000f, new CubeDeformation(0.5000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.1309f, 0.0000f, -0.2182f));
        PartDefinition cube_r17 = leftArm.addOrReplaceChild("cube_r17", CubeListBuilder.create().mirror().texOffs(0, 37).addBox(5.5298f, 0.4835f, -2.5163f, 2.0000f, 7.0000f, 3.0000f, new CubeDeformation(0.6000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 7.0000f, 2.0000f, -0.2618f, 0.0436f, 0.0000f));
        PartDefinition bone2 = leftArm.addOrReplaceChild("bone2", CubeListBuilder.create(), PartPose.offsetAndRotation(5.3500f, 9.2500f, -1.7500f, 0.0000f, 0.3054f, 0.0000f));
        PartDefinition cube_r18 = bone2.addOrReplaceChild("cube_r18", CubeListBuilder.create().mirror().texOffs(0, 17).addBox(-0.9710f, -1.1225f, -3.7507f, 1.0000f, 1.0000f, 3.0000f, new CubeDeformation(0.1000f)).mirror(false), PartPose.offsetAndRotation(0.6000f, 3.7500f, 0.7500f, 0.8026f, 0.3011f, 0.4101f));
        PartDefinition cube_r19 = bone2.addOrReplaceChild("cube_r19", CubeListBuilder.create().mirror().texOffs(0, 17).addBox(0.9419f, -1.0421f, -4.6535f, 1.0000f, 1.0000f, 3.0000f, new CubeDeformation(0.1000f)).mirror(false), PartPose.offsetAndRotation(0.4000f, 3.7500f, 0.7500f, 0.6953f, -0.3893f, -0.1347f));
        PartDefinition cube_r20 = bone2.addOrReplaceChild("cube_r20", CubeListBuilder.create().mirror().texOffs(0, 17).addBox(-0.2207f, -1.0421f, -4.4548f, 1.0000f, 1.0000f, 3.0000f, new CubeDeformation(0.1000f)).mirror(false), PartPose.offsetAndRotation(0.6000f, 3.7500f, 0.7500f, 0.6525f, -0.2185f, 0.0078f));
        PartDefinition cube_r21 = bone2.addOrReplaceChild("cube_r21", CubeListBuilder.create().mirror().texOffs(0, 17).addBox(-1.3597f, -1.0421f, -4.5360f, 1.0000f, 1.0000f, 3.0000f, new CubeDeformation(0.1000f)).mirror(false), PartPose.offsetAndRotation(0.6000f, 3.7500f, 0.7500f, 0.6392f, -0.1139f, 0.0876f));
        PartDefinition stomach = field_78115_e.addOrReplaceChild("stomach", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000f, 3.0000f, -6.6000f, -0.2618f, 0.0000f, 0.0000f));
        PartDefinition cube_r22 = stomach.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs(36, 8).addBox(-3.0000f, 5.0769f, -1.8502f, 6.0000f, 7.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -12.4000f, -3.7000f, 0.2182f, 0.0000f, 0.0000f));
        PartDefinition cube_r23 = stomach.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs(0, 0).addBox(-9.0000f, -3.7037f, -4.4076f, 9.0000f, 8.0000f, 9.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(4.5000f, -5.5000f, 1.2000f, 0.2182f, 0.0000f, 0.0000f));
        PartDefinition upperbody = field_78115_e.addOrReplaceChild("upperbody", CubeListBuilder.create().texOffs(24, 22).addBox(-4.0000f, -14.8158f, -3.1398f, 8.0000f, 3.0000f, 8.0000f, new CubeDeformation(0.1000f)), PartPose.offset(0.0000f, 3.0000f, -5.5000f));
        PartDefinition cube_r24 = upperbody.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs(7, 54).addBox(-4.0000f, -1.0000f, -3.3000f, 8.0000f, 3.0000f, 7.0000f, new CubeDeformation(-0.0500f)), PartPose.offsetAndRotation(0.0000f, -15.1581f, 0.4507f, -0.3054f, 0.0000f, 0.0000f));
        PartDefinition cube_r25 = upperbody.addOrReplaceChild("cube_r25", CubeListBuilder.create().texOffs(0, 17).addBox(-4.0000f, -12.0000f, -0.9000f, 8.0000f, 5.0000f, 8.0000f, new CubeDeformation(0.4000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.1745f, 0.0000f, 0.0000f));
        PartDefinition field_178721_j = field_78115_e.addOrReplaceChild("field_178721_j", CubeListBuilder.create(), PartPose.offset(-4.5000f, -3.0000f, -2.7500f));
        PartDefinition cube_r26 = field_178721_j.addOrReplaceChild("cube_r26", CubeListBuilder.create().texOffs(44, 53).addBox(-9.0000f, -6.0000f, -1.0000f, 5.0000f, 6.0000f, 5.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(5.5000f, 6.0000f, -1.7500f, -0.1289f, 0.0227f, 0.1731f));
        PartDefinition rightFoot = field_178721_j.addOrReplaceChild("rightFoot", CubeListBuilder.create(), PartPose.offset(4.7500f, 5.7500f, -1.7500f));
        PartDefinition cube_r27 = rightFoot.addOrReplaceChild("cube_r27", CubeListBuilder.create().texOffs(27, 43).addBox(-8.0000f, -0.7500f, -4.3000f, 1.0000f, 1.0000f, 5.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(0.0000f, -0.1000f, 0.0000f, 0.0894f, 0.2173f, 0.0193f));
        PartDefinition cube_r28 = rightFoot.addOrReplaceChild("cube_r28", CubeListBuilder.create().texOffs(43, 38).addBox(-5.5000f, -0.7500f, -3.5000f, 1.0000f, 1.0000f, 5.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(-1.0000f, -0.1000f, 0.0000f, 0.0880f, 0.1304f, 0.0115f));
        PartDefinition cube_r29 = rightFoot.addOrReplaceChild("cube_r29", CubeListBuilder.create().texOffs(20, 46).addBox(-4.0000f, -0.7500f, -3.2500f, 1.0000f, 1.0000f, 4.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(-1.0000f, -0.1000f, 0.0000f, 0.0876f, 0.0869f, 0.0076f));
        PartDefinition cube_r30 = rightFoot.addOrReplaceChild("cube_r30", CubeListBuilder.create().texOffs(0, 47).addBox(-1.9500f, -0.8000f, -1.1000f, 1.0000f, 1.0000f, 4.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(-1.0000f, -0.1000f, 0.0000f, 0.1526f, -0.4332f, -0.0530f));
        PartDefinition cube_r31 = rightFoot.addOrReplaceChild("cube_r31", CubeListBuilder.create().texOffs(34, 44).addBox(-8.0000f, -1.0000f, 1.3500f, 5.0000f, 1.0000f, 2.0000f, new CubeDeformation(0.2000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, -0.0175f, 0.0000f, 0.0000f));
        PartDefinition field_178722_k = field_78115_e.addOrReplaceChild("field_178722_k", CubeListBuilder.create(), PartPose.offset(4.5000f, -3.0000f, -2.7500f));
        PartDefinition cube_r32 = field_178722_k.addOrReplaceChild("cube_r32", CubeListBuilder.create().mirror().texOffs(44, 53).addBox(4.0000f, -6.0000f, -1.0000f, 5.0000f, 6.0000f, 5.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(-5.5000f, 6.0000f, -1.7500f, -0.1289f, -0.0227f, -0.1731f));
        PartDefinition leftFoot = field_178722_k.addOrReplaceChild("leftFoot", CubeListBuilder.create(), PartPose.offset(-4.7500f, 5.7500f, -1.7500f));
        PartDefinition cube_r33 = leftFoot.addOrReplaceChild("cube_r33", CubeListBuilder.create().mirror().texOffs(27, 43).addBox(7.0000f, -0.7500f, -4.3000f, 1.0000f, 1.0000f, 5.0000f, new CubeDeformation(0.1000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -0.1000f, 0.0000f, 0.0894f, -0.2173f, -0.0193f));
        PartDefinition cube_r34 = leftFoot.addOrReplaceChild("cube_r34", CubeListBuilder.create().mirror().texOffs(43, 38).addBox(4.5000f, -0.7500f, -3.5000f, 1.0000f, 1.0000f, 5.0000f, new CubeDeformation(0.1000f)).mirror(false), PartPose.offsetAndRotation(1.0000f, -0.1000f, 0.0000f, 0.0880f, -0.1304f, -0.0115f));
        PartDefinition cube_r35 = leftFoot.addOrReplaceChild("cube_r35", CubeListBuilder.create().mirror().texOffs(20, 46).addBox(3.0000f, -0.7500f, -3.2500f, 1.0000f, 1.0000f, 4.0000f, new CubeDeformation(0.1000f)).mirror(false), PartPose.offsetAndRotation(1.0000f, -0.1000f, 0.0000f, 0.0876f, -0.0869f, -0.0076f));
        PartDefinition cube_r36 = leftFoot.addOrReplaceChild("cube_r36", CubeListBuilder.create().mirror().texOffs(0, 47).addBox(0.9500f, -0.8000f, -1.1000f, 1.0000f, 1.0000f, 4.0000f, new CubeDeformation(0.1000f)).mirror(false), PartPose.offsetAndRotation(1.0000f, -0.1000f, 0.0000f, 0.1526f, 0.4332f, 0.0530f));
        PartDefinition cube_r37 = leftFoot.addOrReplaceChild("cube_r37", CubeListBuilder.create().mirror().texOffs(34, 44).addBox(3.0000f, -1.0000f, 1.3500f, 5.0000f, 1.0000f, 2.0000f, new CubeDeformation(0.2000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, -0.0175f, 0.0000f, 0.0000f));
        PartDefinition tail0 = partdefinition.addOrReplaceChild("tail0", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(2.2000f)), PartPose.offsetAndRotation(0.0000f, 20.0000f, 3.0000f, -1.0472f, 0.0000f, 0.0000f));
        PartDefinition tail1 = tail0.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -5.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(2.0000f)), PartPose.offsetAndRotation(0.0000f, -4.0000f, 0.0000f, 0.4363f, 0.0000f, 0.2618f));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -5.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(1.8000f)), PartPose.offsetAndRotation(0.0000f, -6.0000f, 0.0000f, 0.4363f, 0.0000f, 0.2618f));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -5.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(1.6000f)), PartPose.offsetAndRotation(0.0000f, -6.0000f, 0.0000f, 0.4363f, 0.0000f, 0.2618f));
        PartDefinition tail4 = tail3.addOrReplaceChild("tail4", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -5.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(1.4000f)), PartPose.offsetAndRotation(0.0000f, -5.0000f, 0.0000f, 0.4363f, 0.0000f, -0.2618f));
        PartDefinition tail5 = tail4.addOrReplaceChild("tail5", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -5.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(1.2000f)), PartPose.offsetAndRotation(0.0000f, -5.0000f, 0.0000f, 0.4363f, 0.0000f, -0.2618f));
        PartDefinition tail6 = tail5.addOrReplaceChild("tail6", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -5.0000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.6000f)), PartPose.offsetAndRotation(0.0000f, -5.2500f, 0.0000f, 0.4363f, 0.0000f, -0.2618f));
        PartDefinition tail7 = tail6.addOrReplaceChild("tail7", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -4.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -4.5000f, 0.0000f, 0.4363f, 0.0000f, -0.2618f));
        PartDefinition tail8 = tail7.addOrReplaceChild("tail8", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -3.5000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(-0.4000f)), PartPose.offsetAndRotation(0.0000f, -4.0000f, 0.0000f, 0.3491f, 0.0000f, -0.1745f));
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
