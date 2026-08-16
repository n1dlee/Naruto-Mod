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
 * Geometry imported from the 1.12.2 mod's DragonHead.
 * Machine-converted from bytecode: box coordinates and pivots are the originals,
 * so this model shares their +Y-downward authoring convention.
 */
public class DragonHeadModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(NarutoMod.MOD_ID, "dragon_head"), "main");

    private final ModelPart root;

    public DragonHeadModel(ModelPart root) {
        super(net.minecraft.client.renderer.RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partdefinition = mesh.getRoot();
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(176, 44).addBox(-6.0000f, 6.0000f, -26.0000f, 12.0000f, 5.0000f, 16.0000f, new CubeDeformation(1.0000f)).texOffs(112, 30).addBox(-8.0000f, -1.0000f, -11.0000f, 16.0000f, 16.0000f, 16.0000f, new CubeDeformation(1.0000f)).texOffs(112, 0).addBox(-5.0000f, 5.0000f, -26.0000f, 2.0000f, 2.0000f, 4.0000f, new CubeDeformation(1.0000f)).mirror().texOffs(112, 0).addBox(3.0000f, 5.0000f, -26.0000f, 2.0000f, 2.0000f, 4.0000f, new CubeDeformation(1.0000f)).mirror(false), PartPose.offset(0.0000f, 0.0000f, 0.0000f));
        PartDefinition teethUpper = head.addOrReplaceChild("teethUpper", CubeListBuilder.create().texOffs(152, 146).addBox(-6.0000f, -12.0000f, -26.0000f, 12.0000f, 2.0000f, 16.0000f, new CubeDeformation(0.5000f)), PartPose.offset(0.0000f, 24.0000f, 0.0000f));
        PartDefinition bone = head.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 200).addBox(0.0000f, -8.0000f, 0.0000f, 8.0000f, 16.0000f, 0.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(9.0000f, 7.0000f, -11.0000f, 0.0000f, -0.7854f, 0.0000f));
        PartDefinition bone2 = head.addOrReplaceChild("bone2", CubeListBuilder.create().mirror().texOffs(0, 200).addBox(-8.0000f, -8.0000f, 0.0000f, 8.0000f, 16.0000f, 0.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(-9.0000f, 7.0000f, -11.0000f, 0.0000f, 0.7854f, 0.0000f));
        PartDefinition bone3 = head.addOrReplaceChild("bone3", CubeListBuilder.create().texOffs(0, 50).addBox(-8.0000f, -10.0000f, 0.0000f, 16.0000f, 10.0000f, 0.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -2.0000f, -11.0000f, -0.8727f, 0.0000f, 0.0000f));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(176, 65).addBox(-6.0000f, 0.0000f, -16.7500f, 12.0000f, 4.0000f, 16.0000f, new CubeDeformation(1.0000f)), PartPose.offset(0.0000f, 11.0000f, -9.0000f));
        PartDefinition teethLower = jaw.addOrReplaceChild("teethLower", CubeListBuilder.create().texOffs(112, 144).addBox(-6.0000f, -16.0000f, -25.7500f, 12.0000f, 2.0000f, 16.0000f, new CubeDeformation(0.5000f)), PartPose.offset(0.0000f, 13.0000f, 9.0000f));
        PartDefinition hornRight = head.addOrReplaceChild("hornRight", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -2.0000f, 0.0000f, 2.0000f, 4.0000f, 6.0000f, new CubeDeformation(1.0000f)), PartPose.offsetAndRotation(-6.0000f, -2.0000f, -13.0000f, 0.0873f, -0.5236f, 0.0000f));
        PartDefinition hornRight0 = hornRight.addOrReplaceChild("hornRight0", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -2.0000f, 0.0000f, 2.0000f, 4.0000f, 6.0000f, new CubeDeformation(0.8000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 7.0000f, 0.0873f, 0.0873f, 0.0000f));
        PartDefinition hornRight1 = hornRight0.addOrReplaceChild("hornRight1", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -2.0000f, 0.0000f, 2.0000f, 4.0000f, 6.0000f, new CubeDeformation(0.6000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 7.0000f, 0.0873f, 0.0873f, 0.0000f));
        PartDefinition hornRight2 = hornRight1.addOrReplaceChild("hornRight2", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -2.0000f, 0.0000f, 2.0000f, 4.0000f, 6.0000f, new CubeDeformation(0.4000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 7.0000f, 0.0873f, 0.0873f, 0.0000f));
        PartDefinition hornRight3 = hornRight2.addOrReplaceChild("hornRight3", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -2.0000f, 0.0000f, 2.0000f, 4.0000f, 6.0000f, new CubeDeformation(0.2000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 7.0000f, 0.0873f, 0.0873f, 0.0000f));
        PartDefinition hornRight4 = hornRight3.addOrReplaceChild("hornRight4", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -2.0000f, 0.0000f, 2.0000f, 4.0000f, 6.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 7.0000f, 0.0873f, 0.0873f, 0.0000f));
        PartDefinition hornLeft = head.addOrReplaceChild("hornLeft", CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-1.0000f, -2.0000f, 0.0000f, 2.0000f, 4.0000f, 6.0000f, new CubeDeformation(1.0000f)).mirror(false), PartPose.offsetAndRotation(6.0000f, -2.0000f, -13.0000f, 0.0873f, 0.5236f, 0.0000f));
        PartDefinition hornLeft0 = hornLeft.addOrReplaceChild("hornLeft0", CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-1.0000f, -2.0000f, 0.0000f, 2.0000f, 4.0000f, 6.0000f, new CubeDeformation(0.8000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, 7.0000f, 0.0873f, -0.0873f, 0.0000f));
        PartDefinition hornLeft1 = hornLeft0.addOrReplaceChild("hornLeft1", CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-1.0000f, -2.0000f, 0.0000f, 2.0000f, 4.0000f, 6.0000f, new CubeDeformation(0.6000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, 7.0000f, 0.0873f, -0.0873f, 0.0000f));
        PartDefinition hornLeft2 = hornLeft1.addOrReplaceChild("hornLeft2", CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-1.0000f, -2.0000f, 0.0000f, 2.0000f, 4.0000f, 6.0000f, new CubeDeformation(0.4000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, 7.0000f, 0.0873f, -0.0873f, 0.0000f));
        PartDefinition hornLeft3 = hornLeft2.addOrReplaceChild("hornLeft3", CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-1.0000f, -2.0000f, 0.0000f, 2.0000f, 4.0000f, 6.0000f, new CubeDeformation(0.2000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, 7.0000f, 0.0873f, -0.0873f, 0.0000f));
        PartDefinition hornLeft4 = hornLeft3.addOrReplaceChild("hornLeft4", CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-1.0000f, -2.0000f, 0.0000f, 2.0000f, 4.0000f, 6.0000f, new CubeDeformation(0.0000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, 7.0000f, 0.0873f, -0.0873f, 0.0000f));
        PartDefinition whiskerLeft0 = head.addOrReplaceChild("whiskerLeft0", CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-1.0000f, -1.0000f, 0.0000f, 2.0000f, 2.0000f, 6.0000f, new CubeDeformation(0.8000f)).mirror(false), PartPose.offsetAndRotation(6.0000f, 6.0000f, -24.0000f, 0.0000f, 1.0472f, 0.0000f));
        PartDefinition whiskerLeft1 = whiskerLeft0.addOrReplaceChild("whiskerLeft1", CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-1.0000f, -1.0000f, 0.0000f, 2.0000f, 2.0000f, 6.0000f, new CubeDeformation(0.7000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, 6.0000f, -0.0873f, -0.1745f, 0.0000f));
        PartDefinition whiskerLeft2 = whiskerLeft1.addOrReplaceChild("whiskerLeft2", CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-1.0000f, -1.0000f, 0.0000f, 2.0000f, 2.0000f, 6.0000f, new CubeDeformation(0.6000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, 6.0000f, -0.0873f, -0.1745f, 0.0000f));
        PartDefinition whiskerLeft3 = whiskerLeft2.addOrReplaceChild("whiskerLeft3", CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-1.0000f, -1.0000f, 0.0000f, 2.0000f, 2.0000f, 6.0000f, new CubeDeformation(0.5000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, 6.0000f, -0.0873f, -0.1745f, 0.0000f));
        PartDefinition whiskerLeft4 = whiskerLeft3.addOrReplaceChild("whiskerLeft4", CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-1.0000f, -1.0000f, 0.0000f, 2.0000f, 2.0000f, 6.0000f, new CubeDeformation(0.4000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, 6.0000f, -0.0873f, -0.1745f, 0.0000f));
        PartDefinition whiskerLeft5 = whiskerLeft4.addOrReplaceChild("whiskerLeft5", CubeListBuilder.create().mirror().texOffs(0, 0).addBox(-1.0000f, -1.0000f, 0.0000f, 2.0000f, 2.0000f, 6.0000f, new CubeDeformation(0.2000f)).mirror(false), PartPose.offsetAndRotation(0.0000f, 0.0000f, 6.0000f, -0.0873f, -0.1745f, 0.0000f));
        PartDefinition whiskerRight0 = head.addOrReplaceChild("whiskerRight0", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -1.0000f, 0.0000f, 2.0000f, 2.0000f, 6.0000f, new CubeDeformation(0.8000f)), PartPose.offsetAndRotation(-6.0000f, 6.0000f, -24.0000f, 0.0000f, -1.0472f, 0.0000f));
        PartDefinition whiskerRight1 = whiskerRight0.addOrReplaceChild("whiskerRight1", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -1.0000f, 0.0000f, 2.0000f, 2.0000f, 6.0000f, new CubeDeformation(0.7000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 6.0000f, -0.0873f, 0.1745f, 0.0000f));
        PartDefinition whiskerRight2 = whiskerRight1.addOrReplaceChild("whiskerRight2", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -1.0000f, 0.0000f, 2.0000f, 2.0000f, 6.0000f, new CubeDeformation(0.6000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 6.0000f, -0.0873f, 0.1745f, 0.0000f));
        PartDefinition whiskerRight3 = whiskerRight2.addOrReplaceChild("whiskerRight3", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -1.0000f, 0.0000f, 2.0000f, 2.0000f, 6.0000f, new CubeDeformation(0.5000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 6.0000f, -0.0873f, 0.1745f, 0.0000f));
        PartDefinition whiskerRight4 = whiskerRight3.addOrReplaceChild("whiskerRight4", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -1.0000f, 0.0000f, 2.0000f, 2.0000f, 6.0000f, new CubeDeformation(0.4000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 6.0000f, -0.0873f, 0.1745f, 0.0000f));
        PartDefinition whiskerRight5 = whiskerRight4.addOrReplaceChild("whiskerRight5", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0000f, -1.0000f, 0.0000f, 2.0000f, 2.0000f, 6.0000f, new CubeDeformation(0.2000f)), PartPose.offsetAndRotation(0.0000f, 0.0000f, 6.0000f, -0.0873f, 0.1745f, 0.0000f));
        return LayerDefinition.create(mesh, 256, 256);
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
