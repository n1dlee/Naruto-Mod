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
 * BijuCloakModel — geometry converted from the 1.12.2 Naruto mod's ModelBiped-based
 * Susanoo model into the 1.20.1 LayerDefinition format. Cube offsets, texture UVs,
 * poses and per-box inflation are carried over verbatim; only the model API changed.
 */
public class BijuCloakModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(NarutoMod.MOD_ID, "biju_cloak"), "main");

    private final ModelPart head;
    private final ModelPart earLeft_0;
    private final ModelPart earLeft_1;
    private final ModelPart earLeft_2;
    private final ModelPart earLeft_3;
    private final ModelPart earLeft_4;
    private final ModelPart earLeft_5;
    private final ModelPart earLeft_6;
    private final ModelPart earLeft_7;
    private final ModelPart earLeft_8;
    private final ModelPart earRight_0;
    private final ModelPart earRight_1;
    private final ModelPart earRight_2;
    private final ModelPart earRight_3;
    private final ModelPart earRight_4;
    private final ModelPart earRight_5;
    private final ModelPart earRight_6;
    private final ModelPart earRight_7;
    private final ModelPart earRight_8;
    private final ModelPart field_178720_f;
    private final ModelPart sandEar;
    private final ModelPart cube_r1;
    private final ModelPart body;
    private final ModelPart allTails;
    private final ModelPart tail_0_0;
    private final ModelPart tail_0_1;
    private final ModelPart tail_0_2;
    private final ModelPart tail_0_3;
    private final ModelPart tail_0_4;
    private final ModelPart tail_0_5;
    private final ModelPart tail_0_6;
    private final ModelPart tail_0_7;
    private final ModelPart tail_1_0;
    private final ModelPart tail_1_1;
    private final ModelPart tail_1_2;
    private final ModelPart tail_1_3;
    private final ModelPart tail_1_4;
    private final ModelPart tail_1_5;
    private final ModelPart tail_1_6;
    private final ModelPart tail_1_7;
    private final ModelPart tail_2_0;
    private final ModelPart tail_2_1;
    private final ModelPart tail_2_2;
    private final ModelPart tail_2_3;
    private final ModelPart tail_2_4;
    private final ModelPart tail_2_5;
    private final ModelPart tail_2_6;
    private final ModelPart tail_2_7;
    private final ModelPart tail_3_0;
    private final ModelPart tail_3_1;
    private final ModelPart tail_3_2;
    private final ModelPart tail_3_3;
    private final ModelPart tail_3_4;
    private final ModelPart tail_3_5;
    private final ModelPart tail_3_6;
    private final ModelPart tail_3_7;
    private final ModelPart tail_4_0;
    private final ModelPart tail_4_1;
    private final ModelPart tail_4_2;
    private final ModelPart tail_4_3;
    private final ModelPart tail_4_4;
    private final ModelPart tail_4_5;
    private final ModelPart tail_4_6;
    private final ModelPart tail_4_7;
    private final ModelPart tail_5_0;
    private final ModelPart tail_5_1;
    private final ModelPart tail_5_2;
    private final ModelPart tail_5_3;
    private final ModelPart tail_5_4;
    private final ModelPart tail_5_5;
    private final ModelPart tail_5_6;
    private final ModelPart tail_5_7;
    private final ModelPart tail_6_0;
    private final ModelPart tail_6_1;
    private final ModelPart tail_6_2;
    private final ModelPart tail_6_3;
    private final ModelPart tail_6_4;
    private final ModelPart tail_6_5;
    private final ModelPart tail_6_6;
    private final ModelPart tail_6_7;
    private final ModelPart tail_7_0;
    private final ModelPart tail_7_1;
    private final ModelPart tail_7_2;
    private final ModelPart tail_7_3;
    private final ModelPart tail_7_4;
    private final ModelPart tail_7_5;
    private final ModelPart tail_7_6;
    private final ModelPart tail_7_7;
    private final ModelPart tail_8_0;
    private final ModelPart tail_8_1;
    private final ModelPart tail_8_2;
    private final ModelPart tail_8_3;
    private final ModelPart tail_8_4;
    private final ModelPart tail_8_5;
    private final ModelPart tail_8_6;
    private final ModelPart tail_8_7;
    private final ModelPart bipedBodyWear;
    private final ModelPart tailWears;
    private final ModelPart tailWear_0_0;
    private final ModelPart tailWear_0_1;
    private final ModelPart tailWear_0_2;
    private final ModelPart tailWear_0_3;
    private final ModelPart tailWear_0_4;
    private final ModelPart tailWear_0_5;
    private final ModelPart tailWear_0_6;
    private final ModelPart tailWear_0_7;
    private final ModelPart right_arm;
    private final ModelPart bipedRightArmWear;
    private final ModelPart sandArm;
    private final ModelPart left_arm;
    private final ModelPart bipedLeftArmWear;
    private final ModelPart right_leg;
    private final ModelPart bipedRightLegWear;
    private final ModelPart bipedRightLeg_r1;
    private final ModelPart bipedRightLeg_r2;
    private final ModelPart left_leg;
    private final ModelPart bipedLeftLegWear;
    private final ModelPart bipedLeftLeg_r1;
    private final ModelPart bipedLeftLeg_r2;

    public BijuCloakModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.head = root.getChild("head");
        this.field_178720_f = root.getChild("field_178720_f");
        this.body = root.getChild("body");
        this.bipedBodyWear = root.getChild("bipedBodyWear");
        this.right_arm = root.getChild("right_arm");
        this.bipedRightArmWear = root.getChild("bipedRightArmWear");
        this.left_arm = root.getChild("left_arm");
        this.bipedLeftArmWear = root.getChild("bipedLeftArmWear");
        this.right_leg = root.getChild("right_leg");
        this.bipedRightLegWear = root.getChild("bipedRightLegWear");
        this.left_leg = root.getChild("left_leg");
        this.bipedLeftLegWear = root.getChild("bipedLeftLegWear");
        this.earLeft_0 = root.getChild("head").getChild("earLeft_0");
        this.earLeft_1 = root.getChild("head").getChild("earLeft_0").getChild("earLeft_1");
        this.earLeft_2 = root.getChild("head").getChild("earLeft_0").getChild("earLeft_1").getChild("earLeft_2");
        this.earLeft_3 = root.getChild("head").getChild("earLeft_0").getChild("earLeft_1").getChild("earLeft_2").getChild("earLeft_3");
        this.earLeft_4 = root.getChild("head").getChild("earLeft_0").getChild("earLeft_1").getChild("earLeft_2").getChild("earLeft_3").getChild("earLeft_4");
        this.earLeft_5 = root.getChild("head").getChild("earLeft_0").getChild("earLeft_1").getChild("earLeft_2").getChild("earLeft_3").getChild("earLeft_4").getChild("earLeft_5");
        this.earLeft_6 = root.getChild("head").getChild("earLeft_0").getChild("earLeft_1").getChild("earLeft_2").getChild("earLeft_3").getChild("earLeft_4").getChild("earLeft_5").getChild("earLeft_6");
        this.earLeft_7 = root.getChild("head").getChild("earLeft_0").getChild("earLeft_1").getChild("earLeft_2").getChild("earLeft_3").getChild("earLeft_4").getChild("earLeft_5").getChild("earLeft_6").getChild("earLeft_7");
        this.earLeft_8 = root.getChild("head").getChild("earLeft_0").getChild("earLeft_1").getChild("earLeft_2").getChild("earLeft_3").getChild("earLeft_4").getChild("earLeft_5").getChild("earLeft_6").getChild("earLeft_7").getChild("earLeft_8");
        this.earRight_0 = root.getChild("head").getChild("earRight_0");
        this.earRight_1 = root.getChild("head").getChild("earRight_0").getChild("earRight_1");
        this.earRight_2 = root.getChild("head").getChild("earRight_0").getChild("earRight_1").getChild("earRight_2");
        this.earRight_3 = root.getChild("head").getChild("earRight_0").getChild("earRight_1").getChild("earRight_2").getChild("earRight_3");
        this.earRight_4 = root.getChild("head").getChild("earRight_0").getChild("earRight_1").getChild("earRight_2").getChild("earRight_3").getChild("earRight_4");
        this.earRight_5 = root.getChild("head").getChild("earRight_0").getChild("earRight_1").getChild("earRight_2").getChild("earRight_3").getChild("earRight_4").getChild("earRight_5");
        this.earRight_6 = root.getChild("head").getChild("earRight_0").getChild("earRight_1").getChild("earRight_2").getChild("earRight_3").getChild("earRight_4").getChild("earRight_5").getChild("earRight_6");
        this.earRight_7 = root.getChild("head").getChild("earRight_0").getChild("earRight_1").getChild("earRight_2").getChild("earRight_3").getChild("earRight_4").getChild("earRight_5").getChild("earRight_6").getChild("earRight_7");
        this.earRight_8 = root.getChild("head").getChild("earRight_0").getChild("earRight_1").getChild("earRight_2").getChild("earRight_3").getChild("earRight_4").getChild("earRight_5").getChild("earRight_6").getChild("earRight_7").getChild("earRight_8");
        this.sandEar = root.getChild("field_178720_f").getChild("sandEar");
        this.cube_r1 = root.getChild("field_178720_f").getChild("sandEar").getChild("cube_r1");
        this.allTails = root.getChild("body").getChild("allTails");
        this.tail_0_0 = root.getChild("body").getChild("allTails").getChild("tail_0_0");
        this.tail_0_1 = root.getChild("body").getChild("allTails").getChild("tail_0_0").getChild("tail_0_1");
        this.tail_0_2 = root.getChild("body").getChild("allTails").getChild("tail_0_0").getChild("tail_0_1").getChild("tail_0_2");
        this.tail_0_3 = root.getChild("body").getChild("allTails").getChild("tail_0_0").getChild("tail_0_1").getChild("tail_0_2").getChild("tail_0_3");
        this.tail_0_4 = root.getChild("body").getChild("allTails").getChild("tail_0_0").getChild("tail_0_1").getChild("tail_0_2").getChild("tail_0_3").getChild("tail_0_4");
        this.tail_0_5 = root.getChild("body").getChild("allTails").getChild("tail_0_0").getChild("tail_0_1").getChild("tail_0_2").getChild("tail_0_3").getChild("tail_0_4").getChild("tail_0_5");
        this.tail_0_6 = root.getChild("body").getChild("allTails").getChild("tail_0_0").getChild("tail_0_1").getChild("tail_0_2").getChild("tail_0_3").getChild("tail_0_4").getChild("tail_0_5").getChild("tail_0_6");
        this.tail_0_7 = root.getChild("body").getChild("allTails").getChild("tail_0_0").getChild("tail_0_1").getChild("tail_0_2").getChild("tail_0_3").getChild("tail_0_4").getChild("tail_0_5").getChild("tail_0_6").getChild("tail_0_7");
        this.tail_1_0 = root.getChild("body").getChild("allTails").getChild("tail_1_0");
        this.tail_1_1 = root.getChild("body").getChild("allTails").getChild("tail_1_0").getChild("tail_1_1");
        this.tail_1_2 = root.getChild("body").getChild("allTails").getChild("tail_1_0").getChild("tail_1_1").getChild("tail_1_2");
        this.tail_1_3 = root.getChild("body").getChild("allTails").getChild("tail_1_0").getChild("tail_1_1").getChild("tail_1_2").getChild("tail_1_3");
        this.tail_1_4 = root.getChild("body").getChild("allTails").getChild("tail_1_0").getChild("tail_1_1").getChild("tail_1_2").getChild("tail_1_3").getChild("tail_1_4");
        this.tail_1_5 = root.getChild("body").getChild("allTails").getChild("tail_1_0").getChild("tail_1_1").getChild("tail_1_2").getChild("tail_1_3").getChild("tail_1_4").getChild("tail_1_5");
        this.tail_1_6 = root.getChild("body").getChild("allTails").getChild("tail_1_0").getChild("tail_1_1").getChild("tail_1_2").getChild("tail_1_3").getChild("tail_1_4").getChild("tail_1_5").getChild("tail_1_6");
        this.tail_1_7 = root.getChild("body").getChild("allTails").getChild("tail_1_0").getChild("tail_1_1").getChild("tail_1_2").getChild("tail_1_3").getChild("tail_1_4").getChild("tail_1_5").getChild("tail_1_6").getChild("tail_1_7");
        this.tail_2_0 = root.getChild("body").getChild("allTails").getChild("tail_2_0");
        this.tail_2_1 = root.getChild("body").getChild("allTails").getChild("tail_2_0").getChild("tail_2_1");
        this.tail_2_2 = root.getChild("body").getChild("allTails").getChild("tail_2_0").getChild("tail_2_1").getChild("tail_2_2");
        this.tail_2_3 = root.getChild("body").getChild("allTails").getChild("tail_2_0").getChild("tail_2_1").getChild("tail_2_2").getChild("tail_2_3");
        this.tail_2_4 = root.getChild("body").getChild("allTails").getChild("tail_2_0").getChild("tail_2_1").getChild("tail_2_2").getChild("tail_2_3").getChild("tail_2_4");
        this.tail_2_5 = root.getChild("body").getChild("allTails").getChild("tail_2_0").getChild("tail_2_1").getChild("tail_2_2").getChild("tail_2_3").getChild("tail_2_4").getChild("tail_2_5");
        this.tail_2_6 = root.getChild("body").getChild("allTails").getChild("tail_2_0").getChild("tail_2_1").getChild("tail_2_2").getChild("tail_2_3").getChild("tail_2_4").getChild("tail_2_5").getChild("tail_2_6");
        this.tail_2_7 = root.getChild("body").getChild("allTails").getChild("tail_2_0").getChild("tail_2_1").getChild("tail_2_2").getChild("tail_2_3").getChild("tail_2_4").getChild("tail_2_5").getChild("tail_2_6").getChild("tail_2_7");
        this.tail_3_0 = root.getChild("body").getChild("allTails").getChild("tail_3_0");
        this.tail_3_1 = root.getChild("body").getChild("allTails").getChild("tail_3_0").getChild("tail_3_1");
        this.tail_3_2 = root.getChild("body").getChild("allTails").getChild("tail_3_0").getChild("tail_3_1").getChild("tail_3_2");
        this.tail_3_3 = root.getChild("body").getChild("allTails").getChild("tail_3_0").getChild("tail_3_1").getChild("tail_3_2").getChild("tail_3_3");
        this.tail_3_4 = root.getChild("body").getChild("allTails").getChild("tail_3_0").getChild("tail_3_1").getChild("tail_3_2").getChild("tail_3_3").getChild("tail_3_4");
        this.tail_3_5 = root.getChild("body").getChild("allTails").getChild("tail_3_0").getChild("tail_3_1").getChild("tail_3_2").getChild("tail_3_3").getChild("tail_3_4").getChild("tail_3_5");
        this.tail_3_6 = root.getChild("body").getChild("allTails").getChild("tail_3_0").getChild("tail_3_1").getChild("tail_3_2").getChild("tail_3_3").getChild("tail_3_4").getChild("tail_3_5").getChild("tail_3_6");
        this.tail_3_7 = root.getChild("body").getChild("allTails").getChild("tail_3_0").getChild("tail_3_1").getChild("tail_3_2").getChild("tail_3_3").getChild("tail_3_4").getChild("tail_3_5").getChild("tail_3_6").getChild("tail_3_7");
        this.tail_4_0 = root.getChild("body").getChild("allTails").getChild("tail_4_0");
        this.tail_4_1 = root.getChild("body").getChild("allTails").getChild("tail_4_0").getChild("tail_4_1");
        this.tail_4_2 = root.getChild("body").getChild("allTails").getChild("tail_4_0").getChild("tail_4_1").getChild("tail_4_2");
        this.tail_4_3 = root.getChild("body").getChild("allTails").getChild("tail_4_0").getChild("tail_4_1").getChild("tail_4_2").getChild("tail_4_3");
        this.tail_4_4 = root.getChild("body").getChild("allTails").getChild("tail_4_0").getChild("tail_4_1").getChild("tail_4_2").getChild("tail_4_3").getChild("tail_4_4");
        this.tail_4_5 = root.getChild("body").getChild("allTails").getChild("tail_4_0").getChild("tail_4_1").getChild("tail_4_2").getChild("tail_4_3").getChild("tail_4_4").getChild("tail_4_5");
        this.tail_4_6 = root.getChild("body").getChild("allTails").getChild("tail_4_0").getChild("tail_4_1").getChild("tail_4_2").getChild("tail_4_3").getChild("tail_4_4").getChild("tail_4_5").getChild("tail_4_6");
        this.tail_4_7 = root.getChild("body").getChild("allTails").getChild("tail_4_0").getChild("tail_4_1").getChild("tail_4_2").getChild("tail_4_3").getChild("tail_4_4").getChild("tail_4_5").getChild("tail_4_6").getChild("tail_4_7");
        this.tail_5_0 = root.getChild("body").getChild("allTails").getChild("tail_5_0");
        this.tail_5_1 = root.getChild("body").getChild("allTails").getChild("tail_5_0").getChild("tail_5_1");
        this.tail_5_2 = root.getChild("body").getChild("allTails").getChild("tail_5_0").getChild("tail_5_1").getChild("tail_5_2");
        this.tail_5_3 = root.getChild("body").getChild("allTails").getChild("tail_5_0").getChild("tail_5_1").getChild("tail_5_2").getChild("tail_5_3");
        this.tail_5_4 = root.getChild("body").getChild("allTails").getChild("tail_5_0").getChild("tail_5_1").getChild("tail_5_2").getChild("tail_5_3").getChild("tail_5_4");
        this.tail_5_5 = root.getChild("body").getChild("allTails").getChild("tail_5_0").getChild("tail_5_1").getChild("tail_5_2").getChild("tail_5_3").getChild("tail_5_4").getChild("tail_5_5");
        this.tail_5_6 = root.getChild("body").getChild("allTails").getChild("tail_5_0").getChild("tail_5_1").getChild("tail_5_2").getChild("tail_5_3").getChild("tail_5_4").getChild("tail_5_5").getChild("tail_5_6");
        this.tail_5_7 = root.getChild("body").getChild("allTails").getChild("tail_5_0").getChild("tail_5_1").getChild("tail_5_2").getChild("tail_5_3").getChild("tail_5_4").getChild("tail_5_5").getChild("tail_5_6").getChild("tail_5_7");
        this.tail_6_0 = root.getChild("body").getChild("allTails").getChild("tail_6_0");
        this.tail_6_1 = root.getChild("body").getChild("allTails").getChild("tail_6_0").getChild("tail_6_1");
        this.tail_6_2 = root.getChild("body").getChild("allTails").getChild("tail_6_0").getChild("tail_6_1").getChild("tail_6_2");
        this.tail_6_3 = root.getChild("body").getChild("allTails").getChild("tail_6_0").getChild("tail_6_1").getChild("tail_6_2").getChild("tail_6_3");
        this.tail_6_4 = root.getChild("body").getChild("allTails").getChild("tail_6_0").getChild("tail_6_1").getChild("tail_6_2").getChild("tail_6_3").getChild("tail_6_4");
        this.tail_6_5 = root.getChild("body").getChild("allTails").getChild("tail_6_0").getChild("tail_6_1").getChild("tail_6_2").getChild("tail_6_3").getChild("tail_6_4").getChild("tail_6_5");
        this.tail_6_6 = root.getChild("body").getChild("allTails").getChild("tail_6_0").getChild("tail_6_1").getChild("tail_6_2").getChild("tail_6_3").getChild("tail_6_4").getChild("tail_6_5").getChild("tail_6_6");
        this.tail_6_7 = root.getChild("body").getChild("allTails").getChild("tail_6_0").getChild("tail_6_1").getChild("tail_6_2").getChild("tail_6_3").getChild("tail_6_4").getChild("tail_6_5").getChild("tail_6_6").getChild("tail_6_7");
        this.tail_7_0 = root.getChild("body").getChild("allTails").getChild("tail_7_0");
        this.tail_7_1 = root.getChild("body").getChild("allTails").getChild("tail_7_0").getChild("tail_7_1");
        this.tail_7_2 = root.getChild("body").getChild("allTails").getChild("tail_7_0").getChild("tail_7_1").getChild("tail_7_2");
        this.tail_7_3 = root.getChild("body").getChild("allTails").getChild("tail_7_0").getChild("tail_7_1").getChild("tail_7_2").getChild("tail_7_3");
        this.tail_7_4 = root.getChild("body").getChild("allTails").getChild("tail_7_0").getChild("tail_7_1").getChild("tail_7_2").getChild("tail_7_3").getChild("tail_7_4");
        this.tail_7_5 = root.getChild("body").getChild("allTails").getChild("tail_7_0").getChild("tail_7_1").getChild("tail_7_2").getChild("tail_7_3").getChild("tail_7_4").getChild("tail_7_5");
        this.tail_7_6 = root.getChild("body").getChild("allTails").getChild("tail_7_0").getChild("tail_7_1").getChild("tail_7_2").getChild("tail_7_3").getChild("tail_7_4").getChild("tail_7_5").getChild("tail_7_6");
        this.tail_7_7 = root.getChild("body").getChild("allTails").getChild("tail_7_0").getChild("tail_7_1").getChild("tail_7_2").getChild("tail_7_3").getChild("tail_7_4").getChild("tail_7_5").getChild("tail_7_6").getChild("tail_7_7");
        this.tail_8_0 = root.getChild("body").getChild("allTails").getChild("tail_8_0");
        this.tail_8_1 = root.getChild("body").getChild("allTails").getChild("tail_8_0").getChild("tail_8_1");
        this.tail_8_2 = root.getChild("body").getChild("allTails").getChild("tail_8_0").getChild("tail_8_1").getChild("tail_8_2");
        this.tail_8_3 = root.getChild("body").getChild("allTails").getChild("tail_8_0").getChild("tail_8_1").getChild("tail_8_2").getChild("tail_8_3");
        this.tail_8_4 = root.getChild("body").getChild("allTails").getChild("tail_8_0").getChild("tail_8_1").getChild("tail_8_2").getChild("tail_8_3").getChild("tail_8_4");
        this.tail_8_5 = root.getChild("body").getChild("allTails").getChild("tail_8_0").getChild("tail_8_1").getChild("tail_8_2").getChild("tail_8_3").getChild("tail_8_4").getChild("tail_8_5");
        this.tail_8_6 = root.getChild("body").getChild("allTails").getChild("tail_8_0").getChild("tail_8_1").getChild("tail_8_2").getChild("tail_8_3").getChild("tail_8_4").getChild("tail_8_5").getChild("tail_8_6");
        this.tail_8_7 = root.getChild("body").getChild("allTails").getChild("tail_8_0").getChild("tail_8_1").getChild("tail_8_2").getChild("tail_8_3").getChild("tail_8_4").getChild("tail_8_5").getChild("tail_8_6").getChild("tail_8_7");
        this.tailWears = root.getChild("bipedBodyWear").getChild("tailWears");
        this.tailWear_0_0 = root.getChild("bipedBodyWear").getChild("tailWears").getChild("tailWear_0_0");
        this.tailWear_0_1 = root.getChild("bipedBodyWear").getChild("tailWears").getChild("tailWear_0_0").getChild("tailWear_0_1");
        this.tailWear_0_2 = root.getChild("bipedBodyWear").getChild("tailWears").getChild("tailWear_0_0").getChild("tailWear_0_1").getChild("tailWear_0_2");
        this.tailWear_0_3 = root.getChild("bipedBodyWear").getChild("tailWears").getChild("tailWear_0_0").getChild("tailWear_0_1").getChild("tailWear_0_2").getChild("tailWear_0_3");
        this.tailWear_0_4 = root.getChild("bipedBodyWear").getChild("tailWears").getChild("tailWear_0_0").getChild("tailWear_0_1").getChild("tailWear_0_2").getChild("tailWear_0_3").getChild("tailWear_0_4");
        this.tailWear_0_5 = root.getChild("bipedBodyWear").getChild("tailWears").getChild("tailWear_0_0").getChild("tailWear_0_1").getChild("tailWear_0_2").getChild("tailWear_0_3").getChild("tailWear_0_4").getChild("tailWear_0_5");
        this.tailWear_0_6 = root.getChild("bipedBodyWear").getChild("tailWears").getChild("tailWear_0_0").getChild("tailWear_0_1").getChild("tailWear_0_2").getChild("tailWear_0_3").getChild("tailWear_0_4").getChild("tailWear_0_5").getChild("tailWear_0_6");
        this.tailWear_0_7 = root.getChild("bipedBodyWear").getChild("tailWears").getChild("tailWear_0_0").getChild("tailWear_0_1").getChild("tailWear_0_2").getChild("tailWear_0_3").getChild("tailWear_0_4").getChild("tailWear_0_5").getChild("tailWear_0_6").getChild("tailWear_0_7");
        this.sandArm = root.getChild("bipedRightArmWear").getChild("sandArm");
        this.bipedRightLeg_r1 = root.getChild("bipedRightLegWear").getChild("bipedRightLeg_r1");
        this.bipedRightLeg_r2 = root.getChild("bipedRightLegWear").getChild("bipedRightLeg_r2");
        this.bipedLeftLeg_r1 = root.getChild("bipedLeftLegWear").getChild("bipedLeftLeg_r1");
        this.bipedLeftLeg_r2 = root.getChild("bipedLeftLegWear").getChild("bipedLeftLeg_r2");
    }

    /**
     * Shows the first {@code count} tails and hides the rest.
     *
     * The imported model always carried all nine, which is why Kurama Chakra Mode - a form
     * that canonically has no tails at all, just the marked cloak - still sprouted one, and
     * why a one-tail cloak looked identical to a nine-tail one. Each tail is a chain hanging
     * off its own root part, so hiding the root takes the whole tail with it.
     */
    public void setVisibleTails(int count) {
        ModelPart[] tailRoots = {
                this.tail_0_0, this.tail_1_0, this.tail_2_0, this.tail_3_0, this.tail_4_0,
                this.tail_5_0, this.tail_6_0, this.tail_7_0, this.tail_8_0
        };
        this.allTails.visible = count > 0;
        for (int i = 0; i < tailRoots.length; i++) {
            tailRoots[i].visible = i < count;
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, new CubeDeformation(0.4F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_earLeft_0 = p_head.addOrReplaceChild("earLeft_0",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-0.5F, -1.5F, -0.5F, 1, 2, 1, new CubeDeformation(0.8F)),
                PartPose.offsetAndRotation(3.5F, -8.25F, -1.5F, -0.5236F, 0.0F, 0.7854F));
        PartDefinition p_earLeft_1 = p_earLeft_0.addOrReplaceChild("earLeft_1",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-0.5F, -1.5F, -0.5F, 1, 2, 1, new CubeDeformation(0.7F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, -0.1745F));
        PartDefinition p_earLeft_2 = p_earLeft_1.addOrReplaceChild("earLeft_2",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-0.5F, -1.5F, -0.5F, 1, 2, 1, new CubeDeformation(0.6F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, -0.1745F));
        PartDefinition p_earLeft_3 = p_earLeft_2.addOrReplaceChild("earLeft_3",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-0.5F, -1.5F, -0.5F, 1, 2, 1, new CubeDeformation(0.5F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, -0.1745F));
        PartDefinition p_earLeft_4 = p_earLeft_3.addOrReplaceChild("earLeft_4",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-0.5F, -1.5F, -0.5F, 1, 2, 1, new CubeDeformation(0.35F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, -0.1745F));
        PartDefinition p_earLeft_5 = p_earLeft_4.addOrReplaceChild("earLeft_5",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-0.5F, -1.5F, -0.5F, 1, 2, 1, new CubeDeformation(0.2F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, -0.1745F));
        PartDefinition p_earLeft_6 = p_earLeft_5.addOrReplaceChild("earLeft_6",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-0.5F, -1.5F, -0.5F, 1, 2, 1, new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, -0.1745F));
        PartDefinition p_earLeft_7 = p_earLeft_6.addOrReplaceChild("earLeft_7",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-0.5F, -1.5F, -0.5F, 1, 2, 1, new CubeDeformation(-0.1F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, -0.1745F));
        PartDefinition p_earLeft_8 = p_earLeft_7.addOrReplaceChild("earLeft_8",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-0.5F, -1.5F, -0.5F, 1, 2, 1, new CubeDeformation(-0.25F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, -0.1745F));
        PartDefinition p_earRight_0 = p_head.addOrReplaceChild("earRight_0",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-0.5F, -1.5F, -0.5F, 1, 2, 1, new CubeDeformation(0.8F)),
                PartPose.offsetAndRotation(-3.5F, -8.25F, -1.5F, -0.5236F, 0.0F, -0.7854F));
        PartDefinition p_earRight_1 = p_earRight_0.addOrReplaceChild("earRight_1",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-0.5F, -1.5F, -0.5F, 1, 2, 1, new CubeDeformation(0.7F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.1745F));
        PartDefinition p_earRight_2 = p_earRight_1.addOrReplaceChild("earRight_2",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-0.5F, -1.5F, -0.5F, 1, 2, 1, new CubeDeformation(0.6F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.1745F));
        PartDefinition p_earRight_3 = p_earRight_2.addOrReplaceChild("earRight_3",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-0.5F, -1.5F, -0.5F, 1, 2, 1, new CubeDeformation(0.5F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.1745F));
        PartDefinition p_earRight_4 = p_earRight_3.addOrReplaceChild("earRight_4",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-0.5F, -1.5F, -0.5F, 1, 2, 1, new CubeDeformation(0.35F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.1745F));
        PartDefinition p_earRight_5 = p_earRight_4.addOrReplaceChild("earRight_5",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-0.5F, -1.5F, -0.5F, 1, 2, 1, new CubeDeformation(0.2F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.1745F));
        PartDefinition p_earRight_6 = p_earRight_5.addOrReplaceChild("earRight_6",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-0.5F, -1.5F, -0.5F, 1, 2, 1, new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.1745F));
        PartDefinition p_earRight_7 = p_earRight_6.addOrReplaceChild("earRight_7",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-0.5F, -1.5F, -0.5F, 1, 2, 1, new CubeDeformation(-0.1F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.1745F));
        PartDefinition p_earRight_8 = p_earRight_7.addOrReplaceChild("earRight_8",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-0.5F, -1.5F, -0.5F, 1, 2, 1, new CubeDeformation(-0.25F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.1745F));
        PartDefinition p_field_178720_f = root.addOrReplaceChild("field_178720_f",
                CubeListBuilder.create()
                        .texOffs(64, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, new CubeDeformation(0.6F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_sandEar = p_field_178720_f.addOrReplaceChild("sandEar",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-4.425F, -8.0F, 0.0F, 0.0F, 0.0F, -0.2618F));
        PartDefinition p_cube_r1 = p_sandEar.addOrReplaceChild("cube_r1",
                CubeListBuilder.create()
                        .texOffs(118, 0)
                        .addBox(-1.0F, -2.8F, -2.0F, 2, 6, 3),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7782F, -0.0998F, -0.1434F));
        PartDefinition p_body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, new CubeDeformation(0.6F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_allTails = p_body.addOrReplaceChild("allTails",
                CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition p_tail_0_0 = p_allTails.addOrReplaceChild("tail_0_0",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4),
                PartPose.offsetAndRotation(0.0F, 10.5F, 2.0F, -1.0472F, 0.0F, 0.0F));
        PartDefinition p_tail_0_1 = p_tail_0_0.addOrReplaceChild("tail_0_1",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_0_2 = p_tail_0_1.addOrReplaceChild("tail_0_2",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.6F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_0_3 = p_tail_0_2.addOrReplaceChild("tail_0_3",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_0_4 = p_tail_0_3.addOrReplaceChild("tail_0_4",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_0_5 = p_tail_0_4.addOrReplaceChild("tail_0_5",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_0_6 = p_tail_0_5.addOrReplaceChild("tail_0_6",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.6F)),
                PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_0_7 = p_tail_0_6.addOrReplaceChild("tail_0_7",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-1.0F)),
                PartPose.offsetAndRotation(0.0F, -3.75F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_1_0 = p_allTails.addOrReplaceChild("tail_1_0",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4),
                PartPose.offsetAndRotation(0.0F, 10.5F, 2.0F, -1.0472F, -0.5236F, -0.2618F));
        PartDefinition p_tail_1_1 = p_tail_1_0.addOrReplaceChild("tail_1_1",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_1_2 = p_tail_1_1.addOrReplaceChild("tail_1_2",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.6F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_1_3 = p_tail_1_2.addOrReplaceChild("tail_1_3",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_1_4 = p_tail_1_3.addOrReplaceChild("tail_1_4",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_1_5 = p_tail_1_4.addOrReplaceChild("tail_1_5",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_1_6 = p_tail_1_5.addOrReplaceChild("tail_1_6",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.6F)),
                PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_1_7 = p_tail_1_6.addOrReplaceChild("tail_1_7",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-1.0F)),
                PartPose.offsetAndRotation(0.0F, -3.75F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_2_0 = p_allTails.addOrReplaceChild("tail_2_0",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4),
                PartPose.offsetAndRotation(0.0F, 10.5F, 2.0F, -1.0472F, 0.5236F, 0.2618F));
        PartDefinition p_tail_2_1 = p_tail_2_0.addOrReplaceChild("tail_2_1",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_2_2 = p_tail_2_1.addOrReplaceChild("tail_2_2",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.6F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_2_3 = p_tail_2_2.addOrReplaceChild("tail_2_3",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_2_4 = p_tail_2_3.addOrReplaceChild("tail_2_4",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_2_5 = p_tail_2_4.addOrReplaceChild("tail_2_5",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_2_6 = p_tail_2_5.addOrReplaceChild("tail_2_6",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.6F)),
                PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_2_7 = p_tail_2_6.addOrReplaceChild("tail_2_7",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-1.0F)),
                PartPose.offsetAndRotation(0.0F, -3.75F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_3_0 = p_allTails.addOrReplaceChild("tail_3_0",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4),
                PartPose.offsetAndRotation(0.0F, 10.5F, 2.0F, -1.0472F, -1.0472F, -0.5236F));
        PartDefinition p_tail_3_1 = p_tail_3_0.addOrReplaceChild("tail_3_1",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_3_2 = p_tail_3_1.addOrReplaceChild("tail_3_2",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.6F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_3_3 = p_tail_3_2.addOrReplaceChild("tail_3_3",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_3_4 = p_tail_3_3.addOrReplaceChild("tail_3_4",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_3_5 = p_tail_3_4.addOrReplaceChild("tail_3_5",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_3_6 = p_tail_3_5.addOrReplaceChild("tail_3_6",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.6F)),
                PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_3_7 = p_tail_3_6.addOrReplaceChild("tail_3_7",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-1.0F)),
                PartPose.offsetAndRotation(0.0F, -3.75F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_4_0 = p_allTails.addOrReplaceChild("tail_4_0",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4),
                PartPose.offsetAndRotation(0.0F, 10.5F, 2.0F, -1.0472F, 1.0472F, 0.5236F));
        PartDefinition p_tail_4_1 = p_tail_4_0.addOrReplaceChild("tail_4_1",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_4_2 = p_tail_4_1.addOrReplaceChild("tail_4_2",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.6F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_4_3 = p_tail_4_2.addOrReplaceChild("tail_4_3",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_4_4 = p_tail_4_3.addOrReplaceChild("tail_4_4",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_4_5 = p_tail_4_4.addOrReplaceChild("tail_4_5",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_4_6 = p_tail_4_5.addOrReplaceChild("tail_4_6",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.6F)),
                PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_4_7 = p_tail_4_6.addOrReplaceChild("tail_4_7",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-1.0F)),
                PartPose.offsetAndRotation(0.0F, -3.75F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_5_0 = p_allTails.addOrReplaceChild("tail_5_0",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4),
                PartPose.offsetAndRotation(0.0F, 10.5F, 2.0F, -1.5718F, -0.2618F, 0.0F));
        PartDefinition p_tail_5_1 = p_tail_5_0.addOrReplaceChild("tail_5_1",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_5_2 = p_tail_5_1.addOrReplaceChild("tail_5_2",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.6F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_5_3 = p_tail_5_2.addOrReplaceChild("tail_5_3",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_5_4 = p_tail_5_3.addOrReplaceChild("tail_5_4",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_5_5 = p_tail_5_4.addOrReplaceChild("tail_5_5",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_5_6 = p_tail_5_5.addOrReplaceChild("tail_5_6",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.6F)),
                PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_5_7 = p_tail_5_6.addOrReplaceChild("tail_5_7",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-1.0F)),
                PartPose.offsetAndRotation(0.0F, -3.75F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_6_0 = p_allTails.addOrReplaceChild("tail_6_0",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4),
                PartPose.offsetAndRotation(0.0F, 10.5F, 2.0F, -1.5718F, 0.2618F, 0.0F));
        PartDefinition p_tail_6_1 = p_tail_6_0.addOrReplaceChild("tail_6_1",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_6_2 = p_tail_6_1.addOrReplaceChild("tail_6_2",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.6F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_6_3 = p_tail_6_2.addOrReplaceChild("tail_6_3",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_6_4 = p_tail_6_3.addOrReplaceChild("tail_6_4",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_6_5 = p_tail_6_4.addOrReplaceChild("tail_6_5",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_6_6 = p_tail_6_5.addOrReplaceChild("tail_6_6",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.6F)),
                PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_6_7 = p_tail_6_6.addOrReplaceChild("tail_6_7",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-1.0F)),
                PartPose.offsetAndRotation(0.0F, -3.75F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_7_0 = p_allTails.addOrReplaceChild("tail_7_0",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4),
                PartPose.offsetAndRotation(0.0F, 10.5F, 2.0F, -1.5718F, 0.7854F, 0.0F));
        PartDefinition p_tail_7_1 = p_tail_7_0.addOrReplaceChild("tail_7_1",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_7_2 = p_tail_7_1.addOrReplaceChild("tail_7_2",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.6F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_7_3 = p_tail_7_2.addOrReplaceChild("tail_7_3",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_7_4 = p_tail_7_3.addOrReplaceChild("tail_7_4",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_7_5 = p_tail_7_4.addOrReplaceChild("tail_7_5",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_7_6 = p_tail_7_5.addOrReplaceChild("tail_7_6",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.6F)),
                PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_7_7 = p_tail_7_6.addOrReplaceChild("tail_7_7",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-1.0F)),
                PartPose.offsetAndRotation(0.0F, -3.75F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_8_0 = p_allTails.addOrReplaceChild("tail_8_0",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4),
                PartPose.offsetAndRotation(0.0F, 10.5F, 2.0F, -1.5718F, -0.7854F, 0.0F));
        PartDefinition p_tail_8_1 = p_tail_8_0.addOrReplaceChild("tail_8_1",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_8_2 = p_tail_8_1.addOrReplaceChild("tail_8_2",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.6F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_8_3 = p_tail_8_2.addOrReplaceChild("tail_8_3",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_8_4 = p_tail_8_3.addOrReplaceChild("tail_8_4",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_8_5 = p_tail_8_4.addOrReplaceChild("tail_8_5",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.3F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_8_6 = p_tail_8_5.addOrReplaceChild("tail_8_6",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.6F)),
                PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tail_8_7 = p_tail_8_6.addOrReplaceChild("tail_8_7",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-1.0F)),
                PartPose.offsetAndRotation(0.0F, -3.75F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_bipedBodyWear = root.addOrReplaceChild("bipedBodyWear",
                CubeListBuilder.create()
                        .texOffs(80, 16)
                        .addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, new CubeDeformation(0.65F))
                        .texOffs(80, 32)
                        .addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, new CubeDeformation(0.7F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_tailWears = p_bipedBodyWear.addOrReplaceChild("tailWears",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_tailWear_0_0 = p_tailWears.addOrReplaceChild("tailWear_0_0",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 10.5F, 2.0F, -1.0472F, 0.0F, 0.0F));
        PartDefinition p_tailWear_0_1 = p_tailWear_0_0.addOrReplaceChild("tailWear_0_1",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tailWear_0_2 = p_tailWear_0_1.addOrReplaceChild("tailWear_0_2",
                CubeListBuilder.create()
                        .texOffs(102, 4)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.61F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tailWear_0_3 = p_tailWear_0_2.addOrReplaceChild("tailWear_0_3",
                CubeListBuilder.create()
                        .texOffs(102, 4)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.31F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tailWear_0_4 = p_tailWear_0_3.addOrReplaceChild("tailWear_0_4",
                CubeListBuilder.create()
                        .texOffs(102, 4)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(0.01F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tailWear_0_5 = p_tailWear_0_4.addOrReplaceChild("tailWear_0_5",
                CubeListBuilder.create()
                        .texOffs(102, 4)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.29F)),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tailWear_0_6 = p_tailWear_0_5.addOrReplaceChild("tailWear_0_6",
                CubeListBuilder.create()
                        .texOffs(102, 4)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.59F)),
                PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_tailWear_0_7 = p_tailWear_0_6.addOrReplaceChild("tailWear_0_7",
                CubeListBuilder.create()
                        .texOffs(102, 4)
                        .addBox(-2.0F, -5.5F, -2.0F, 4, 6, 4, new CubeDeformation(-0.99F)),
                PartPose.offsetAndRotation(0.0F, -3.75F, 0.0F, 0.2618F, 0.0F, 0.0F));
        PartDefinition p_right_arm = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.6F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition p_bipedRightArmWear = root.addOrReplaceChild("bipedRightArmWear",
                CubeListBuilder.create()
                        .texOffs(104, 16)
                        .addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.65F))
                        .texOffs(104, 32)
                        .addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.7F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition p_sandArm = p_bipedRightArmWear.addOrReplaceChild("sandArm",
                CubeListBuilder.create(),
                PartPose.offset(-1.6421F, 7.959F, -3.46F));
        PartDefinition p_left_arm = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(32, 48)
                        .addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.6F)),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition p_bipedLeftArmWear = root.addOrReplaceChild("bipedLeftArmWear",
                CubeListBuilder.create()
                        .texOffs(96, 48)
                        .addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.65F))
                        .texOffs(112, 48)
                        .addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.7F)),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition p_right_leg = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.6F)),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition p_bipedRightLegWear = root.addOrReplaceChild("bipedRightLegWear",
                CubeListBuilder.create()
                        .texOffs(64, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.65F)),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition p_bipedRightLeg_r1 = p_bipedRightLegWear.addOrReplaceChild("bipedRightLeg_r1",
                CubeListBuilder.create()
                        .texOffs(64, 32)
                        .addBox(0.0F, 0.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.7F)),
                PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, -0.1309F, 0.0F, 0.1745F));
        PartDefinition p_bipedRightLeg_r2 = p_bipedRightLegWear.addOrReplaceChild("bipedRightLeg_r2",
                CubeListBuilder.create()
                        .texOffs(64, 32)
                        .addBox(0.0F, 0.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.7F)),
                PartPose.offsetAndRotation(-2.0F, 0.0F, 0.0F, 0.1309F, 0.0F, 0.1745F));
        PartDefinition p_left_leg = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(16, 48)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.6F)),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        PartDefinition p_bipedLeftLegWear = root.addOrReplaceChild("bipedLeftLegWear",
                CubeListBuilder.create()
                        .texOffs(80, 48)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.65F)),
                PartPose.offset(1.9F, 12.0F, 0.0F));
        PartDefinition p_bipedLeftLeg_r1 = p_bipedLeftLegWear.addOrReplaceChild("bipedLeftLeg_r1",
                CubeListBuilder.create()
                        .texOffs(64, 48)
                        .addBox(-4.0F, 0.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.7F)),
                PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, -0.1309F, 0.0F, -0.1745F));
        PartDefinition p_bipedLeftLeg_r2 = p_bipedLeftLegWear.addOrReplaceChild("bipedLeftLeg_r2",
                CubeListBuilder.create()
                        .texOffs(64, 48)
                        .addBox(-4.0F, 0.0F, -2.0F, 4, 12, 4, new CubeDeformation(0.7F)),
                PartPose.offsetAndRotation(2.0F, 0.0F, 0.0F, 0.1309F, 0.0F, -0.1745F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /**
     * Copies the wearer's current limb pose onto the shroud.
     *
     * The shroud was drawn in its authored rest pose every frame, so a sprinting player ran
     * out from underneath a cloak standing perfectly upright - body swinging, gold shell
     * frozen. Rather than re-deriving a walk cycle here, this lifts the rotations straight off
     * the PlayerModel the renderer has ALREADY finished posing for this frame. That means the
     * shroud inherits everything for free: the run, the sneak, and every jutsu stance in
     * PlayerAnimHandler, with no second copy of the animation logic to drift out of step.
     *
     * The extra wear layers are children of their limbs and follow automatically; only the six
     * roots need driving.
     */
    public void animateFrom(net.minecraft.client.model.PlayerModel<?> source) {
        copy(source.head, this.head);
        copy(source.head, this.field_178720_f);
        copy(source.body, this.body);
        copy(source.body, this.bipedBodyWear);
        copy(source.rightArm, this.right_arm);
        copy(source.leftArm, this.left_arm);
        copy(source.rightLeg, this.right_leg);
        copy(source.leftLeg, this.left_leg);
    }

    private static void copy(ModelPart from, ModelPart to) {
        to.xRot = from.xRot;
        to.yRot = from.yRot;
        to.zRot = from.zRot;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        this.head.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.field_178720_f.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.body.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.bipedBodyWear.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.right_arm.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.bipedRightArmWear.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.left_arm.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.bipedLeftArmWear.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.right_leg.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.bipedRightLegWear.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.left_leg.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.bipedLeftLegWear.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
