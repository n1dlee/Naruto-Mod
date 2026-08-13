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
 * Geometry imported from the 1.12.2 mod's Karasu.
 * Machine-converted from bytecode: box coordinates and pivots are the originals,
 * so this model shares their +Y-downward authoring convention.
 */
public class KarasuModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(NarutoMod.MOD_ID, "puppet_karasu"), "main");

    private final ModelPart root;

    public KarasuModel(ModelPart root) {
        super(net.minecraft.client.renderer.RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partdefinition = mesh.getRoot();
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0000f, -8.0000f, -4.0000f, 8.0000f, 8.0000f, 8.0000f, new CubeDeformation(0.0000f)), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -1.0000f, -2.0000f, 2.0000f, 1.0000f, 2.0000f, new CubeDeformation(0.2500f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, -2.0000f, 0.5236f, 0.0000f, 0.0000f));
        PartDefinition shooter = head.addOrReplaceChild("shooter", CubeListBuilder.create().texOffs(11, 16).addBox(-0.5000f, -0.5000f, -1.0000f, 1.0000f, 1.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offset(0.0000f, -0.5000f, -5.0000f));
        PartDefinition hat = partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition bone3 = hat.addOrReplaceChild("bone3", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0000f, -4.0000f, -4.0000f, 8.0000f, 8.0000f, 8.0000f, new CubeDeformation(0.5000f)), PartPose.offsetAndRotation(0.0000f, -4.0000f, 0.5000f, 0.2618f, 0.0000f, 0.0000f));
        PartDefinition bone = hat.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0000f, -4.0000f, -4.0000f, 8.0000f, 8.0000f, 8.0000f, new CubeDeformation(0.5000f)), PartPose.offsetAndRotation(0.0000f, -4.0000f, 0.0000f, 0.0000f, 0.0000f, 0.2618f));
        PartDefinition bone2 = hat.addOrReplaceChild("bone2", CubeListBuilder.create().mirror().texOffs(32, 0).addBox(-4.0000f, -4.0000f, -4.0000f, 8.0000f, 8.0000f, 8.0000f, new CubeDeformation(0.5000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, -4.0000f, 0.0000f, 0.0000f, 0.0000f, -0.2618f));
        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0000f, 0.0000f, -2.0000f, 8.0000f, 12.0000f, 4.0000f, new CubeDeformation(0.0000f)).texOffs(16, 32).addBox(-4.0000f, 0.0000f, -2.0000f, 8.0000f, 12.0000f, 4.0000f, new CubeDeformation(0.6000f)), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition right_leg = body.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1.5000f, 0.0000f, -2.0000f, 3.0000f, 12.0000f, 4.0000f, new CubeDeformation(-0.2000f)).texOffs(0, 32).addBox(-2.0000f, 0.0000f, -2.0000f, 4.0000f, 12.0000f, 4.0000f, new CubeDeformation(0.6000f)), PartPose.offsetAndRotation(-1.9000f, 12.0000f, 0.0000f, 0.0000f, 0.0000f, 0.0873f));
        PartDefinition left_leg = body.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-1.5000f, 0.0000f, -2.0000f, 3.0000f, 12.0000f, 4.0000f, new CubeDeformation(-0.2000f)).texOffs(0, 48).addBox(-2.0000f, 0.0000f, -2.0000f, 4.0000f, 12.0000f, 4.0000f, new CubeDeformation(0.6000f)), PartPose.offsetAndRotation(1.9000f, 12.0000f, 0.0000f, 0.0000f, 0.0000f, -0.0873f));
        PartDefinition rightArm2 = body.addOrReplaceChild("rightArm2", CubeListBuilder.create().texOffs(40, 16).addBox(-2.0000f, -2.0000f, -2.0000f, 3.0000f, 12.0000f, 4.0000f, new CubeDeformation(-0.2000f)).texOffs(40, 32).addBox(-2.0000f, -2.0000f, -2.0000f, 3.0000f, 12.0000f, 4.0000f, new CubeDeformation(0.5000f)), PartPose.offsetAndRotation(-5.0000f, 7.5000f, 0.0000f, 0.0000f, 0.0000f, 0.2182f));
        PartDefinition blade2 = rightArm2.addOrReplaceChild("blade2", CubeListBuilder.create().texOffs(24, 0).addBox(1.0000f, -5.0000f, -1.0000f, 0.0000f, 6.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offset(-1.5000f, 14.5000f, 0.0000f));
        PartDefinition leftArm2 = body.addOrReplaceChild("leftArm2", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0000f, -2.0000f, -2.0000f, 3.0000f, 12.0000f, 4.0000f, new CubeDeformation(-0.2000f)).texOffs(48, 48).addBox(-1.0000f, -2.0000f, -2.0000f, 3.0000f, 12.0000f, 4.0000f, new CubeDeformation(0.5000f)), PartPose.offsetAndRotation(5.0000f, 7.5000f, 0.0000f, 0.0000f, 0.0000f, -0.2618f));
        PartDefinition blade3 = leftArm2.addOrReplaceChild("blade3", CubeListBuilder.create().mirror().texOffs(24, 0).addBox(-1.0000f, -5.0000f, -1.0000f, 0.0000f, 6.0000f, 2.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offset(1.5000f, 14.5000f, 0.0000f));
        PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-2.0000f, -2.0000f, -2.0000f, 3.0000f, 12.0000f, 4.0000f, new CubeDeformation(-0.2000f)).texOffs(40, 32).addBox(-2.0000f, -2.0000f, -2.0000f, 3.0000f, 12.0000f, 4.0000f, new CubeDeformation(0.6000f)), PartPose.offsetAndRotation(-5.0000f, 2.5000f, 0.0000f, 0.0000f, 0.0000f, 0.3491f));
        PartDefinition blade0 = right_arm.addOrReplaceChild("blade0", CubeListBuilder.create().texOffs(24, 0).addBox(1.0000f, -5.0000f, -1.0000f, 0.0000f, 6.0000f, 2.0000f, new CubeDeformation(0.0000f)), PartPose.offset(-1.5000f, 14.5000f, 0.0000f));
        PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0000f, -2.0000f, -2.0000f, 3.0000f, 12.0000f, 4.0000f, new CubeDeformation(-0.2000f)).texOffs(48, 48).addBox(-1.0000f, -2.0000f, -2.0000f, 3.0000f, 12.0000f, 4.0000f, new CubeDeformation(0.6000f)), PartPose.offsetAndRotation(5.0000f, 2.5000f, 0.0000f, 0.0000f, 0.0000f, -0.3491f));
        PartDefinition blade1 = left_arm.addOrReplaceChild("blade1", CubeListBuilder.create().mirror().texOffs(24, 0).addBox(-1.0000f, -5.0000f, -1.0000f, 0.0000f, 6.0000f, 2.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offset(1.5000f, 14.5000f, 0.0000f));
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
