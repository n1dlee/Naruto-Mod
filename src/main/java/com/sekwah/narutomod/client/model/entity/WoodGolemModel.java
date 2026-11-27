package com.sekwah.narutomod.client.model.entity;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.entity.WoodGolemEntity;
import com.sekwah.narutomod.util.WoodGolemMotion;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class WoodGolemModel extends HumanoidModel<WoodGolemEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(NarutoMod.MOD_ID, "wood_golem"), "main");
    public static final float SCALE = 2.25f;
    private final ModelPart collar;

    public WoodGolemModel(ModelPart root) {
        super(root);
        this.collar = this.body.getChild("collar");
    }

    public static LayerDefinition createLayer() {
        var mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
        PartDefinition root = mesh.getRoot();
        var body = root.getChild("body");
        body.addOrReplaceChild("collar", CubeListBuilder.create().texOffs(16, 16)
                .addBox(-4.8f, -1.8f, -2.4f, 9.6f, 3.7f, 4.8f, new CubeDeformation(0.13f)),
                PartPose.offset(0, 1, 0.3f));
        body.addOrReplaceChild("back_roots", CubeListBuilder.create().texOffs(16, 16)
                .addBox(-2.6f, -3.8f, -0.5f, 2.1f, 11f, 2.3f)
                .texOffs(16, 16).addBox(0.8f, -1.6f, -0.7f, 2.1f, 9.4f, 2.4f),
                PartPose.offsetAndRotation(0, 2, 2, 0.23f, 0, -0.09f));
        root.getChild("head").addOrReplaceChild("wood_brow", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.9f, -6.1f, -4.6f, 9.8f, 2.5f, 2.8f)
                .texOffs(0, 0).addBox(-5.0f, -5.4f, -2.1f, 1.9f, 5.1f, 3.6f)
                .texOffs(0, 0).addBox(3.1f, -5.2f, -2.1f, 1.9f, 4.6f, 3.8f), PartPose.ZERO);
        gauntlet(root.getChild("right_arm"), true);
        gauntlet(root.getChild("left_arm"), false);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void gauntlet(PartDefinition arm, boolean right) {
        float side = right ? -1 : 1;
        int u = right ? 40 : 32;
        int v = right ? 16 : 48;
        arm.addOrReplaceChild("shoulder_cap", CubeListBuilder.create().texOffs(u, v)
                .addBox(-2.6f, -2.5f, -2.3f, 5.2f, 4.3f, 4.6f),
                PartPose.offsetAndRotation(side * 1.1f, 0, 0, 0.07f, 0.12f * side, -0.14f * side));
        arm.addOrReplaceChild("knuckles", CubeListBuilder.create().texOffs(u, v)
                .addBox(-2.1f, -1.8f, -2.9f, 4.2f, 4.0f, 4.8f)
                .texOffs(u, v).addBox(side * 0.7f - 0.9f, 0.1f, -3.4f, 1.8f, 2.5f, 2.5f),
                PartPose.offset(side * 0.4f, 8.9f, 0));
    }

    @Override
    public void setupAnim(WoodGolemEntity golem, float stride, float speed, float age, float yaw, float pitch) {
        this.rightArm.resetPose();
        super.setupAnim(golem, stride * 0.83f, speed * 0.67f, age, yaw * 0.7f, pitch * 0.66f);
        float partial = Mth.clamp(age - golem.tickCount, 0, 1);
        float birth = (float) (1 - WoodGolemMotion.ease(golem.growth(partial)));
        float fall = (float) WoodGolemMotion.ease(golem.dismantleProgress(partial));
        this.rightArm.yScale = 1;
        this.collar.xRot = 0.06f * Mth.sin(stride * 0.83f) * speed;
        this.collar.yRot = 0;
        this.head.xRot += birth * 0.31f + fall * 0.55f;
        this.body.xRot = birth * 0.21f + fall * 0.29f;
        this.leftArm.xRot += birth * 0.49f + fall * 0.48f;
        this.rightArm.xRot += birth * 0.58f + fall * 0.48f;
        int strike = golem.swipeAge();
        if (strike >= 0) {
            strikePose(strike + partial);
        }
    }

    private void strikePose(double age) {
        Vec3 arm = WoodGolemMotion.arm(age);
        double reach = arm.length();
        double x = -arm.x;
        double y = -arm.y;
        this.rightArm.setPos(-0.74f * 16 / SCALE, 24 - 3.31f * 16 / SCALE, 0);
        this.rightArm.xRot = (float) Math.asin(Mth.clamp(-arm.z / reach, -1, 1));
        this.rightArm.zRot = (float) Math.atan2(-x, y);
        this.rightArm.yRot = 0;
        this.rightArm.yScale = (float) (reach * 16 / (SCALE * 9.6));
        this.leftArm.xRot = (float) (-0.6 * WoodGolemMotion.ease(age / WoodGolemMotion.WINDUP));
        this.collar.yRot = 0.12f * this.rightArm.zRot;
    }
}
