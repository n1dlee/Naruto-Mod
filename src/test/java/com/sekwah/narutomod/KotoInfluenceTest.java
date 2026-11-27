package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.KotoInfluencePacket;
import com.sekwah.narutomod.util.KotoInfluence;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KotoInfluenceTest {
    @Test
    void casterMustAimAtTheEyeWithoutRequiringAReciprocalGaze() {
        var front = new Vec3(0, 0, 1);
        assertTrue(KotoInfluence.aimed(Vec3.ZERO, front, new Vec3(0.5, 0.18, 4)));
        assertFalse(KotoInfluence.aimed(Vec3.ZERO, front, new Vec3(1, 0, 4)));
        assertFalse(KotoInfluence.aimed(Vec3.ZERO, front, new Vec3(0, 0, -3)));
        assertFalse(KotoInfluence.aimed(Vec3.ZERO, front, new Vec3(0, 0, 16.1)));
    }

    @Test
    void leaseExpiresAtWorldTimeAndDoesNotFollowSomeoneAcrossDimensionsOrOutOfRange() {
        assertTrue(KotoInfluence.maintains(199, 200, true, 24 * 24));
        assertFalse(KotoInfluence.maintains(200, 200, true, 1));
        assertFalse(KotoInfluence.maintains(199, 200, false, 1));
        assertFalse(KotoInfluence.maintains(199, 200, true, 577));
    }

    @Test
    void perceptiveEyeShortensGameplayButZeroGraphicsScaleOnlyHidesTheContour() {
        assertEquals(42, KotoInfluence.playerDuration(false));
        assertEquals(18, KotoInfluence.playerDuration(true));
        assertEquals(0, KotoInfluence.afterimage(6, 0));
        assertTrue(KotoInfluence.afterimage(6, 1) > 0.95);
        assertEquals(0, KotoInfluence.afterimage(12, 1));
        assertEquals(0, KotoInfluence.afterimage(-1, 1));
    }

    @Test
    void onlyRecipientVisionCarriesAnAnchorAndDuration() {
        var level = new ResourceLocation("minecraft", "overworld");
        var anchor = new Vec3(1.2, 72.25, -3.1);
        for (var cue : KotoInfluencePacket.Cue.values()) {
            var packet = new KotoInfluencePacket(level, 91, 14, cue, 18, anchor);
            var bytes = new FriendlyByteBuf(Unpooled.buffer());
            try {
                KotoInfluencePacket.encode(packet, bytes);
                var decoded = KotoInfluencePacket.decode(bytes);
                assertEquals(91, decoded.id());
                assertEquals(14, decoded.subject());
                assertEquals(cue, decoded.cue());
                assertEquals(cue == KotoInfluencePacket.Cue.VISION ? anchor : Vec3.ZERO, decoded.anchor());
                assertEquals(cue == KotoInfluencePacket.Cue.VISION ? 18 : 0, decoded.ticks());
                assertEquals(0, bytes.readableBytes());
            } finally {
                bytes.release();
            }
        }
    }
}
