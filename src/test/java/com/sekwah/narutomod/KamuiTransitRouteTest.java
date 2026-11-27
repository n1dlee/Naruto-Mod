package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.KamuiTransitPacket;
import com.sekwah.narutomod.util.KamuiTransitRoute;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KamuiTransitRouteTest {
    @Test
    void islandsAreStableAndPlacedOnAPerTravelerGrid() {
        var actor = UUID.fromString("dbc001e4-6629-4bf7-8913-6dc090395c72");
        var other = UUID.fromString("66b2461a-3d3a-48f3-9c12-694caab7b4fc");
        var platform = KamuiTransitRoute.island(actor);
        assertEquals(platform, KamuiTransitRoute.island(actor));
        assertNotEquals(platform, KamuiTransitRoute.island(other));
        assertEquals(0, platform.getX() % 96);
        assertEquals(0, platform.getZ() % 96);
        assertEquals(64, platform.getY());
    }

    @Test
    void keepsExactCoordinatesAndOnlyUsesAValidatedAlternative() {
        var feet = new Vec3(108.875, 51.04, -3.875);
        Vec3 alternative = feet.add(0, 2, 0);
        assertEquals(feet, KamuiTransitRoute.landing(feet, p -> true));
        assertEquals(alternative, KamuiTransitRoute.landing(feet, alternative::equals));
        assertNull(KamuiTransitRoute.landing(feet, p -> false));
    }

    @Test
    void breakingTheOriginalFocusCannotChangeEntryIntoAnUnintendedTrip() {
        assertTrue(KamuiTransitRoute.maintains(Vec3.ZERO, new Vec3(0.2, 0.1, 0.35), 1.45));
        assertFalse(KamuiTransitRoute.maintains(Vec3.ZERO, new Vec3(0, 0, 1.46), 1.45));
        assertTrue(KamuiTransitRoute.EXIT_WINDUP < KamuiTransitRoute.WINDUP);
    }

    @Test
    void arrivalAndCancellationEachHaveOneAnchorInTheirOwnWorld() {
        var pocket = new ResourceLocation("narutomod", "kamui");
        var surface = new ResourceLocation("minecraft", "overworld");
        for (var cue : KamuiTransitPacket.Cue.values()) {
            var packet = new KamuiTransitPacket(cue == KamuiTransitPacket.Cue.OPEN ? pocket : surface, 71, cue,
                    new Vec3(25, 65.5, -915), 1.95f, 10);
            var bytes = new FriendlyByteBuf(Unpooled.buffer());
            try {
                KamuiTransitPacket.encode(packet, bytes);
                assertEquals(packet, KamuiTransitPacket.decode(bytes));
                assertEquals(0, bytes.readableBytes());
            } finally {
                bytes.release();
            }
        }
    }
}
