package com.sekwah.narutomod.client.model.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * A single ice mirror: a tall, thin, slightly-tapered pane, with a raised border so the edges
 * catch the light instead of the whole thing reading as a flat sticker.
 *
 * Written by hand rather than imported. The 1.12.2 mod renders this technique as one closed
 * dome, which cannot be taken apart - and being able to take it apart is the entire point of
 * making the mirrors entities.
 */
public class IceMirrorModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "ice_mirror"), "main");

    /** Feet to top, in blocks, unscaled - the renderer needs it to stand the pane up. */
    public static final float HEIGHT_BLOCKS = 32f / 16f;

    private final ModelPart root;

    public IceMirrorModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        // Authored in the vanilla convention: +Y runs downward, so the pane hangs from 0 to
        // -32 and the renderer's flip stands it on the ground.
        PartDefinition pane = parts.addOrReplaceChild("pane",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-5.0f, -32.0f, -0.5f, 10.0f, 32.0f, 1.0f, new CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 0.0f, 0.0f));

        // Border: four thin bars framing the pane, inset very slightly so they do not z-fight.
        pane.addOrReplaceChild("frame_left",
                CubeListBuilder.create().texOffs(0, 34)
                        .addBox(-6.0f, -32.0f, -0.6f, 1.0f, 32.0f, 1.2f, new CubeDeformation(0.0f)),
                PartPose.ZERO);
        pane.addOrReplaceChild("frame_right",
                CubeListBuilder.create().texOffs(0, 34)
                        .addBox(5.0f, -32.0f, -0.6f, 1.0f, 32.0f, 1.2f, new CubeDeformation(0.0f)),
                PartPose.ZERO);
        pane.addOrReplaceChild("frame_top",
                CubeListBuilder.create().texOffs(4, 34)
                        .addBox(-6.0f, -33.0f, -0.6f, 12.0f, 1.0f, 1.2f, new CubeDeformation(0.0f)),
                PartPose.ZERO);
        pane.addOrReplaceChild("frame_bottom",
                CubeListBuilder.create().texOffs(4, 34)
                        .addBox(-6.0f, 0.0f, -0.6f, 12.0f, 1.0f, 1.2f, new CubeDeformation(0.0f)),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
