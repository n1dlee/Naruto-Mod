package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.NagashiPacket;
import com.sekwah.narutomod.util.NagashiWave;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NagashiWaveTest {
    @Test
    void frontSpreadsAndStopsExactlyAtProfileRadius() {
        assertEquals(0, NagashiWave.radius(0, 3.5));
        assertTrue(NagashiWave.radius(1, 3.5) < NagashiWave.radius(2, 3.5));
        assertEquals(3.5, NagashiWave.radius(5, 3.5));
        assertEquals(5.0, NagashiWave.radius(12, 5.0));
    }

    @Test
    void targetCanTouchTheEdgeButNotTheCornerOfTheBroadPhase() {
        assertTrue(NagashiWave.reaches(Vec3.ZERO, 0, new AABB(3.5, 0, -0.3, 4.1, 1.8, 0.3), 3.5));
        assertFalse(NagashiWave.reaches(Vec3.ZERO, 0, new AABB(3.501, 0, -0.3, 4.1, 1.8, 0.3), 3.5));
        assertFalse(NagashiWave.reaches(Vec3.ZERO, 0, new AABB(2.6, 0, 2.6, 3.3, 1.8, 3.3), 3.5));
    }

    @Test
    void aTargetOnAnotherLevelIsNotInTheGroundWave() {
        assertFalse(NagashiWave.reaches(Vec3.ZERO, 2, new AABB(0.2, 2, 0, 0.8, 3.8, 0.6), 3.5));
        assertFalse(NagashiWave.reaches(Vec3.ZERO, -3, new AABB(0.2, -3, 0, 0.8, -1.2, 0.6), 3.5));
        assertTrue(NagashiWave.reaches(Vec3.ZERO, 0.5, new AABB(0.2, 0.5, 0, 0.8, 2.3, 0.6), 3.5));
    }

    @Test
    void raysAreUnitVectorsInTheGroundPlane() {
        for (int ray = 0; ray < NagashiWave.SPOKES; ray++) {
            Vec3 direction = NagashiWave.direction(ray);
            assertEquals(0, direction.y);
            assertEquals(1, direction.length(), 1.0e-7);
            assertEquals(-1, direction.dot(NagashiWave.direction(ray + NagashiWave.SPOKES / 2)), 1.0e-7);
        }
    }

    @Test
    void packetKeepsCoverLengthsAndAcceptedHitOutcome() {
        for (NagashiPacket.Phase phase : NagashiPacket.Phase.values()) {
            List<Float> spokes = phase == NagashiPacket.Phase.FRONT
                    ? Collections.nCopies(NagashiWave.SPOKES, 1.25f) : List.of();
            NagashiPacket packet = new NagashiPacket(new ResourceLocation("minecraft", "overworld"),
                    42, phase, new Vec3(-27.125, 74.5, 15.75), 3.5f, spokes, 13);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                NagashiPacket.encode(packet, buffer);
                assertEquals(packet, NagashiPacket.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally {
                buffer.release();
            }
        }
    }
}
