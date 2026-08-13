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
 * Fox-themed chakra shell for the Kurama Cloak:
 * stages 1-2 (tails 4-8) = small worn claw plating on the player's own forearms,
 * stage 3 (tail 9, Full Avatar) = a full standing FOX — haunched hind legs, tapered
 * belly-to-chest torso, hanging arms ending in clawed paws, and a proper vulpine head:
 * long protruding snout with a jaw, cheek fur, and Kurama's signature tall ears.
 *
 * Full-Avatar body is ground-anchored (feet at local Y=0) and authored in the standard
 * vanilla entity-model convention (negative Y = up); the renderer applies the vanilla
 * flip via scale(-S,-S,S) — see KuramaTailRenderer. The worn claws use the same flip.
 */
public class KuramaAvatarModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "kurama_avatar"), "main");

    /**
     * Feet to ear tip, in blocks, unscaled: the fox stands from local Y=0 up to Y=-59 (head
     * pivot -40, ear pivot -8 from that, ear box 11 tall). Both renderers divide the height
     * they want by this, so neither carries a magic scale factor that can drift from the
     * other's - which is exactly how the player's fox ended up four times the boss's.
     */
    public static final float FULL_BODY_HEIGHT_BLOCKS = 59f / 16f;

    // Worn stages (tails 4-8)
    private final ModelPart leftClaw;
    private final ModelPart rightClaw;

    // Full Avatar (tail 9): complete standing fox, ground-anchored
    private final List<ModelPart> fullBody = new ArrayList<>();

    private int stage = 3;

    public KuramaAvatarModel(ModelPart modelPart) {
        super(RenderType::entityTranslucent);
        this.leftClaw = modelPart.getChild("left_claw");
        this.rightClaw = modelPart.getChild("right_claw");
        for (String name : new String[] {
                "left_leg", "right_leg", "belly", "chest",
                "left_arm", "right_arm", "head"}) {
            this.fullBody.add(modelPart.getChild(name));
        }
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // --- Worn stages (tails 4-8): claw plating fitted onto the player's own forearms ---
        root.addOrReplaceChild("left_claw",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1F, -1F, -3F, 2, 2, 6),
                PartPose.offset(5F, -6F, 0F));
        root.addOrReplaceChild("right_claw",
                CubeListBuilder.create().texOffs(0, 9).addBox(-1F, -1F, -3F, 2, 2, 6),
                PartPose.offset(-5F, -6F, 0F));

        // --- Full Avatar: standing fox, feet at local Y=0 ---

        // Hind legs: shin + a bulky haunch/thigh at the top (fox's crouched hindquarters)
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 20)
                        .addBox(-3F, -10F, -3F, 6, 10, 6)
                        .texOffs(0, 37).addBox(-4F, -20F, -5F, 8, 10, 10),
                PartPose.offset(5.5F, 0F, 1F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(25, 20)
                        .addBox(-3F, -10F, -3F, 6, 10, 6)
                        .texOffs(37, 37).addBox(-4F, -20F, -5F, 8, 10, 10),
                PartPose.offset(-5.5F, 0F, 1F));

        // Tapered torso: narrower belly rising into a broader chest, leaned slightly forward
        root.addOrReplaceChild("belly",
                CubeListBuilder.create().texOffs(0, 58).addBox(-5.5F, -10F, -4F, 11, 10, 9),
                PartPose.offset(0F, -20F, 0F));
        root.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(41, 58).addBox(-7F, -10F, -5F, 14, 10, 10),
                PartPose.offsetAndRotation(0F, -30F, 0F, -0.08F, 0F, 0F));

        // Arms hanging from the chest, ending in clawed paws (claws jut forward)
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(82, 0)
                        .addBox(-2F, 0F, -2F, 4, 16, 4)
                        .texOffs(99, 0).addBox(-2.5F, 16F, -2F, 5, 3, 7),
                PartPose.offsetAndRotation(8.5F, -38F, 0F, 0F, 0F, 0.1F));
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(82, 25)
                        .addBox(-2F, 0F, -2F, 4, 16, 4)
                        .texOffs(99, 25).addBox(-2.5F, 16F, -2F, 5, 3, 7),
                PartPose.offsetAndRotation(-8.5F, -38F, 0F, 0F, 0F, -0.1F));

        // Vulpine head: skull + long snout with jaw + cheek fur + Kurama's tall ears,
        // grouped under one pivot so it reads as a single head
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 80).addBox(-5F, -9F, -5F, 10, 9, 10),
                PartPose.offset(0F, -40F, 2F));
        head.addOrReplaceChild("snout",
                CubeListBuilder.create().texOffs(41, 80).addBox(-2.5F, -4F, 0F, 5, 4, 8),
                PartPose.offset(0F, -2F, 5F));
        head.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(41, 93).addBox(-2F, 0F, 0F, 4, 2, 6),
                PartPose.offsetAndRotation(0F, -1.5F, 5F, 0.1F, 0F, 0F));
        head.addOrReplaceChild("left_cheek",
                CubeListBuilder.create().texOffs(68, 80).addBox(-1F, -5F, -2.5F, 2, 6, 5),
                PartPose.offset(5.5F, -2F, 1F));
        head.addOrReplaceChild("right_cheek",
                CubeListBuilder.create().texOffs(68, 92).addBox(-1F, -5F, -2.5F, 2, 6, 5),
                PartPose.offset(-5.5F, -2F, 1F));
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(83, 80).addBox(-1.5F, -11F, -1F, 3, 11, 2),
                PartPose.offsetAndRotation(3F, -8F, 1F, 0F, 0F, -0.12F));
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(94, 80).addBox(-1.5F, -11F, -1F, 3, 11, 2),
                PartPose.offsetAndRotation(-3F, -8F, 1F, 0F, 0F, 0.12F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int overlay,
                                float red, float green, float blue, float alpha) {
        if (this.stage < 3) {
            // Worn exoskeleton stages (tails 4-8) — just the claw plating on the player's arms
            this.leftClaw.render(poseStack, vertexConsumer, packedLight, overlay, red, green, blue, alpha);
            this.rightClaw.render(poseStack, vertexConsumer, packedLight, overlay, red, green, blue, alpha);
            return;
        }
        // Full Avatar (tail 9): complete standing fox from the ground up
        for (ModelPart part : this.fullBody) {
            part.render(poseStack, vertexConsumer, packedLight, overlay, red, green, blue, alpha);
        }
    }
}
