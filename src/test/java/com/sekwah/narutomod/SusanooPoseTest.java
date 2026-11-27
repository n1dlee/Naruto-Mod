package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.FinalChakraPacket;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket.Phase;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket.Shape;
import com.sekwah.narutomod.util.SpectralSword;
import com.sekwah.narutomod.util.SusanooPose;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SusanooPoseTest {
    @Test
    void cameraYawRotatesTheWholeRigAroundTheFeet() {
        var feet = new Vec3(14, 64, -21);
        var forward = SusanooPose.frame(feet, 0);
        var west = SusanooPose.frame(feet, 90);
        assertEquals(0, feet.add(-1, 2, 3).distanceTo(forward.at(1, 2, 3)), 1e-6);
        assertEquals(0, feet.add(-3, 2, -1).distanceTo(west.at(1, 2, 3)), 1e-6);
    }

    @Test
    void everySteepShotStartsInTheHandAndTheArmRecovers() {
        var frame = SusanooPose.frame(new Vec3(-43, 89, 17), -115);
        var aim = new Vec3(.14, -.98, .02).normalize();
        for (int stage = 2; stage <= 3; stage++) {
            var rest = SusanooPose.rest(frame, stage, 1, 0);
            assertEquals(rest, SusanooPose.throwArm(frame, stage, aim, 0));
            for (int shot = 0; shot < SusanooPose.SHOTS; shot++) {
                int tick = SusanooPose.releaseTick(shot);
                assertEquals(7 + 2 * shot, tick);
                var hand = SusanooPose.throwArm(frame, stage, aim, tick).hand();
                assertEquals(hand, SusanooPose.releasePoint(frame, stage, aim, shot));
                assertTrue(rest.hand().distanceTo(hand) > .5);
            }
            assertEquals(rest, SusanooPose.throwArm(frame, stage, aim, SusanooPose.THROW_LENGTH));
        }
    }

    @Test
    void armorRaisesTheThrowSocketAboveTheSkeletalForm() {
        var frame = SusanooPose.frame(Vec3.ZERO, 0);
        var aim = new Vec3(0, 0, 1);
        assertTrue(SusanooPose.releasePoint(frame, 3, aim, 0).y > SusanooPose.releasePoint(frame, 2, aim, 0).y);
    }

    @Test
    void movingCarrierKeepsItsHandOnTheAuthoritativeBladeDuringContact() {
        var initial = new Vec3(87, 65, 405);
        var moving = SusanooPose.frame(initial.add(.45, 0, -.67), -38);
        var rest = SusanooPose.rest(moving, 3, 1, 0);
        assertEquals(rest, SusanooPose.cutArm(moving, initial, 3, 0));
        for (double tick = SpectralSword.CUT_START; tick <= SpectralSword.CUT_END; tick += .5) {
            var arm = SusanooPose.cutArm(moving, initial, 3, tick);
            var blade = SpectralSword.sample(initial, -38, 3, tick);
            assertEquals(0, blade.guard().distanceTo(arm.hand()), 1e-6);
            assertEquals(rest.shoulder(), arm.shoulder());
        }
        assertEquals(rest, SusanooPose.cutArm(moving, initial, 3, SpectralSword.LENGTH));
    }

    @Test
    void actionSnapshotRoundTripsWithServerClockAndFrozenYaw() {
        var shape = new Shape(new Vec3(15, 90, 20), new Vec3(-1, 0, 0), 3, 0x4088FF);
        var dimension = new ResourceLocation("minecraft", "overworld");
        var snapshot = new FinalChakraPacket(dimension, 54, Phase.YASAKA_GATHER, shape, 714L, 91.25f);
        var cancel = new FinalChakraPacket(dimension, 54, Phase.YASAKA_STOP, shape, 714L, 91.25f);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            FinalChakraPacket.encode(snapshot, buffer);
            FinalChakraPacket.encode(cancel, buffer);
            assertEquals(snapshot, FinalChakraPacket.decode(buffer));
            assertEquals(cancel, FinalChakraPacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
            assertNotEquals(Phase.SWORD_STOP, cancel.phase());
        } finally {
            buffer.release();
        }
    }
}
