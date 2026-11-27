package com.sekwah.narutomod;

import com.sekwah.narutomod.client.model.entity.BijuCloakModel;
import com.sekwah.narutomod.client.model.entity.KuramaFoxModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KuramaCloakRigTest {
    @Test
    void bakedShroudResetsThePreviousWearersTailMotion() {
        var root = BijuCloakModel.createBodyLayer().bakeRoot();
        var model = new BijuCloakModel(root);
        var tails = root.getChild("body").getChild("allTails");
        var base = tails.getChild("tail_0_0");
        float rest = base.xRot;
        model.setVisibleTails(4);
        model.moveRedTails(4, 8.25f, 0.42f, 0.71f);
        assertNotEquals(rest, base.xRot);
        assertTrue(tails.getChild("tail_3_0").visible);
        model.setVisibleTails(1);
        assertEquals(rest, base.xRot);
        assertFalse(tails.getChild("tail_3_0").visible);
    }

    @Test
    void pawAnimationDoesNotWriteTheTailOrLeakIntoTheNextFox() {
        var root = KuramaFoxModel.createBodyLayer().bakeRoot();
        var model = new KuramaFoxModel(root);
        var body = root.getChild("body");
        var paw = body.getChild("biped_body").getChild("biped_right_arm");
        var tail = body.getChild("tails").getChild("Tail00");
        model.waveTails(10);
        float beforePaw = paw.xRot;
        float beforeTail = tail.xRot;
        model.strikePaw(0.35f);
        assertNotEquals(beforePaw, paw.xRot);
        assertEquals(beforeTail, tail.xRot);
        model.waveTails(49.5f);
        assertEquals(beforePaw, paw.xRot);
    }
}
