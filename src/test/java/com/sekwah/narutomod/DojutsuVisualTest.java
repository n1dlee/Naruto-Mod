package com.sekwah.narutomod;

import com.sekwah.narutomod.util.DojutsuVisual;
import com.sekwah.narutomod.util.DojutsuVisual.Command;
import com.sekwah.narutomod.util.DojutsuVisual.Eye;
import com.sekwah.narutomod.util.DojutsuVisual.State;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DojutsuVisualTest {
    @Test
    void togglePrefersClansOwnEyeButCanSelectOwnedAlternative() {
        assertEquals(Command.BYAKUGAN, DojutsuVisual.select("hyuga", true, true, false));
        assertEquals(Command.SHARINGAN, DojutsuVisual.select("hyuga", true, true, true));
        assertEquals(Command.SHARINGAN, DojutsuVisual.select("uchiha", true, true, false));
        assertEquals(Command.BYAKUGAN, DojutsuVisual.select("none", false, true, true));
        assertEquals(Command.NONE, DojutsuVisual.select("none", false, false, true));
    }

    @Test
    void nativeByakuganAndTransplantedSharinganDoNotOverwriteTheSameSide() {
        int code = DojutsuVisual.resolve(new State(false, false, 3, true, false, true, true, true));
        assertEquals(Eye.TOMOE_THREE, DojutsuVisual.eye(code, true));
        assertEquals(Eye.BYAKUGAN_FOCUS, DojutsuVisual.eye(code, false));
    }

    @Test
    void eachPassiveTransplantHasItsOwnEyeAtRest() {
        int code = DojutsuVisual.resolve(new State(false, false, 3, true, true, false, false, true));
        assertEquals(Eye.MANGEKYO, DojutsuVisual.eye(code, true));
        assertEquals(Eye.BYAKUGAN, DojutsuVisual.eye(code, false));
        int dormant = DojutsuVisual.resolve(new State(false, false, 0, false, false, false, true, true));
        assertEquals(Eye.NONE, DojutsuVisual.eye(dormant, true));
        assertEquals(Eye.NONE, DojutsuVisual.eye(dormant, false));
    }

    @Test
    void closedActiveEyeReturnsToPassiveAndUnknownWireFieldsAreNotVisible() {
        int code = DojutsuVisual.resolve(new State(true, true, 0, false, false, false, false, false));
        assertEquals(Eye.RINNEGAN, DojutsuVisual.eye(code, true));
        assertEquals(Eye.RINNEGAN, DojutsuVisual.eye(code, false));
        assertEquals(Eye.NONE, DojutsuVisual.eye(255, false));
        assertEquals(Eye.NONE, DojutsuVisual.eye(255, true));
    }

    @Test
    void visualRevealIsFourTicksAndAtlasRectanglesStaySeparate() {
        assertEquals(0, DojutsuVisual.reveal(-0.1f));
        assertEquals(0.5f, DojutsuVisual.reveal(2), 1e-6);
        assertEquals(1, DojutsuVisual.reveal(8));
        assertEquals(9, DojutsuVisual.textureU(true));
        assertEquals(13, DojutsuVisual.textureU(false));
    }
}
