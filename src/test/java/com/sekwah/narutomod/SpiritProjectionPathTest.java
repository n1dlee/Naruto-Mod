package com.sekwah.narutomod;

import com.sekwah.narutomod.network.c2s.SpiritProjectionInputPacket;
import com.sekwah.narutomod.network.s2c.SpiritProjectionPacket;
import com.sekwah.narutomod.util.SpiritProjectionPath;
import com.sekwah.narutomod.util.SpiritProjectionPath.Input;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritProjectionPathTest {
    @Test
    void diagonalAndVerticalInputCannotExceedServerSpeed() {
        var command = new Input(0, (byte) 1, (byte) 1, (byte) 1);
        Vec3 next = SpiritProjectionPath.step(Vec3.ZERO, Vec3.ZERO, command, p -> true);
        assertEquals(0.42, next.length(), 0.001);
        assertTrue(next.x > 0 && next.y > 0 && next.z > 0);
    }

    @Test
    void avatarStopsAtTheTetherAndCannotLoadAChunk() {
        var command = new Input(0, (byte) 0, (byte) 1, (byte) 0);
        Vec3 near = new Vec3(0, 0, 15.8);
        assertEquals(16, SpiritProjectionPath.step(Vec3.ZERO, near, command, p -> true).length(), 0.001);
        assertEquals(near, SpiritProjectionPath.step(Vec3.ZERO, near, command, p -> false));
    }

    @Test
    void bodyMovementAndLifetimeCancelProjectionRatherThanTeleportingTheBody() {
        assertTrue(SpiritProjectionPath.keepsBody(Vec3.ZERO, new Vec3(0, 0, 0.12), 24));
        assertFalse(SpiritProjectionPath.keepsBody(Vec3.ZERO, new Vec3(0, 0, 0.40), 24));
        assertFalse(SpiritProjectionPath.keepsBody(Vec3.ZERO, Vec3.ZERO, SpiritProjectionPath.MAX_AGE));
    }

    @Test
    void inputIsBoundedAndCarriesNoEntityIdOrDestination() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SpiritProjectionInputPacket.encode(new SpiritProjectionInputPacket(14, new Input(Float.NaN,
                    (byte) 90, (byte) -17, (byte) 1)), buffer);
            assertEquals(new SpiritProjectionInputPacket(14, new Input(0, (byte) 1, (byte) -1, (byte) 1)),
                    SpiritProjectionInputPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void packetKeepsBodyAndAvatarSeparateThroughRelease() {
        var event = new SpiritProjectionPacket(new ResourceLocation("minecraft", "overworld"), 6, 17,
                SpiritProjectionPacket.State.RELEASE, new Vec3(3, 65.4, 81), new Vec3(6, 70, 84), 55);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SpiritProjectionPacket.encode(event, buffer);
            assertEquals(event, SpiritProjectionPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
