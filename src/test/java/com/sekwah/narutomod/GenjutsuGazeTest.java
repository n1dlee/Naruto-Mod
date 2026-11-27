package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.SharinganGazePacket;
import com.sekwah.narutomod.util.GenjutsuGaze;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenjutsuGazeTest {
    private static final Vec3 FACING = new Vec3(0, 0, 1);
    private static final Vec3 TARGET = new Vec3(0, 0, 7);

    @Test
    void requiresFacingEachOtherWithinTenBlocks() {
        assertTrue(GenjutsuGaze.eyeContact(Vec3.ZERO, FACING, TARGET, FACING.reverse()));
        assertFalse(GenjutsuGaze.eyeContact(Vec3.ZERO, FACING, TARGET, FACING));
        assertFalse(GenjutsuGaze.eyeContact(Vec3.ZERO, FACING, TARGET.add(0, 0, 4), FACING.reverse()));
        assertFalse(GenjutsuGaze.eyeContact(Vec3.ZERO, FACING, TARGET.add(1.5, 0, 0), FACING.reverse()));
        assertFalse(GenjutsuGaze.eyeContact(Vec3.ZERO, FACING, Vec3.ZERO, FACING.reverse()));
    }

    @Test
    void onlyActiveMatureEyeFullyRejectsIllusion() {
        assertEquals(50, GenjutsuGaze.duration(0));
        assertEquals(30, GenjutsuGaze.duration(1));
        assertEquals(17, GenjutsuGaze.duration(2));
        assertEquals(0, GenjutsuGaze.duration(3));
    }

    @Test
    void accessibilityScaleDoesNotAffectGameplayDuration() {
        assertEquals(0, GenjutsuGaze.distortion(8, 50, 0));
        assertEquals(0.4f, GenjutsuGaze.distortion(8, 50, 0.4), 0.001f);
        assertTrue(GenjutsuGaze.distortion(49, 50, 1) < 0.15f);
        assertEquals(0, GenjutsuGaze.distortion(50, 50, 1));
    }

    @Test
    void publicPacketsDoNotCarryAnyPersonalDuration() {
        var dimension = new ResourceLocation("minecraft", "overworld");
        for (var cue : SharinganGazePacket.Cue.values()) {
            var packet = new SharinganGazePacket(dimension, 11, cue, 17, 867);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                SharinganGazePacket.encode(packet, buffer);
                var decoded = SharinganGazePacket.decode(buffer);
                assertEquals(cue, decoded.cue());
                assertEquals(11, decoded.subject());
                assertEquals(867, decoded.seed());
                assertEquals(cue == SharinganGazePacket.Cue.VICTIM ? 17 : 0, decoded.duration());
                assertEquals(0, buffer.readableBytes());
            } finally {
                buffer.release();
            }
        }
    }
}
