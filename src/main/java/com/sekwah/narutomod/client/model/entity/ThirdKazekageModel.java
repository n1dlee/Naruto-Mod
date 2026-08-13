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
 * Geometry imported from the 1.12.2 mod's ThirdKazekage.
 * Machine-converted from bytecode: box coordinates and pivots are the originals,
 * so this model shares their +Y-downward authoring convention.
 */
public class ThirdKazekageModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(NarutoMod.MOD_ID, "puppet_third_kazekage"), "main");

    private final ModelPart root;

    public ThirdKazekageModel(ModelPart root) {
        super(net.minecraft.client.renderer.RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partdefinition = mesh.getRoot();
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0000f, -8.0000f, -4.0000f, 8.0000f, 8.0000f, 8.0000f, new CubeDeformation(0.0000f)), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(50, 24).addBox(-1.5000f, -1.0000f, -4.0100f, 3.0000f, 2.0000f, 4.0000f, new CubeDeformation(0.0000f)), PartPose.offset(0.0000f, -1.0000f, 0.0000f));
        PartDefinition jaw2 = head.addOrReplaceChild("jaw2", CubeListBuilder.create().texOffs(36, 24).addBox(-4.0000f, -2.0000f, -4.0000f, 3.0000f, 3.0000f, 4.0000f, new CubeDeformation(-0.0100f)).mirror().texOffs(36, 24).addBox(1.0000f, -2.0000f, -4.0000f, 3.0000f, 3.0000f, 4.0000f, new CubeDeformation(-0.0100f)).mirror(false), PartPose.offset(0.0000f, -1.0000f, 0.0000f));
        PartDefinition hat = partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition niceHair = hat.addOrReplaceChild("niceHair", CubeListBuilder.create(), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition cube_r1 = niceHair.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(16, 16).addBox(-0.9464f, 0.1951f, -0.6367f, 2.0000f, 10.0000f, 2.0000f, new CubeDeformation(0.1500f)), PartPose.offsetAndRotation(3.4118f, -8.1983f, -3.0337f, -0.0873f, 0.5236f, -0.0873f));
        PartDefinition cube_r2 = niceHair.addOrReplaceChild("cube_r2", CubeListBuilder.create().mirror().texOffs(16, 16).addBox(-1.0536f, 0.1951f, -0.6367f, 2.0000f, 10.0000f, 2.0000f, new CubeDeformation(0.1500f)).mirror(false), PartPose.offsetAndRotation(-3.4118f, -8.1983f, -3.0337f, -0.0873f, -0.5236f, 0.0873f));
        PartDefinition cube_r3 = niceHair.addOrReplaceChild("cube_r3", CubeListBuilder.create().mirror().texOffs(32, 11).addBox(-4.0000f, -6.1500f, 0.1500f, 8.0000f, 6.0000f, 7.0000f, new CubeDeformation(0.1500f)).mirror(false), PartPose.offsetAndRotation(0.0050f, -7.9697f, -3.8572f, -0.8727f, 0.0873f, 0.0000f));
        PartDefinition cube_r4 = niceHair.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(32, 11).addBox(-4.0000f, -6.1500f, 0.1500f, 8.0000f, 6.0000f, 7.0000f, new CubeDeformation(0.2000f)), PartPose.offsetAndRotation(0.0050f, -7.9697f, -3.8572f, -1.0908f, -0.0873f, 0.0000f));
        PartDefinition cube_r5 = niceHair.addOrReplaceChild("cube_r5", CubeListBuilder.create().mirror().texOffs(32, 11).addBox(-4.0000f, -6.6500f, 0.2500f, 8.0000f, 6.0000f, 7.0000f, new CubeDeformation(0.2500f)).mirror(false), PartPose.offsetAndRotation(0.0050f, -7.9697f, -3.8572f, -1.2654f, 0.1745f, 0.0000f));
        PartDefinition cube_r6 = niceHair.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(32, 11).addBox(-4.0000f, -7.1500f, 0.3500f, 8.0000f, 6.0000f, 7.0000f, new CubeDeformation(0.3000f)), PartPose.offsetAndRotation(0.0050f, -7.9697f, -3.8572f, -1.4399f, -0.1745f, 0.0000f));
        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition collar = body.addOrReplaceChild("collar", CubeListBuilder.create(), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition collar1 = collar.addOrReplaceChild("collar1", CubeListBuilder.create().texOffs(32, 0).addBox(-7.0000f, -10.0000f, 0.0000f, 14.0000f, 10.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -0.1160f, -2.8840f, -1.0472f, 0.0000f, 0.0000f));
        PartDefinition collar2 = collar.addOrReplaceChild("collar2", CubeListBuilder.create().mirror().texOffs(32, 0).addBox(-7.0000f, -10.0000f, 0.0000f, 14.0000f, 10.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -0.1160f, -2.8840f, -1.0908f, 0.0000f, 0.0873f));
        PartDefinition collar3 = collar.addOrReplaceChild("collar3", CubeListBuilder.create().texOffs(32, 0).addBox(-7.0000f, -10.0000f, 0.0000f, 14.0000f, 10.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -0.1160f, -2.8840f, -1.1345f, 0.0000f, -0.0873f));
        PartDefinition collar4 = collar.addOrReplaceChild("collar4", CubeListBuilder.create().mirror().texOffs(32, 0).addBox(-7.0000f, -10.0000f, 0.0000f, 14.0000f, 10.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -0.1160f, -2.8840f, -1.1781f, 0.0000f, 0.0873f));
        PartDefinition collar5 = collar.addOrReplaceChild("collar5", CubeListBuilder.create().texOffs(32, 0).addBox(-7.0000f, -10.0000f, 0.0000f, 14.0000f, 10.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -0.1160f, -2.6340f, -1.2217f, 0.0000f, -0.0873f));
        PartDefinition collar6 = collar.addOrReplaceChild("collar6", CubeListBuilder.create().mirror().texOffs(32, 0).addBox(-7.0000f, -10.0000f, 0.0000f, 14.0000f, 10.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -0.1160f, -2.6340f, -1.2654f, 0.0000f, 0.0873f));
        PartDefinition collar7 = collar.addOrReplaceChild("collar7", CubeListBuilder.create().texOffs(32, 0).addBox(-7.0000f, -10.0000f, 0.0000f, 14.0000f, 10.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -0.1160f, -2.6340f, -1.3090f, 0.0000f, -0.0873f));
        PartDefinition collar8 = collar.addOrReplaceChild("collar8", CubeListBuilder.create().mirror().texOffs(32, 0).addBox(-7.0000f, -10.0000f, 0.0000f, 14.0000f, 10.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -0.1160f, -2.6340f, -1.3526f, 0.0000f, 0.0873f));
        PartDefinition collar9 = collar.addOrReplaceChild("collar9", CubeListBuilder.create().texOffs(32, 0).addBox(-7.0000f, -10.0000f, 0.0000f, 14.0000f, 10.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -0.1160f, -2.8840f, -1.3963f, 0.0000f, -0.0873f));
        PartDefinition collar10 = collar.addOrReplaceChild("collar10", CubeListBuilder.create().mirror().texOffs(32, 0).addBox(-7.0000f, -10.0000f, 0.0000f, 14.0000f, 10.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -0.1160f, -2.8840f, -1.4399f, 0.0000f, 0.0873f));
        PartDefinition collar11 = collar.addOrReplaceChild("collar11", CubeListBuilder.create().mirror().texOffs(32, 0).addBox(-7.0000f, -10.0000f, 0.0000f, 14.0000f, 10.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -0.1160f, -2.8840f, -1.4835f, 0.0000f, -0.0873f));
        PartDefinition collar12 = collar.addOrReplaceChild("collar12", CubeListBuilder.create().texOffs(32, 0).addBox(-7.0000f, -10.0000f, 0.0000f, 14.0000f, 10.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -0.1160f, -2.8840f, -1.5272f, 0.0000f, 0.0873f));
        PartDefinition collar13 = collar.addOrReplaceChild("collar13", CubeListBuilder.create().mirror().texOffs(32, 0).addBox(-7.0000f, -10.0000f, 0.0000f, 14.0000f, 10.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -0.1160f, -2.8840f, -1.5708f, 0.0000f, -0.0873f));
        PartDefinition collar14 = collar.addOrReplaceChild("collar14", CubeListBuilder.create().texOffs(32, 0).addBox(-7.0000f, -10.0000f, 0.0000f, 14.0000f, 10.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -0.1160f, -2.8840f, -1.6144f, 0.0000f, 0.0873f));
        PartDefinition collar15 = collar.addOrReplaceChild("collar15", CubeListBuilder.create().mirror().texOffs(32, 0).addBox(-7.0000f, -10.0000f, 0.0000f, 14.0000f, 10.0000f, 1.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -0.1160f, -2.8840f, -1.6581f, 0.0000f, -0.0873f));
        PartDefinition collar16 = collar.addOrReplaceChild("collar16", CubeListBuilder.create().texOffs(32, 0).addBox(-7.0000f, -10.0000f, 0.0000f, 14.0000f, 10.0000f, 1.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -0.1160f, -2.8840f, -1.7017f, 0.0000f, 0.0000f));
        PartDefinition bone = body.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(16, 36).addBox(-4.0000f, 0.0000f, -2.0000f, 8.0000f, 24.0000f, 4.0000f, new CubeDeformation(0.5000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, -0.0873f, 0.0000f, 0.0000f));
        PartDefinition bone2 = body.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(40, 36).addBox(-4.0000f, 0.0000f, -2.0000f, 8.0000f, 24.0000f, 4.0000f, new CubeDeformation(0.5000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 0.0000f, 0.0873f, 0.0000f, 0.0000f));
        PartDefinition right_leg = body.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0000f, 0.0000f, -2.0000f, 4.0000f, 12.0000f, 4.0000f, new CubeDeformation(0.0000f)), PartPose.offset(-1.9000f, 12.0000f, 0.0000f));
        PartDefinition left_leg = body.addOrReplaceChild("left_leg", CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0000f, 0.0000f, -2.0000f, 4.0000f, 12.0000f, 4.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offset(1.9000f, 12.0000f, 0.0000f));
        PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 32).addBox(-3.0000f, -2.0000f, -2.0000f, 4.0000f, 12.0000f, 4.0000f, new CubeDeformation(0.0000f)).texOffs(0, 48).addBox(-3.0000f, -2.0000f, -2.0000f, 4.0000f, 12.0000f, 4.0000f, new CubeDeformation(0.5000f)), PartPose.offset(-5.0000f, 2.0000f, 0.0000f));
        PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().mirror().texOffs(0, 32).addBox(-1.0000f, -2.0000f, -2.0000f, 4.0000f, 12.0000f, 4.0000f, new CubeDeformation(0.0000f)).mirror(false).mirror().texOffs(0, 48).addBox(-1.0000f, -2.0000f, -2.0000f, 4.0000f, 12.0000f, 4.0000f, new CubeDeformation(0.5000f)).mirror(false), PartPose.offset(5.0000f, 2.0000f, 0.0000f));
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
