package com.sekwah.narutomod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.client.renderer.ProjectileModelPose;
import com.sekwah.narutomod.util.ProjectileAxis;
import com.sekwah.narutomod.util.WaterDragonFlight;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectileAxisTest {
    @Test
    void muzzlePositionAndServerPathStayHorizontalForALevelCast() {
        Vec3 eyes = new Vec3(5, 64.62, -13);
        Vec3 mouth = eyes.add(0, -0.2, 0);
        Vec3 heading = new Vec3(1, 0, 1).normalize();
        Vec3 end = mouth.add(heading.scale(25));
        Vec3 step = WaterDragonFlight.advance(mouth, end, 2.1).subtract(mouth);
        assertEquals(0, step.y, 1.0e-8);
        assertTrue(step.normalize().dot(heading) > 0.99999);
    }

    @Test
    void bakedNegativeZNozzleAndCanonicalPositiveZAxisBothFaceFlight() {
        for (int yaw : new int[]{0, 45, 90, 135, 180, 225, 270, 315}) {
            for (int pitch : new int[]{-89, -70, -30, 0, 30, 70, 89}) {
                Vec3 heading = Vec3.directionFromRotation(pitch, yaw);
                var physical = ProjectileAxis.direction(ProjectileAxis.decode(ProjectileAxis.encode(heading)), Vec3.ZERO);
                var mesh = new PoseStack();
                ProjectileModelPose.align(mesh, physical);
                Vector3f positiveNose = mesh.last().pose().transformDirection(0, 0, 1, new Vector3f());
                assertAxis(physical, positiveNose);

                mesh.mulPose(Axis.YP.rotationDegrees(180));
                Vector3f legacyNose = mesh.last().pose().transformDirection(0, 0, -1, new Vector3f());
                assertAxis(physical, legacyNose);
            }
        }
    }

    @Test
    void launchAxisDoesNotDependOnTheOwnerAfterItIsEncoded() {
        Vec3 fixed = new Vec3(-0.72, 0.22, 0.48).normalize();
        var packet = ProjectileAxis.encode(fixed);
        assertTrue(ProjectileAxis.direction(ProjectileAxis.decode(packet), fixed.reverse()).dot(fixed) > 0.99999);
        assertTrue(ProjectileAxis.direction(Vec3.ZERO, fixed.reverse()).distanceToSqr(fixed.reverse()) < 1.0e-16);
        assertEquals(new Vec3(0, 0, 1), ProjectileAxis.direction(new Vec3(Double.NaN, 0, 0), Vec3.ZERO));
    }

    private static void assertAxis(Vec3 expected, Vector3f direction) {
        Vec3 model = ProjectileAxis.decode(direction).normalize();
        assertTrue(model.dot(expected) >= 0.99999, () -> "Model points " + model + " instead of " + expected);
    }
}
