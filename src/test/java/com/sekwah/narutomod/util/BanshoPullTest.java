package com.sekwah.narutomod.util;

import com.sekwah.narutomod.network.s2c.BanshoTetherPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BanshoPullTest {
    @Test
    void capsSpeedAndCannotOvershootTheStoppingDistance() {
        Vec3 there = new Vec3(16, 0, 0);
        assertEquals(1.08, BanshoPull.velocity(there, Vec3.ZERO, 8, false, 0, true).length(), 1.0e-9);
        assertEquals(0.64, BanshoPull.velocity(there, Vec3.ZERO, 8, true, 0, true).length(), 1.0e-9);
        double close = BanshoPull.STOP + 0.06;
        assertTrue(BanshoPull.velocity(new Vec3(close, 0, 0), Vec3.ZERO, 6, false, 0, true).length() < 0.06);
        assertEquals(Vec3.ZERO, BanshoPull.velocity(new Vec3(0.3, 0, 0), Vec3.ZERO, 6, false, 0, true));
    }

    @Test
    void fullResistanceCancelsPullAndGroundedTargetsAreNotForcedIntoFloor() {
        var high = new Vec3(5, 7, 0);
        assertEquals(0, BanshoPull.velocity(high, Vec3.ZERO, 7, false, 1, false).lengthSqr(), 1.0e-9);
        assertEquals(0, BanshoPull.velocity(high, Vec3.ZERO, 7, false, 0, true).y, 1.0e-9);
        assertTrue(BanshoPull.velocity(high, Vec3.ZERO, 7, false, 0, false).y < 0);
        assertTrue(BanshoPull.velocity(Vec3.ZERO, high, 7, true, 0, true).y <= 0.19);
    }

    @Test
    void packetKeepsTheTwoAnchorsAndTheBrokenPhase() {
        var original = new BanshoTetherPacket(new ResourceLocation("minecraft", "overworld"), 11, BanshoPull.Stage.BREAK,
                new Vec3(0.25, 8, 40.1), new Vec3(9.75, 9, 36));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            BanshoTetherPacket.encode(original, buffer);
            assertEquals(original, BanshoTetherPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
