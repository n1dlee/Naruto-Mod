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
 * SusanooClothedModel — geometry converted from the 1.12.2 Naruto mod's ModelBiped-based
 * Susanoo model into the 1.20.1 LayerDefinition format. Cube offsets, texture UVs,
 * poses and per-box inflation are carried over verbatim; only the model API changed.
 */
public class SusanooClothedModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(NarutoMod.MOD_ID, "susanoo_clothed"), "main");

    private final ModelPart head;
    private final ModelPart Chin;
    private final ModelPart cube_r1;
    private final ModelPart cube_r2;
    private final ModelPart Hat;
    private final ModelPart cube_r3;
    private final ModelPart cube_r4;
    private final ModelPart cube_r5;
    private final ModelPart cube_r6;
    private final ModelPart cube_r7;
    private final ModelPart cube_r8;
    private final ModelPart cube_r9;
    private final ModelPart cube_r10;
    private final ModelPart cube_r11;
    private final ModelPart cube_r12;
    private final ModelPart Midhorn;
    private final ModelPart cube_r13;
    private final ModelPart field_178720_f;
    private final ModelPart body;
    private final ModelPart Coat;
    private final ModelPart Cloak;
    private final ModelPart Cloak2;
    private final ModelPart bottomr;
    private final ModelPart cube_r14;
    private final ModelPart cube_r15;
    private final ModelPart bottoml;
    private final ModelPart cube_r16;
    private final ModelPart cube_r17;
    private final ModelPart right_arm;
    private final ModelPart bone3;
    private final ModelPart spikes;
    private final ModelPart Shoulderspike;
    private final ModelPart Shoulderspike3;
    private final ModelPart bone;
    private final ModelPart bone2;
    private final ModelPart cube_r18;
    private final ModelPart sword;
    private final ModelPart left_arm;
    private final ModelPart bone4;
    private final ModelPart spikes2;
    private final ModelPart Shoulderspike2;
    private final ModelPart Shoulderspike4;
    private final ModelPart bone7;
    private final ModelPart bone8;
    private final ModelPart cube_r19;
    private final ModelPart right_leg;
    private final ModelPart bone5;
    private final ModelPart cube_r20;
    private final ModelPart left_leg;
    private final ModelPart bone6;
    private final ModelPart cube_r21;

    public SusanooClothedModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.head = root.getChild("head");
        this.field_178720_f = root.getChild("field_178720_f");
        this.body = root.getChild("body");
        this.right_arm = root.getChild("right_arm");
        this.left_arm = root.getChild("left_arm");
        this.right_leg = root.getChild("right_leg");
        this.left_leg = root.getChild("left_leg");
        this.Chin = root.getChild("head").getChild("Chin");
        this.cube_r1 = root.getChild("head").getChild("Chin").getChild("cube_r1");
        this.cube_r2 = root.getChild("head").getChild("Chin").getChild("cube_r2");
        this.Hat = root.getChild("head").getChild("Hat");
        this.cube_r3 = root.getChild("head").getChild("Hat").getChild("cube_r3");
        this.cube_r4 = root.getChild("head").getChild("Hat").getChild("cube_r4");
        this.cube_r5 = root.getChild("head").getChild("Hat").getChild("cube_r5");
        this.cube_r6 = root.getChild("head").getChild("Hat").getChild("cube_r6");
        this.cube_r7 = root.getChild("head").getChild("Hat").getChild("cube_r7");
        this.cube_r8 = root.getChild("head").getChild("Hat").getChild("cube_r8");
        this.cube_r9 = root.getChild("head").getChild("Hat").getChild("cube_r9");
        this.cube_r10 = root.getChild("head").getChild("Hat").getChild("cube_r10");
        this.cube_r11 = root.getChild("head").getChild("Hat").getChild("cube_r11");
        this.cube_r12 = root.getChild("head").getChild("Hat").getChild("cube_r12");
        this.Midhorn = root.getChild("head").getChild("Hat").getChild("Midhorn");
        this.cube_r13 = root.getChild("head").getChild("Hat").getChild("Midhorn").getChild("cube_r13");
        this.Coat = root.getChild("body").getChild("Coat");
        this.Cloak = root.getChild("body").getChild("Coat").getChild("Cloak");
        this.Cloak2 = root.getChild("body").getChild("Coat").getChild("Cloak2");
        this.bottomr = root.getChild("body").getChild("bottomr");
        this.cube_r14 = root.getChild("body").getChild("bottomr").getChild("cube_r14");
        this.cube_r15 = root.getChild("body").getChild("bottomr").getChild("cube_r15");
        this.bottoml = root.getChild("body").getChild("bottoml");
        this.cube_r16 = root.getChild("body").getChild("bottoml").getChild("cube_r16");
        this.cube_r17 = root.getChild("body").getChild("bottoml").getChild("cube_r17");
        this.bone3 = root.getChild("right_arm").getChild("bone3");
        this.spikes = root.getChild("right_arm").getChild("bone3").getChild("spikes");
        this.Shoulderspike = root.getChild("right_arm").getChild("bone3").getChild("spikes").getChild("Shoulderspike");
        this.Shoulderspike3 = root.getChild("right_arm").getChild("bone3").getChild("spikes").getChild("Shoulderspike3");
        this.bone = root.getChild("right_arm").getChild("bone3").getChild("bone");
        this.bone2 = root.getChild("right_arm").getChild("bone3").getChild("bone").getChild("bone2");
        this.cube_r18 = root.getChild("right_arm").getChild("bone3").getChild("bone").getChild("bone2").getChild("cube_r18");
        this.sword = root.getChild("right_arm").getChild("bone3").getChild("bone").getChild("sword");
        this.bone4 = root.getChild("left_arm").getChild("bone4");
        this.spikes2 = root.getChild("left_arm").getChild("bone4").getChild("spikes2");
        this.Shoulderspike2 = root.getChild("left_arm").getChild("bone4").getChild("spikes2").getChild("Shoulderspike2");
        this.Shoulderspike4 = root.getChild("left_arm").getChild("bone4").getChild("spikes2").getChild("Shoulderspike4");
        this.bone7 = root.getChild("left_arm").getChild("bone4").getChild("bone7");
        this.bone8 = root.getChild("left_arm").getChild("bone4").getChild("bone7").getChild("bone8");
        this.cube_r19 = root.getChild("left_arm").getChild("bone4").getChild("bone7").getChild("bone8").getChild("cube_r19");
        this.bone5 = root.getChild("right_leg").getChild("bone5");
        this.cube_r20 = root.getChild("right_leg").getChild("bone5").getChild("cube_r20");
        this.bone6 = root.getChild("left_leg").getChild("bone6");
        this.cube_r21 = root.getChild("left_leg").getChild("bone6").getChild("cube_r21");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -7.5F, -4.0F, 8, 8, 8, new CubeDeformation(-0.8F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_Chin = p_head.addOrReplaceChild("Chin",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 24.5F, 0.0F));
        PartDefinition p_cube_r1 = p_Chin.addOrReplaceChild("cube_r1",
                CubeListBuilder.create()
                        .texOffs(62, 0)
                        .addBox(-2.0F, -1.15F, -0.9F, 4, 1, 3, new CubeDeformation(-0.2F)),
                PartPose.offsetAndRotation(0.0F, -24.0445F, -2.4358F, 0.0873F, 0.0F, 0.0F));
        PartDefinition p_cube_r2 = p_Chin.addOrReplaceChild("cube_r2",
                CubeListBuilder.create()
                        .texOffs(23, 18)
                        .addBox(-1.0F, -1.9F, -0.05F, 2, 2, 1, new CubeDeformation(-0.2F)),
                PartPose.offsetAndRotation(0.0F, -22.9413F, -3.0381F, 0.1745F, 0.0F, 0.0F));
        PartDefinition p_Hat = p_head.addOrReplaceChild("Hat",
                CubeListBuilder.create(),
                PartPose.offset(0.0023F, -3.2067F, -1.5353F));
        PartDefinition p_cube_r3 = p_Hat.addOrReplaceChild("cube_r3",
                CubeListBuilder.create()
                        .texOffs(41, 0)
                        .addBox(-1.5F, -1.825F, 0.35F, 3, 3, 1),
                PartPose.offsetAndRotation(-0.0023F, -2.7933F, -2.4647F, -0.2182F, 0.0F, 0.0F));
        PartDefinition p_cube_r4 = p_Hat.addOrReplaceChild("cube_r4",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(16, 60)
                        .addBox(-3.25F, -1.0F, -0.5F, 5, 2, 3, new CubeDeformation(-0.5F))
                        .mirror(false),
                PartPose.offsetAndRotation(2.6902F, -3.7943F, -2.3505F, -0.2182F, 0.0F, -0.5672F));
        PartDefinition p_cube_r5 = p_Hat.addOrReplaceChild("cube_r5",
                CubeListBuilder.create()
                        .texOffs(16, 60)
                        .addBox(-1.75F, -1.0F, -0.5F, 5, 2, 3, new CubeDeformation(-0.5F)),
                PartPose.offsetAndRotation(-2.6948F, -3.7943F, -2.3505F, -0.2182F, 0.0F, 0.5672F));
        PartDefinition p_cube_r6 = p_Hat.addOrReplaceChild("cube_r6",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-0.5F, -0.33F, -0.09F, 1, 4, 1, new CubeDeformation(-0.1F)),
                PartPose.offsetAndRotation(-0.0023F, -2.0433F, -2.4647F, 0.1745F, 0.0F, 0.0F));
        PartDefinition p_cube_r7 = p_Hat.addOrReplaceChild("cube_r7",
                CubeListBuilder.create()
                        .texOffs(25, 9)
                        .addBox(-4.525F, -1.0F, -1.2332F, 9, 3, 7, new CubeDeformation(-0.9F)),
                PartPose.offsetAndRotation(-0.0023F, -4.2549F, -1.0964F, -0.2182F, 0.0F, 0.0F));
        PartDefinition p_cube_r8 = p_Hat.addOrReplaceChild("cube_r8",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(18, 33)
                        .addBox(-8.525F, -2.75F, -4.1F, 1, 9, 8, new CubeDeformation(-0.8F))
                        .mirror(false),
                PartPose.offsetAndRotation(4.4977F, -0.7933F, 1.5353F, 0.0F, 0.0F, 0.0873F));
        PartDefinition p_cube_r9 = p_Hat.addOrReplaceChild("cube_r9",
                CubeListBuilder.create()
                        .texOffs(18, 33)
                        .addBox(7.525F, -2.75F, -4.1F, 1, 9, 8, new CubeDeformation(-0.8F)),
                PartPose.offsetAndRotation(-4.5023F, -0.7933F, 1.5353F, 0.0F, 0.0F, -0.0873F));
        PartDefinition p_cube_r10 = p_Hat.addOrReplaceChild("cube_r10",
                CubeListBuilder.create()
                        .texOffs(52, 26)
                        .addBox(-4.4F, -4.6F, -0.5F, 9, 9, 1, new CubeDeformation(-0.9F)),
                PartPose.offsetAndRotation(0.0727F, 0.2601F, 4.7365F, -0.0873F, 3.1416F, 0.0F));
        PartDefinition p_cube_r11 = p_Hat.addOrReplaceChild("cube_r11",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 33)
                        .addBox(-1.075F, 0.725F, -0.4F, 2, 2, 2, new CubeDeformation(-0.5F))
                        .mirror(false),
                PartPose.offsetAndRotation(5.0168F, -6.0109F, -1.9532F, -0.1745F, 0.1745F, 0.6545F));
        PartDefinition p_cube_r12 = p_Hat.addOrReplaceChild("cube_r12",
                CubeListBuilder.create()
                        .texOffs(0, 33)
                        .addBox(-0.925F, 0.725F, -0.4F, 2, 2, 2, new CubeDeformation(-0.5F)),
                PartPose.offsetAndRotation(-5.0213F, -6.0109F, -1.9532F, -0.1745F, -0.1745F, -0.6545F));
        PartDefinition p_Midhorn = p_Hat.addOrReplaceChild("Midhorn",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0281F, -5.2452F, -1.5647F, -0.2618F, -0.0436F, 0.0F));
        PartDefinition p_cube_r13 = p_Midhorn.addOrReplaceChild("cube_r13",
                CubeListBuilder.create()
                        .texOffs(32, 62)
                        .addBox(-3.2197F, -1.75F, -0.1F, 5, 5, 1, new CubeDeformation(-0.8F)),
                PartPose.offsetAndRotation(-0.0303F, 0.0F, 0.0F, -0.0524F, 0.0F, -0.7854F));
        PartDefinition p_field_178720_f = root.addOrReplaceChild("field_178720_f",
                CubeListBuilder.create()
                        .texOffs(74, 16)
                        .addBox(-4.0F, -5.675F, -4.05F, 8, 3, 0, new CubeDeformation(-0.8F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(28, 18)
                        .addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4)
                        .texOffs(0, 16)
                        .addBox(-4.5F, 2.0F, -2.5F, 9, 10, 5),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_Coat = p_body.addOrReplaceChild("Coat",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 20.9167F, 0.0F));
        PartDefinition p_Cloak = p_Coat.addOrReplaceChild("Cloak",
                CubeListBuilder.create(),
                PartPose.offset(-3.9098F, -15.9076F, 0.0F));
        PartDefinition p_Cloak2 = p_Coat.addOrReplaceChild("Cloak2",
                CubeListBuilder.create(),
                PartPose.offset(3.9098F, -15.9076F, 0.0F));
        PartDefinition p_bottomr = p_body.addOrReplaceChild("bottomr",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-2.9495F, 11.7705F, 0.0F, 3.1416F, 0.0F, 0.8727F));
        PartDefinition p_cube_r14 = p_bottomr.addOrReplaceChild("cube_r14",
                CubeListBuilder.create()
                        .texOffs(59, 57)
                        .addBox(1.0F, -1.15F, -2.5F, 3, 1, 5),
                PartPose.offsetAndRotation(-1.9755F, -1.2705F, 0.0F, 0.0F, 0.0F, 0.6109F));
        PartDefinition p_cube_r15 = p_bottomr.addOrReplaceChild("cube_r15",
                CubeListBuilder.create()
                        .texOffs(52, 36)
                        .addBox(-4.9719F, -1.643F, -2.5F, 6, 2, 5),
                PartPose.offsetAndRotation(2.3224F, 1.9135F, 0.0F, 0.0F, 0.0F, 0.3054F));
        PartDefinition p_bottoml = p_body.addOrReplaceChild("bottoml",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(2.9495F, 11.7705F, 0.0F, 3.1416F, 0.0F, -0.8727F));
        PartDefinition p_cube_r16 = p_bottoml.addOrReplaceChild("cube_r16",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(59, 57)
                        .addBox(-4.0F, -1.15F, -2.5F, 3, 1, 5)
                        .mirror(false),
                PartPose.offsetAndRotation(1.9755F, -1.2705F, 0.0F, 0.0F, 0.0F, -0.6109F));
        PartDefinition p_cube_r17 = p_bottoml.addOrReplaceChild("cube_r17",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(52, 36)
                        .addBox(-1.0281F, -1.643F, -2.5F, 6, 2, 5)
                        .mirror(false),
                PartPose.offsetAndRotation(-2.3224F, 1.9135F, 0.0F, 0.0F, 0.0F, -0.3054F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create(),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition p_bone3 = p_right_arm.addOrReplaceChild("bone3",
                CubeListBuilder.create()
                        .texOffs(52, 16)
                        .addBox(-2.5F, -2.2F, -2.5F, 5, 5, 5)
                        .texOffs(50, 0)
                        .addBox(-2.0F, -2.0F, -2.0F, 4, 8, 4),
                PartPose.offsetAndRotation(-1.0F, 0.0F, 0.0F, -0.0873F, -0.5236F, 0.1745F));
        PartDefinition p_spikes = p_bone3.addOrReplaceChild("spikes",
                CubeListBuilder.create(),
                PartPose.offset(6.0F, 22.0F, 0.0F));
        PartDefinition p_Shoulderspike = p_spikes.addOrReplaceChild("Shoulderspike",
                CubeListBuilder.create()
                        .texOffs(30, 50)
                        .addBox(-2.0F, 0.0F, -3.0F, 4, 0, 6),
                PartPose.offsetAndRotation(-7.5F, -24.0F, 0.0F, 0.0F, 0.0F, 1.0472F));
        PartDefinition p_Shoulderspike3 = p_spikes.addOrReplaceChild("Shoulderspike3",
                CubeListBuilder.create()
                        .texOffs(30, 50)
                        .addBox(-2.0F, 0.0F, -3.0F, 4, 0, 6),
                PartPose.offsetAndRotation(-8.75F, -21.75F, 0.0F, 0.0F, 0.0F, 0.7854F));
        PartDefinition p_bone = p_bone3.addOrReplaceChild("bone",
                CubeListBuilder.create()
                        .texOffs(48, 50)
                        .addBox(0.0F, 0.0F, -4.0F, 4, 8, 4, new CubeDeformation(-0.1F)),
                PartPose.offsetAndRotation(-2.0F, 5.9F, 2.0F, -0.2618F, 0.0F, 0.0F));
        PartDefinition p_bone2 = p_bone.addOrReplaceChild("bone2",
                CubeListBuilder.create(),
                PartPose.offset(11.65F, 19.85F, -2.95F));
        PartDefinition p_cube_r18 = p_bone2.addOrReplaceChild("cube_r18",
                CubeListBuilder.create()
                        .texOffs(49, 0)
                        .addBox(-2.225F, -1.5F, 1.325F, 1, 3, 1)
                        .texOffs(49, 0)
                        .addBox(-1.5F, -1.5F, 0.5F, 1, 3, 1)
                        .texOffs(49, 0)
                        .addBox(-0.65F, -1.5F, -0.225F, 1, 3, 1),
                PartPose.offsetAndRotation(-11.5F, -16.5F, -0.5F, 0.0F, 0.7854F, 0.0F));
        PartDefinition p_sword = p_bone.addOrReplaceChild("sword",
                CubeListBuilder.create()
                        .texOffs(76, 0)
                        .addBox(1.5F, 4.0F, -6.0F, 1, 2, 8, new CubeDeformation(-0.2F))
                        .texOffs(74, 0)
                        .addBox(2.0F, 3.0F, -26.0F, 0, 4, 20)
                        .texOffs(77, 0)
                        .addBox(1.5F, 2.5F, -8.4F, 1, 5, 2, new CubeDeformation(0.3F))
                        .texOffs(87, 0)
                        .addBox(1.5F, 3.5F, -6.15F, 1, 3, 1, new CubeDeformation(-0.1F)),
                PartPose.offset(0.0F, 0.0F, 1.0F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create(),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition p_bone4 = p_left_arm.addOrReplaceChild("bone4",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(52, 16)
                        .addBox(-2.5F, -2.2F, -2.5F, 5, 5, 5)
                        .mirror(false)
                        .mirror()
                        .texOffs(50, 0)
                        .addBox(-2.0F, -2.0F, -2.0F, 4, 8, 4)
                        .mirror(false),
                PartPose.offsetAndRotation(1.0F, 0.0F, 0.0F, -0.0873F, 0.5236F, -0.1745F));
        PartDefinition p_spikes2 = p_bone4.addOrReplaceChild("spikes2",
                CubeListBuilder.create(),
                PartPose.offset(-6.0F, 22.0F, 0.0F));
        PartDefinition p_Shoulderspike2 = p_spikes2.addOrReplaceChild("Shoulderspike2",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(30, 50)
                        .addBox(-2.0F, 0.0F, -3.0F, 4, 0, 6)
                        .mirror(false),
                PartPose.offsetAndRotation(7.5F, -24.0F, 0.0F, 0.0F, 0.0F, -1.0472F));
        PartDefinition p_Shoulderspike4 = p_spikes2.addOrReplaceChild("Shoulderspike4",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(30, 50)
                        .addBox(-2.0F, 0.0F, -3.0F, 4, 0, 6)
                        .mirror(false),
                PartPose.offsetAndRotation(8.75F, -21.75F, 0.0F, 0.0F, 0.0F, -0.7854F));
        PartDefinition p_bone7 = p_bone4.addOrReplaceChild("bone7",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(48, 50)
                        .addBox(-4.0F, 0.0F, -4.0F, 4, 8, 4, new CubeDeformation(-0.1F))
                        .mirror(false),
                PartPose.offsetAndRotation(2.0F, 5.9F, 2.0F, -0.2618F, 0.0F, 0.0F));
        PartDefinition p_bone8 = p_bone7.addOrReplaceChild("bone8",
                CubeListBuilder.create(),
                PartPose.offset(-11.65F, 19.85F, -2.95F));
        PartDefinition p_cube_r19 = p_bone8.addOrReplaceChild("cube_r19",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(49, 0)
                        .addBox(1.225F, -1.5F, 1.325F, 1, 3, 1)
                        .mirror(false)
                        .mirror()
                        .texOffs(49, 0)
                        .addBox(0.5F, -1.5F, 0.5F, 1, 3, 1)
                        .mirror(false)
                        .mirror()
                        .texOffs(49, 0)
                        .addBox(-0.35F, -1.5F, -0.225F, 1, 3, 1)
                        .mirror(false),
                PartPose.offsetAndRotation(11.5F, -16.5F, -0.5F, 0.0F, -0.7854F, 0.0F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 50)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.1F)),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition p_bone5 = p_right_leg.addOrReplaceChild("bone5",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.1F, 6.0F, -1.0F, 0.0F, 0.0F, -0.5236F));
        PartDefinition p_cube_r20 = p_bone5.addOrReplaceChild("cube_r20",
                CubeListBuilder.create()
                        .texOffs(0, 4)
                        .addBox(-1.0F, -1.0F, -1.0F, 2, 2, 2, new CubeDeformation(0.1F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7854F, -0.6545F, 0.0F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 50)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.1F))
                        .mirror(false),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        PartDefinition p_bone6 = p_left_leg.addOrReplaceChild("bone6",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.1F, 6.0F, -1.0F, 0.0F, 0.0F, 0.5236F));
        PartDefinition p_cube_r21 = p_bone6.addOrReplaceChild("cube_r21",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 4)
                        .addBox(-1.0F, -1.0F, -1.0F, 2, 2, 2, new CubeDeformation(0.1F))
                        .mirror(false),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7854F, 0.6545F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        this.head.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.field_178720_f.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.body.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.right_arm.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.left_arm.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.right_leg.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.left_leg.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
