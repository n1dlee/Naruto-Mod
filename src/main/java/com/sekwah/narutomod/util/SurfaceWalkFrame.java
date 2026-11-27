package com.sekwah.narutomod.util;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class SurfaceWalkFrame {
    private SurfaceWalkFrame() {
    }

    public static Quaternionf orientation(Direction surface, float bodyYaw, Vec3 preferredForward) {
        Vec3 out = SurfaceStride.normal(surface.getOpposite());
        Vec3 tangent = SurfaceStride.project(preferredForward, out).normalize();
        Quaternionf frame = surface == Direction.UP ? new Quaternionf().rotateX(Mth.PI)
                : new Quaternionf().rotationTo(0, 1, 0, (float) out.x, (float) out.y, (float) out.z);
        if (tangent.lengthSqr() < 1.0e-6) {
            return frame;
        }
        Vec3 vanilla = Vec3.directionFromRotation(0.0f, bodyYaw);
        Vector3f rotated = frame.transform((float) vanilla.x, 0, (float) vanilla.z, new Vector3f());
        Vec3 facing = new Vec3(rotated.x, rotated.y, rotated.z);
        float turn = (float) Math.atan2(out.dot(facing.cross(tangent)), facing.dot(tangent));
        return frame.premul(new Quaternionf().rotationAxis(turn, (float) out.x, (float) out.y, (float) out.z));
    }

    public static float cameraRoll(Direction surface, float yaw, float strength) {
        if (surface == Direction.DOWN) {
            return 0.0f;
        }
        if (surface == Direction.UP) {
            return 180.0f * strength;
        }
        Vec3 right = Vec3.directionFromRotation(0.0f, yaw + 90.0f);
        return (float) SurfaceStride.normal(surface).dot(right) * 90.0f * strength;
    }
}
