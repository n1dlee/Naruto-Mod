package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.KamuiPhasePacket;
import com.sekwah.narutomod.util.KamuiPhaseWindow;
import com.sekwah.narutomod.util.KamuiPhaseWindow.Stage;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KamuiPhaseWindowTest {
    @Test
    void openingAndUnresolvedExitAreNotDamageImmunity() {
        assertEquals(Stage.OPENING, KamuiPhaseWindow.advance(2, true, true));
        assertEquals(Stage.PHASED, KamuiPhaseWindow.advance(3, true, true));
        assertEquals(Stage.RESOLVING, KamuiPhaseWindow.advance(KamuiPhaseWindow.OPEN + KamuiPhaseWindow.HOLD, true, true));
        assertEquals(Stage.RESOLVING, KamuiPhaseWindow.advance(4, true, false));
        assertEquals(Stage.RESOLVING, KamuiPhaseWindow.advance(4, false, true));
        assertTrue(Stage.RESOLVING.collisionOff());
        assertFalse(Stage.RESOLVING.protects());
        assertFalse(Stage.SEALED.collisionOff());
    }

    @Test
    void prefersCurrentPositionThenLastSafeAnchor() {
        Vec3 inside = new Vec3(9, 40, 2);
        Vec3 lastClear = new Vec3(8, 40, 2);
        Vec3 origin = new Vec3(5, 40, 2);
        assertEquals(inside, KamuiPhaseWindow.settle(inside, lastClear, origin, point -> true));
        assertEquals(lastClear, KamuiPhaseWindow.settle(inside, lastClear, origin, point -> !inside.equals(point)));
        assertEquals(origin, KamuiPhaseWindow.settle(inside, lastClear, origin, origin::equals));
    }

    @Test
    void ifBothAnchorsWereCoveredNeverFabricateAClearExit() {
        Vec3 feet = new Vec3(7.5, 49, 3.5);
        Vec3 newClear = feet.add(0, 2, 0);
        assertEquals(newClear, KamuiPhaseWindow.settle(feet, feet, feet, newClear::equals));
        assertNull(KamuiPhaseWindow.settle(feet, feet, feet, point -> false));
    }

    @Test
    void clientReceivesThePhysicalStageAndActualSolidSurfaceNormal() {
        var packet = new KamuiPhasePacket(new ResourceLocation("minecraft", "overworld"), 12, Stage.PHASED, 11,
                new Vec3(8, 64.3, 12.04), new Vec3(-1, 0, 0));
        var bytes = new FriendlyByteBuf(Unpooled.buffer());
        try {
            KamuiPhasePacket.encode(packet, bytes);
            assertEquals(packet, KamuiPhasePacket.decode(bytes));
            assertEquals(0, bytes.readableBytes());
        } finally {
            bytes.release();
        }
    }
}
