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
 * Geometry imported from the 1.12.2 mod's Enma.
 * Machine-converted from bytecode: box coordinates and pivots are the originals,
 * so this model shares their +Y-downward authoring convention.
 */
public class EnmaModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(NarutoMod.MOD_ID, "enma"), "main");

    private final ModelPart root;

    public EnmaModel(ModelPart root) {
        super(net.minecraft.client.renderer.RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partdefinition = mesh.getRoot();
        PartDefinition field_78116_c = partdefinition.addOrReplaceChild("field_78116_c", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0000f, -8.0000f, -4.0000f, 8.0000f, 8.0000f, 8.0000f, new CubeDeformation(0.0000f)), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition hair = field_78116_c.addOrReplaceChild("hair", CubeListBuilder.create(), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition bone44 = hair.addOrReplaceChild("bone44", CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0000f, -2.4957f, 0.1653f, 6.0000f, 2.0000f, 8.0000f, new CubeDeformation(0.1000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -6.2500f, -4.1000f, 0.1309f, 0.1309f, 0.2618f));
        PartDefinition bone49 = hair.addOrReplaceChild("bone49", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0000f, -2.0000f, 0.1000f, 6.0000f, 2.0000f, 8.0000f, new CubeDeformation(0.3000f)), PartPose.offsetAndRotation(0.0000f, -7.7500f, -3.3500f, -0.2618f, 0.1745f, 0.3054f));
        PartDefinition bone50 = hair.addOrReplaceChild("bone50", CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0000f, -2.0000f, 0.1000f, 6.0000f, 2.0000f, 8.0000f, new CubeDeformation(0.4000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -8.7500f, -2.1000f, -0.6981f, 0.2182f, 0.3491f));
        PartDefinition bone51 = hair.addOrReplaceChild("bone51", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0000f, -2.0000f, 0.1000f, 6.0000f, 2.0000f, 8.0000f, new CubeDeformation(0.5000f)), PartPose.offsetAndRotation(0.0000f, -9.2500f, -0.1000f, -1.0472f, 0.2618f, 0.2618f));
        PartDefinition bone52 = hair.addOrReplaceChild("bone52", CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0000f, -2.0000f, 0.1000f, 6.0000f, 2.0000f, 8.0000f, new CubeDeformation(0.6000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -8.7500f, 1.9000f, -1.2217f, 0.2182f, 0.1745f));
        PartDefinition bone53 = hair.addOrReplaceChild("bone53", CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0000f, -2.0000f, 0.1000f, 6.0000f, 2.0000f, 8.0000f, new CubeDeformation(0.7000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.2500f, 2.9000f, -1.3526f, 0.1309f, 0.1309f));
        PartDefinition bone54 = hair.addOrReplaceChild("bone54", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0000f, -2.0000f, 0.1000f, 6.0000f, 2.0000f, 8.0000f, new CubeDeformation(0.3000f)), PartPose.offsetAndRotation(0.0000f, -3.2500f, 3.1500f, -1.3090f, 0.0436f, 0.0873f));
        PartDefinition bone55 = hair.addOrReplaceChild("bone55", CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0000f, -2.0000f, 0.1000f, 6.0000f, 2.0000f, 8.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.7500f, 4.1500f, -1.4835f, -0.0873f, 0.0436f));
        PartDefinition bone56 = hair.addOrReplaceChild("bone56", CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-4.0000f, -2.4957f, 0.1653f, 6.0000f, 2.0000f, 8.0000f, new CubeDeformation(0.1000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -6.2500f, -4.1000f, 0.1309f, -0.1309f, -0.2618f));
        PartDefinition bone57 = hair.addOrReplaceChild("bone57", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0000f, -2.0000f, 0.1000f, 6.0000f, 2.0000f, 8.0000f, new CubeDeformation(0.3000f)), PartPose.offsetAndRotation(0.0000f, -7.7500f, -3.3500f, -0.2618f, -0.1745f, -0.3054f));
        PartDefinition bone58 = hair.addOrReplaceChild("bone58", CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-4.0000f, -2.0000f, 0.1000f, 6.0000f, 2.0000f, 8.0000f, new CubeDeformation(0.4000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -8.7500f, -2.1000f, -0.6981f, -0.2182f, -0.3491f));
        PartDefinition bone59 = hair.addOrReplaceChild("bone59", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0000f, -2.0000f, 0.1000f, 6.0000f, 2.0000f, 8.0000f, new CubeDeformation(0.5000f)), PartPose.offsetAndRotation(0.0000f, -9.2500f, -0.1000f, -1.0472f, -0.2618f, -0.2618f));
        PartDefinition bone60 = hair.addOrReplaceChild("bone60", CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-4.0000f, -2.0000f, 0.1000f, 6.0000f, 2.0000f, 8.0000f, new CubeDeformation(0.6000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -8.7500f, 1.9000f, -1.2217f, -0.2182f, -0.1745f));
        PartDefinition bone61 = hair.addOrReplaceChild("bone61", CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-4.0000f, -2.0000f, 0.1000f, 6.0000f, 2.0000f, 8.0000f, new CubeDeformation(0.7000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -7.2500f, 2.9000f, -1.3526f, -0.1309f, -0.1309f));
        PartDefinition bone62 = hair.addOrReplaceChild("bone62", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0000f, -2.0000f, 0.1000f, 6.0000f, 2.0000f, 8.0000f, new CubeDeformation(0.3000f)), PartPose.offsetAndRotation(0.0000f, -3.2500f, 3.1500f, -1.3090f, -0.0436f, -0.0873f));
        PartDefinition bone63 = hair.addOrReplaceChild("bone63", CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-4.0000f, -2.0000f, 0.1000f, 6.0000f, 2.0000f, 8.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.7500f, 4.1500f, -1.4835f, 0.0873f, -0.0436f));
        PartDefinition beard = field_78116_c.addOrReplaceChild("beard", CubeListBuilder.create(), PartPose.offset(0.0000f, -1.0000f, -4.2000f));
        PartDefinition goatee = beard.addOrReplaceChild("goatee", CubeListBuilder.create().texOffs(0, 57).addBox(-4.0000f, 0.0000f, 0.0000f, 8.0000f, 3.0000f, 4.0000f, new CubeDeformation(0.0500f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.2182f, 0.0000f, 0.0000f));
        PartDefinition bone26 = beard.addOrReplaceChild("bone26", CubeListBuilder.create().texOffs(32, 45).addBox(-0.0610f, -0.6947f, -0.0608f, 3.0000f, 3.0000f, 3.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(-4.0000f, -3.0000f, 0.2000f, -0.0873f, 0.0000f, 0.0873f));
        PartDefinition bone27 = bone26.addOrReplaceChild("bone27", CubeListBuilder.create().texOffs(32, 51).addBox(-0.0634f, 0.0499f, -0.0630f, 3.0000f, 4.0000f, 3.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 2.2500f, 0.0000f, 0.0436f, 0.0000f, -0.0873f));
        PartDefinition bone28 = beard.addOrReplaceChild("bone28", CubeListBuilder.create().mirror().texOffs(32, 45).addBox(-2.9390f, -0.6947f, -0.0608f, 3.0000f, 3.0000f, 3.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(4.0000f, -3.0000f, 0.2000f, -0.0873f, 0.0000f, -0.0873f));
        PartDefinition bone29 = bone28.addOrReplaceChild("bone29", CubeListBuilder.create().mirror().texOffs(32, 51).addBox(-2.9366f, 0.0499f, -0.0630f, 3.0000f, 4.0000f, 3.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 2.2500f, 0.0000f, 0.0436f, 0.0000f, 0.0873f));
        PartDefinition field_178720_f = partdefinition.addOrReplaceChild("field_178720_f", CubeListBuilder.create().texOffs(24, 8).addBox(-4.0000f, -6.5000f, -4.0000f, 8.0000f, 2.0000f, 8.0000f, new CubeDeformation(0.1000f)), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition field_78115_e = partdefinition.addOrReplaceChild("field_78115_e", CubeListBuilder.create().texOffs(24, 26).addBox(-4.0000f, 6.0000f, -0.7500f, 8.0000f, 6.0000f, 4.0000f, new CubeDeformation(0.5000f)), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition chest = field_78115_e.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(24, 36).addBox(-4.0000f, -5.5000f, -4.5000f, 8.0000f, 5.0000f, 4.0000f, new CubeDeformation(0.5000f)), PartPose.offsetAndRotation(0.0000f, 5.5000f, 3.7500f, 0.2182f, 0.0000f, 0.0000f));
        PartDefinition bodyLayer = field_78115_e.addOrReplaceChild("bodyLayer", CubeListBuilder.create().texOffs(0, 26).addBox(-4.0000f, 6.5000f, -0.7500f, 8.0000f, 6.0000f, 4.0000f, new CubeDeformation(1.2500f)), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition belt = bodyLayer.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(24, 0).addBox(-4.0000f, -15.5000f, -2.0000f, 8.0000f, 1.0000f, 4.0000f, new CubeDeformation(0.8000f)), PartPose.offset(0.0000f, 24.0000f, 1.2500f));
        PartDefinition bone6 = belt.addOrReplaceChild("bone6", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0000f, -1.0000f, -0.5000f, 2.0000f, 4.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(-0.2500f, -15.2500f, -3.0000f, -0.2182f, 0.0000f, 0.3491f));
        PartDefinition bone7 = bone6.addOrReplaceChild("bone7", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, 0.0000f, 0.0000f, 2.0000f, 4.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 2.7500f, -0.5000f, 0.0873f, 0.0000f, -0.1745f));
        PartDefinition bone5 = belt.addOrReplaceChild("bone5", CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-1.0000f, -1.0000f, -0.5000f, 2.0000f, 4.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.2500f, -15.2500f, -3.0000f, -0.2182f, 0.0000f, -0.3491f));
        PartDefinition bone8 = bone5.addOrReplaceChild("bone8", CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-1.0000f, 0.0000f, 0.0000f, 2.0000f, 4.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 2.7500f, -0.5000f, 0.0873f, 0.0000f, 0.1745f));
        PartDefinition bone11 = bodyLayer.addOrReplaceChild("bone11", CubeListBuilder.create().texOffs(0, 36).addBox(-4.0000f, -5.7500f, -4.5000f, 8.0000f, 5.0000f, 4.0000f, new CubeDeformation(1.2500f)), PartPose.offsetAndRotation(0.0000f, 5.5000f, 3.7500f, 0.2182f, 0.0000f, 0.0000f));
        PartDefinition tail = field_78115_e.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(56, 16).addBox(-1.0000f, 0.0000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 11.0000f, 3.2500f, 0.5236f, 0.0000f, 0.0000f));
        PartDefinition tail0 = tail.addOrReplaceChild("tail0", CubeListBuilder.create().texOffs(56, 16).addBox(-1.0000f, 0.0000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 3.5000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail1 = tail0.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(56, 16).addBox(-1.0000f, 0.0000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 3.5000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(56, 16).addBox(-1.0000f, 0.0000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(-0.1000f)), PartPose.offsetAndRotation(0.0000f, 3.5000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(56, 16).addBox(-1.0000f, 0.0000f, -1.0000f, 2.0000f, 4.0000f, 2.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(0.0000f, 3.5000f, 0.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition field_178723_h = partdefinition.addOrReplaceChild("field_178723_h", CubeListBuilder.create(), PartPose.offset(-6.0000f, 2.0000f, 0.0000f));
        PartDefinition bone = field_178723_h.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 45).addBox(-3.0000f, -2.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.0000f)).texOffs(32, 18).addBox(-4.0000f, -1.7500f, -2.0000f, 6.0000f, 4.0000f, 4.0000f, new CubeDeformation(0.8000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, -0.5236f, 0.2618f));
        PartDefinition bone2 = bone.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(44, 41).addBox(-2.0000f, 0.0000f, -4.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-0.1000f)), PartPose.offsetAndRotation(-1.0000f, 6.0000f, 2.0000f, -0.5236f, 0.0000f, 0.0000f));
        PartDefinition field_178724_i = partdefinition.addOrReplaceChild("field_178724_i", CubeListBuilder.create(), PartPose.offset(6.0000f, 2.0000f, 0.0000f));
        PartDefinition bone30 = field_178724_i.addOrReplaceChild("bone30", CubeListBuilder.create().mirror().texOffs(0, 45).addBox(-1.0000f, -2.0000f, -2.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(0.0000f)).mirror(false).mirror().texOffs(32, 18).addBox(-2.0000f, -1.7500f, -2.0000f, 6.0000f, 4.0000f, 4.0000f, new CubeDeformation(0.8000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0000f, 0.5236f, -0.2618f));
        PartDefinition bone31 = bone30.addOrReplaceChild("bone31", CubeListBuilder.create().mirror().texOffs(44, 41).addBox(-2.0000f, 0.0000f, -4.0000f, 4.0000f, 8.0000f, 4.0000f, new CubeDeformation(-0.1000f)).mirror(false), PartPose.offsetAndRotation(1.0000f, 6.0000f, 2.0000f, -0.5236f, 0.0000f, 0.0000f));
        PartDefinition field_178721_j = partdefinition.addOrReplaceChild("field_178721_j", CubeListBuilder.create(), PartPose.offset(-1.9000f, 12.0000f, 0.0000f));
        PartDefinition bone3 = field_178721_j.addOrReplaceChild("bone3", CubeListBuilder.create().texOffs(48, 0).addBox(-1.9000f, 1.0000f, -2.0000f, 4.0000f, 6.0000f, 4.0000f, new CubeDeformation(0.5000f)), PartPose.offsetAndRotation(-0.1000f, -1.0000f, 1.2500f, -0.2618f, 0.4363f, 0.0000f));
        PartDefinition bone4 = bone3.addOrReplaceChild("bone4", CubeListBuilder.create().texOffs(16, 45).addBox(-1.9000f, -0.1028f, 0.0789f, 4.0000f, 6.0000f, 4.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(0.0000f, 7.5000f, -2.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition rightFoot = bone4.addOrReplaceChild("rightFoot", CubeListBuilder.create().texOffs(48, 10).addBox(-1.6000f, -0.7500f, -3.0000f, 3.0000f, 1.0000f, 4.0000f, new CubeDeformation(0.0000f)).texOffs(48, 22).addBox(-1.6000f, -0.2500f, -3.0000f, 3.0000f, 1.0000f, 4.0000f, new CubeDeformation(0.0000f)), PartPose.offset(0.2000f, 5.2500f, 2.2500f));
        PartDefinition bone143 = rightFoot.addOrReplaceChild("bone143", CubeListBuilder.create().texOffs(24, 21).addBox(-0.5000f, -0.5000f, -2.0500f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offset(1.1000f, -0.1500f, -2.7500f));
        PartDefinition bone144 = bone143.addOrReplaceChild("bone144", CubeListBuilder.create().texOffs(24, 18).addBox(-0.5000f, -0.5000f, -1.8000f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, -1.9000f, 0.5236f, 0.0000f, 0.0000f));
        PartDefinition bone9 = rightFoot.addOrReplaceChild("bone9", CubeListBuilder.create().texOffs(24, 21).addBox(-0.5000f, -0.5000f, -2.0500f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(1.1000f, -0.1500f, -0.7500f, 0.0000f, -0.5236f, 0.0000f));
        PartDefinition bone10 = bone9.addOrReplaceChild("bone10", CubeListBuilder.create().texOffs(24, 18).addBox(-0.5000f, -0.5000f, -1.8000f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, -1.9000f, 0.5236f, 0.0000f, 0.0000f));
        PartDefinition bone20 = rightFoot.addOrReplaceChild("bone20", CubeListBuilder.create().texOffs(24, 21).addBox(-0.5000f, -0.5000f, -2.0500f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offset(-0.0500f, -0.1500f, -2.7500f));
        PartDefinition bone21 = bone20.addOrReplaceChild("bone21", CubeListBuilder.create().texOffs(24, 18).addBox(-0.5000f, -0.5000f, -1.8000f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, -1.9000f, 0.5236f, 0.0000f, 0.0000f));
        PartDefinition bone22 = rightFoot.addOrReplaceChild("bone22", CubeListBuilder.create().texOffs(24, 21).addBox(-0.5000f, -0.5000f, -2.0500f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offset(-1.2000f, -0.1500f, -2.7500f));
        PartDefinition bone23 = bone22.addOrReplaceChild("bone23", CubeListBuilder.create().texOffs(24, 18).addBox(-0.5000f, -0.5000f, -1.8000f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, -1.9000f, 0.5236f, 0.0000f, 0.0000f));
        PartDefinition bone24 = rightFoot.addOrReplaceChild("bone24", CubeListBuilder.create().texOffs(24, 21).addBox(-0.5000f, -0.5000f, -2.0500f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(-1.2000f, -0.1500f, -2.0000f, 0.0000f, 0.4363f, 0.0000f));
        PartDefinition bone25 = bone24.addOrReplaceChild("bone25", CubeListBuilder.create().texOffs(24, 18).addBox(-0.5000f, -0.5000f, -1.8000f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, -1.9000f, 0.5236f, 0.0000f, 0.0000f));
        PartDefinition field_178722_k = partdefinition.addOrReplaceChild("field_178722_k", CubeListBuilder.create(), PartPose.offset(1.9000f, 12.0000f, 0.0000f));
        PartDefinition bone32 = field_178722_k.addOrReplaceChild("bone32", CubeListBuilder.create().mirror().texOffs(48, 0).addBox(-2.1000f, 1.0000f, -2.0000f, 4.0000f, 6.0000f, 4.0000f, new CubeDeformation(0.5000f)).mirror(false), PartPose.offsetAndRotation(0.1000f, -1.0000f, 1.2500f, -0.2618f, -0.4363f, 0.0000f));
        PartDefinition bone33 = bone32.addOrReplaceChild("bone33", CubeListBuilder.create().mirror().texOffs(16, 45).addBox(-2.1000f, -0.1028f, 0.0789f, 4.0000f, 6.0000f, 4.0000f, new CubeDeformation(0.1000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 7.5000f, -2.0000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition leftFoot = bone33.addOrReplaceChild("leftFoot", CubeListBuilder.create().mirror().texOffs(48, 10).addBox(-1.4000f, -0.7500f, -3.0000f, 3.0000f, 1.0000f, 4.0000f, new CubeDeformation(0.0000f)).mirror(false).mirror().texOffs(48, 22).addBox(-1.4000f, -0.2500f, -3.0000f, 3.0000f, 1.0000f, 4.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offset(-0.2000f, 5.2500f, 2.2500f));
        PartDefinition bone34 = leftFoot.addOrReplaceChild("bone34", CubeListBuilder.create().mirror().texOffs(24, 21).addBox(-0.5000f, -0.5000f, -2.0500f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offset(-1.1000f, -0.1500f, -2.7500f));
        PartDefinition bone35 = bone34.addOrReplaceChild("bone35", CubeListBuilder.create().mirror().texOffs(24, 18).addBox(-0.5000f, -0.5000f, -1.8000f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, -1.9000f, 0.5236f, 0.0000f, 0.0000f));
        PartDefinition bone36 = leftFoot.addOrReplaceChild("bone36", CubeListBuilder.create().mirror().texOffs(24, 21).addBox(-0.5000f, -0.5000f, -2.0500f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(-1.1000f, -0.1500f, -0.7500f, 0.0000f, 0.5236f, 0.0000f));
        PartDefinition bone37 = bone36.addOrReplaceChild("bone37", CubeListBuilder.create().mirror().texOffs(24, 18).addBox(-0.5000f, -0.5000f, -1.8000f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, -1.9000f, 0.5236f, 0.0000f, 0.0000f));
        PartDefinition bone38 = leftFoot.addOrReplaceChild("bone38", CubeListBuilder.create().mirror().texOffs(24, 21).addBox(-0.5000f, -0.5000f, -2.0500f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offset(0.0500f, -0.1500f, -2.7500f));
        PartDefinition bone39 = bone38.addOrReplaceChild("bone39", CubeListBuilder.create().mirror().texOffs(24, 18).addBox(-0.5000f, -0.5000f, -1.8000f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, -1.9000f, 0.5236f, 0.0000f, 0.0000f));
        PartDefinition bone40 = leftFoot.addOrReplaceChild("bone40", CubeListBuilder.create().mirror().texOffs(24, 21).addBox(-0.5000f, -0.5000f, -2.0500f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offset(1.2000f, -0.1500f, -2.7500f));
        PartDefinition bone41 = bone40.addOrReplaceChild("bone41", CubeListBuilder.create().mirror().texOffs(24, 18).addBox(-0.5000f, -0.5000f, -1.8000f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, -1.9000f, 0.5236f, 0.0000f, 0.0000f));
        PartDefinition bone42 = leftFoot.addOrReplaceChild("bone42", CubeListBuilder.create().mirror().texOffs(24, 21).addBox(-0.5000f, -0.5000f, -2.0500f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(1.2000f, -0.1500f, -2.0000f, 0.0000f, -0.4363f, 0.0000f));
        PartDefinition bone43 = bone42.addOrReplaceChild("bone43", CubeListBuilder.create().mirror().texOffs(24, 18).addBox(-0.5000f, -0.5000f, -1.8000f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, -1.9000f, 0.5236f, 0.0000f, 0.0000f));
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
