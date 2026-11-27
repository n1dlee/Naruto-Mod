package com.sekwah.narutomod;

import com.sekwah.narutomod.util.SusanooChakraMaterial;
import com.sekwah.narutomod.util.SusanooForm;
import com.sekwah.narutomod.util.SusanooImpactGeometry;
import com.sekwah.narutomod.util.SusanooPose;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SusanooChakraPresentationTest {
    @Test
    void plateRimBrightensAtGrazingAnglesOnEitherSide() {
        var normal = new Vec3(0, 0, 2);
        assertEquals(0, SusanooChakraMaterial.rim(normal, new Vec3(0, 0, 10)), 1e-8);
        assertEquals(1, SusanooChakraMaterial.rim(normal, new Vec3(3, 0, 0)), 1e-8);
        assertEquals(SusanooChakraMaterial.rim(normal, new Vec3(2, 0, 1)),
                SusanooChakraMaterial.rim(normal.scale(-1), new Vec3(2, 0, 1)), 1e-8);
        assertEquals(0, SusanooChakraMaterial.rim(Vec3.ZERO, new Vec3(3, 0, 0)), 1e-8);
    }

    @Test
    void wingVeinPeakTravelsAwayFromItsRoot() {
        double position = .42;
        double arrival = position * 12.5 / .29;
        assertEquals(1, SusanooChakraMaterial.vein(0, 0, 0), 1e-8);
        assertTrue(SusanooChakraMaterial.vein(0, position, 0) < .9);
        assertEquals(1, SusanooChakraMaterial.vein(arrival, position, 0), 1e-8);
    }

    @Test
    void shieldContactsStayOnTheIncomingSideAfterYawRotation() {
        var frame = SusanooPose.frame(new Vec3(16, 72, -4), 37);
        var front = SusanooImpactGeometry.sample(frame, SusanooForm.RIBS, frame.at(0, 1.7, 6), Vec3.ZERO);
        var back = SusanooImpactGeometry.sample(frame, SusanooForm.RIBS, frame.at(0, 1.7, -6), Vec3.ZERO);
        assertTrue(front.point().subtract(frame.feet()).dot(frame.forward()) > .4);
        assertTrue(back.point().subtract(frame.feet()).dot(frame.forward()) < -.4);
        assertEquals(1, front.normal().dot(frame.forward()), 1e-8);
        assertEquals(-1, back.normal().dot(frame.forward()), 1e-8);
    }

    @Test
    void giantGroundHitStaysOnShinAndHighProjectileStaysOnHelmet() {
        var frame = SusanooPose.frame(new Vec3(13, 91, 8), -47);
        var ground = SusanooImpactGeometry.sample(frame, SusanooForm.PERFECT, frame.at(1.8, 1.6, 4), Vec3.ZERO);
        var high = SusanooImpactGeometry.sample(frame, SusanooForm.PERFECT, frame.at(0, 16.7, 0), frame.forward());
        assertEquals(frame.feet().y + 1.6, ground.point().y, 1e-8);
        assertTrue(ground.point().subtract(frame.feet()).dot(frame.right()) > 1);
        assertEquals(frame.feet().y + 16.7, high.point().y, 1e-8);
        assertEquals(-1, high.normal().dot(frame.forward()), 1e-8);
    }

    @Test
    void environmentalDamageHasAStableNeutralCue() {
        var frame = SusanooPose.frame(Vec3.ZERO, 230);
        var contact = SusanooImpactGeometry.sample(frame, SusanooForm.SKELETON, null, Vec3.ZERO);
        assertEquals(new Vec3(0, 1.65, 0), contact.point());
        assertEquals(new Vec3(0, 1, 0), contact.normal());
    }
}
