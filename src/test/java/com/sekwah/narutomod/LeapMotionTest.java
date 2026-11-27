package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.LeapFxPacket;
import com.sekwah.narutomod.util.LeapMotion;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LeapMotionTest {
    @Test
    void forwardInputFollowsVanillaYawAndDodgeIsLower() {
        Vec3 look = new Vec3(-1, 0, 0);
        Vec3 forward = LeapMotion.impulse(2, look, 90, 0, 1);
        Vec3 dodge = LeapMotion.impulse(2, look, 90, 0, -1);
        assertEquals(-3.2, forward.x, 1.0e-6);
        assertEquals(0.0, forward.z, 1.0e-6);
        assertTrue(dodge.x > 0);
        assertTrue(dodge.y < forward.y);
    }

    @Test
    void impulseClampsProgressionTierAndNormalizesDiagonalInput() {
        Vec3 straight = LeapMotion.impulse(4, new Vec3(0, 0, 1), 0, 0, 1);
        Vec3 diagonal = LeapMotion.impulse(99, new Vec3(0, 0, 1), 0, 1, 1);
        assertEquals(straight.horizontalDistance(), diagonal.horizontalDistance(), 1.0e-6);
        assertEquals(straight.y, diagonal.y);
    }

    @Test
    void failedImpulseAndSteppingOffLedgeAreNotTakeoff() {
        assertFalse(LeapMotion.airborne(64.0, 64.0, true));
        assertFalse(LeapMotion.airborne(64.0, 64.04, false));
        assertFalse(LeapMotion.airborne(64.0, 63.1, false));
        assertTrue(LeapMotion.airborne(64.0, 64.16, false));
    }

    @Test
    void softContactAndHardDropHaveDifferentStrengths() {
        assertTrue(LeapMotion.impact(0.25, 0.07) < LeapMotion.impact(4.7, 0.81));
        assertEquals(1, LeapMotion.impact(19, 3));
    }

    @Test
    void groundOrWaterStateAndImpactPositionAreCarriedByPacket() {
        for (var phase : LeapFxPacket.Phase.values()) {
            var packet = new LeapFxPacket(new ResourceLocation("minecraft", "overworld"), 20, phase,
                    new Vec3(17.5, 63.875, -6.25), new Vec3(1, 0, 0), 14, 0.5f);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                LeapFxPacket.encode(packet, buffer);
                assertEquals(packet, LeapFxPacket.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally {
                buffer.release();
            }
        }
    }
}
