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
 * Canon Susanoo progression (set via {@link #setStage(int)}):
 * 1 = a hovering skeletal ribcage wrapped AROUND the player + two skeletal arms — no legs,
 *     no statue, just the bones (Sasuke's first manifestations);
 * 2 = + skull, shoulder plates and a blade in the right hand;
 * 3 = + musculature filling in over the ribs;
 * 4 = Complete Body — the hovering torso rises onto full legs + waist armor + arm shield.
 *
 * The whole stages-1-3 group lives under one "torso" parent whose pivot is shifted up at
 * stage 4 so the torso sits on the legs (ground at local Y=0). Authored in the standard
 * vanilla entity-model convention (negative Y = up); the renderer applies the vanilla flip
 * via scale(-S,-S,S) — see SusanooRenderer. Stage-gated parts are toggled via
 * ModelPart.visible, since children render recursively with their parent.
 */
public class SusanooModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "susanoo"), "main");

    private static final int RIB_PAIRS = 5;
    private static final int MUSCLE_PAIRS = 4;
    private static final float TORSO_LIFT_STAGE4 = -20F;

    private final ModelPart torso;
    private final ModelPart skull;
    private final ModelPart leftShoulder;
    private final ModelPart rightShoulder;
    private final ModelPart blade;
    private final List<ModelPart> muscles = new ArrayList<>();
    private final ModelPart shield;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart waist;

    private int stage = 1;

    public SusanooModel(ModelPart modelPart) {
        super(RenderType::entityTranslucent);
        this.torso = modelPart.getChild("torso");
        this.skull = this.torso.getChild("skull");
        this.leftShoulder = this.torso.getChild("left_shoulder");
        this.rightShoulder = this.torso.getChild("right_shoulder");
        this.blade = this.torso.getChild("blade");
        for (int i = 0; i < MUSCLE_PAIRS; i++) {
            this.muscles.add(this.torso.getChild("muscle_left_" + i));
            this.muscles.add(this.torso.getChild("muscle_right_" + i));
        }
        this.shield = this.torso.getChild("shield");
        this.leftLeg = modelPart.getChild("left_leg");
        this.rightLeg = modelPart.getChild("right_leg");
        this.waist = modelPart.getChild("waist");
    }

    public void setStage(int stage) {
        this.stage = stage;
        // Complete Body: the hovering torso rises up onto the legs
        this.torso.y = stage >= 4 ? TORSO_LIFT_STAGE4 : 0F;
        this.skull.visible = stage >= 2;
        this.leftShoulder.visible = stage >= 2;
        this.rightShoulder.visible = stage >= 2;
        this.blade.visible = stage >= 2;
        for (ModelPart muscle : this.muscles) {
            muscle.visible = stage >= 3;
        }
        this.shield.visible = stage >= 4;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // --- Hovering torso group (stages 1-3 render this alone, wrapped around the player) ---
        PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));

        // Spine: thin bone column just behind the player's back
        torso.addOrReplaceChild("spine",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1F, -26F, -1F, 2, 24, 2),
                PartPose.offset(0F, 0F, 3.5F));

        // Ribs: short bone arcs wrapping around the player's body (centered on the player,
        // not offset in front — the user stands inside the cage)
        for (int i = 0; i < RIB_PAIRS; i++) {
            float height = -6F - i * 4.5F;
            float angleOut = 0.5F + i * 0.05F;
            torso.addOrReplaceChild("rib_left_" + i,
                    CubeListBuilder.create().texOffs(9, 0).addBox(0F, -1F, -1F, 8, 2, 2),
                    PartPose.offsetAndRotation(1.5F, height, 0F, -0.15F, 0F, -angleOut));
            torso.addOrReplaceChild("rib_right_" + i,
                    CubeListBuilder.create().texOffs(9, 5).addBox(-8F, -1F, -1F, 8, 2, 2),
                    PartPose.offsetAndRotation(-1.5F, height, 0F, -0.15F, 0F, angleOut));
        }

        // Skeletal arms with fists — present from stage 1 (canon: the ribcage stage already
        // manifests arms that attack and guard)
        torso.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(0, 30)
                        .addBox(-1.5F, 0F, -1.5F, 3, 14, 3)
                        .texOffs(13, 30).addBox(-2.5F, 14F, -2.5F, 5, 5, 5),
                PartPose.offsetAndRotation(9F, -24F, 0F, 0F, 0F, 0.12F));
        torso.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(0, 45)
                        .addBox(-1.5F, 0F, -1.5F, 3, 14, 3)
                        .texOffs(13, 45).addBox(-2.5F, 14F, -2.5F, 5, 5, 5),
                PartPose.offsetAndRotation(-9F, -24F, 0F, 0F, 0F, -0.12F));

        // Stage 2+: skull crowning the ribcage, shoulder plates, blade gripped low
        torso.addOrReplaceChild("skull",
                CubeListBuilder.create().texOffs(34, 0).addBox(-4F, -8F, -4.5F, 8, 8, 9),
                PartPose.offset(0F, -26F, 0.5F));
        torso.addOrReplaceChild("left_shoulder",
                CubeListBuilder.create().texOffs(34, 20).addBox(-2F, -2F, -4F, 4, 4, 8),
                PartPose.offset(10F, -25F, 0F));
        torso.addOrReplaceChild("right_shoulder",
                CubeListBuilder.create().texOffs(34, 20).addBox(-2F, -2F, -4F, 4, 4, 8),
                PartPose.offset(-10F, -25F, 0F));
        torso.addOrReplaceChild("blade",
                CubeListBuilder.create().texOffs(60, 0).addBox(-0.5F, -1F, 0F, 1, 2, 18),
                PartPose.offsetAndRotation(10.5F, -6F, 1F, -0.15F, 0F, 0F));

        // Stage 3+: musculature plates layered over the ribs
        for (int i = 0; i < MUSCLE_PAIRS; i++) {
            float height = -8F - i * 4.5F;
            torso.addOrReplaceChild("muscle_left_" + i,
                    CubeListBuilder.create().texOffs(60, 25).addBox(0F, -1.5F, -1.5F, 5, 3, 3),
                    PartPose.offsetAndRotation(2.5F, height, 0F, -0.15F, 0F, -0.4F));
            torso.addOrReplaceChild("muscle_right_" + i,
                    CubeListBuilder.create().texOffs(60, 32).addBox(-5F, -1.5F, -1.5F, 5, 3, 3),
                    PartPose.offsetAndRotation(-2.5F, height, 0F, -0.15F, 0F, 0.4F));
        }

        // Stage 4: forearm shield on the left arm side
        torso.addOrReplaceChild("shield",
                CubeListBuilder.create().texOffs(80, 30).addBox(-1F, -12F, -7F, 2, 20, 14),
                PartPose.offset(-12F, -8F, 1F));

        // --- Complete Body (stage 4 only): legs + waist, ground-anchored at Y=0 ---
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(80, 0).addBox(-3.5F, -20F, -3.5F, 7, 20, 7),
                PartPose.offset(4.5F, 0F, 0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(80, 0).addBox(-3.5F, -20F, -3.5F, 7, 20, 7),
                PartPose.offset(-4.5F, 0F, 0F));
        root.addOrReplaceChild("waist",
                CubeListBuilder.create().texOffs(0, 60).addBox(-6F, -6F, -4F, 12, 6, 8),
                PartPose.offset(0F, -20F, 0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int overlay,
                                float red, float green, float blue, float alpha) {
        this.torso.render(poseStack, vertexConsumer, packedLight, overlay, red, green, blue, alpha);
        if (this.stage >= 4) {
            this.leftLeg.render(poseStack, vertexConsumer, packedLight, overlay, red, green, blue, alpha);
            this.rightLeg.render(poseStack, vertexConsumer, packedLight, overlay, red, green, blue, alpha);
            this.waist.render(poseStack, vertexConsumer, packedLight, overlay, red, green, blue, alpha);
        }
    }
}
