package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.KikaichuPacket;
import com.sekwah.narutomod.util.KikaichuFlight;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KikaichuFlightTest {
    @Test
    void onlyMeasuredChakraLossCanFundTheReturn() {
        assertEquals(0f, KikaichuFlight.credit(25, 25));
        assertEquals(0f, KikaichuFlight.credit(Float.NaN, 4));
        assertEquals(1.16f, KikaichuFlight.credit(2, 0), 0.001f);
        assertEquals(7.54f, KikaichuFlight.credit(100, 0), 0.001f);
    }

    @Test
    void abortedReturnDiscardsTheCargoRatherThanDeliveringOnRetry() {
        var cargo = new KikaichuFlight.Cargo();
        cargo.collect(20, 7);
        assertTrue(cargo.glowing());
        assertEquals(0, cargo.deliver(false));
        assertEquals(0, cargo.deliver(true));
        assertFalse(cargo.glowing());
    }

    @Test
    void oneSuccessfulReturnPaysOnceAndCannotReload() {
        var cargo = new KikaichuFlight.Cargo();
        cargo.collect(6, 2);
        cargo.collect(13, 0);
        assertEquals(2.32f, cargo.deliver(true), 0.001f);
        assertEquals(0, cargo.deliver(true));
    }

    @Test
    void flightIsCappedAndDoesNotOvershootTheSleeve() {
        Vec3 sleeve = new Vec3(0.5, 0.12, 0);
        Vec3 next = KikaichuFlight.advance(Vec3.ZERO, sleeve, true);
        assertEquals(sleeve, next);
        assertTrue(KikaichuFlight.reached(next, sleeve));
        assertEquals(0.7, KikaichuFlight.advance(Vec3.ZERO, new Vec3(8, 0, 0), false).length(), 0.0001);
    }

    @Test
    void wireCarriesOneSwarmWithTheCargoBitAndTwoAnchors() {
        var packet = new KikaichuPacket(new ResourceLocation("minecraft", "overworld"), 8, 10, KikaichuFlight.Stage.RETURN,
                new Vec3(0.25, 61, -4.2), new Vec3(0.17, 61.08, -3.5), true);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            KikaichuPacket.encode(packet, buffer);
            assertEquals(packet, KikaichuPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
