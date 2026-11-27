package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.CrowFlockPacket;
import com.sekwah.narutomod.util.CrowFlock;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrowFlockTest {
    @Test
    void sevenLanesFanOutSymmetricallyAndOnlyTravelAfterGathering() {
        assertEquals(0, CrowFlock.travel(CrowFlock.GATHER));
        assertEquals(9, CrowFlock.travel(CrowFlock.GATHER + CrowFlock.FLIGHT));
        assertEquals(9, CrowFlock.travel(99));
        assertEquals(0, CrowFlock.heading(0, 3).x, 0.001);
        assertEquals(CrowFlock.heading(0, 0).x, -CrowFlock.heading(0, 6).x, 0.001);
    }

    @Test
    void blockedLaneNeverHitsThroughCoverAndAnOpenLaneCanPass() {
        Vec3 heading = CrowFlock.heading(0, 3);
        AABB target = new AABB(-0.3, -0.4, 4.5, 0.3, 0.7, 5.1);
        assertFalse(CrowFlock.touches(target, Vec3.ZERO, heading, 3, 5, 4));
        assertTrue(CrowFlock.touches(target, Vec3.ZERO, heading, 3, 5, 9));
        assertFalse(CrowFlock.touches(target, Vec3.ZERO, heading, 5.8, 6.7, 9));
    }

    @Test
    void graphicalBudgetDoesNotRemoveAnEntireLane() {
        assertEquals(CrowFlock.LANES, CrowFlock.birdBudget(true, 0));
        assertEquals(CrowFlock.LANES, CrowFlock.birdBudget(false, 22 * 22));
        assertEquals(17, CrowFlock.birdBudget(false, 0));
    }

    @Test
    void clipUpdateKeepsFlightAgeAndPrivateVisionCarriesNoLanes() {
        var dimension = new ResourceLocation("minecraft", "overworld");
        var origin = new Vec3(5, 70.3, -92);
        var publicFlock = new CrowFlockPacket(dimension, 53, 0, origin, 90, 12,
                List.of(0, 345, 441, 890, 910, 910, 202));
        var personal = new CrowFlockPacket(dimension, 53, 65, origin, 90, 0, List.of());
        for (var expected : List.of(publicFlock, personal)) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                CrowFlockPacket.encode(expected, buffer);
                assertEquals(expected, CrowFlockPacket.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally {
                buffer.release();
            }
        }
        assertEquals(3.45, publicFlock.limit(1), 0.001);
    }
}
