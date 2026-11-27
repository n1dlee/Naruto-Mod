package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.GatsugaPacket;
import com.sekwah.narutomod.util.GatsugaMotion;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatsugaMotionTest {
    @Test
    void bothLanesBendSeparatelyAndConvergeNearTheirOwnEndpoints() {
        Vec3 side = new Vec3(1, 0, 0);
        var human = new GatsugaMotion.Lane(new Vec3(0, 0, 0), new Vec3(0.42, 0, 9), side, false);
        var wolf = new GatsugaMotion.Lane(new Vec3(-1, 0, 0), new Vec3(-0.42, 0, 9), side, true);
        var midway = GatsugaMotion.path(human, 0.5f).distanceTo(GatsugaMotion.path(wolf, 0.5f));
        var finish = GatsugaMotion.path(human, 1).distanceTo(GatsugaMotion.path(wolf, 1));
        assertTrue(midway > finish + 1);
        assertEquals(9, GatsugaMotion.path(wolf, 1).z, 1.0e-8);
    }

    @Test
    void blockedTrackNeverReceivesTheVelocityFromItsTwin() {
        Vec3 longRequest = new Vec3(3, 1, 4);
        Vec3 want = GatsugaMotion.step(Vec3.ZERO, longRequest);
        assertEquals(1.05, want.length(), 1.0e-8);
        assertEquals(0, want.y);
        assertTrue(GatsugaMotion.obstructed(want, Vec3.ZERO));
        assertFalse(GatsugaMotion.obstructed(want, want));
    }

    @Test
    void bothTracksShareOneHitBudget() {
        var ledger = new GatsugaMotion.HitLedger();
        for (int i = 0; i < 16; i++) {
            assertTrue(ledger.mark(new UUID(0, i)));
            assertFalse(ledger.mark(new UUID(0, i)));
        }
        assertFalse(ledger.mark(new UUID(0, 17)));
    }

    @Test
    void blockedWolfPhaseKeepsOnlyItsOwnConfirmedEndpoint() {
        var packet = new GatsugaPacket(new ResourceLocation("minecraft", "overworld"), 8, 12, GatsugaMotion.Stage.BLOCK,
                true, new Vec3(-0.2, 71, 8), new Vec3(-1, 71, 9));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            GatsugaPacket.encode(packet, buffer);
            assertEquals(packet, GatsugaPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
