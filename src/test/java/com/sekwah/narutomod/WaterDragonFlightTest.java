package com.sekwah.narutomod;

import com.sekwah.narutomod.entity.jutsuprojectile.DragonTrail;
import com.sekwah.narutomod.network.s2c.WaterDragonImpactPacket;
import com.sekwah.narutomod.util.WaterDragonFlight;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WaterDragonFlightTest {
    @Test
    void flightClampsShortLastStepAndKeepsAim() {
        Vec3 end = new Vec3(3, 4, 0);
        Vec3 middle = WaterDragonFlight.advance(Vec3.ZERO, end, 2.5);
        assertEquals(new Vec3(1.5, 2, 0), middle);
        assertEquals(end, WaterDragonFlight.advance(middle, end, 3));
        assertEquals(end, WaterDragonFlight.advance(end, end, 2.4));
    }

    @Test
    void headWidthCatchesCornerOfThinBlockAndReportsSurfacePoint() {
        AABB pane = new AABB(1.25, 0.2, 0.4, 1.375, 1, 1);
        var contact = WaterDragonFlight.sweep(Vec3.ZERO, new Vec3(2.4, 0, 0), pane, 0.5);
        assertNotNull(contact);
        assertEquals(new Vec3(0.75, 0, 0), contact.center());
        assertEquals(new Vec3(1.25, 0.2, 0.4), contact.point());
        assertEquals(Direction.WEST, contact.normal());
        assertNull(WaterDragonFlight.sweep(Vec3.ZERO, new Vec3(2.4, 0, 0), pane, 0.1));
    }

    @Test
    void simultaneousContactDoesNotDisplaceBlockAndWallOccludesLaterContact() {
        Vec3 end = new Vec3(2.4, 0, 0);
        var wall = WaterDragonFlight.sweep(Vec3.ZERO, end, new AABB(1, -1, -1, 1.1, 1, 1), 0.4);
        var behind = WaterDragonFlight.sweep(Vec3.ZERO, end, new AABB(1.3, -1, -1, 2.0, 1, 1), 0.4);
        var ahead = WaterDragonFlight.sweep(Vec3.ZERO, end, new AABB(0.8, -1, -1, 1.0, 1, 1), 0.4);
        assertNotNull(wall);
        assertNotNull(behind);
        assertNotNull(ahead);
        assertFalse(wall.precedes(wall));
        assertFalse(behind.precedes(wall));
        assertTrue(ahead.precedes(wall));
    }

    @Test
    void headStopsAtEndOfStepAndDoesNotHitTouchingWallWhenMovingAway() {
        AABB box = new AABB(1, -1, -1, 2, 1, 1);
        Vec3 contact = new Vec3(0.5, 0, 0);
        var hit = WaterDragonFlight.sweep(Vec3.ZERO, contact, box, 0.5);
        assertNotNull(hit);
        assertEquals(1.0, hit.fraction());
        assertNull(WaterDragonFlight.sweep(contact, Vec3.ZERO, box, 0.5));
    }

    @Test
    void centerOfOneBlockPassageHasClearanceForHead() {
        Vec3 start = new Vec3(0, 0, 0.5);
        Vec3 end = new Vec3(2.4, 0, 0.5);
        assertNull(WaterDragonFlight.sweep(start, end, new AABB(0, -1, -1, 3, 1, 0), 0.45));
        assertNull(WaterDragonFlight.sweep(start, end, new AABB(0, -1, 1, 3, 1, 2), 0.45));
    }

    @Test
    void sphereUsesClosestHitboxPointRatherThanFeetOrBroadPhaseCorner() {
        Vec3 near = WaterDragonFlight.nearestPoint(new AABB(2, -1, -1, 4, 3, 1), Vec3.ZERO);
        Vec3 far = WaterDragonFlight.nearestPoint(new AABB(2.5, 2.5, 2.5, 3, 3, 3), Vec3.ZERO);
        assertEquals(4.0, near.lengthSqr());
        assertTrue(far.lengthSqr() > 9.0);
    }

    @Test
    void trailFollowsActualCornerAndDoesNotInventHistoryForLateObserver() {
        DragonTrail trail = new DragonTrail();
        Vec3 head = new Vec3(2, 0, 2);
        assertEquals(List.of(head), trail.sample(head, 4, 5));
        trail.push(Vec3.ZERO);
        trail.push(new Vec3(2, 0, 0));
        assertEquals(List.of(head, new Vec3(2, 0, 1), new Vec3(2, 0, 0), new Vec3(1, 0, 0), Vec3.ZERO),
                trail.sample(head, 4, 5));
    }

    @Test
    void trailDropsOldPointsAndResetRemovesStaleCullingBounds() {
        DragonTrail trail = new DragonTrail();
        for (int i = 0; i < 20; i++) {
            trail.push(new Vec3(i, 0, 0));
        }
        AABB head = new AABB(19, 0, 0, 20, 1, 1);
        assertEquals(7.5, trail.bounds(head, 0.5).minX);
        trail.clear();
        assertEquals(head, trail.bounds(head, 0.5));
    }

    @Test
    void impactPacketPreservesExactPositionAndContactKind() {
        var packet = new WaterDragonImpactPacket(new ResourceLocation("minecraft", "the_nether"),
                new Vec3(13.1875, 81.25, -77.625), new Vec3(0, 0, -1), WaterDragonImpactPacket.Kind.BLOCK, 4206);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            WaterDragonImpactPacket.encode(packet, buffer);
            assertEquals(packet, WaterDragonImpactPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
