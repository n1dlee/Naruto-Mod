package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.ChidoriImpactPacket;
import com.sekwah.narutomod.util.ChidoriStrike;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChidoriThrustTest {

    /** Eyes at head height, looking due north (negative Z is north in Minecraft). */
    private static final Vec3 EYES = new Vec3(0, 1.62, 0);
    private static final Vec3 FIST = new Vec3(0, 1.62, -ChidoriStrike.REACH);

    @Test
    void stopsAtTheFirstFaceOfATarget() {
        Vec3 hit = ChidoriStrike.firstContact(target(0, -2), EYES, FIST).orElseThrow();
        assertEquals(0, hit.x, 1.0E-6);
        assertEquals(1.62, hit.y, 1.0E-6);
        assertEquals(-1.7, hit.z, 1.0E-6);
    }

    @Test
    void somebodyBehindYouIsNeverInFront() {
        assertTrue(ChidoriStrike.firstContact(target(0, 2), EYES, FIST).isEmpty());
    }

    @Test
    void reachStopsAtTheEndOfTheArm() {
        assertTrue(ChidoriStrike.firstContact(target(0, -ChidoriStrike.REACH - 0.5), EYES, FIST).isEmpty());
    }

    @Test
    void somebodyOffToOneSideIsNotHit() {
        assertTrue(ChidoriStrike.firstContact(target(2, -2), EYES, FIST).isEmpty());
    }

    @Test
    void aZeroLengthThrustDoesNotHitAnOverlappingTarget() {
        assertTrue(ChidoriStrike.firstContact(target(0, 0), EYES, EYES).isEmpty());
    }

    @Test
    void wallClippingRejectsTheTargetBehindIt() {
        Vec3 wall = new Vec3(0, 1.62, -1.5);
        assertTrue(ChidoriStrike.firstContact(target(0, -2), EYES, wall).isEmpty());
        assertTrue(ChidoriStrike.firstContact(target(0, -1), EYES, wall).isPresent());
    }

    @Test
    void packetKeepsEachOutcomeAndWorldPosition() {
        for (var kind : ChidoriImpactPacket.Kind.values()) {
            var packet = new ChidoriImpactPacket(new ResourceLocation("minecraft", "overworld"),
                    new Vec3(-2.25, 65, 3.5), new Vec3(0, 1, 0), kind, 73);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                ChidoriImpactPacket.encode(packet, buffer);
                assertEquals(packet, ChidoriImpactPacket.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally {
                buffer.release();
            }
        }
    }

    private static AABB target(double x, double z) {
        return new AABB(x - 0.3, 0.0, z - 0.3, x + 0.3, 1.8, z + 0.3);
    }
}
