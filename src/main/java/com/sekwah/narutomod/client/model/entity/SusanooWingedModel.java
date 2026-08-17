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
 * SusanooWingedModel — geometry converted from the 1.12.2 Naruto mod's ModelBiped-based
 * Susanoo model into the 1.20.1 LayerDefinition format. Cube offsets, texture UVs,
 * poses and per-box inflation are carried over verbatim; only the model API changed.
 */
public class SusanooWingedModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(NarutoMod.MOD_ID, "susanoo_winged"), "main");

    private final ModelPart head;
    private final ModelPart chin_r1;
    private final ModelPart bipedHead_r1;
    private final ModelPart bipedHead_r2;
    private final ModelPart hair;
    private final ModelPart Hair_r1;
    private final ModelPart Hair_r2;
    private final ModelPart Hair_r3;
    private final ModelPart Hair_r4;
    private final ModelPart Nose;
    private final ModelPart bridge_r1;
    private final ModelPart HeadDecor;
    private final ModelPart Flame1;
    private final ModelPart flame2_r1;
    private final ModelPart flame3_r1;
    private final ModelPart flame3_r2;
    private final ModelPart flame2_r2;
    private final ModelPart Flame2;
    private final ModelPart Flame3;
    private final ModelPart Flame4;
    private final ModelPart flame3_r3;
    private final ModelPart flame4_r1;
    private final ModelPart flame4_r2;
    private final ModelPart flame3_r4;
    private final ModelPart Horn1;
    private final ModelPart cube_r1;
    private final ModelPart cube_r2;
    private final ModelPart Flame5;
    private final ModelPart Horn2;
    private final ModelPart cube_r3;
    private final ModelPart cube_r4;
    private final ModelPart field_178720_f;
    private final ModelPart body;
    private final ModelPart BeltPads1;
    private final ModelPart bone7;
    private final ModelPart bone8;
    private final ModelPart bone9;
    private final ModelPart BeltPads2;
    private final ModelPart bone6;
    private final ModelPart bone10;
    private final ModelPart bone11;
    private final ModelPart right_arm;
    private final ModelPart rightShoulderPad;
    private final ModelPart Shoulderpadlr_r1;
    private final ModelPart Shoulderpadll_r1;
    private final ModelPart sword;
    private final ModelPart left_arm;
    private final ModelPart leftShoulderPad;
    private final ModelPart Shoulderpadlr_r2;
    private final ModelPart Shoulderpadll_r2;
    private final ModelPart right_leg;
    private final ModelPart rightDress;
    private final ModelPart Dressb_r1;
    private final ModelPart Dressf_r1;
    private final ModelPart BeltPads3;
    private final ModelPart bone18;
    private final ModelPart beltpadtr_r1;
    private final ModelPart bone19;
    private final ModelPart beltpadtr_r2;
    private final ModelPart bone20;
    private final ModelPart beltpadtr_r3;
    private final ModelPart left_leg;
    private final ModelPart leftDress;
    private final ModelPart Dressb_r2;
    private final ModelPart Dressf_r2;
    private final ModelPart BeltPads4;
    private final ModelPart bone21;
    private final ModelPart beltpadtr_r4;
    private final ModelPart bone22;
    private final ModelPart beltpadtr_r5;
    private final ModelPart bone23;
    private final ModelPart beltpadtr_r6;
    private final ModelPart rightWing;
    private final ModelPart bone3;
    private final ModelPart rightClaw;
    private final ModelPart finger5_r1;
    private final ModelPart finger4_r1;
    private final ModelPart finger3_r1;
    private final ModelPart finger2_r1;
    private final ModelPart thumb_r1;
    private final ModelPart flap1;
    private final ModelPart flap2;
    private final ModelPart flap3;
    private final ModelPart flap4;
    private final ModelPart flap5;
    private final ModelPart flap6;
    private final ModelPart flap7;
    private final ModelPart flap8;
    private final ModelPart leftWing;
    private final ModelPart bone2;
    private final ModelPart leftClaw;
    private final ModelPart finger5_r2;
    private final ModelPart finger4_r2;
    private final ModelPart finger3_r2;
    private final ModelPart finger2_r2;
    private final ModelPart thumb_r2;
    private final ModelPart flap9;
    private final ModelPart flap10;
    private final ModelPart flap11;
    private final ModelPart flap12;
    private final ModelPart flap13;
    private final ModelPart flap14;
    private final ModelPart flap15;
    private final ModelPart flap16;

    public SusanooWingedModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.head = root.getChild("head");
        this.field_178720_f = root.getChild("field_178720_f");
        this.body = root.getChild("body");
        this.right_arm = root.getChild("right_arm");
        this.left_arm = root.getChild("left_arm");
        this.right_leg = root.getChild("right_leg");
        this.left_leg = root.getChild("left_leg");
        this.rightWing = root.getChild("rightWing");
        this.leftWing = root.getChild("leftWing");
        this.chin_r1 = root.getChild("head").getChild("chin_r1");
        this.bipedHead_r1 = root.getChild("head").getChild("bipedHead_r1");
        this.bipedHead_r2 = root.getChild("head").getChild("bipedHead_r2");
        this.hair = root.getChild("head").getChild("hair");
        this.Hair_r1 = root.getChild("head").getChild("hair").getChild("Hair_r1");
        this.Hair_r2 = root.getChild("head").getChild("hair").getChild("Hair_r2");
        this.Hair_r3 = root.getChild("head").getChild("hair").getChild("Hair_r3");
        this.Hair_r4 = root.getChild("head").getChild("hair").getChild("Hair_r4");
        this.Nose = root.getChild("head").getChild("Nose");
        this.bridge_r1 = root.getChild("head").getChild("Nose").getChild("bridge_r1");
        this.HeadDecor = root.getChild("head").getChild("HeadDecor");
        this.Flame1 = root.getChild("head").getChild("HeadDecor").getChild("Flame1");
        this.flame2_r1 = root.getChild("head").getChild("HeadDecor").getChild("Flame1").getChild("flame2_r1");
        this.flame3_r1 = root.getChild("head").getChild("HeadDecor").getChild("Flame1").getChild("flame3_r1");
        this.flame3_r2 = root.getChild("head").getChild("HeadDecor").getChild("Flame1").getChild("flame3_r2");
        this.flame2_r2 = root.getChild("head").getChild("HeadDecor").getChild("Flame1").getChild("flame2_r2");
        this.Flame2 = root.getChild("head").getChild("HeadDecor").getChild("Flame2");
        this.Flame3 = root.getChild("head").getChild("HeadDecor").getChild("Flame3");
        this.Flame4 = root.getChild("head").getChild("HeadDecor").getChild("Flame4");
        this.flame3_r3 = root.getChild("head").getChild("HeadDecor").getChild("Flame4").getChild("flame3_r3");
        this.flame4_r1 = root.getChild("head").getChild("HeadDecor").getChild("Flame4").getChild("flame4_r1");
        this.flame4_r2 = root.getChild("head").getChild("HeadDecor").getChild("Flame4").getChild("flame4_r2");
        this.flame3_r4 = root.getChild("head").getChild("HeadDecor").getChild("Flame4").getChild("flame3_r4");
        this.Horn1 = root.getChild("head").getChild("HeadDecor").getChild("Horn1");
        this.cube_r1 = root.getChild("head").getChild("HeadDecor").getChild("Horn1").getChild("cube_r1");
        this.cube_r2 = root.getChild("head").getChild("HeadDecor").getChild("Horn1").getChild("cube_r2");
        this.Flame5 = root.getChild("head").getChild("HeadDecor").getChild("Flame5");
        this.Horn2 = root.getChild("head").getChild("HeadDecor").getChild("Horn2");
        this.cube_r3 = root.getChild("head").getChild("HeadDecor").getChild("Horn2").getChild("cube_r3");
        this.cube_r4 = root.getChild("head").getChild("HeadDecor").getChild("Horn2").getChild("cube_r4");
        this.BeltPads1 = root.getChild("body").getChild("BeltPads1");
        this.bone7 = root.getChild("body").getChild("BeltPads1").getChild("bone7");
        this.bone8 = root.getChild("body").getChild("BeltPads1").getChild("bone8");
        this.bone9 = root.getChild("body").getChild("BeltPads1").getChild("bone9");
        this.BeltPads2 = root.getChild("body").getChild("BeltPads2");
        this.bone6 = root.getChild("body").getChild("BeltPads2").getChild("bone6");
        this.bone10 = root.getChild("body").getChild("BeltPads2").getChild("bone10");
        this.bone11 = root.getChild("body").getChild("BeltPads2").getChild("bone11");
        this.rightShoulderPad = root.getChild("right_arm").getChild("rightShoulderPad");
        this.Shoulderpadlr_r1 = root.getChild("right_arm").getChild("rightShoulderPad").getChild("Shoulderpadlr_r1");
        this.Shoulderpadll_r1 = root.getChild("right_arm").getChild("rightShoulderPad").getChild("Shoulderpadll_r1");
        this.sword = root.getChild("right_arm").getChild("sword");
        this.leftShoulderPad = root.getChild("left_arm").getChild("leftShoulderPad");
        this.Shoulderpadlr_r2 = root.getChild("left_arm").getChild("leftShoulderPad").getChild("Shoulderpadlr_r2");
        this.Shoulderpadll_r2 = root.getChild("left_arm").getChild("leftShoulderPad").getChild("Shoulderpadll_r2");
        this.rightDress = root.getChild("right_leg").getChild("rightDress");
        this.Dressb_r1 = root.getChild("right_leg").getChild("rightDress").getChild("Dressb_r1");
        this.Dressf_r1 = root.getChild("right_leg").getChild("rightDress").getChild("Dressf_r1");
        this.BeltPads3 = root.getChild("right_leg").getChild("BeltPads3");
        this.bone18 = root.getChild("right_leg").getChild("BeltPads3").getChild("bone18");
        this.beltpadtr_r1 = root.getChild("right_leg").getChild("BeltPads3").getChild("bone18").getChild("beltpadtr_r1");
        this.bone19 = root.getChild("right_leg").getChild("BeltPads3").getChild("bone19");
        this.beltpadtr_r2 = root.getChild("right_leg").getChild("BeltPads3").getChild("bone19").getChild("beltpadtr_r2");
        this.bone20 = root.getChild("right_leg").getChild("BeltPads3").getChild("bone20");
        this.beltpadtr_r3 = root.getChild("right_leg").getChild("BeltPads3").getChild("bone20").getChild("beltpadtr_r3");
        this.leftDress = root.getChild("left_leg").getChild("leftDress");
        this.Dressb_r2 = root.getChild("left_leg").getChild("leftDress").getChild("Dressb_r2");
        this.Dressf_r2 = root.getChild("left_leg").getChild("leftDress").getChild("Dressf_r2");
        this.BeltPads4 = root.getChild("left_leg").getChild("BeltPads4");
        this.bone21 = root.getChild("left_leg").getChild("BeltPads4").getChild("bone21");
        this.beltpadtr_r4 = root.getChild("left_leg").getChild("BeltPads4").getChild("bone21").getChild("beltpadtr_r4");
        this.bone22 = root.getChild("left_leg").getChild("BeltPads4").getChild("bone22");
        this.beltpadtr_r5 = root.getChild("left_leg").getChild("BeltPads4").getChild("bone22").getChild("beltpadtr_r5");
        this.bone23 = root.getChild("left_leg").getChild("BeltPads4").getChild("bone23");
        this.beltpadtr_r6 = root.getChild("left_leg").getChild("BeltPads4").getChild("bone23").getChild("beltpadtr_r6");
        this.bone3 = root.getChild("rightWing").getChild("bone3");
        this.rightClaw = root.getChild("rightWing").getChild("bone3").getChild("rightClaw");
        this.finger5_r1 = root.getChild("rightWing").getChild("bone3").getChild("rightClaw").getChild("finger5_r1");
        this.finger4_r1 = root.getChild("rightWing").getChild("bone3").getChild("rightClaw").getChild("finger4_r1");
        this.finger3_r1 = root.getChild("rightWing").getChild("bone3").getChild("rightClaw").getChild("finger3_r1");
        this.finger2_r1 = root.getChild("rightWing").getChild("bone3").getChild("rightClaw").getChild("finger2_r1");
        this.thumb_r1 = root.getChild("rightWing").getChild("bone3").getChild("rightClaw").getChild("thumb_r1");
        this.flap1 = root.getChild("rightWing").getChild("bone3").getChild("flap1");
        this.flap2 = root.getChild("rightWing").getChild("bone3").getChild("flap2");
        this.flap3 = root.getChild("rightWing").getChild("bone3").getChild("flap3");
        this.flap4 = root.getChild("rightWing").getChild("bone3").getChild("flap4");
        this.flap5 = root.getChild("rightWing").getChild("bone3").getChild("flap5");
        this.flap6 = root.getChild("rightWing").getChild("bone3").getChild("flap6");
        this.flap7 = root.getChild("rightWing").getChild("bone3").getChild("flap7");
        this.flap8 = root.getChild("rightWing").getChild("bone3").getChild("flap8");
        this.bone2 = root.getChild("leftWing").getChild("bone2");
        this.leftClaw = root.getChild("leftWing").getChild("bone2").getChild("leftClaw");
        this.finger5_r2 = root.getChild("leftWing").getChild("bone2").getChild("leftClaw").getChild("finger5_r2");
        this.finger4_r2 = root.getChild("leftWing").getChild("bone2").getChild("leftClaw").getChild("finger4_r2");
        this.finger3_r2 = root.getChild("leftWing").getChild("bone2").getChild("leftClaw").getChild("finger3_r2");
        this.finger2_r2 = root.getChild("leftWing").getChild("bone2").getChild("leftClaw").getChild("finger2_r2");
        this.thumb_r2 = root.getChild("leftWing").getChild("bone2").getChild("leftClaw").getChild("thumb_r2");
        this.flap9 = root.getChild("leftWing").getChild("bone2").getChild("flap9");
        this.flap10 = root.getChild("leftWing").getChild("bone2").getChild("flap10");
        this.flap11 = root.getChild("leftWing").getChild("bone2").getChild("flap11");
        this.flap12 = root.getChild("leftWing").getChild("bone2").getChild("flap12");
        this.flap13 = root.getChild("leftWing").getChild("bone2").getChild("flap13");
        this.flap14 = root.getChild("leftWing").getChild("bone2").getChild("flap14");
        this.flap15 = root.getChild("leftWing").getChild("bone2").getChild("flap15");
        this.flap16 = root.getChild("leftWing").getChild("bone2").getChild("flap16");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(3, 3)
                        .addBox(-4.0F, -8.0F, -4.0F, 8, 8, 7)
                        .texOffs(33, 73)
                        .addBox(-3.5F, -2.1F, -3.8F, 7, 2, 0)
                        .texOffs(40, 74)
                        .addBox(-3.5F, -5.1F, -3.8F, 3, 2, 0)
                        .texOffs(40, 74)
                        .addBox(0.5F, -5.1F, -3.8F, 3, 2, 0),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_chin_r1 = p_head.addOrReplaceChild("chin_r1",
                CubeListBuilder.create()
                        .texOffs(32, 25)
                        .addBox(-1.5F, -1.5F, -1.15F, 5, 1, 4),
                PartPose.offsetAndRotation(-1.0F, 1.0F, -2.5F, 0.3054F, 0.0F, 0.0F));
        PartDefinition p_bipedHead_r1 = p_head.addOrReplaceChild("bipedHead_r1",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(40, 74)
                        .addBox(-1.0F, -1.5F, 0.0F, 3, 3, 0)
                        .mirror(false),
                PartPose.offsetAndRotation(3.75F, -1.85F, -2.55F, 0.0F, -1.5708F, 0.0F));
        PartDefinition p_bipedHead_r2 = p_head.addOrReplaceChild("bipedHead_r2",
                CubeListBuilder.create()
                        .texOffs(40, 74)
                        .addBox(-3.25F, 0.75F, -1.75F, 3, 3, 0),
                PartPose.offsetAndRotation(-2.0F, -4.1F, -3.8F, 0.0F, 1.5708F, 0.0F));
        PartDefinition p_hair = p_head.addOrReplaceChild("hair",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.1634F, 3.0945F));
        PartDefinition p_Hair_r1 = p_hair.addOrReplaceChild("Hair_r1",
                CubeListBuilder.create()
                        .texOffs(0, 85)
                        .addBox(-5.0F, -1.5F, -0.25F, 10, 3, 0),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.2654F, 0.0F, 0.0F));
        PartDefinition p_Hair_r2 = p_hair.addOrReplaceChild("Hair_r2",
                CubeListBuilder.create()
                        .texOffs(0, 85)
                        .addBox(-5.0F, -1.5F, 0.0F, 10, 3, 0),
                PartPose.offsetAndRotation(0.0F, -1.4142F, 1.4142F, -0.7854F, 0.0F, 0.0F));
        PartDefinition p_Hair_r3 = p_hair.addOrReplaceChild("Hair_r3",
                CubeListBuilder.create()
                        .texOffs(0, 98)
                        .addBox(-5.0F, -3.6175F, -4.5583F, 10, 0, 10)
                        .texOffs(0, 75)
                        .addBox(-5.0F, -9.3675F, -4.5583F, 10, 8, 10, new CubeDeformation(-2.0F))
                        .texOffs(0, 75)
                        .addBox(-5.0F, -5.6175F, -4.5583F, 10, 8, 10),
                PartPose.offsetAndRotation(0.0F, -5.546F, -2.6362F, -1.0036F, 0.0F, 0.0F));
        PartDefinition p_Hair_r4 = p_hair.addOrReplaceChild("Hair_r4",
                CubeListBuilder.create()
                        .texOffs(0, 75)
                        .addBox(-5.0F, -4.0F, -5.0F, 10, 8, 10, new CubeDeformation(-1.0F)),
                PartPose.offsetAndRotation(0.0F, -6.9828F, 0.4412F, -1.0036F, 0.0F, 0.0F));
        PartDefinition p_Nose = p_head.addOrReplaceChild("Nose",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, -27.5F, -7.0F, 1, 1, 3)
                        .texOffs(0, 4)
                        .addBox(-1.5F, -27.5F, -4.3F, 3, 1, 1),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition p_bridge_r1 = p_Nose.addOrReplaceChild("bridge_r1",
                CubeListBuilder.create()
                        .texOffs(5, 0)
                        .addBox(-0.5F, -0.8F, -0.5F, 1, 2, 1),
                PartPose.offsetAndRotation(0.0F, -28.0F, -3.8F, -0.6109F, 0.0F, 0.0F));
        PartDefinition p_HeadDecor = p_head.addOrReplaceChild("HeadDecor",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition p_Flame1 = p_HeadDecor.addOrReplaceChild("Flame1",
                CubeListBuilder.create()
                        .texOffs(0, 60)
                        .addBox(-1.0F, -3.5F, -1.0F, 2, 5, 0),
                PartPose.offsetAndRotation(-5.75F, -29.0F, -3.0F, 0.7418F, -2.4871F, -1.2654F));
        PartDefinition p_flame2_r1 = p_Flame1.addOrReplaceChild("flame2_r1",
                CubeListBuilder.create()
                        .texOffs(0, 18)
                        .addBox(-0.25F, -4.0F, -1.0F, 2, 6, 0),
                PartPose.offsetAndRotation(2.75F, 0.0F, -1.0F, -0.48F, -0.1745F, 2.3126F));
        PartDefinition p_flame3_r1 = p_Flame1.addOrReplaceChild("flame3_r1",
                CubeListBuilder.create()
                        .texOffs(0, 60)
                        .addBox(-0.5F, -2.0F, 0.25F, 1, 5, 0),
                PartPose.offsetAndRotation(1.7206F, -0.7186F, -1.2151F, -0.2182F, -0.3491F, 0.4363F));
        PartDefinition p_flame3_r2 = p_Flame1.addOrReplaceChild("flame3_r2",
                CubeListBuilder.create()
                        .texOffs(0, 60)
                        .addBox(-0.75F, -3.0F, -0.5F, 1, 5, 0),
                PartPose.offsetAndRotation(-1.9379F, 0.15F, -0.5178F, -0.0873F, -0.3491F, -0.7854F));
        PartDefinition p_flame2_r2 = p_Flame1.addOrReplaceChild("flame2_r2",
                CubeListBuilder.create()
                        .texOffs(0, 60)
                        .addBox(0.0F, -3.0F, -0.5F, 1, 5, 0),
                PartPose.offsetAndRotation(-1.9379F, 0.15F, -0.5178F, -0.2182F, -0.3491F, -0.3927F));
        PartDefinition p_Flame2 = p_HeadDecor.addOrReplaceChild("Flame2",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 36.0F, 0.0F));
        PartDefinition p_Flame3 = p_HeadDecor.addOrReplaceChild("Flame3",
                CubeListBuilder.create()
                        .texOffs(12, 62)
                        .addBox(-1.0F, -3.0F, 0.0F, 2, 5, 0),
                PartPose.offsetAndRotation(-1.0F, -33.75F, -2.75F, -0.5236F, 0.0F, -0.2182F));
        PartDefinition p_Flame4 = p_HeadDecor.addOrReplaceChild("Flame4",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 60)
                        .addBox(-1.0F, -3.5F, -1.0F, 2, 5, 0)
                        .mirror(false),
                PartPose.offsetAndRotation(5.75F, -29.0F, -3.0F, 0.7418F, 2.4871F, 1.2654F));
        PartDefinition p_flame3_r3 = p_Flame4.addOrReplaceChild("flame3_r3",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 18)
                        .addBox(-1.75F, -4.0F, -1.0F, 2, 6, 0)
                        .mirror(false),
                PartPose.offsetAndRotation(-2.75F, 0.0F, -1.0F, -0.48F, 0.1745F, -2.3126F));
        PartDefinition p_flame4_r1 = p_Flame4.addOrReplaceChild("flame4_r1",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 60)
                        .addBox(-0.5F, -2.0F, 0.25F, 1, 5, 0)
                        .mirror(false),
                PartPose.offsetAndRotation(-1.7206F, -0.7186F, -1.2151F, -0.2182F, 0.3491F, -0.4363F));
        PartDefinition p_flame4_r2 = p_Flame4.addOrReplaceChild("flame4_r2",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 60)
                        .addBox(-0.25F, -3.0F, -0.5F, 1, 5, 0)
                        .mirror(false),
                PartPose.offsetAndRotation(1.9379F, 0.15F, -0.5178F, -0.0873F, 0.3491F, 0.7854F));
        PartDefinition p_flame3_r4 = p_Flame4.addOrReplaceChild("flame3_r4",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 60)
                        .addBox(-1.0F, -3.0F, -0.5F, 1, 5, 0)
                        .mirror(false),
                PartPose.offsetAndRotation(1.9379F, 0.15F, -0.5178F, -0.2182F, 0.3491F, 0.3927F));
        PartDefinition p_Horn1 = p_HeadDecor.addOrReplaceChild("Horn1",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_cube_r1 = p_Horn1.addOrReplaceChild("cube_r1",
                CubeListBuilder.create()
                        .texOffs(51, 63)
                        .addBox(-1.25F, -1.0F, -0.5F, 2, 2, 2),
                PartPose.offsetAndRotation(-2.25F, -31.75F, -4.0F, -0.6109F, 0.0F, 0.0F));
        PartDefinition p_cube_r2 = p_Horn1.addOrReplaceChild("cube_r2",
                CubeListBuilder.create()
                        .texOffs(51, 67)
                        .addBox(-0.5F, -0.95F, -0.5F, 1, 1, 2),
                PartPose.offsetAndRotation(-2.5F, -32.6104F, -5.2287F, -1.0472F, 0.0F, 0.0F));
        PartDefinition p_Flame5 = p_HeadDecor.addOrReplaceChild("Flame5",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(12, 62)
                        .addBox(-1.0F, -3.0F, 0.0F, 2, 5, 0)
                        .mirror(false),
                PartPose.offsetAndRotation(1.0F, -33.75F, -2.75F, -0.5236F, 0.0F, 0.2182F));
        PartDefinition p_Horn2 = p_HeadDecor.addOrReplaceChild("Horn2",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_cube_r3 = p_Horn2.addOrReplaceChild("cube_r3",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(51, 63)
                        .addBox(-0.75F, -1.0F, -0.5F, 2, 2, 2)
                        .mirror(false),
                PartPose.offsetAndRotation(2.25F, -31.75F, -4.0F, -0.6109F, 0.0F, 0.0F));
        PartDefinition p_cube_r4 = p_Horn2.addOrReplaceChild("cube_r4",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(51, 67)
                        .addBox(-0.5F, -0.95F, -0.5F, 1, 1, 2)
                        .mirror(false),
                PartPose.offsetAndRotation(2.5F, -32.6104F, -5.2287F, -1.0472F, 0.0F, 0.0F));
        PartDefinition p_field_178720_f = root.addOrReplaceChild("field_178720_f",
                CubeListBuilder.create()
                        .texOffs(60, 0)
                        .addBox(-2.95F, -5.0F, -4.1F, 6, 1, 0),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(28, 30)
                        .addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4)
                        .texOffs(24, 18)
                        .addBox(-4.5F, 10.0F, -2.5F, 9, 2, 5),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_BeltPads1 = p_body.addOrReplaceChild("BeltPads1",
                CubeListBuilder.create(),
                PartPose.offset(4.0F, 10.5F, 0.0F));
        PartDefinition p_bone7 = p_BeltPads1.addOrReplaceChild("bone7",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(86, 55)
                        .addBox(-1.1F, 0.875F, -2.0F, 1, 3, 4)
                        .mirror(false),
                PartPose.offsetAndRotation(-8.0F, 1.0F, 0.0F, 0.0F, 0.0F, 2.8362F));
        PartDefinition p_bone8 = p_BeltPads1.addOrReplaceChild("bone8",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(62, 55)
                        .addBox(-1.1F, 1.0F, -1.25F, 1, 3, 3)
                        .mirror(false),
                PartPose.offsetAndRotation(-6.5F, 0.9F, -2.0F, -0.7011F, 1.3355F, 2.3663F));
        PartDefinition p_bone9 = p_BeltPads1.addOrReplaceChild("bone9",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(74, 55)
                        .addBox(-1.1F, 1.0F, -1.75F, 1, 3, 3)
                        .mirror(false),
                PartPose.offsetAndRotation(-6.5F, 0.9F, 2.0F, 0.7011F, -1.3355F, 2.3663F));
        PartDefinition p_BeltPads2 = p_body.addOrReplaceChild("BeltPads2",
                CubeListBuilder.create(),
                PartPose.offset(-4.0F, 10.5F, 0.0F));
        PartDefinition p_bone6 = p_BeltPads2.addOrReplaceChild("bone6",
                CubeListBuilder.create()
                        .texOffs(86, 55)
                        .addBox(0.1F, 0.925F, -2.0F, 1, 3, 4),
                PartPose.offsetAndRotation(8.0F, 1.0F, 0.0F, 0.0F, 0.0F, -2.8362F));
        PartDefinition p_bone10 = p_BeltPads2.addOrReplaceChild("bone10",
                CubeListBuilder.create()
                        .texOffs(62, 55)
                        .addBox(0.1F, 1.0F, -1.25F, 1, 3, 3),
                PartPose.offsetAndRotation(6.5F, 0.9F, -2.0F, -0.7011F, -1.3355F, -2.3663F));
        PartDefinition p_bone11 = p_BeltPads2.addOrReplaceChild("bone11",
                CubeListBuilder.create()
                        .texOffs(74, 55)
                        .addBox(0.1F, 1.0F, -1.75F, 1, 3, 3),
                PartPose.offsetAndRotation(6.5F, 0.9F, 2.0F, 0.7011F, 1.3355F, -2.3663F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(49, 0)
                        .addBox(-2.5F, 5.0F, -1.5F, 3, 5, 3)
                        .texOffs(0, 34)
                        .addBox(-3.0F, -2.0F, -2.0F, 4, 7, 4),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition p_rightShoulderPad = p_right_arm.addOrReplaceChild("rightShoulderPad",
                CubeListBuilder.create(),
                PartPose.offset(-3.5F, 0.5F, 1.5F));
        PartDefinition p_Shoulderpadlr_r1 = p_rightShoulderPad.addOrReplaceChild("Shoulderpadlr_r1",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(38, 46)
                        .addBox(-0.5F, -3.5F, -2.25F, 1, 7, 4, new CubeDeformation(0.05F))
                        .mirror(false),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.7925F, 0.1745F));
        PartDefinition p_Shoulderpadll_r1 = p_rightShoulderPad.addOrReplaceChild("Shoulderpadll_r1",
                CubeListBuilder.create()
                        .texOffs(38, 46)
                        .addBox(-0.5F, -3.5F, -2.25F, 1, 7, 4, new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, 0.0F, -0.3491F, 0.1745F));
        PartDefinition p_sword = p_right_arm.addOrReplaceChild("sword",
                CubeListBuilder.create()
                        .texOffs(76, 0)
                        .addBox(1.5F, 4.0F, -6.0F, 1, 2, 8, new CubeDeformation(-0.2F))
                        .texOffs(74, 0)
                        .addBox(2.0F, 3.0F, -26.0F, 0, 4, 20)
                        .texOffs(77, 0)
                        .addBox(1.5F, 2.55F, -8.4F, 1, 5, 2, new CubeDeformation(0.3F))
                        .texOffs(87, 0)
                        .addBox(1.5F, 3.55F, -6.15F, 1, 3, 1, new CubeDeformation(-0.1F)),
                PartPose.offset(-3.0F, 3.85F, 3.0F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(49, 0)
                        .addBox(-0.5F, 5.0F, -1.5F, 3, 5, 3)
                        .mirror(false)
                        .mirror()
                        .texOffs(0, 34)
                        .addBox(-1.0F, -2.0F, -2.0F, 4, 7, 4)
                        .mirror(false),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition p_leftShoulderPad = p_left_arm.addOrReplaceChild("leftShoulderPad",
                CubeListBuilder.create(),
                PartPose.offset(3.5F, 0.5F, 1.5F));
        PartDefinition p_Shoulderpadlr_r2 = p_leftShoulderPad.addOrReplaceChild("Shoulderpadlr_r2",
                CubeListBuilder.create()
                        .texOffs(38, 46)
                        .addBox(-0.5F, -3.5F, -2.25F, 1, 7, 4, new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 2.7925F, -0.1745F));
        PartDefinition p_Shoulderpadll_r2 = p_leftShoulderPad.addOrReplaceChild("Shoulderpadll_r2",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(38, 46)
                        .addBox(-0.5F, -3.5F, -2.25F, 1, 7, 4, new CubeDeformation(0.05F))
                        .mirror(false),
                PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, 0.0F, 0.3491F, -0.1745F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(47, 16)
                        .addBox(-1.5F, 9.0F, -1.5F, 3, 3, 3)
                        .texOffs(0, 45)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 9, 4),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition p_rightDress = p_right_leg.addOrReplaceChild("rightDress",
                CubeListBuilder.create()
                        .texOffs(27, 0)
                        .addBox(-4.0F, 0.0F, -2.5F, 4, 6, 3),
                PartPose.offset(1.9F, 0.0F, 1.0F));
        PartDefinition p_Dressb_r1 = p_rightDress.addOrReplaceChild("Dressb_r1",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(36, 9)
                        .addBox(0.0F, -3.0F, -0.5F, 4, 6, 1)
                        .mirror(false),
                PartPose.offsetAndRotation(0.0F, 2.9753F, 0.6304F, -0.0436F, -3.1416F, 0.0F));
        PartDefinition p_Dressf_r1 = p_rightDress.addOrReplaceChild("Dressf_r1",
                CubeListBuilder.create()
                        .texOffs(36, 9)
                        .addBox(-4.0F, 0.0F, -1.0F, 4, 6, 1),
                PartPose.offsetAndRotation(0.0F, 0.0F, -2.0F, -0.0436F, 0.0F, 0.0F));
        PartDefinition p_BeltPads3 = p_right_leg.addOrReplaceChild("BeltPads3",
                CubeListBuilder.create(),
                PartPose.offset(-1.1F, -0.5F, 0.0F));
        PartDefinition p_bone18 = p_BeltPads3.addOrReplaceChild("bone18",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-1.0F, -1.0F, 0.0F, 0.0F, 0.0F, -2.8362F));
        PartDefinition p_beltpadtr_r1 = p_bone18.addOrReplaceChild("beltpadtr_r1",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(87, 41)
                        .addBox(0.5125F, -4.5527F, -2.0F, 1, 6, 4)
                        .mirror(false),
                PartPose.offsetAndRotation(-1.6125F, -2.2473F, 0.0F, 0.0F, 0.0F, -0.1309F));
        PartDefinition p_bone19 = p_BeltPads3.addOrReplaceChild("bone19",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.5F, -0.9F, -2.0F, 0.7011F, 1.3355F, -2.3663F));
        PartDefinition p_beltpadtr_r2 = p_bone19.addOrReplaceChild("beltpadtr_r2",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(62, 42)
                        .addBox(-0.5F, -3.0F, -1.5F, 1, 6, 3)
                        .mirror(false),
                PartPose.offsetAndRotation(-0.6F, -4.0F, 0.5F, 0.0F, 0.0F, -0.0436F));
        PartDefinition p_bone20 = p_BeltPads3.addOrReplaceChild("bone20",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.5F, -0.9F, 2.0F, -0.7011F, -1.3355F, -2.3663F));
        PartDefinition p_beltpadtr_r3 = p_bone20.addOrReplaceChild("beltpadtr_r3",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(75, 42)
                        .addBox(-0.5F, -3.0F, -2.0F, 1, 6, 3)
                        .mirror(false),
                PartPose.offsetAndRotation(-0.6F, -4.0F, 0.0F, 0.0F, 0.0F, -0.0873F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(47, 16)
                        .addBox(-1.5F, 9.0F, -1.5F, 3, 3, 3)
                        .mirror(false)
                        .mirror()
                        .texOffs(0, 45)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 9, 4)
                        .mirror(false),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        PartDefinition p_leftDress = p_left_leg.addOrReplaceChild("leftDress",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(27, 0)
                        .addBox(0.0F, 0.0F, -2.5F, 4, 6, 3)
                        .mirror(false),
                PartPose.offset(-1.9F, 0.0F, 1.0F));
        PartDefinition p_Dressb_r2 = p_leftDress.addOrReplaceChild("Dressb_r2",
                CubeListBuilder.create()
                        .texOffs(36, 9)
                        .addBox(-4.0F, -3.0F, -0.5F, 4, 6, 1),
                PartPose.offsetAndRotation(0.0F, 2.9753F, 0.6304F, -0.0436F, 3.1416F, 0.0F));
        PartDefinition p_Dressf_r2 = p_leftDress.addOrReplaceChild("Dressf_r2",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(36, 9)
                        .addBox(0.0F, 0.0F, -1.0F, 4, 6, 1)
                        .mirror(false),
                PartPose.offsetAndRotation(0.0F, 0.0F, -2.0F, -0.0436F, 0.0F, 0.0F));
        PartDefinition p_BeltPads4 = p_left_leg.addOrReplaceChild("BeltPads4",
                CubeListBuilder.create(),
                PartPose.offset(1.1F, -0.5F, 0.0F));
        PartDefinition p_bone21 = p_BeltPads4.addOrReplaceChild("bone21",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(1.0F, -1.0F, 0.0F, 0.0F, 0.0F, 2.8362F));
        PartDefinition p_beltpadtr_r4 = p_bone21.addOrReplaceChild("beltpadtr_r4",
                CubeListBuilder.create()
                        .texOffs(87, 41)
                        .addBox(-1.5125F, -4.6027F, -2.0F, 1, 6, 4),
                PartPose.offsetAndRotation(1.6125F, -2.2473F, 0.0F, 0.0F, 0.0F, 0.1309F));
        PartDefinition p_bone22 = p_BeltPads4.addOrReplaceChild("bone22",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.5F, -0.9F, -2.0F, 0.7011F, -1.3355F, 2.3663F));
        PartDefinition p_beltpadtr_r5 = p_bone22.addOrReplaceChild("beltpadtr_r5",
                CubeListBuilder.create()
                        .texOffs(62, 42)
                        .addBox(-0.5F, -3.0F, -1.0F, 1, 6, 3),
                PartPose.offsetAndRotation(0.6F, -4.0F, 0.0F, 0.0F, 0.0F, 0.0436F));
        PartDefinition p_bone23 = p_BeltPads4.addOrReplaceChild("bone23",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.5F, -0.9F, 2.0F, -0.7011F, 1.3355F, 2.3663F));
        PartDefinition p_beltpadtr_r6 = p_bone23.addOrReplaceChild("beltpadtr_r6",
                CubeListBuilder.create()
                        .texOffs(75, 42)
                        .addBox(-0.5F, -3.0F, -2.0F, 1, 6, 3),
                PartPose.offsetAndRotation(0.6F, -4.0F, 0.0F, 0.0F, 0.0F, 0.0873F));
        PartDefinition p_rightWing = root.addOrReplaceChild("rightWing",
                CubeListBuilder.create()
                        .texOffs(4, 6)
                        .addBox(-0.5F, -0.5F, 0.0F, 1, 1, 1),
                PartPose.offsetAndRotation(-1.5F, 2.5F, 2.0F, 0.0F, 0.0F, -0.4363F));
        PartDefinition p_bone3 = p_rightWing.addOrReplaceChild("bone3",
                CubeListBuilder.create()
                        .texOffs(16, 34)
                        .addBox(-3.5F, -15.5F, 0.0F, 4, 16, 1),
                PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, -0.2618F, 0.0F, 0.0F));
        PartDefinition p_rightClaw = p_bone3.addOrReplaceChild("rightClaw",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(1.0523F, -12.7159F, -0.1F, 0.0F, 0.3054F, 1.2217F));
        PartDefinition p_finger5_r1 = p_rightClaw.addOrReplaceChild("finger5_r1",
                CubeListBuilder.create()
                        .texOffs(0, 6)
                        .addBox(-0.2499F, -0.6416F, -1.5148F, 1, 1, 2),
                PartPose.offsetAndRotation(-3.6024F, 1.2574F, 0.2148F, -0.1309F, 0.829F, 0.6981F));
        PartDefinition p_finger4_r1 = p_rightClaw.addOrReplaceChild("finger4_r1",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(0.5F, -0.5F, -2.3951F, 1, 1, 3),
                PartPose.offsetAndRotation(-1.1831F, 0.5692F, 0.3951F, -0.7418F, 0.0436F, 0.1309F));
        PartDefinition p_finger3_r1 = p_rightClaw.addOrReplaceChild("finger3_r1",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, -0.5F, -2.5951F, 1, 1, 3),
                PartPose.offsetAndRotation(-1.1831F, 0.5692F, 0.3951F, -0.7418F, 0.0F, -0.0436F));
        PartDefinition p_finger2_r1 = p_rightClaw.addOrReplaceChild("finger2_r1",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.7319F, -0.5F, -2.1264F, 1, 1, 3),
                PartPose.offsetAndRotation(-2.0204F, 0.5145F, 0.0264F, -0.7418F, 0.0F, -0.3491F));
        PartDefinition p_thumb_r1 = p_rightClaw.addOrReplaceChild("thumb_r1",
                CubeListBuilder.create()
                        .texOffs(0, 6)
                        .addBox(-0.9421F, 0.2F, -1.0F, 1, 1, 2),
                PartPose.offsetAndRotation(1.8898F, 1.2159F, -0.2763F, -0.0436F, -0.2618F, 0.6109F));
        PartDefinition p_flap1 = p_bone3.addOrReplaceChild("flap1",
                CubeListBuilder.create()
                        .texOffs(26, 46)
                        .addBox(-1.0F, 0.0F, -0.5F, 2, 16, 1, new CubeDeformation(0.2F)),
                PartPose.offset(-1.5F, -14.5F, 0.5F));
        PartDefinition p_flap2 = p_bone3.addOrReplaceChild("flap2",
                CubeListBuilder.create()
                        .texOffs(26, 46)
                        .addBox(-1.0F, 0.0F, -0.5F, 2, 16, 1, new CubeDeformation(0.2F)),
                PartPose.offset(-1.5F, -12.5F, 0.5F));
        PartDefinition p_flap3 = p_bone3.addOrReplaceChild("flap3",
                CubeListBuilder.create()
                        .texOffs(26, 46)
                        .addBox(-1.0F, 0.0F, -0.5F, 2, 16, 1, new CubeDeformation(0.2F)),
                PartPose.offset(-1.5F, -10.5F, 0.5F));
        PartDefinition p_flap4 = p_bone3.addOrReplaceChild("flap4",
                CubeListBuilder.create()
                        .texOffs(26, 46)
                        .addBox(-1.0F, 0.0F, -0.5F, 2, 16, 1, new CubeDeformation(0.2F)),
                PartPose.offset(-1.5F, -8.5F, 0.5F));
        PartDefinition p_flap5 = p_bone3.addOrReplaceChild("flap5",
                CubeListBuilder.create()
                        .texOffs(26, 46)
                        .addBox(-1.0F, -1.0F, -0.5F, 2, 16, 1, new CubeDeformation(0.2F)),
                PartPose.offset(-1.5F, -6.5F, 0.5F));
        PartDefinition p_flap6 = p_bone3.addOrReplaceChild("flap6",
                CubeListBuilder.create()
                        .texOffs(32, 46)
                        .addBox(-1.0F, 0.0F, -0.5F, 2, 14, 1, new CubeDeformation(0.2F)),
                PartPose.offset(-1.5F, -4.5F, 0.5F));
        PartDefinition p_flap7 = p_bone3.addOrReplaceChild("flap7",
                CubeListBuilder.create()
                        .texOffs(48, 46)
                        .addBox(-1.0F, 0.0F, -0.5F, 2, 13, 1, new CubeDeformation(0.1F)),
                PartPose.offset(-1.5F, -2.5F, 0.5F));
        PartDefinition p_flap8 = p_bone3.addOrReplaceChild("flap8",
                CubeListBuilder.create()
                        .texOffs(48, 46)
                        .addBox(-1.0F, 0.0F, -0.5F, 2, 13, 1, new CubeDeformation(0.1F)),
                PartPose.offset(-0.5F, -0.5F, 0.5F));
        PartDefinition p_leftWing = root.addOrReplaceChild("leftWing",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(4, 6)
                        .addBox(-0.5F, -0.5F, 0.0F, 1, 1, 1)
                        .mirror(false),
                PartPose.offsetAndRotation(1.5F, 2.5F, 2.0F, 0.0F, 0.0F, 0.4363F));
        PartDefinition p_bone2 = p_leftWing.addOrReplaceChild("bone2",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(16, 34)
                        .addBox(-0.5F, -15.5F, 0.0F, 4, 16, 1)
                        .mirror(false),
                PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, -0.2618F, 0.0F, 0.0F));
        PartDefinition p_leftClaw = p_bone2.addOrReplaceChild("leftClaw",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-1.0523F, -12.7159F, -0.1F, 0.0F, -0.3054F, -1.2217F));
        PartDefinition p_finger5_r2 = p_leftClaw.addOrReplaceChild("finger5_r2",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 6)
                        .addBox(-0.7501F, -0.6416F, -1.5148F, 1, 1, 2)
                        .mirror(false),
                PartPose.offsetAndRotation(3.6024F, 1.2574F, 0.2148F, -0.1309F, -0.829F, -0.6981F));
        PartDefinition p_finger4_r2 = p_leftClaw.addOrReplaceChild("finger4_r2",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 0)
                        .addBox(-1.5F, -0.5F, -2.3951F, 1, 1, 3)
                        .mirror(false),
                PartPose.offsetAndRotation(1.1831F, 0.5692F, 0.3951F, -0.7418F, -0.0436F, -0.1309F));
        PartDefinition p_finger3_r2 = p_leftClaw.addOrReplaceChild("finger3_r2",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 0)
                        .addBox(-0.5F, -0.5F, -2.5951F, 1, 1, 3)
                        .mirror(false),
                PartPose.offsetAndRotation(1.1831F, 0.5692F, 0.3951F, -0.7418F, 0.0F, 0.0436F));
        PartDefinition p_finger2_r2 = p_leftClaw.addOrReplaceChild("finger2_r2",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 0)
                        .addBox(-0.2681F, -0.5F, -2.1264F, 1, 1, 3)
                        .mirror(false),
                PartPose.offsetAndRotation(2.0204F, 0.5145F, 0.0264F, -0.7418F, 0.0F, 0.3491F));
        PartDefinition p_thumb_r2 = p_leftClaw.addOrReplaceChild("thumb_r2",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 6)
                        .addBox(-0.0579F, 0.2F, -1.0F, 1, 1, 2)
                        .mirror(false),
                PartPose.offsetAndRotation(-1.8898F, 1.2159F, -0.2763F, -0.0436F, 0.2618F, -0.6109F));
        PartDefinition p_flap9 = p_bone2.addOrReplaceChild("flap9",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(26, 46)
                        .addBox(-1.0F, 0.0F, -0.5F, 2, 16, 1, new CubeDeformation(0.2F))
                        .mirror(false),
                PartPose.offset(1.5F, -14.5F, 0.5F));
        PartDefinition p_flap10 = p_bone2.addOrReplaceChild("flap10",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(26, 46)
                        .addBox(-1.0F, 0.0F, -0.5F, 2, 16, 1, new CubeDeformation(0.2F))
                        .mirror(false),
                PartPose.offset(1.5F, -12.5F, 0.5F));
        PartDefinition p_flap11 = p_bone2.addOrReplaceChild("flap11",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(26, 46)
                        .addBox(-1.0F, 0.0F, -0.5F, 2, 16, 1, new CubeDeformation(0.2F))
                        .mirror(false),
                PartPose.offset(1.5F, -10.5F, 0.5F));
        PartDefinition p_flap12 = p_bone2.addOrReplaceChild("flap12",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(26, 46)
                        .addBox(-1.0F, 0.0F, -0.5F, 2, 16, 1, new CubeDeformation(0.2F))
                        .mirror(false),
                PartPose.offset(1.5F, -8.5F, 0.5F));
        PartDefinition p_flap13 = p_bone2.addOrReplaceChild("flap13",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(26, 46)
                        .addBox(-1.0F, -1.0F, -0.5F, 2, 16, 1, new CubeDeformation(0.2F))
                        .mirror(false),
                PartPose.offset(1.5F, -6.5F, 0.5F));
        PartDefinition p_flap14 = p_bone2.addOrReplaceChild("flap14",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(32, 46)
                        .addBox(-1.0F, 0.0F, -0.5F, 2, 14, 1, new CubeDeformation(0.2F))
                        .mirror(false),
                PartPose.offset(1.5F, -4.5F, 0.5F));
        PartDefinition p_flap15 = p_bone2.addOrReplaceChild("flap15",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(48, 46)
                        .addBox(-1.0F, 0.0F, -0.5F, 2, 13, 1, new CubeDeformation(0.1F))
                        .mirror(false),
                PartPose.offset(1.5F, -2.5F, 0.5F));
        PartDefinition p_flap16 = p_bone2.addOrReplaceChild("flap16",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(48, 46)
                        .addBox(-1.0F, 0.0F, -0.5F, 2, 13, 1, new CubeDeformation(0.1F))
                        .mirror(false),
                PartPose.offset(0.5F, -0.5F, 0.5F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /**
     * Drives the sword arm through a swing.
     *
     * The blade is a child of {@code right_arm} in the converted rig, exactly as it was in the
     * 1.12.2 model this came from, so turning the arm carries the sword with it and nothing
     * else has to move. An earlier attempt turned the entire giant instead, on the assumption
     * that the ported bodies were single meshes with no joint to use - they are not, and a
     * twenty-block figure rotating bodily to swing looks like it is being shoved rather than
     * striking.
     *
     * Angles follow the 1.12.2 convention this geometry was authored in, where a more negative
     * xRot lifts the arm forward and up; the original model raised its arm to -PI/2 to hold
     * the sword out, and these are the same units.
     *
     * @param progress 0 at the first frame of the swing through to 1 at the last, or negative
     *                 when there is no swing. Must be called EVERY frame including the
     *                 negative case: one model instance draws every Susanoo on screen, so a
     *                 pose left behind by the last one is inherited by the next.
     */
    public void swingSword(float progress) {
        if (progress < 0f) {
            this.right_arm.xRot = 0f;
            this.right_arm.zRot = 0f;
            return;
        }
        float xRot;
        float zRot;
        if (progress < WINDUP_END) {
            // Lifting it. Eased, so the blade gathers weight instead of snapping upright.
            float t = ease(progress / WINDUP_END);
            xRot = RAISED_X * t;
            zRot = RAISED_Z * t;
        } else if (progress < STRIKE_END) {
            // The cut. Linear and fast - this is the part that is supposed to be hard to read.
            float t = (progress - WINDUP_END) / (STRIKE_END - WINDUP_END);
            xRot = lerp(t, RAISED_X, FOLLOW_X);
            zRot = lerp(t, RAISED_Z, FOLLOW_Z);
        } else {
            // Bringing it back to guard.
            float t = ease((progress - STRIKE_END) / (1f - STRIKE_END));
            xRot = FOLLOW_X * (1f - t);
            zRot = FOLLOW_Z * (1f - t);
        }
        this.right_arm.xRot = xRot;
        this.right_arm.zRot = zRot;
    }

    /** Where the wind-up ends and where the cut ends, as fractions of the whole swing. */
    private static final float WINDUP_END = 0.30f;
    private static final float STRIKE_END = 0.60f;
    /** Overhead and slightly out, at the top of the wind-up. */
    private static final float RAISED_X = -2.75f;
    private static final float RAISED_Z = 0.35f;
    /** Down and across, at the end of the follow-through. */
    private static final float FOLLOW_X = -0.15f;
    private static final float FOLLOW_Z = -0.30f;

    private static float lerp(float t, float from, float to) {
        return from + (to - from) * t;
    }

    /** Smoothstep. Slow at both ends, quick through the middle. */
    private static float ease(float t) {
        float clamped = t < 0f ? 0f : (t > 1f ? 1f : t);
        return clamped * clamped * (3f - 2f * clamped);
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
        this.rightWing.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftWing.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
