package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.KuramaCloakPacket;
import com.sekwah.narutomod.util.KuramaCloakMotion;
import com.sekwah.narutomod.util.KuramaCloakMotion.Blow;
import com.sekwah.narutomod.util.KuramaCloakMotion.Strike;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KuramaCloakTest {
    @Test
    void giantPawReachesAStandingTargetFromNearTheGround() {
        var paw = new Blow(Strike.PAW, Vec3.ZERO, 0, true);
        assertTrue(paw.covers(new AABB(-0.3, 0, 5.5, 0.3, 1.8, 6.1)));
        assertFalse(paw.covers(new AABB(-0.3, 12, 5.5, 0.3, 13.8, 6.1)));
        assertFalse(paw.covers(new AABB(-0.3, 0, -6, 0.3, 1.8, -5)));
    }

    @Test
    void tailCoversASeparateSectorAndHasItsOwnImpactBeat() {
        var paw = new Blow(Strike.PAW, Vec3.ZERO, 0, true);
        var tail = new Blow(Strike.TAIL, Vec3.ZERO, 0, true);
        var far = new AABB(-14.3, 0, 15.7, -13.7, 1.8, 16.3);
        assertFalse(paw.covers(far));
        assertTrue(tail.covers(far));
        assertTrue(Strike.PAW.windup() < Strike.TAIL.windup());
        assertTrue(tail.endpoint(0).x * tail.endpoint(1).x < 0);
    }

    @Test
    void lastTailHasItsOwnRevealAndRequiresKage() {
        assertEquals(4, KuramaCloakMotion.desiredTails(1, 3));
        assertEquals(8, KuramaCloakMotion.desiredTails(0.99f, 4));
        assertEquals(9, KuramaCloakMotion.desiredTails(1, 4));
        assertEquals(0, KuramaCloakMotion.unfold(-3, 9));
        assertTrue(KuramaCloakMotion.unfold(8, 9) < KuramaCloakMotion.unfold(8, 8));
        assertEquals(1, KuramaCloakMotion.unfold(16, 9));
    }

    @Test
    void authoritativePacketPreservesPhaseAndFrozenAttackAxis() {
        var world = new ResourceLocation("minecraft", "overworld");
        var packet = new KuramaCloakPacket(world, 61, KuramaCloakPacket.Phase.CONTACT,
                new Blow(Strike.PAW, new Vec3(61, 82.75, -49.25), 126.82f, true));
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            KuramaCloakPacket.encode(packet, buf);
            assertEquals(packet, KuramaCloakPacket.decode(buf));
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }
}
