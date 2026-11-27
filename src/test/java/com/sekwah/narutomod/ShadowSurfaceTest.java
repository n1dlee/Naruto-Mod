package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.ShadowTetherPacket;
import com.sekwah.narutomod.util.ShadowSurface;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowSurfaceTest {
    @Test
    void followsTheHeightOfAnActualStepWithoutFlyingOverTheSurface() {
        Vec3 target = new Vec3(3, 0.5, 0);
        var route = ShadowSurface.path(Vec3.ZERO, target, 0, (last, flat) -> new Vec3(flat.x, flat.x >= 1.5 ? 0.5 : 0, flat.z));
        assertTrue(ShadowSurface.reaches(route, target));
        assertTrue(route.stream().anyMatch(point -> point.y == 0));
        assertTrue(route.stream().anyMatch(point -> point.y == 0.5));
    }

    @Test
    void holeOrDarkSegmentEndsThePathAndABendCanRoundTheEdge() {
        Vec3 target = new Vec3(3, 0, 0);
        var straight = ShadowSurface.path(Vec3.ZERO, target, 0,
                (last, flat) -> flat.x > 0.9 && flat.x < 2.0 && Math.abs(flat.z) < 0.55 ? null : flat);
        assertFalse(ShadowSurface.reaches(straight, target));
        var curved = ShadowSurface.path(Vec3.ZERO, target, 1.05,
                (last, flat) -> flat.x > 0.9 && flat.x < 2.0 && Math.abs(flat.z) < 0.55 ? null : flat);
        assertTrue(ShadowSurface.reaches(curved, target));
        assertFalse(ShadowSurface.stepHeight(Vec3.ZERO, new Vec3(0, -1.5, 0)));
    }

    @Test
    void mirrorIsHorizontalAndPlayerInfluenceIsStrictlyCapped() {
        Vec3 player = ShadowSurface.mirror(new Vec3(0, -0.4, 0), new Vec3(9, 9, -9), true);
        Vec3 mob = ShadowSurface.mirror(new Vec3(0, -0.4, 0), new Vec3(9, 9, -9), false);
        assertEquals(-0.4, player.y, 0.0001);
        assertEquals(0.096, player.horizontalDistance(), 0.0001);
        assertEquals(0.19, mob.horizontalDistance(), 0.0001);
        assertTrue(ShadowSurface.PLAYER_TIME < ShadowSurface.MOB_TIME);
    }

    @Test
    void wireFormatCarriesOneBoundedPathAtSubBlockPrecision() {
        Vec3 first = new Vec3(512.28, 61.50, -16.73);
        List<Vec3> points = List.of(first, first.add(0.25, 0, 0.5), first.add(0.6, 0.5, 1));
        var level = new ResourceLocation("minecraft", "overworld");
        var packet = new ShadowTetherPacket(level, 71, 199, ShadowTetherPacket.Cue.SNAP, points);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ShadowTetherPacket.encode(packet, buffer);
            var decoded = ShadowTetherPacket.decode(buffer);
            assertEquals(first, decoded.path().get(0));
            assertEquals(points.get(1), decoded.path().get(1));
            assertTrue(points.get(2).distanceTo(decoded.path().get(2)) < 0.014);
            assertEquals(ShadowTetherPacket.Cue.SNAP, decoded.cue());
            assertEquals(0, buffer.readableBytes());
            assertThrows(IllegalArgumentException.class, () -> new ShadowTetherPacket(level, 71, 199,
                    ShadowTetherPacket.Cue.BOUND, java.util.Collections.nCopies(38, first)));
        } finally {
            buffer.release();
        }
    }
}
