package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.ShinranshinPacket;
import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.Shinranshin;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShinranshinTest {
    @Test
    void aimIsThreeDimensionalAndRangeBounded() {
        Vec3 forward = new Vec3(0, 0, 1);
        double cone = Math.cos(Math.toRadians(18));
        assertTrue(JutsuAim.aimed(Vec3.ZERO, forward, new Vec3(0, 0.8, 7), 8, cone));
        assertFalse(JutsuAim.aimed(Vec3.ZERO, forward, new Vec3(0, 3, 7), 8, cone));
        assertFalse(JutsuAim.aimed(Vec3.ZERO, forward, new Vec3(0, 0, -6), 8, cone));
        assertFalse(JutsuAim.aimed(Vec3.ZERO, forward, new Vec3(0, 0, 8.1), 8, cone));
    }

    @Test
    void aWallAndAnExpiredLeaseCannotHoldTheVictim() {
        assertTrue(Shinranshin.maintains(19, 20, 24, true));
        assertFalse(Shinranshin.maintains(19, 20, 24, false));
        assertFalse(Shinranshin.maintains(20, 20, 24, true));
        assertFalse(Shinranshin.maintains(19, 20, 100, true));
    }

    @Test
    void restoreCannotOverwriteAnotherTargetOwnersAssignment() {
        UUID assigned = UUID.fromString("635f1001-a21d-4ca2-b494-b2dd28dd7778");
        UUID unrelated = UUID.fromString("fcee00df-e3dc-4d60-b2dc-975bb753fb9e");
        assertTrue(Shinranshin.ownsTarget(assigned, assigned, false));
        assertTrue(Shinranshin.ownsTarget(null, null, false));
        assertFalse(Shinranshin.ownsTarget(assigned, unrelated, false));
        assertFalse(Shinranshin.ownsTarget(null, null, true));
    }

    @Test
    void wireCarriesBothEndpointsAndTheSequenceIdentity() {
        var packet = new ShinranshinPacket(new ResourceLocation("minecraft", "overworld"), 12, 83, 68,
                Shinranshin.Stage.BREAK, 5, new Vec3(1.2, 67, 0.12), new Vec3(-8, 67.4, 2.01));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ShinranshinPacket.encode(packet, buffer);
            assertEquals(packet, ShinranshinPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
