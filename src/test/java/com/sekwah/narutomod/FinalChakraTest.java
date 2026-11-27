package com.sekwah.narutomod;

import com.sekwah.narutomod.abilities.jutsus.KuramaChakraModeAbility;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.entity.TailedBeastVariant;
import com.sekwah.narutomod.events.YasakaVolleyEvents;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket;
import com.sekwah.narutomod.util.BijudamaProfile;
import com.sekwah.narutomod.util.FinalChakraGeometry;
import com.sekwah.narutomod.util.MagatamaCluster;
import com.sekwah.narutomod.util.SpectralSword;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinalChakraTest {
    @Test
    void swordDamageIsRestrictedToTheSharedCutWindowAndContactLine() {
        assertFalse(SpectralSword.active(SpectralSword.CUT_START - 1));
        assertTrue(SpectralSword.active(SpectralSword.CUT_START));
        assertTrue(SpectralSword.active(SpectralSword.CUT_END));
        assertFalse(SpectralSword.active(SpectralSword.CUT_END + 1));
        double time = (SpectralSword.CUT_START + SpectralSword.CUT_END) / 2.0;
        var blade = SpectralSword.sample(Vec3.ZERO, 0, 4, time);
        var onBlade = blade.guard().lerp(blade.tip(), 0.65);
        assertNotNull(blade.contact(new AABB(onBlade, onBlade).inflate(0.28), 0.08));
        var behind = new Vec3(0, 3, -9);
        assertNull(blade.contact(new AABB(behind, behind).inflate(0.3), 0.6));
    }

    @Test
    void loweringOrReloadingTheShellDoesNotRefillItAndLockoutIsPersisted() {
        var original = new NinjaData(false);
        original.setSusanooActive(true);
        original.setSusanooDurability(110);
        original.setSusanooActive(false);
        original.setSusanooActive(true);
        assertEquals(110f, original.getSusanooDurability());
        original.setSusanooBrokenTicks(315);
        var restored = new NinjaData(false);
        restored.deserializeNBT(original.serializeNBT());
        assertEquals(315, restored.getSusanooBrokenTicks());
        restored.setSusanooActive(true);
        assertEquals(110f, restored.getSusanooDurability());
    }

    @Test
    void magatamaKeepsTheSameAimOnReleaseAtSteepPitch() {
        Vec3 aim = new Vec3(0.012, 1, 0);
        Vec3 right = new Vec3(0, 0, 1);
        assertEquals(0, aim.normalize().distanceTo(YasakaVolleyEvents.direction(aim, right, 1)), 1e-12);
        assertTrue(YasakaVolleyEvents.direction(aim, right, 0).z < 0);
        assertTrue(YasakaVolleyEvents.direction(aim, right, 2).z > 0);
        assertEquals(1, YasakaVolleyEvents.direction(aim, right, 2).length(), 1e-6);
    }

    @Test
    void magatamaLedgerCapsRepeatedSplashAcrossSaveAndReload() {
        UUID volley = new UUID(9, 4);
        UUID target = new UUID(3, 7);
        var ledger = new MagatamaCluster();
        assertTrue(ledger.claim(volley, target, 299, 430));
        assertFalse(ledger.claim(volley, target, 300, 430));
        var reloaded = MagatamaCluster.read(ledger.save(new CompoundTag()));
        assertFalse(reloaded.claim(volley, target, 308, 430));
        assertTrue(reloaded.claim(new UUID(9, 5), target, 308, 440));
        assertFalse(reloaded.claim(volley, target, 453, 430));
    }

    @Test
    void swappingBackToRedCloakRemovesTheGoldToggle() {
        var player = new NinjaData(false);
        player.setKuramaCloakActive(true);
        player.setKcmActive(true);
        player.getToggleAbilityData().addAbility(KuramaChakraModeAbility.ID);
        assertFalse(player.isKuramaCloakActive());
        assertTrue(player.isKcmActive());
        player.setKuramaCloakActive(true);
        assertTrue(player.isKuramaCloakActive());
        assertFalse(player.isKcmActive());
        assertFalse(player.getToggleAbilityData().getAbilitiesHashSet().contains(KuramaChakraModeAbility.ID));
    }

    @Test
    void bijudamaUsesSphericalBoundsAndKeepsBeastScalingBelowTheDamageCeiling() {
        var corner = new AABB(6, 0, 6, 6.6, 1, 6.6);
        assertTrue(FinalChakraGeometry.sphericalDistance(Vec3.ZERO, corner) > 7);
        assertEquals(0f, BijudamaProfile.damage(8, 7, FinalChakraGeometry.sphericalDistance(Vec3.ZERO, corner)));
        assertTrue(BijudamaProfile.damage(8, 7, 0) > BijudamaProfile.damage(1, 7, 0));
        assertTrue(BijudamaProfile.damage(8, 7, 0) < 76);
        assertTrue(BijudamaProfile.damage(8, 7, 7) < BijudamaProfile.damage(8, 7, 0));
        assertEquals(13f, BijudamaProfile.limitRadius(400));
    }

    @Test
    void mouthProfilesAndImpactPacketsPreserveAuthoritativeCoordinates() {
        var low = BijudamaProfile.mouth(TailedBeastVariant.ISOBU, Vec3.ZERO, 6, 7, 90);
        var tall = BijudamaProfile.mouth(TailedBeastVariant.GYUKI, Vec3.ZERO, 6, 7, 90);
        assertNotEquals(low.y, tall.y);
        assertTrue(low.x < 0);
        assertTrue(BijudamaProfile.core(7, 0) < BijudamaProfile.core(7, 1));
        var expected = new FinalChakraPacket(new ResourceLocation("minecraft", "overworld"), 52,
                FinalChakraPacket.Phase.BIJU_HIT, new FinalChakraPacket.Shape(low, new Vec3(-1, 0, 0), 8, 0xB77CE0));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            FinalChakraPacket.encode(expected, buffer);
            assertEquals(expected, FinalChakraPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
