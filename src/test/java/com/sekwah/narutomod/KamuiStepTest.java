package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.KamuiStepPacket;
import com.sekwah.narutomod.util.KamuiStep;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KamuiStepTest {
    private static final Vec3 ORIGIN = new Vec3(4.25, 64.5, -5.5);
    private static final Vec3 EXIT = ORIGIN.add(0, 0, 6);

    @Test
    void testsLandingSpaceRatherThanSweepingThroughTheWall() {
        var visits = new ArrayList<Vec3>();
        Vec3 target = KamuiStep.choose(ORIGIN, 0, point -> {
            visits.add(point);
            return point.equals(EXIT);
        });
        assertEquals(EXIT, target);
        assertEquals(1, visits.size());
        assertEquals(ORIGIN.add(0, 1, 4.5), KamuiStep.choose(ORIGIN, 0, point -> point.equals(ORIGIN.add(0, 1, 4.5))));
        assertNull(KamuiStep.choose(ORIGIN, 0, point -> false));
    }

    @Test
    void releaseChecksOnlyTheCommittedExitAndRejectsExcessiveDrift() {
        var visits = new ArrayList<Vec3>();
        assertFalse(KamuiStep.canRelease(ORIGIN, ORIGIN, EXIT, point -> {
            visits.add(point);
            return false;
        }));
        assertEquals(1, visits.size());
        assertEquals(EXIT, visits.get(0));
        assertTrue(KamuiStep.canRelease(ORIGIN, ORIGIN.add(0.36, 0, 0), EXIT, EXIT::equals));
        assertFalse(KamuiStep.canRelease(ORIGIN, ORIGIN.add(0.95, 0, 0), EXIT, EXIT::equals));
    }

    @Test
    void yawAlwaysProvidesHorizontalFacing() {
        Vec3 cardinal = KamuiStep.facing(90);
        assertEquals(-1.0, cardinal.x, 0.000001);
        assertEquals(0, cardinal.y, 0.000001);
        assertEquals(0, cardinal.z, 0.000001);
        assertEquals(1, KamuiStep.facing(36.5f).length(), 0.000001);
    }

    @Test
    void worldBorderMustContainTheShouldersNotJustIntersectThem() {
        assertTrue(KamuiStep.insideBorder(new AABB(0.1, 0, 0.1, 0.9, 1.8, 0.9), 0, 0, 1, 1));
        assertFalse(KamuiStep.insideBorder(new AABB(0.3, 0, 0.1, 1.1, 1.8, 0.9), 0, 0, 1, 1));
    }

    @Test
    void onlySuccessfulPacketContainsAnExitAnchor() {
        var world = new ResourceLocation("minecraft", "overworld");
        for (var phase : KamuiStepPacket.Phase.values()) {
            var packet = new KamuiStepPacket(world, 501, phase, ORIGIN, EXIT, 180, 1.8f, 912, true);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                KamuiStepPacket.encode(packet, buffer);
                KamuiStepPacket decoded = KamuiStepPacket.decode(buffer);
                assertEquals(phase, decoded.phase());
                assertEquals(ORIGIN, decoded.from());
                assertEquals(packet.hasExit() ? EXIT : ORIGIN, decoded.to());
                assertEquals(packet.leftEye(), decoded.leftEye());
                assertEquals(0, buffer.readableBytes());
            } finally {
                buffer.release();
            }
        }
    }
}
