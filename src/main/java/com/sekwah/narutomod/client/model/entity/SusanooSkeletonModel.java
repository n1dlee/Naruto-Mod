package com.sekwah.narutomod.client.model.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * SusanooSkeletonModel — geometry converted from the 1.12.2 Naruto mod's ModelBiped-based
 * Susanoo model into the 1.20.1 LayerDefinition format. Cube offsets, texture UVs,
 * poses and per-box inflation are carried over verbatim; only the model API changed.
 */
public class SusanooSkeletonModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(NarutoMod.MOD_ID, "susanoo_skeleton"), "main");

    private final ModelPart head;
    private final ModelPart head7_r1;
    private final ModelPart head6_r1;
    private final ModelPart head5_r1;
    private final ModelPart head_r1;
    private final ModelPart HornStyle1;
    private final ModelPart right;
    private final ModelPart cube_r1;
    private final ModelPart cube_r2;
    private final ModelPart cube_r3;
    private final ModelPart left;
    private final ModelPart cube_r4;
    private final ModelPart cube_r5;
    private final ModelPart cube_r6;
    private final ModelPart HornStyle2;
    private final ModelPart right5;
    private final ModelPart cube_r7;
    private final ModelPart cube_r8;
    private final ModelPart cube_r9;
    private final ModelPart left8;
    private final ModelPart cube_r10;
    private final ModelPart cube_r11;
    private final ModelPart cube_r12;
    private final ModelPart field_178720_f;
    private final ModelPart body;
    private final ModelPart right_arm;
    private final ModelPart cube_r13;
    private final ModelPart cube_r14;
    private final ModelPart cube_r15;
    private final ModelPart rightHand;
    private final ModelPart rightFingers;
    private final ModelPart left_arm;
    private final ModelPart cube_r16;
    private final ModelPart cube_r17;
    private final ModelPart cube_r18;
    private final ModelPart leftHand;
    private final ModelPart leftFingers;

    public SusanooSkeletonModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.head = root.getChild("head");
        this.field_178720_f = root.getChild("field_178720_f");
        this.body = root.getChild("body");
        this.right_arm = root.getChild("right_arm");
        this.left_arm = root.getChild("left_arm");
        this.head7_r1 = root.getChild("head").getChild("head7_r1");
        this.head6_r1 = root.getChild("head").getChild("head6_r1");
        this.head5_r1 = root.getChild("head").getChild("head5_r1");
        this.head_r1 = root.getChild("head").getChild("head_r1");
        this.HornStyle1 = root.getChild("head").getChild("HornStyle1");
        this.right = root.getChild("head").getChild("HornStyle1").getChild("right");
        this.cube_r1 = root.getChild("head").getChild("HornStyle1").getChild("right").getChild("cube_r1");
        this.cube_r2 = root.getChild("head").getChild("HornStyle1").getChild("right").getChild("cube_r2");
        this.cube_r3 = root.getChild("head").getChild("HornStyle1").getChild("right").getChild("cube_r3");
        this.left = root.getChild("head").getChild("HornStyle1").getChild("left");
        this.cube_r4 = root.getChild("head").getChild("HornStyle1").getChild("left").getChild("cube_r4");
        this.cube_r5 = root.getChild("head").getChild("HornStyle1").getChild("left").getChild("cube_r5");
        this.cube_r6 = root.getChild("head").getChild("HornStyle1").getChild("left").getChild("cube_r6");
        this.HornStyle2 = root.getChild("head").getChild("HornStyle2");
        this.right5 = root.getChild("head").getChild("HornStyle2").getChild("right5");
        this.cube_r7 = root.getChild("head").getChild("HornStyle2").getChild("right5").getChild("cube_r7");
        this.cube_r8 = root.getChild("head").getChild("HornStyle2").getChild("right5").getChild("cube_r8");
        this.cube_r9 = root.getChild("head").getChild("HornStyle2").getChild("right5").getChild("cube_r9");
        this.left8 = root.getChild("head").getChild("HornStyle2").getChild("left8");
        this.cube_r10 = root.getChild("head").getChild("HornStyle2").getChild("left8").getChild("cube_r10");
        this.cube_r11 = root.getChild("head").getChild("HornStyle2").getChild("left8").getChild("cube_r11");
        this.cube_r12 = root.getChild("head").getChild("HornStyle2").getChild("left8").getChild("cube_r12");
        this.cube_r13 = root.getChild("right_arm").getChild("cube_r13");
        this.cube_r14 = root.getChild("right_arm").getChild("cube_r14");
        this.cube_r15 = root.getChild("right_arm").getChild("cube_r15");
        this.rightHand = root.getChild("right_arm").getChild("rightHand");
        this.rightFingers = root.getChild("right_arm").getChild("rightHand").getChild("rightFingers");
        this.cube_r16 = root.getChild("left_arm").getChild("cube_r16");
        this.cube_r17 = root.getChild("left_arm").getChild("cube_r17");
        this.cube_r18 = root.getChild("left_arm").getChild("cube_r18");
        this.leftHand = root.getChild("left_arm").getChild("leftHand");
        this.leftFingers = root.getChild("left_arm").getChild("leftHand").getChild("leftFingers");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(36, 147)
                        .addBox(-10.0F, -23.0F, -10.0F, 20, 14, 20)
                        .texOffs(0, 203)
                        .addBox(-9.0F, -24.0F, -9.0F, 18, 1, 18)
                        .texOffs(85, 201)
                        .addBox(-9.0F, -21.0F, -11.0F, 18, 12, 3),
                PartPose.offset(0.0F, -8.0F, 0.0F));
        PartDefinition p_head7_r1 = p_head.addOrReplaceChild("head7_r1",
                CubeListBuilder.create()
                        .texOffs(152, 76)
                        .addBox(-4.0F, -1.5F, 0.5F, 8, 3, 2),
                PartPose.offsetAndRotation(0.0F, -0.5F, -9.0F, 0.1745F, 0.0F, 0.0F));
        PartDefinition p_head6_r1 = p_head.addOrReplaceChild("head6_r1",
                CubeListBuilder.create()
                        .texOffs(115, 43)
                        .addBox(-4.0F, -5.0F, -0.622F, 8, 10, 8),
                PartPose.offsetAndRotation(0.0F, -1.3025F, 4.122F, 0.4363F, 0.0F, 0.0F));
        PartDefinition p_head5_r1 = p_head.addOrReplaceChild("head5_r1",
                CubeListBuilder.create()
                        .texOffs(141, 108)
                        .addBox(-7.0F, -2.5F, -1.5F, 14, 3, 3),
                PartPose.offsetAndRotation(0.0F, -7.6846F, -8.9841F, 0.2182F, 0.0F, 0.0F));
        PartDefinition p_head_r1 = p_head.addOrReplaceChild("head_r1",
                CubeListBuilder.create()
                        .texOffs(132, 149)
                        .addBox(-9.0F, 1.0F, -33.0F, 18, 9, 18),
                PartPose.offsetAndRotation(0.0F, -15.0F, 23.0F, 0.1309F, 0.0F, 0.0F));
        PartDefinition p_HornStyle1 = p_head.addOrReplaceChild("HornStyle1",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 32.0F, 0.0F));
        PartDefinition p_right = p_HornStyle1.addOrReplaceChild("right",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_cube_r1 = p_right.addOrReplaceChild("cube_r1",
                CubeListBuilder.create()
                        .texOffs(0, 312)
                        .addBox(-17.0F, -15.5406F, -15.0F, 33, 30, 30, new CubeDeformation(-14.0F)),
                PartPose.offsetAndRotation(-19.0517F, -58.4594F, -0.5F, 0.0F, 0.0F, 1.1781F));
        PartDefinition p_cube_r2 = p_right.addOrReplaceChild("cube_r2",
                CubeListBuilder.create()
                        .texOffs(0, 312)
                        .addBox(-14.0F, -15.0F, -15.0F, 31, 30, 30, new CubeDeformation(-12.0F)),
                PartPose.offsetAndRotation(-13.1084F, -54.1158F, -0.5F, 0.0F, 0.0F, 0.1745F));
        PartDefinition p_cube_r3 = p_right.addOrReplaceChild("cube_r3",
                CubeListBuilder.create()
                        .texOffs(0, 312)
                        .addBox(-17.5F, -15.0F, -15.0F, 32, 30, 30, new CubeDeformation(-13.0F)),
                PartPose.offsetAndRotation(-15.0F, -54.5F, -0.5F, 0.0F, 0.0F, 0.7854F));
        PartDefinition p_left = p_HornStyle1.addOrReplaceChild("left",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_cube_r4 = p_left.addOrReplaceChild("cube_r4",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 312)
                        .addBox(-16.0F, -15.5406F, -15.0F, 33, 30, 30, new CubeDeformation(-14.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(19.0517F, -58.4594F, -0.5F, 0.0F, 0.0F, -1.1781F));
        PartDefinition p_cube_r5 = p_left.addOrReplaceChild("cube_r5",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 312)
                        .addBox(-14.5F, -15.0F, -15.0F, 32, 30, 30, new CubeDeformation(-13.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(15.0F, -54.5F, -0.5F, 0.0F, 0.0F, -0.7854F));
        PartDefinition p_cube_r6 = p_left.addOrReplaceChild("cube_r6",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 312)
                        .addBox(-17.0F, -15.0F, -15.0F, 31, 30, 30, new CubeDeformation(-12.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(13.1084F, -54.1158F, -0.5F, 0.0F, 0.0F, -0.1745F));
        PartDefinition p_HornStyle2 = p_head.addOrReplaceChild("HornStyle2",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 35.0F, -2.0F));
        PartDefinition p_right5 = p_HornStyle2.addOrReplaceChild("right5",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-5.8867F, -56.8719F, -5.5F, 0.0F, -1.2217F, 0.0F));
        PartDefinition p_cube_r7 = p_right5.addOrReplaceChild("cube_r7",
                CubeListBuilder.create()
                        .texOffs(0, 312)
                        .addBox(-17.0F, -15.5406F, -15.0F, 33, 30, 30, new CubeDeformation(-14.0F)),
                PartPose.offsetAndRotation(-10.165F, -4.5875F, 0.0F, 0.0F, 0.0F, 1.1781F));
        PartDefinition p_cube_r8 = p_right5.addOrReplaceChild("cube_r8",
                CubeListBuilder.create()
                        .texOffs(0, 312)
                        .addBox(-14.0F, -15.0F, -15.0F, 31, 30, 30, new CubeDeformation(-12.0F)),
                PartPose.offsetAndRotation(-4.2217F, -0.2439F, 0.0F, 0.0F, 0.0F, 0.1745F));
        PartDefinition p_cube_r9 = p_right5.addOrReplaceChild("cube_r9",
                CubeListBuilder.create()
                        .texOffs(0, 312)
                        .addBox(-17.5F, -15.0F, -15.0F, 32, 30, 30, new CubeDeformation(-13.0F)),
                PartPose.offsetAndRotation(-6.1133F, -0.6281F, 0.0F, 0.0F, 0.0F, 0.7854F));
        PartDefinition p_left8 = p_HornStyle2.addOrReplaceChild("left8",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(5.8867F, -56.8719F, -5.5F, 0.0F, 1.2217F, 0.0F));
        PartDefinition p_cube_r10 = p_left8.addOrReplaceChild("cube_r10",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 312)
                        .addBox(-16.0F, -15.5406F, -15.0F, 33, 30, 30, new CubeDeformation(-14.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(10.165F, -4.5875F, 0.0F, 0.0F, 0.0F, -1.1781F));
        PartDefinition p_cube_r11 = p_left8.addOrReplaceChild("cube_r11",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 312)
                        .addBox(-17.0F, -15.0F, -15.0F, 31, 30, 30, new CubeDeformation(-12.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(4.2217F, -0.2439F, 0.0F, 0.0F, 0.0F, -0.1745F));
        PartDefinition p_cube_r12 = p_left8.addOrReplaceChild("cube_r12",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 312)
                        .addBox(-14.5F, -15.0F, -15.0F, 32, 30, 30, new CubeDeformation(-13.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(6.1133F, -0.6281F, 0.0F, 0.0F, 0.0F, -0.7854F));
        PartDefinition p_field_178720_f = root.addOrReplaceChild("field_178720_f",
                CubeListBuilder.create()
                        .texOffs(177, 202)
                        .addBox(-9.0F, -21.0F, -11.05F, 18, 12, 0),
                PartPose.offset(0.0F, -8.0F, 0.0F));
        PartDefinition p_body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 50)
                        .addBox(-16.0F, 0.0F, -14.0F, 32, 24, 27)
                        .mirror(false)
                        .texOffs(0, 0)
                        .addBox(-13.0F, 18.0F, -11.0F, 26, 18, 22),
                PartPose.offset(0.0F, -8.0F, 0.0F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create(),
                PartPose.offset(-17.0F, -7.0F, -1.0F));
        PartDefinition p_cube_r13 = p_right_arm.addOrReplaceChild("cube_r13",
                CubeListBuilder.create()
                        .texOffs(0, 115)
                        .addBox(-3.0F, -8.0F, -3.0F, 6, 16, 6),
                PartPose.offsetAndRotation(-8.25F, 33.9513F, -3.6693F, -0.6109F, 0.0F, 0.0F));
        PartDefinition p_cube_r14 = p_right_arm.addOrReplaceChild("cube_r14",
                CubeListBuilder.create()
                        .texOffs(0, 142)
                        .addBox(-3.0F, 0.0F, -3.0F, 6, 28, 6),
                PartPose.offsetAndRotation(-4.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.1745F));
        PartDefinition p_cube_r15 = p_right_arm.addOrReplaceChild("cube_r15",
                CubeListBuilder.create()
                        .texOffs(114, 0)
                        .addBox(-3.0F, -8.0F, -5.0F, 6, 16, 10),
                PartPose.offsetAndRotation(-3.3421F, 0.9674F, 0.0F, 0.0F, 0.0F, 1.1345F));
        PartDefinition p_rightHand = p_right_arm.addOrReplaceChild("rightHand",
                CubeListBuilder.create()
                        .texOffs(2, 246)
                        .addBox(-8.0F, -3.1436F, -11.9735F, 19, 23, 21, new CubeDeformation(-3.0F)),
                PartPose.offsetAndRotation(-8.25F, 40.6069F, -8.3621F, -0.6109F, 0.0F, 0.0F));
        PartDefinition p_rightFingers = p_rightHand.addOrReplaceChild("rightFingers",
                CubeListBuilder.create()
                        .texOffs(97, 246)
                        .addBox(-3.0F, -20.0F, -10.5F, 19, 23, 21, new CubeDeformation(-3.0F)),
                PartPose.offsetAndRotation(-5.0F, 16.8564F, -1.4735F, 0.0F, 0.0F, 1.0472F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create(),
                PartPose.offset(17.0F, -7.0F, -1.0F));
        PartDefinition p_cube_r16 = p_left_arm.addOrReplaceChild("cube_r16",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 115)
                        .addBox(-3.0F, -8.0F, -3.0F, 6, 16, 6)
                        .mirror(false),
                PartPose.offsetAndRotation(8.25F, 33.9513F, -3.6693F, -0.6109F, 0.0F, 0.0F));
        PartDefinition p_cube_r17 = p_left_arm.addOrReplaceChild("cube_r17",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 142)
                        .addBox(-3.0F, 0.0F, -3.0F, 6, 28, 6)
                        .mirror(false),
                PartPose.offsetAndRotation(4.0F, 3.0F, 0.0F, 0.0F, 0.0F, -0.1745F));
        PartDefinition p_cube_r18 = p_left_arm.addOrReplaceChild("cube_r18",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(114, 0)
                        .addBox(-3.0F, -8.0F, -5.0F, 6, 16, 10)
                        .mirror(false),
                PartPose.offsetAndRotation(3.3421F, 0.9674F, 0.0F, 0.0F, 0.0F, -1.1345F));
        PartDefinition p_leftHand = p_left_arm.addOrReplaceChild("leftHand",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(2, 246)
                        .addBox(-11.0F, -3.1436F, -11.9735F, 19, 23, 21, new CubeDeformation(-3.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(8.25F, 40.6069F, -8.3621F, -0.6109F, 0.0F, 0.0F));
        PartDefinition p_leftFingers = p_leftHand.addOrReplaceChild("leftFingers",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(97, 246)
                        .addBox(-16.0F, -20.0F, -10.5F, 19, 23, 21, new CubeDeformation(-3.0F))
                        .mirror(false),
                PartPose.offset(5.0F, 16.8564F, -1.4735F));
        return LayerDefinition.create(mesh, 512, 512);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        this.head.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.field_178720_f.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.body.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.right_arm.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.left_arm.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
