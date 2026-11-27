package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.MedicalPalmPacket;
import com.sekwah.narutomod.util.MedicalPalm;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicalPalmTest {
    @Test
    void acceptedPulseRequiresARealPositiveHealthDifference() {
        assertFalse(MedicalPalm.accepted(20, 20));
        assertFalse(MedicalPalm.accepted(15, 15));
        assertFalse(MedicalPalm.accepted(15, 14.5f));
        assertFalse(MedicalPalm.accepted(15, Float.POSITIVE_INFINITY));
        assertTrue(MedicalPalm.accepted(19.8f, 20));
    }

    @Test
    void beatsStartAfterHandsGatherAndDoNotFireEveryTick() {
        assertFalse(MedicalPalm.beat(0));
        assertFalse(MedicalPalm.beat(MedicalPalm.GATHER - 1));
        assertTrue(MedicalPalm.beat(MedicalPalm.GATHER));
        assertFalse(MedicalPalm.beat(MedicalPalm.GATHER + 1));
        assertTrue(MedicalPalm.beat(MedicalPalm.GATHER + MedicalPalm.INTERVAL));
    }

    @Test
    void anOccludedOrFarContactNeverReceivesTheCast() {
        assertTrue(MedicalPalm.inFrontOfCover(2, 2.2));
        assertFalse(MedicalPalm.inFrontOfCover(2.4, 2.2));
        assertFalse(MedicalPalm.inFrontOfCover(4.1, 10.0));
    }

    @Test
    void acceptedPulseKeepsHealthGainAndHandednessOnTheWire() {
        var packet = new MedicalPalmPacket(new ResourceLocation("minecraft", "overworld"), 281, MedicalPalm.Phase.HEAL,
                new Vec3(100.5, 71.1, 22.6), new Vec3(103, 71.2, 22.8), 0.2f, false);
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            MedicalPalmPacket.encode(packet, buf);
            assertEquals(packet, MedicalPalmPacket.decode(buf));
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }
}
