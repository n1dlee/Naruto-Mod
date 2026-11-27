package com.sekwah.narutomod;

import com.sekwah.narutomod.util.SurfaceStride;
import com.sekwah.narutomod.util.SurfaceWalkFrame;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SurfaceWalkTest {
    @Test
    void pressureIntoWallDoesNotMoveLegsOrPlayFootstep() {
        SurfaceStride stride = new SurfaceStride();
        stride.advance(Vec3.ZERO, Direction.NORTH);
        for (int tick = 1; tick <= 20; tick++) {
            assertEquals(0, stride.advance(new Vec3(0, 0, -0.034 * tick), Direction.NORTH));
        }
        assertEquals(0, stride.sample(1).pace());
        assertEquals(0, stride.sample(1).phase());
    }

    @Test
    void sideStrafeAdvancesByDistanceAndAlternatesFeet() {
        SurfaceStride stride = new SurfaceStride();
        stride.advance(Vec3.ZERO, Direction.NORTH);
        assertEquals(0, stride.advance(new Vec3(0.3, 0, -0.026), Direction.NORTH));
        assertEquals(-1, stride.advance(new Vec3(0.47, 0, -0.028), Direction.NORTH));
        assertEquals(1, stride.advance(new Vec3(0.93, 0, -0.031), Direction.NORTH));
        assertEquals(0, new Vec3(1, 0, 0).distanceTo(stride.forward()), 1.0e-9);
    }

    @Test
    void stopHoldsPhaseAndCoastsToNeutral() {
        SurfaceStride stride = new SurfaceStride();
        stride.advance(Vec3.ZERO, Direction.WEST);
        Vec3 stopped = new Vec3(0, 0.24, 0);
        stride.advance(stopped, Direction.WEST);
        float phase = stride.sample(1).phase();
        for (int tick = 0; tick < 5; tick++) {
            assertEquals(0, stride.advance(stopped, Direction.WEST));
        }
        assertEquals(phase, stride.sample(1).phase());
        assertEquals(0, stride.sample(1).pace());
    }

    @Test
    void changingFaceOrTeleportingDoesNotEmitPhantomStep() {
        SurfaceStride stride = new SurfaceStride();
        stride.advance(Vec3.ZERO, Direction.NORTH);
        stride.advance(new Vec3(0, 0.45, 0), Direction.NORTH);
        assertEquals(0, stride.advance(new Vec3(0, 0.66, 0), Direction.UP));
        assertEquals(0, stride.advance(new Vec3(15, 0.66, 0), Direction.UP));
        assertEquals(0, stride.sample(1).phase());
    }

    @Test
    void ceilingAndWaterDiscardVerticalJitter() {
        for (Direction surface : new Direction[] {Direction.UP, Direction.DOWN}) {
            SurfaceStride stride = new SurfaceStride();
            stride.advance(Vec3.ZERO, surface);
            assertEquals(0, stride.advance(new Vec3(0, 0.6, 0), surface));
            assertEquals(-1, stride.advance(new Vec3(0, 0.6, 0.48), surface));
        }
    }

    @Test
    void surfaceFrameAlignsUpAndVanillaForwardOnAllSixFaces() {
        for (Direction face : Direction.values()) {
            Vec3 up = SurfaceStride.normal(face.getOpposite());
            Vec3 forward = face.getAxis().isVertical() ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
            var frame = SurfaceWalkFrame.orientation(face, 0, forward);
            assertVector(up, frame.transform(new Vector3f(0, 1, 0)));
            assertVector(forward, frame.transform(new Vector3f(0, 0, 1)));
        }
    }

    @Test
    void facingStraightIntoWallKeepsFiniteRotation() {
        for (Direction face : Direction.values()) {
            var frame = SurfaceWalkFrame.orientation(face, 35, SurfaceStride.normal(face));
            assertTrue(frame.isFinite());
            assertVector(SurfaceStride.normal(face.getOpposite()), frame.transform(new Vector3f(0, 1, 0)));
        }
    }

    @Test
    void accessibleCameraSettingDisablesWallAndCeilingBank() {
        for (Direction face : Direction.values()) {
            assertEquals(0f, SurfaceWalkFrame.cameraRoll(face, 30, 0), 1e-6f);
        }
        assertEquals(28.8f, SurfaceWalkFrame.cameraRoll(Direction.UP, 100, 0.16f), 1e-4f);
        assertEquals(SurfaceWalkFrame.cameraRoll(Direction.EAST, 0, 1),
                -SurfaceWalkFrame.cameraRoll(Direction.WEST, 0, 1), 1e-4f);
    }

    private static void assertVector(Vec3 expected, Vector3f actual) {
        assertEquals(expected.x, actual.x, 1.0e-5);
        assertEquals(expected.y, actual.y, 1.0e-5);
        assertEquals(expected.z, actual.z, 1.0e-5);
    }
}
