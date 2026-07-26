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

/**
 * A single Kurama chakra tail (3 articulated segments). The renderer draws this
 * same geometry multiple times (fanned out + individually animated) to produce
 * 1, 4, or 9 tails depending on rank — see KuramaTailRenderer.
 */
public class KuramaTailModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "kurama_tail"), "main");

    private final ModelPart base;
    private final ModelPart mid;
    private final ModelPart tip;

    public KuramaTailModel(ModelPart modelPart) {
        super(RenderType::entityTranslucent);
        this.base = modelPart.getChild("base");
        this.mid = this.base.getChild("mid");
        this.tip = this.mid.getChild("tip");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition base = root.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2F, -2F, 0F, 4, 4, 6),
                PartPose.offsetAndRotation(0F, 0F, 0F, -0.3F, 0F, 0F));
        PartDefinition mid = base.addOrReplaceChild("mid",
                CubeListBuilder.create().texOffs(0, 10).addBox(-1.5F, -1.5F, 0F, 3, 3, 5),
                PartPose.offsetAndRotation(0F, 0F, 6F, -0.25F, 0F, 0F));
        mid.addOrReplaceChild("tip",
                CubeListBuilder.create().texOffs(0, 18).addBox(-1F, -1F, 0F, 2, 2, 4),
                PartPose.offsetAndRotation(0F, 0F, 5F, -0.2F, 0F, 0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    /** Applies a sine-wave sway to the mid/tip segments; wave is a small offset in radians. */
    public void animate(float wave) {
        this.mid.xRot = -0.25F + wave;
        this.tip.xRot = -0.2F + wave * 1.5F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int overlay,
                                float red, float green, float blue, float alpha) {
        this.base.render(poseStack, vertexConsumer, packedLight, overlay, red, green, blue, alpha);
    }
}
