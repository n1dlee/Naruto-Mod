package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.GunbaiGuardPacket;
import com.sekwah.narutomod.util.GunbaiMotion;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GunbaiMotionTest {
    private static final Vec3 FRONT = new Vec3(0, 0, 1);

    @Test
    void guardInterceptsOnlyAnIncomingSegmentOnTheFanPlane() {
        assertEquals(new Vec3(0.2, 0.1, 0), GunbaiMotion.intercept(Vec3.ZERO, FRONT,
                new Vec3(0.2, 0.1, 0.8), new Vec3(0.2, 0.1, -0.2)));
        assertNull(GunbaiMotion.intercept(Vec3.ZERO, FRONT, new Vec3(0, 0, -1), new Vec3(0, 0, 1)));
        assertNull(GunbaiMotion.intercept(Vec3.ZERO, FRONT, new Vec3(0, 0, 1), new Vec3(0, 0, 0.1)));
    }

    @Test
    void arrowsOutsideTheActualFanCannotReflect() {
        assertNull(GunbaiMotion.intercept(Vec3.ZERO, FRONT, new Vec3(0.8, 0, 1), new Vec3(0.8, 0, -1)));
        assertNull(GunbaiMotion.intercept(Vec3.ZERO, FRONT, new Vec3(0, 0.9, 1), new Vec3(0, 0.9, -1)));
        assertNull(GunbaiMotion.intercept(Vec3.ZERO, FRONT, new Vec3(0.3, 0, 0.05), new Vec3(-0.3, 0, 0.02)));
    }

    @Test
    void bouncePreservesTangentialDirectionButNeverAmplifiesFastArrows() {
        Vec3 rebound = GunbaiMotion.bounce(new Vec3(1, 0, -4), FRONT);
        assertTrue(rebound.x > 0 && rebound.z > 0);
        assertEquals(2.9, rebound.length(), 0.001);
        assertEquals(0.91, GunbaiMotion.bounce(new Vec3(0, 0, -1), FRONT).length(), 0.001);
    }

    @Test
    void onlyTheConfirmedContactAndNormalDriveTheReflectionVfx() {
        var packet = new GunbaiGuardPacket(new ResourceLocation("minecraft", "overworld"), new Vec3(10, 63.15, -7), FRONT);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            GunbaiGuardPacket.encode(packet, buffer);
            assertEquals(packet, GunbaiGuardPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
