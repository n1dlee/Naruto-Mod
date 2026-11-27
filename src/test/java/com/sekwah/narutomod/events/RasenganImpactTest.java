package com.sekwah.narutomod.events;

import com.sekwah.narutomod.abilities.jutsus.RasenganJutsuAbility;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.network.s2c.RasenganImpactPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RasenganImpactTest {
    @Test
    void heldSphereCanOnlyBeConsumedOnce() {
        var data = new NinjaData(false);
        assertFalse(data.consumeHeldRasengan());

        data.setRasenganHeld(true);
        data.adjustRasenganCharge(20);
        assertTrue(data.consumeHeldRasengan());
        assertFalse(data.isRasenganHeld());
        assertTrue(data.isRasenganConsumed());
        assertEquals(20, data.getRasenganCharge());
        assertFalse(data.consumeHeldRasengan());
    }

    @Test
    void spentToggleRefusesUpkeepUntilItsEndHookRuns() {
        var data = new NinjaData(false);
        var ability = new RasenganJutsuAbility();
        data.setRasenganHeld(true);
        assertTrue(data.consumeHeldRasengan());

        assertFalse(ability.handleCost(null, data, 0));
        ability.handleAbilityEnded(null, data, 0);
        assertFalse(data.isRasenganConsumed());
        assertFalse(data.isRasenganHeld());
    }

    @Test
    void contactUsesTheNearFaceAtAimHeight() {
        var target = new AABB(-0.35, 0, 2, 0.35, 1.8, 2.7);
        var eye = new Vec3(0, 1.62, 0);
        Vec3 result = RasenganImpactEvents.contactOnTarget(target, eye, new Vec3(0, 0, 1), 3.0);
        assertEquals(new Vec3(0, 1.62, 2), result);
    }

    @Test
    void entityPacketRoundTripsWithoutBlockDebris() {
        var packet = new RasenganImpactPacket(new ResourceLocation("minecraft", "overworld"),
                new Vec3(-122.125, 65.5, 100.375), new Vec3(0, 0, -1), 0.75F, -1, 42);
        assertEquals(packet, roundTrip(packet));
        assertFalse(packet.hitsBlock());
    }

    @Test
    void blockPacketCarriesOriginalMaterialAndFace() {
        var packet = new RasenganImpactPacket(new ResourceLocation("minecraft", "the_nether"),
                new Vec3(2.75, 90, -3.125), new Vec3(0, 1, 0), 0.45F, 1024, -18);
        RasenganImpactPacket received = roundTrip(packet);
        assertEquals(packet, received);
        assertTrue(received.hitsBlock());
    }

    private static RasenganImpactPacket roundTrip(RasenganImpactPacket packet) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            RasenganImpactPacket.encode(packet, buffer);
            RasenganImpactPacket decoded = RasenganImpactPacket.decode(buffer);
            assertFalse(buffer.isReadable());
            return decoded;
        } finally {
            buffer.release();
        }
    }
}
