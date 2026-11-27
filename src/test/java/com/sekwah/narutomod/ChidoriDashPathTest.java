package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.ChidoriDashPacket;
import com.sekwah.narutomod.util.ChidoriDashPath;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChidoriDashPathTest {
    @Test
    void zeroDistanceDoesNotDamageEvenIfTheHandOverlapsTarget() {
        assertNull(ChidoriDashPath.contact(Vec3.ZERO, Vec3.ZERO, targetAt(0, 0)));
    }

    @Test
    void onlyChecksTravelThatVanillaMovementActuallyAllowed() {
        Vec3 wanted = new Vec3(2, 0, 0);
        Vec3 stoppedAtWall = new Vec3(0.65, 0, 0);
        assertTrue(ChidoriDashPath.obstructed(wanted, stoppedAtWall));
        assertNull(ChidoriDashPath.contact(Vec3.ZERO, stoppedAtWall, targetAt(1.6, 0)));
    }

    @Test
    void contactUsesRealNearSideOfTargetRatherThanItsCenter() {
        var contact = ChidoriDashPath.contact(Vec3.ZERO, new Vec3(2, 0, 0), targetAt(1.3, 0));
        assertNotNull(contact);
        assertEquals(1.0, contact.point().x, 1.0e-8);
        assertEquals(0.62, contact.center().x, 1.0e-8);
    }

    @Test
    void sidestepExitsDashingAttack() {
        assertNull(ChidoriDashPath.contact(Vec3.ZERO, new Vec3(2, 0, 0), targetAt(1.2, 1.15)));
    }

    @Test
    void firstContactOccursBeforeTargetBehindIt() {
        Vec3 end = new Vec3(2, 0, 0);
        var nearest = ChidoriDashPath.contact(Vec3.ZERO, end, targetAt(1.2, 0));
        var rear = ChidoriDashPath.contact(Vec3.ZERO, end, targetAt(1.8, 0));
        assertNotNull(nearest);
        assertNotNull(rear);
        assertTrue(nearest.distanceSqr() < rear.distanceSqr());
    }

    @Test
    void verticalAimUsesYawInsteadOfLaunchingUpward() {
        var heading = ChidoriDashPath.groundHeading(new Vec3(0, 1, 0), 90);
        assertEquals(-1, heading.x, 1.0e-5);
        assertEquals(0, heading.y, 1.0e-5);
        assertEquals(1, heading.length(), 1.0e-5);
    }

    @Test
    void codecKeepsDashPhasesAndAuthoritativeSegment() {
        for (var phase : ChidoriDashPacket.Phase.values()) {
            var packet = new ChidoriDashPacket(new ResourceLocation("minecraft", "overworld"), 67, phase,
                    new Vec3(37.25, 80.75, 283.5), new Vec3(38.4375, 80.75, 283.5), new Vec3(1, 0, 0));
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                ChidoriDashPacket.encode(packet, buffer);
                assertEquals(packet, ChidoriDashPacket.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally {
                buffer.release();
            }
        }
    }

    private static AABB targetAt(double x, double z) {
        return new AABB(x - 0.3, -0.7, z - 0.3, x + 0.3, 0.9, z + 0.3);
    }
}
