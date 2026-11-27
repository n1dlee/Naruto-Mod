package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.FlickerPacket;
import com.sekwah.narutomod.util.FlickerPath;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlickerPathTest {
    @Test
    void movingTangentiallyOverFloorDoesNotBlock() {
        var floor = new AABB(-5, -1, -3, 8, 0, 3);
        assertEquals(1.0, FlickerPath.contact(Vec3.ZERO, new Vec3(6, 0, 0), floor, 0.6, 1.8));
    }

    @Test
    void torsoStopsBeforeThinWall() {
        var wall = new AABB(2.0, 0, -0.4, 2.04, 2.4, 0.4);
        double cut = FlickerPath.contact(Vec3.ZERO, new Vec3(6, 0, 0), wall, 0.6, 1.8);
        assertEquals((2.0 - 0.3 + 1.0e-4) / 6, cut, 1.0e-7);
    }

    @Test
    void lowOverhangBlocksStandingButNotShorterPose() {
        var roof = new AABB(2, 1.35, -1, 3, 1.85, 1);
        Vec3 end = new Vec3(4, 0, 0);
        assertTrue(FlickerPath.contact(Vec3.ZERO, end, roof, 0.6, 1.8) < 1.0);
        assertEquals(1.0, FlickerPath.contact(Vec3.ZERO, end, roof, 0.6, 1.15));
    }

    @Test
    void diagonalShoulderCanContactObstacleOutsideCenterRay() {
        var side = new AABB(2.3, 0, 1.7, 2.5, 2.0, 2.0);
        assertTrue(FlickerPath.contact(Vec3.ZERO, new Vec3(5, 0, 5), side, 0.6, 1.8) < 1);
    }

    @Test
    void serverSegmentPreservesPreparationAndActualEndpoints() {
        var world = new ResourceLocation("minecraft", "overworld");
        Vec3 from = new Vec3(5.0, 73.125, 0.5);
        var mark = new FlickerPacket(world, 37, from, from, 1.8f, 738);
        var travel = new FlickerPacket(world, 37, from, new Vec3(11.25, 73.125, 0.5), 1.8f, 738);
        assertTrue(mark.gathering());
        assertFalse(travel.gathering());
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            FlickerPacket.encode(travel, buffer);
            assertEquals(travel, FlickerPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
