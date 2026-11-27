package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.ShadowCloneCloudPacket;
import com.sekwah.narutomod.util.ShadowCloneFormation;
import com.sekwah.narutomod.util.ShadowCloneChakra;
import com.sekwah.narutomod.util.ShadowCloneFormation.Profile;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowCloneFormationTest {
    @Test
    void openArenaHasRoomForEveryCloneWithoutIntersectingTheCasterOrEachOther() {
        Vec3 caster = new Vec3(-42.2, 64, 25.7);
        List<Vec3> sites = ShadowCloneFormation.candidates(caster, 57, Profile.MASS);
        assertTrue(sites.size() > Profile.MASS.count);
        for (int i = 0; i < sites.size(); i++) {
            assertEquals(caster.y, sites.get(i).y);
            assertTrue(caster.distanceTo(sites.get(i)) >= 1.26);
            for (int j = i + 1; j < sites.size(); j++) {
                assertTrue(sites.get(i).distanceTo(sites.get(j)) >= 1.26);
            }
        }
    }

    @Test
    void dividesByActualClonesPlusTheCaster() {
        assertEquals(2500, ShadowCloneChakra.split(10_000, 3));
        assertEquals(5000, ShadowCloneChakra.split(10_000, 1));
        assertEquals(0, ShadowCloneChakra.split(10_000, 0));
        assertEquals(0, ShadowCloneChakra.split(Float.NaN, 4));
    }

    @Test
    void deathsReturnOnlyUnspentSharesOnceAcrossReloadAndExpiry() {
        var ledger = new ShadowCloneChakra();
        UUID owner = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        float share = ShadowCloneChakra.split(10_000, 3);
        for (UUID clone : List.of(first, second, third)) {
            ledger.allocate(clone, owner, share, 600);
        }
        assertEquals(3, ledger.count(owner));
        assertTrue(ledger.spend(first, 500));
        assertTrue(ledger.spend(second, 1000));
        assertTrue(ledger.spend(third, 2000));
        assertFalse(ledger.spend(third, 501));
        ledger.release(first);
        var restored = ShadowCloneChakra.read(ledger.save(new CompoundTag()));
        restored.release(first);
        restored.release(second);
        restored.expire(600);
        assertEquals(3000, restored.claim(owner, 3000));
        assertEquals(1000, restored.claim(owner, 10_000));
        assertEquals(0, restored.claim(owner, 10_000));
        assertEquals(0, restored.count(owner));
    }

    @Test
    void effectsOnlyUsePublishedSitesAndRejectMegaPackets() {
        ResourceLocation dimension = new ResourceLocation("minecraft", "overworld");
        var sites = new ArrayList<>(List.of(new Vec3(4.5, 65, 12.7), new Vec3(3.25, 65.5, -4)));
        var packet = new ShadowCloneCloudPacket(dimension, ShadowCloneCloudPacket.Beat.MASS_FORM, sites);
        sites.clear();
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ShadowCloneCloudPacket.encode(packet, buffer);
            assertEquals(2, packet.sites().size());
            assertEquals(packet, ShadowCloneCloudPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
        assertThrows(IllegalArgumentException.class, () -> new ShadowCloneCloudPacket(dimension,
                ShadowCloneCloudPacket.Beat.MASS_FORM, Collections.nCopies(6, Vec3.ZERO)));
    }
}
