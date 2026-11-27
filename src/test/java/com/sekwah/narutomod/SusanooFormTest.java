package com.sekwah.narutomod;

import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.util.SpectralSword;
import com.sekwah.narutomod.util.SusanooForm;
import com.sekwah.narutomod.util.SusanooPose;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SusanooFormTest {
    @Test
    void progressionAndSaveIdsAreIndependent() {
        assertEquals(SusanooForm.ARMORED, SusanooForm.fromId(3));
        assertEquals(SusanooForm.PERFECT, SusanooForm.fromId(4));
        SusanooForm step = SusanooForm.NONE;
        for (int i = 1; i <= 7; i++) {
            var next = step.stepToward(SusanooForm.PERFECT);
            assertEquals(i, next.ordinal());
            assertTrue(next.integrity() > step.integrity());
            assertEquals(next, SusanooForm.fromId(next.id()));
            step = next;
        }
        assertEquals(SusanooForm.PERFECT, step);
    }

    @Test
    void scrollAndBossHealthPhasesSelectDifferentCeilings() {
        assertEquals(SusanooForm.SKELETON, SusanooForm.forPower(1, false));
        assertEquals(SusanooForm.SINEW, SusanooForm.forPower(.4f, true));
        assertEquals(SusanooForm.AVATAR, SusanooForm.forPower(.5f, true));
        assertEquals(SusanooForm.STANDING, SusanooForm.forPower(.9f, true));
        assertEquals(SusanooForm.PERFECT, SusanooForm.forPower(1, true));
        assertEquals(SusanooForm.ARMORED, SusanooForm.forBossPhase(4, true));
        assertEquals(SusanooForm.ARMORED, SusanooForm.limit(4, true));
        assertEquals(SusanooForm.PERFECT, SusanooForm.forBossPhase(4, false));
    }

    @Test
    void intermediateStagesKeepTheStandardDamageTierAndHitbox() {
        var data = new NinjaData(false);
        data.setSusanooActive(true);
        var frame = SusanooPose.frame(new Vec3(1, 64, 2), 112);
        var direction = new Vec3(.34, .14, -.92).normalize();
        for (SusanooForm form : new SusanooForm[]{SusanooForm.SINEW, SusanooForm.AVATAR, SusanooForm.STANDING}) {
            int id = form.id();
            data.setSusanooStage(id);
            assertEquals(id, data.getSusanooStage());
            assertFalse(data.isGiantForm());
            assertEquals(18, SpectralSword.length(id));
            assertEquals(SusanooPose.throwArm(frame, id, direction, SusanooPose.releaseTick(1)).hand(),
                    SusanooPose.releasePoint(frame, id, direction, 1));
        }
        data.setSusanooStage(SusanooForm.PERFECT.id());
        assertTrue(data.isGiantForm());
    }
}
