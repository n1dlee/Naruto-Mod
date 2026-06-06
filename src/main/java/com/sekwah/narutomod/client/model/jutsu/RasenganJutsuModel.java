package com.sekwah.narutomod.client.model.jutsu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class RasenganJutsuModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "rasengan"), "main");

    private final ModelPart main;

    public RasenganJutsuModel(ModelPart modelPart) {
        super(RenderType::entityTranslucent);
        this.main = modelPart.getChild("main");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();
        PartDefinition main = root.addOrReplaceChild("main",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-6F, -6F, -6F, 12, 12, 12),
                PartPose.ZERO);
        main.addOrReplaceChild("ring_x",
                CubeListBuilder.create()
                        .texOffs(0, 24)
                        .addBox(-11F, -1F, -1F, 22, 2, 2),
                PartPose.ZERO);
        main.addOrReplaceChild("ring_y",
                CubeListBuilder.create()
                        .texOffs(0, 28)
                        .addBox(-1F, -11F, -1F, 2, 22, 2),
                PartPose.ZERO);
        main.addOrReplaceChild("ring_z",
                CubeListBuilder.create()
                        .texOffs(0, 32)
                        .addBox(-1F, -1F, -11F, 2, 2, 22),
                PartPose.ZERO);
        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int overlay,
                               float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        poseStack.scale(1.4F, 1.4F, 1.4F);
        this.main.render(poseStack, vertexConsumer, packedLight, overlay, red, green, blue, alpha);
        poseStack.popPose();
    }
}
