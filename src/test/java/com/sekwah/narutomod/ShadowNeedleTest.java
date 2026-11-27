package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.ShadowTetherPacket;
import com.sekwah.narutomod.util.ShadowNeedle;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowNeedleTest {
    @Test
    void anotherFloorDoesNotShareTheStrikeVolume() {
        var root = new Vec3(6, 62.5, 81.125);
        assertTrue(ShadowNeedle.reaches(root, root.add(0.29, 0.01, 0.18), 0.60));
        assertFalse(ShadowNeedle.reaches(root, root.add(0, 1.0, 0), 0.60));
        assertFalse(ShadowNeedle.reaches(root, root.add(0.56, 0, 0), 0.60));
    }

    @Test
    void acceptedNeedleRisesOnceWhileAMissOnlyTwitchesAtTheFloor() {
        assertEquals(0, ShadowNeedle.height(0, true));
        assertEquals(ShadowNeedle.STRIKE_HEIGHT, ShadowNeedle.height(1.4f, true), 0.001);
        assertTrue(ShadowNeedle.height(1.4f, false) < 0.24);
        assertEquals(0, ShadowNeedle.height(7, true));
    }

    @Test
    void multipleBranchesUseIndependentRoutesAndHitKinds() {
        var level = new ResourceLocation("minecraft", "overworld");
        var branch = List.of(Vec3.ZERO, new Vec3(0.5, 0, 1));
        for (var cue : List.of(ShadowTetherPacket.Cue.SEW_TRACE, ShadowTetherPacket.Cue.SEW_HIT, ShadowTetherPacket.Cue.SEW_MISS)) {
            var packet = new ShadowTetherPacket(level, 2, -31, cue, branch);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                ShadowTetherPacket.encode(packet, buffer);
                assertEquals(packet, ShadowTetherPacket.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally {
                buffer.release();
            }
        }
        assertEquals(3, ShadowNeedle.BRANCHES);
    }
}
