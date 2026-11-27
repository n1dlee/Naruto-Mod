package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.TsukuyomiPacket;
import com.sekwah.narutomod.util.TsukuyomiScene;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TsukuyomiSceneTest {
    @Test
    void ordinarySharinganDoesNotCancelItachisSignatureIllusion() {
        assertEquals(34, TsukuyomiScene.duration(0));
        assertEquals(30, TsukuyomiScene.duration(1));
        assertEquals(25, TsukuyomiScene.duration(2));
        assertEquals(20, TsukuyomiScene.duration(3));
    }

    @Test
    void zeroEffectsScaleDisablesSceneAndHardLimitEndsIt() {
        assertEquals(1f, TsukuyomiScene.sceneAlpha(6, 34, 1));
        assertEquals(0f, TsukuyomiScene.sceneAlpha(6, 34, 0));
        assertEquals(0f, TsukuyomiScene.sceneAlpha(34, 34, 1));
        assertTrue(TsukuyomiScene.sceneAlpha(33, 34, 1) < 0.06f);
    }

    @Test
    void echoesArriveAtSeparateTimesAndOldStopCannotCloseAnotherScene() {
        assertTrue(TsukuyomiScene.echoAmount(0, 0.13f) > 0.1f);
        assertEquals(0f, TsukuyomiScene.echoAmount(2, 0.13f));
        assertTrue(TsukuyomiScene.echoAmount(2, 0.55f) > 0.2f);
        assertFalse(TsukuyomiScene.matches(5, 6));
        assertTrue(TsukuyomiScene.matches(5, 5));
    }

    @Test
    void packetPreservesOnlyTheRecipientsSession() {
        var dimension = new ResourceLocation("minecraft", "the_nether");
        for (int duration : new int[] {34, 0}) {
            var packet = new TsukuyomiPacket(dimension, 18, 104, duration);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                TsukuyomiPacket.encode(packet, buffer);
                assertEquals(packet, TsukuyomiPacket.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally {
                buffer.release();
            }
        }
    }
}
