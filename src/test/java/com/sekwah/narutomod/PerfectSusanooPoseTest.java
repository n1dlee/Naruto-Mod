package com.sekwah.narutomod;

import com.sekwah.narutomod.util.GiantForm;
import com.sekwah.narutomod.util.PerfectSusanooPose;
import com.sekwah.narutomod.util.SpectralSword;
import com.sekwah.narutomod.util.SusanooPose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfectSusanooPoseTest {
    @Test
    void oneFootStaysPlantedWhileOtherFootLiftsAndLands() {
        var gait = new PerfectSusanooPose.Gait();
        UnaryOperator<Vec3> floor = point -> new Vec3(point.x, point.z > .5 ? 65 : 64, point.z);
        var at = new Vec3(18, 64, 0);
        var neutral = gait.reset(SusanooPose.frame(at, 0), floor);
        var travel = new Vec3(0, 0, .34);
        PerfectSusanooPose.Stance step = neutral;
        for (int i = 1; i <= 6; i++) {
            step = gait.update(SusanooPose.frame(at.add(travel.scale(i)), 0), travel, floor);
            assertEquals(neutral.right(), step.right());
            if (i < 6) {
                assertEquals(-1, step.swingSide());
                assertTrue(step.left().y > neutral.left().y);
            }
        }
        assertEquals(0, step.swingSide());
        assertEquals(65, step.left().y, 1e-8);
        var next = gait.update(SusanooPose.frame(at.add(travel.scale(7)), 0), travel, floor);
        assertEquals(1, next.swingSide());
        assertEquals(step.left(), next.left());
    }

    @Test
    void aStationaryGiantDoesNotShuffleAndSkipsOldFeetOnReset() {
        var gait = new PerfectSusanooPose.Gait();
        var frame = SusanooPose.frame(Vec3.ZERO, 35);
        var start = gait.reset(frame, UnaryOperator.identity());
        for (int i = 0; i < 15; i++) {
            assertEquals(start, gait.update(frame, Vec3.ZERO, UnaryOperator.identity()));
        }
        var remote = SusanooPose.frame(new Vec3(65, 12, -9), 81);
        assertEquals(remote.at(-PerfectSusanooPose.FOOT_WIDTH, 0, .16), gait.reset(remote, UnaryOperator.identity()).left());
    }

    @Test
    void giantBladeReachesGroundLevelInFrontButNotBehind() {
        var target = new AABB(-.37, 0, 10, .37, 1.8, 10.7);
        var behind = target.move(0, 0, -22);
        boolean touched = false;
        assertFalse(SpectralSword.active(4, 9));
        assertFalse(SpectralSword.active(4, 17));
        for (double tick = 10; tick <= 16; tick += .5) {
            var blade = SpectralSword.sample(Vec3.ZERO, 0, 4, tick);
            touched |= blade.contact(target, .95) != null;
            assertNull(blade.contact(behind, .95));
        }
        assertTrue(touched);
        assertEquals(18, SpectralSword.length(3));
        assertEquals(26, SpectralSword.length(4));
    }

    @Test
    void giantHandsMatchEveryAuthoritativeReleaseAndContact() {
        var frame = SusanooPose.frame(new Vec3(23, 80, -92), 71);
        var aim = new Vec3(.21, -.67, -.68).normalize();
        for (int shot = 0; shot < SusanooPose.SHOTS; shot++) {
            var expected = PerfectSusanooPose.throwArm(frame, aim, SusanooPose.releaseTick(shot)).hand();
            assertEquals(expected, SusanooPose.releasePoint(frame, 4, aim, shot));
            assertTrue(expected.y > frame.feet().y + 8.5);
        }
        var moved = SusanooPose.frame(frame.feet().add(.8, 0, -.32), 71);
        for (double age = 10; age <= 16; age += .5) {
            var blade = SpectralSword.sample(frame.feet(), frame.yaw(), 4, age);
            var arm = PerfectSusanooPose.cutArm(moved, frame.feet(), new Vec3(.1, -.24, -.15), age);
            assertEquals(0, blade.guard().distanceTo(arm.hand()), 1e-6);
        }
    }

    @Test
    void wingsDeployInOrderAndClearanceMatchesTheCollisionCore() {
        assertEquals(0, PerfectSusanooPose.wingOpening(8, 0, -1));
        assertNotEquals(PerfectSusanooPose.wingOpening(13, 0, -1), PerfectSusanooPose.wingOpening(13, 0, 1));
        assertEquals(1, PerfectSusanooPose.wingOpening(20, 0, 1));
        assertTrue(PerfectSusanooPose.wingOpening(20, 1, 1) < 1);
        var clearance = GiantForm.susanooClearance(new Vec3(-2, 64, 7));
        assertEquals(GiantForm.SUSANOO_WIDTH, clearance.getXsize(), 1e-6);
        assertEquals(GiantForm.HEADROOM, clearance.getYsize(), 1e-6);
        assertEquals(64, clearance.minY, 1e-6);
    }
}
