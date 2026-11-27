package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.SubstitutionFxPacket;
import com.sekwah.narutomod.util.SubstitutionRoute;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SubstitutionRouteTest {
    private static final ResourceLocation OVERWORLD = new ResourceLocation("minecraft", "overworld");

    @Test
    void rangeUsesFeetPositionInsteadOfElevatedHudMarker() {
        assertTrue(SubstitutionRoute.usesMarker(Vec3.ZERO, new Vec3(70, 2, 0), OVERWORLD, OVERWORLD));
        assertFalse(SubstitutionRoute.usesMarker(Vec3.ZERO, new Vec3(70.001, 2, 0), OVERWORLD, OVERWORLD));
    }

    @Test
    void removedAndCrossDimensionMarkersNeverSupplyATeleport() {
        assertFalse(SubstitutionRoute.usesMarker(Vec3.ZERO, null, OVERWORLD, OVERWORLD));
        assertFalse(SubstitutionRoute.usesMarker(Vec3.ZERO, new Vec3(4, 2, 0), OVERWORLD,
                new ResourceLocation("minecraft", "the_nether")));
    }

    @Test
    void onePacketCarriesBothPreciseEndpointsAndSeed() {
        var packet = new SubstitutionFxPacket(OVERWORLD,
                new Vec3(-19.4, 48.0, 27.625), new Vec3(0.125, 50.0, 27.0), 9307);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SubstitutionFxPacket.encode(packet, buffer);
            assertEquals(packet, SubstitutionFxPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
