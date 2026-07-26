package com.sekwah.narutomod.client.model.entity;

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

import java.util.ArrayList;
import java.util.List;

/**
 * Kuchiyose summon beast — a squat giant-toad silhouette (broad body, wide head with
 * eye bumps, four stubby haunched legs). The same geometry serves all three clan
 * contracts; the renderer tints it per variant. Ground-anchored (feet at Y=0),
 * vanilla model convention (negative Y = up, renderer applies the scale(-S,-S,S) flip).
 */
public class SummonBeastModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "summon_beast"), "main");

    private final List<ModelPart> parts = new ArrayList<>();

    public SummonBeastModel(ModelPart modelPart) {
        super(RenderType::entityCutoutNoCull);
        for (String name : new String[] {
                "front_left_leg", "front_right_leg", "back_left_leg", "back_right_leg",
                "body", "head"}) {
            this.parts.add(modelPart.getChild(name));
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Four stubby legs, haunched wider at the top
        root.addOrReplaceChild("front_left_leg",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2F, -6F, -2F, 4, 6, 4),
                PartPose.offset(6F, 0F, -5F));
        root.addOrReplaceChild("front_right_leg",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2F, -6F, -2F, 4, 6, 4),
                PartPose.offset(-6F, 0F, -5F));
        root.addOrReplaceChild("back_left_leg",
                CubeListBuilder.create().texOffs(0, 11).addBox(-3F, -8F, -3F, 6, 8, 6),
                PartPose.offset(7F, 0F, 5F));
        root.addOrReplaceChild("back_right_leg",
                CubeListBuilder.create().texOffs(0, 11).addBox(-3F, -8F, -3F, 6, 8, 6),
                PartPose.offset(-7F, 0F, 5F));

        // Broad squat body, slightly higher at the rear (toad posture)
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 26).addBox(-8F, -9F, -8F, 16, 9, 17),
                PartPose.offsetAndRotation(0F, -5F, 0F, -0.12F, 0F, 0F));

        // Wide head with eye bumps on top
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 53).addBox(-6F, -6F, -9F, 12, 6, 9),
                PartPose.offset(0F, -13F, -7F));
        head.addOrReplaceChild("left_eye",
                CubeListBuilder.create().texOffs(43, 0).addBox(-1.5F, -3F, -1.5F, 3, 3, 3),
                PartPose.offset(3.5F, -6F, -6F));
        head.addOrReplaceChild("right_eye",
                CubeListBuilder.create().texOffs(43, 7).addBox(-1.5F, -3F, -1.5F, 3, 3, 3),
                PartPose.offset(-3.5F, -6F, -6F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int overlay,
                                float red, float green, float blue, float alpha) {
        for (ModelPart part : this.parts) {
            part.render(poseStack, vertexConsumer, packedLight, overlay, red, green, blue, alpha);
        }
    }
}
