package com.sekwah.narutomod;

import com.sekwah.narutomod.util.PerfectSusanooPose;
import com.sekwah.narutomod.util.SpectralSword;
import com.sekwah.narutomod.util.SusanooForm;
import com.sekwah.narutomod.util.SusanooFullBody;
import com.sekwah.narutomod.util.SusanooIntegrityMeter;
import com.sekwah.narutomod.util.SusanooPose;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SusanooStandingAndIntegrityTest {
    @Test
    void standingRigUsesOneOriginForFeetHelmetAndHand() {
        int stage = SusanooForm.STANDING.id();
        Vec3 root = new Vec3(753, 65, -611);
        var frame = SusanooPose.frame(root, 123);
        var space = new SusanooFullBody.Space(root, SusanooFullBody.scale(stage));
        assertEquals(root, space.toWorld(root));
        Vec3 eye = frame.at(0, PerfectSusanooPose.EYE_HEIGHT, 0);
        assertEquals(SusanooFullBody.STANDING_EYE_HEIGHT, space.toWorld(eye).y - root.y, 1e-7);
        var aim = new Vec3(0, -.3, 1).normalize();
        for (int i = 0; i < SusanooPose.SHOTS; i++) {
            int tick = SusanooPose.releaseTick(i);
            var worldArm = SusanooPose.throwArm(frame, stage, aim, tick);
            assertEquals(0, PerfectSusanooPose.throwArm(frame, aim, tick).hand().distanceTo(space.toRig(worldArm).hand()), 1e-8);
            assertEquals(worldArm.hand(), SusanooPose.releasePoint(frame, stage, aim, i));
        }
        var stance = new PerfectSusanooPose.Gait(space.scale()).reset(frame, UnaryOperator.identity());
        assertEquals(PerfectSusanooPose.FOOT_WIDTH, stance.left().distanceTo(stance.right()), 1e-8);
    }

    @Test
    void standingSwordUsesItsOwnContactArcWithoutGiantTiming() {
        int stage = SusanooForm.STANDING.id();
        var initial = new Vec3(56, 81, 9);
        var frame = SusanooPose.frame(initial.add(.42, 0, .3), -43);
        assertEquals(18, SpectralSword.length(stage));
        for (int tick = SpectralSword.startTick(stage); tick <= SpectralSword.endTick(stage); tick++) {
            var draw = PerfectSusanooPose.cutArm(frame, initial, new Vec3(.1, -.09, .1), tick, stage);
            var hit = SpectralSword.sample(initial, -43, stage, tick);
            assertEquals(0, hit.guard().distanceTo(draw.hand()), 1e-7);
        }
    }

    @Test
    void cameraAndWearerSettleAtTheSameSocketWithoutPullingDownVanillaGiant() {
        double eye = 81.62;
        assertEquals(0, SusanooFullBody.rise(eye, 80, 0), 1e-8);
        assertEquals(SusanooFullBody.seatShift(1.62) * .5, SusanooFullBody.rise(eye, 80, .5), 1e-8);
        assertEquals(80 + SusanooFullBody.STANDING_EYE_HEIGHT, eye + SusanooFullBody.rise(eye, 80, 1), 1e-8);
        assertEquals(0, SusanooFullBody.rise(95.3, 80, 1), 1e-8);
    }

    @Test
    void armorHudKeepsLiveNumbersAndShowsLockoutAtZero() {
        var active = new SusanooIntegrityMeter(310, 1240, 0);
        assertEquals("310", active.remainingText());
        assertEquals("1240", active.maximumText());
        assertEquals(25, active.percent());
        assertTrue(active.critical());
        assertFalse(active.broken());
        assertEquals("73.4", new SusanooIntegrityMeter(73.4f, 1240, 0).remainingText());
        assertEquals("<0.1", new SusanooIntegrityMeter(.01f, 1240, 0).remainingText());
        assertEquals(100, new SusanooIntegrityMeter(1500, 1240, 0).percent());
        var broken = new SusanooIntegrityMeter(310, 1240, 21);
        assertTrue(broken.broken());
        assertEquals(0, broken.percent());
        assertEquals(2, broken.seconds());
    }
}
