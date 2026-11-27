package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.NinkenCuePacket;
import com.sekwah.narutomod.util.NinkenRegistry;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NinkenRegistryTest {
    private static final UUID OWNER = UUID.fromString("f5f49aad-43d5-4d62-8fbc-a11c1900aeb2");
    private static final UUID DOG = UUID.fromString("2129d624-94d8-428b-b55d-398637bb8ccf");
    private static final UUID REPLACEMENT = UUID.fromString("787a1baf-cd98-40cd-bc20-b0a600b372bc");

    @Test
    void unloadedPartnerStillOwnsTheSlotUntilExpiryAndRestartDoesNotResetAge() {
        var registry = new NinkenRegistry();
        assertTrue(registry.claim(OWNER, DOG, 1000));
        var saved = NinkenRegistry.read(registry.save(new CompoundTag()));
        assertFalse(saved.claim(OWNER, REPLACEMENT, 3399));
        assertEquals(DOG, saved.active(OWNER, 3399));
        assertNull(saved.active(OWNER, 3400));
        assertTrue(saved.claim(OWNER, REPLACEMENT, 3400));
    }

    @Test
    void anOldEntityOrOtherOwnerCannotRemoveTheReplacement() {
        var registry = new NinkenRegistry();
        assertTrue(registry.claim(OWNER, DOG, 0));
        registry.release(OWNER, DOG);
        assertTrue(registry.claim(OWNER, REPLACEMENT, 40));
        registry.release(OWNER, DOG);
        registry.release(DOG, REPLACEMENT);
        assertEquals(REPLACEMENT, registry.active(OWNER, 41));
    }

    @Test
    void phaseAndActualGroundAnchorAreKeptOnTheWire() {
        var packet = new NinkenCuePacket(new ResourceLocation("minecraft", "the_nether"), 291, NinkenCuePacket.Stage.ARRIVE,
                new Vec3(10.1, 72.5, -9.8), new Vec3(-1, 0, 0));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            NinkenCuePacket.encode(packet, buffer);
            assertEquals(packet, NinkenCuePacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
