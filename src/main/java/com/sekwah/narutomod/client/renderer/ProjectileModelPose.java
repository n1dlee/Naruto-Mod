package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.util.ProjectileAxis;
import net.minecraft.world.phys.Vec3;

public final class ProjectileModelPose {
    private ProjectileModelPose() {
    }

    public static void align(PoseStack poses, Vec3 heading) {
        var angles = ProjectileAxis.angles(heading);
        poses.mulPose(Axis.YP.rotationDegrees(-angles.yaw()));
        poses.mulPose(Axis.XP.rotationDegrees(angles.pitch()));
    }
}
