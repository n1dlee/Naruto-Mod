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
 * Geometry imported from the 1.12.2 mod's GiantToad.
 * Machine-converted from bytecode: box coordinates and pivots are the originals,
 * so this model shares their +Y-downward authoring convention.
 */
public class GiantToadModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(NarutoMod.MOD_ID, "giant_toad"), "main");

    private final ModelPart root;

    public GiantToadModel(ModelPart root) {
        super(net.minecraft.client.renderer.RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partdefinition = mesh.getRoot();
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 20).addBox(-4.4600f, -5.5580f, -6.0708f, 9.0000f, 5.0000f, 8.0000f, new CubeDeformation(0.0000f)), PartPose.offset(0.0000f, 11.5800f, -5.4640f));
        PartDefinition neck = head.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 42).addBox(-4.5000f, -0.0449f, 0.0000f, 9.0000f, 3.0000f, 4.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0400f, -5.5151f, 1.8721f, -0.9163f, 0.0000f, 0.0000f));
        PartDefinition browRight = head.addOrReplaceChild("browRight", CubeListBuilder.create().texOffs(13, 49).addBox(-2.2900f, -0.5000f, 0.2500f, 4.0000f, 1.0000f, 5.0000f, new CubeDeformation(0.3000f)), PartPose.offsetAndRotation(-2.7100f, -4.3080f, -6.5708f, 0.0000f, 0.0873f, 0.5672f));
        PartDefinition browLeft = head.addOrReplaceChild("browLeft", CubeListBuilder.create().mirror().texOffs(13, 49).addBox(-1.7100f, -0.5000f, 0.2500f, 4.0000f, 1.0000f, 5.0000f, new CubeDeformation(0.3000f)).mirror(false), PartPose.offsetAndRotation(2.7100f, -4.3080f, -6.5708f, 0.0000f, -0.0873f, -0.5672f));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 33).addBox(-4.5000f, -0.0901f, -4.8784f, 9.0000f, 2.0000f, 8.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0400f, -0.5003f, -1.1917f, 0.0873f, 0.0000f, 0.0000f));
        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0000f, 11.5800f, -5.4640f));
        PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create(), PartPose.offsetAndRotation(0.2500f, 1.3200f, 0.4640f, -0.7854f, 0.0000f, 0.0000f));
        PartDefinition part1 = chest.addOrReplaceChild("part1", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0000f, -5.7157f, -2.0345f, 12.0000f, 11.0000f, 9.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(-0.2000f, -1.5397f, 3.6327f, -0.0873f, 0.0000f, 0.0000f));
        PartDefinition part2 = chest.addOrReplaceChild("part2", CubeListBuilder.create(), PartPose.offset(-0.2000f, -1.5397f, 3.8827f));
        PartDefinition part3 = part2.addOrReplaceChild("part3", CubeListBuilder.create().texOffs(29, 28).addBox(-5.5000f, 0.1254f, -4.9000f, 11.0000f, 6.0000f, 5.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, -5.9657f, -1.8845f, 0.2443f, 0.0000f, 0.0000f));
        PartDefinition part4 = part2.addOrReplaceChild("part4", CubeListBuilder.create().texOffs(33, 0).addBox(-5.5000f, 0.0076f, -0.0243f, 11.0000f, 6.0000f, 3.0000f, new CubeDeformation(0.0000f)), PartPose.offsetAndRotation(0.0000f, 0.0343f, -5.6845f, 0.5323f, 0.0000f, 0.0000f));
        PartDefinition bunda = chest.addOrReplaceChild("bunda", CubeListBuilder.create().texOffs(30, 39).addBox(-5.5000f, -0.5000f, -0.5000f, 11.0000f, 9.0000f, 4.0000f, new CubeDeformation(-0.1000f)), PartPose.offsetAndRotation(-0.2420f, -6.0646f, 10.9442f, -0.4363f, 0.0000f, 0.0000f));
        PartDefinition armRight = body.addOrReplaceChild("armRight", CubeListBuilder.create().texOffs(52, 9).addBox(-1.5760f, -1.7778f, -1.4648f, 3.0000f, 8.0000f, 3.0000f, new CubeDeformation(0.2000f)), PartPose.offsetAndRotation(-5.0500f, -0.6800f, 2.5540f, -0.5236f, 0.5236f, 0.3491f));
        PartDefinition forearmRight = armRight.addOrReplaceChild("forearmRight", CubeListBuilder.create().texOffs(40, 52).addBox(-0.7120f, -0.9138f, -1.3328f, 3.0000f, 6.0000f, 3.0000f, new CubeDeformation(0.1000f)), PartPose.offsetAndRotation(-0.6600f, 6.8980f, -0.1000f, 0.0000f, 0.0000f, -0.5236f));
        PartDefinition handRight = forearmRight.addOrReplaceChild("handRight", CubeListBuilder.create(), PartPose.offsetAndRotation(1.0100f, 5.4780f, 0.1520f, 1.0472f, 0.2618f, 0.0000f));
        PartDefinition part5 = handRight.addOrReplaceChild("part5", CubeListBuilder.create().texOffs(16, 55).addBox(-1.0000f, -1.0000f, -3.7500f, 2.0000f, 2.0000f, 4.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(-1.1037f, -0.2173f, 0.4960f, 0.0000f, 0.3491f, 0.0000f));
        PartDefinition part6 = handRight.addOrReplaceChild("part6", CubeListBuilder.create().texOffs(16, 55).addBox(-1.0000f, -1.0000f, -3.7500f, 2.0000f, 2.0000f, 4.0000f, new CubeDeformation(-0.2000f)), PartPose.offset(-0.1037f, -0.2173f, 0.4960f));
        PartDefinition part7 = handRight.addOrReplaceChild("part7", CubeListBuilder.create().texOffs(16, 55).addBox(-1.0000f, -1.0000f, -3.7500f, 2.0000f, 2.0000f, 4.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(0.8963f, -0.2173f, 0.4960f, 0.0000f, -0.3491f, 0.0000f));
        PartDefinition blade = handRight.addOrReplaceChild("blade", CubeListBuilder.create().texOffs(0, 62).addBox(-4.2500f, -0.5000f, -0.5000f, 8.0000f, 1.0000f, 1.0000f, new CubeDeformation(0.0000f)).texOffs(18, 61).addBox(3.7500f, 0.0000f, -0.5000f, 10.0000f, 0.0000f, 1.0000f, new CubeDeformation(0.0200f)).texOffs(18, 62).addBox(3.5000f, 0.0000f, -0.5000f, 1.0000f, 0.0000f, 1.0000f, new CubeDeformation(0.0200f)), PartPose.offsetAndRotation(-0.2497f, 0.1945f, -1.0331f, 0.7854f, 0.1745f, 0.0000f));
        PartDefinition armLeft = body.addOrReplaceChild("armLeft", CubeListBuilder.create().mirror().texOffs(52, 9).addBox(-1.4240f, -1.7778f, -1.4648f, 3.0000f, 8.0000f, 3.0000f, new CubeDeformation(0.2000f)).mirror(false), PartPose.offsetAndRotation(5.0500f, -0.6800f, 2.5540f, -0.5236f, -0.5236f, -0.3491f));
        PartDefinition forearmLeft = armLeft.addOrReplaceChild("forearmLeft", CubeListBuilder.create().mirror().texOffs(40, 52).addBox(-2.2880f, -0.9138f, -1.3328f, 3.0000f, 6.0000f, 3.0000f, new CubeDeformation(0.1000f)).mirror(false), PartPose.offsetAndRotation(0.6600f, 6.8980f, -0.1000f, 0.0000f, 0.0000f, 0.5236f));
        PartDefinition handLeft = forearmLeft.addOrReplaceChild("handLeft", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.2600f, 6.7280f, -0.8480f, 1.0472f, -0.2618f, 0.0000f));
        PartDefinition part8 = handLeft.addOrReplaceChild("part8", CubeListBuilder.create().mirror().texOffs(16, 55).addBox(-1.0000f, -1.0000f, -3.7500f, 2.0000f, 2.0000f, 4.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offsetAndRotation(1.6040f, -0.0618f, 2.0292f, 0.0000f, -0.3491f, 0.0000f));
        PartDefinition part9 = handLeft.addOrReplaceChild("part9", CubeListBuilder.create().mirror().texOffs(16, 55).addBox(-1.0000f, -1.0000f, -3.7500f, 2.0000f, 2.0000f, 4.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offset(0.6040f, -0.0618f, 2.0292f));
        PartDefinition part10 = handLeft.addOrReplaceChild("part10", CubeListBuilder.create().mirror().texOffs(16, 55).addBox(-1.0000f, -1.0000f, -3.7500f, 2.0000f, 2.0000f, 4.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offsetAndRotation(-0.3960f, -0.0618f, 2.0292f, 0.0000f, 0.3491f, 0.0000f));
        PartDefinition legRight = partdefinition.addOrReplaceChild("legRight", CubeListBuilder.create(), PartPose.offsetAndRotation(-5.6770f, 19.8471f, 1.9223f, 0.2618f, 1.0472f, 0.0000f));
        PartDefinition thighRight = legRight.addOrReplaceChild("thighRight", CubeListBuilder.create().texOffs(32, 10).addBox(-2.9010f, -1.6142f, -9.4876f, 5.0000f, 3.0000f, 10.0000f, new CubeDeformation(0.2000f)), PartPose.offsetAndRotation(0.2410f, 1.0282f, 0.8872f, -0.6981f, 0.0000f, 0.0000f));
        PartDefinition legLowerRight = legRight.addOrReplaceChild("legLowerRight", CubeListBuilder.create(), PartPose.offsetAndRotation(-0.0653f, -4.0517f, -5.8381f, -0.5236f, 0.0000f, 0.0000f));
        PartDefinition part11 = legLowerRight.addOrReplaceChild("part11", CubeListBuilder.create().texOffs(0, 49).addBox(-1.3772f, -3.0266f, -0.0999f, 3.0000f, 3.0000f, 7.0000f, new CubeDeformation(0.2000f)), PartPose.offsetAndRotation(-0.1735f, 1.0450f, -0.8540f, -0.7418f, 0.0000f, 0.0000f));
        PartDefinition footRight = legLowerRight.addOrReplaceChild("footRight", CubeListBuilder.create(), PartPose.offsetAndRotation(-0.0107f, 5.1235f, 4.6603f, 0.2182f, 0.0000f, 0.0000f));
        PartDefinition part12 = footRight.addOrReplaceChild("part12", CubeListBuilder.create().texOffs(26, 52).addBox(-1.0000f, -1.0000f, -4.7000f, 2.0000f, 2.0000f, 5.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(-0.8960f, -0.0341f, -0.0512f, 0.0000f, 0.3491f, 0.0000f));
        PartDefinition part13 = footRight.addOrReplaceChild("part13", CubeListBuilder.create().texOffs(26, 52).addBox(-1.0000f, -1.0000f, -4.7000f, 2.0000f, 2.0000f, 5.0000f, new CubeDeformation(-0.2000f)), PartPose.offset(0.1040f, -0.0341f, -0.0512f));
        PartDefinition part14 = footRight.addOrReplaceChild("part14", CubeListBuilder.create().texOffs(26, 52).addBox(-1.0000f, -1.0000f, -4.7000f, 2.0000f, 2.0000f, 5.0000f, new CubeDeformation(-0.2000f)), PartPose.offsetAndRotation(1.1040f, -0.0341f, -0.0512f, 0.0000f, -0.3491f, 0.0000f));
        PartDefinition legLeft = partdefinition.addOrReplaceChild("legLeft", CubeListBuilder.create(), PartPose.offsetAndRotation(5.6770f, 19.8471f, 1.9223f, 0.2618f, -1.0472f, 0.0000f));
        PartDefinition thighLeft = legLeft.addOrReplaceChild("thighLeft", CubeListBuilder.create().mirror().texOffs(32, 10).addBox(-2.0990f, -1.6142f, -9.4876f, 5.0000f, 3.0000f, 10.0000f, new CubeDeformation(0.2000f)).mirror(false), PartPose.offsetAndRotation(-0.2410f, 1.0282f, 0.8872f, -0.6981f, 0.0000f, 0.0000f));
        PartDefinition legLowerLeft = legLeft.addOrReplaceChild("legLowerLeft", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0653f, -4.0517f, -5.8381f, -0.5236f, 0.0000f, 0.0000f));
        PartDefinition part15 = legLowerLeft.addOrReplaceChild("part15", CubeListBuilder.create().mirror().texOffs(0, 49).addBox(-1.6228f, -3.0266f, -0.0999f, 3.0000f, 3.0000f, 7.0000f, new CubeDeformation(0.2000f)).mirror(false), PartPose.offsetAndRotation(0.1735f, 1.0450f, -0.8540f, -0.7418f, 0.0000f, 0.0000f));
        PartDefinition footLeft = legLowerLeft.addOrReplaceChild("footLeft", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0107f, 5.1235f, 4.6603f, 0.2182f, 0.0000f, 0.0000f));
        PartDefinition part16 = footLeft.addOrReplaceChild("part16", CubeListBuilder.create().mirror().texOffs(26, 52).addBox(-1.0000f, -1.0000f, -4.7000f, 2.0000f, 2.0000f, 5.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offsetAndRotation(0.8960f, -0.0341f, -0.0512f, 0.0000f, -0.3491f, 0.0000f));
        PartDefinition part17 = footLeft.addOrReplaceChild("part17", CubeListBuilder.create().mirror().texOffs(26, 52).addBox(-1.0000f, -1.0000f, -4.7000f, 2.0000f, 2.0000f, 5.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offset(-0.1040f, -0.0341f, -0.0512f));
        PartDefinition part18 = footLeft.addOrReplaceChild("part18", CubeListBuilder.create().mirror().texOffs(26, 52).addBox(-1.0000f, -1.0000f, -4.7000f, 2.0000f, 2.0000f, 5.0000f, new CubeDeformation(-0.2000f)).mirror(false), PartPose.offsetAndRotation(-1.1040f, -0.0341f, -0.0512f, 0.0000f, 0.3491f, 0.0000f));
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
