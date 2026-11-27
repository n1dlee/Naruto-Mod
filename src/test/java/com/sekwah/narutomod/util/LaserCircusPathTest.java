package com.sekwah.narutomod.util;

import com.sekwah.narutomod.network.s2c.LaserCircusPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaserCircusPathTest {
    @Test
    void separateBeamArcsReturnToTheSameEndpointExactly() {
        var from = new Vec3(-20, 70, 88);
        var to = from.add(12, 4, 0);
        var left = LaserCircusPath.route(from, to, 0);
        var right = LaserCircusPath.route(from, to, 3);
        assertEquals(LaserCircusPath.SAMPLES, left.size());
        assertEquals(from, left.get(0));
        assertEquals(to, right.get(right.size() - 1));
        assertTrue(left.get(6).distanceTo(right.get(6)) > 1.2);
    }

    @Test
    void untargetedFanRemainsInFrontWhenPointedVertically() {
        var from = new Vec3(-2, 0, 1);
        var aim = new Vec3(0, 1, 0);
        var first = LaserCircusPath.endpoint(from, aim, 0);
        var last = LaserCircusPath.endpoint(from, aim, LaserCircusPath.BRANCHES - 1);
        assertTrue(first.subtract(from).dot(aim) >= LaserCircusPath.RANGE - 0.37);
        assertTrue(last.distanceTo(first) > 2.5);
    }

    @Test
    void segmentedBeamPacketPreservesTheServerPoints() {
        var from = new Vec3(5, 2, 9);
        var route = LaserCircusPath.route(from, from.add(0, 0, 9), 1);
        var dimension = new ResourceLocation("minecraft", "overworld");
        var original = new LaserCircusPacket(dimension, 12, LaserCircusPacket.Stage.BLOCK, route.subList(0, 4));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            LaserCircusPacket.encode(original, buffer);
            assertEquals(original, LaserCircusPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
            LaserCircusPacket.encode(new LaserCircusPacket(dimension, 12, LaserCircusPacket.Stage.BLOCK, route.subList(0, 1)), buffer);
            assertThrows(IllegalArgumentException.class, () -> LaserCircusPacket.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
