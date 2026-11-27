package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.ShadowGripPacket;
import com.sekwah.narutomod.util.ShadowGrip;
import com.sekwah.narutomod.util.ShadowSurface;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowGripTest {
    @Test
    void liftsFirstAndLeavesVanillaInvulnerabilityTimeBetweenPulses() {
        assertEquals(-1, ShadowGrip.pulseAt(ShadowGrip.LIFT - 1));
        assertEquals(0, ShadowGrip.pulseAt(6));
        assertEquals(-1, ShadowGrip.pulseAt(16));
        assertEquals(1, ShadowGrip.pulseAt(17));
        assertEquals(2, ShadowGrip.pulseAt(28));
        assertEquals(-1, ShadowGrip.pulseAt(39));
        assertTrue(ShadowGrip.duration() > 28);
    }

    @Test
    void evenRecapturingTheSameEntityCannotResumeTheOldGrip() {
        assertTrue(ShadowGrip.matches(15, 15, true));
        assertFalse(ShadowGrip.matches(15, 16, true));
        assertFalse(ShadowGrip.matches(15, 15, false));
    }

    @Test
    void thePvPLinkExpiresBeforeTheThirdPossiblePulse() {
        assertTrue(6 < ShadowSurface.PLAYER_TIME);
        assertTrue(17 < ShadowSurface.PLAYER_TIME);
        assertTrue(28 > ShadowSurface.PLAYER_TIME);
    }

    @Test
    void aRejectedHitAndReleaseAreDistinctClientEvents() {
        var level = new ResourceLocation("minecraft", "overworld");
        for (var step : ShadowGripPacket.Step.values()) {
            var packet = new ShadowGripPacket(level, 439, 112, step, new Vec3(-11, 64.125, 18), 1.8f);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                ShadowGripPacket.encode(packet, buffer);
                assertEquals(packet, ShadowGripPacket.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally {
                buffer.release();
            }
        }
    }
}
