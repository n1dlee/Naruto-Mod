package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.AdamantineChainPacket;
import com.sekwah.narutomod.util.AdamantineChain;
import com.sekwah.narutomod.util.AdamantineChain.Phase;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdamantineChainTest {
    @Test
    void targetingRejectsSideBehindAndBeyondRangeButAllowsAimedElevation() {
        Vec3 aim = new Vec3(0, 0.4, 1).normalize();
        assertTrue(AdamantineChain.inCone(Vec3.ZERO, new Vec3(0, 3, 8), aim));
        assertFalse(AdamantineChain.inCone(Vec3.ZERO, new Vec3(0, 3, -8), aim));
        assertFalse(AdamantineChain.inCone(Vec3.ZERO, new Vec3(6, 0, 2), aim));
        assertFalse(AdamantineChain.inCone(Vec3.ZERO, aim.scale(10.01), aim));
    }

    @Test
    void noFullReachBeforeTheTelegraphAndTensionUsesTheConfirmedEndpoints() {
        Vec3 from = new Vec3(0, 1.5, 0);
        Vec3 to = new Vec3(2, 1.5, 7);
        assertEquals(0f, AdamantineChain.extension(1));
        assertTrue(AdamantineChain.extension(AdamantineChain.WINDUP - 1) < 1);
        assertEquals(1f, AdamantineChain.extension(AdamantineChain.WINDUP));
        assertEquals(from, AdamantineChain.point(from, to, 0, false));
        assertEquals(to, AdamantineChain.point(from, to, 1, true));
        assertTrue(AdamantineChain.point(from, to, 0.5, false).y > from.y);
        assertTrue(AdamantineChain.point(from, to, 0.5, true).y < from.y);
    }

    @Test
    void pvpSealCannotInheritTheTailedBeastDuration() {
        assertEquals(22, AdamantineChain.duration(true, true));
        assertEquals(46, AdamantineChain.duration(false, false));
        assertEquals(62, AdamantineChain.duration(false, true));
    }

    @Test
    void clippedTipAndRetractSurviveTheWireFormat() {
        var packet = new AdamantineChainPacket(new ResourceLocation("minecraft", "the_nether"), 719, Phase.RETRACT, 3,
                new Vec3(-100.25, 72.4, 15.2), new Vec3(-93.4, 69, 18), 0.47f, 0.625f);
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            AdamantineChainPacket.encode(packet, buf);
            assertEquals(packet, AdamantineChainPacket.decode(buf));
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }
}
