package com.sekwah.narutomod;

import com.sekwah.narutomod.network.s2c.BodyRitePacket;
import com.sekwah.narutomod.util.EightGateCycle;
import com.sekwah.narutomod.util.SageFocus;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BodyRiteTest {
    @Test
    void twoPlayersCannotShareTheOverchargeClockOrCommit() {
        var first = new SageFocus();
        var second = new SageFocus();
        for (int i = 1; i < SageFocus.DANGER_TICKS; i++) {
            assertEquals(SageFocus.Beat.OVERFLOW, first.advance(100, Vec3.ZERO, true).beat());
            assertEquals(SageFocus.Beat.DRAW, second.advance(0, Vec3.ZERO, true).beat());
        }
        assertEquals(SageFocus.Beat.PETRIFY, first.advance(100, Vec3.ZERO, true).beat());
        assertEquals(SageFocus.Beat.OVERFLOW, second.advance(100, Vec3.ZERO, true).beat());
        second.commit();
        assertTrue(second.committed());
        assertFalse(first.committed());
        second.reset();
        assertFalse(second.committed());
    }

    @Test
    void movementAndAirborneReleaseDoNotExploitASavedReserve() {
        var focus = new SageFocus();
        assertEquals(60, focus.advance(59, Vec3.ZERO, true).charge());
        assertTrue(focus.canRelease(60, new Vec3(0.01, 0, 0), true));
        assertFalse(focus.canRelease(60, Vec3.ZERO, false));
        assertFalse(focus.canRelease(60, new Vec3(0.07, 0, 0), true));
        var fall = focus.advance(100, new Vec3(0, -0.2, 0), false);
        assertEquals(SageFocus.Beat.FRACTURE, fall.beat());
        assertEquals(98, fall.charge());
        assertFalse(focus.canRelease(98, Vec3.ZERO, true));
        assertEquals(1200, SageFocus.duration(100));
    }

    @Test
    void slowCreepingStillLeavesTheMeditationAnchor() {
        var focus = new SageFocus();
        var sample = focus.advance(0, Vec3.ZERO, true);
        for (int i = 1; i <= 5; i++) {
            sample = focus.advance(sample.charge(), new Vec3(i * 0.029, 0, 0), true);
        }
        assertEquals(SageFocus.Beat.FRACTURE, sample.beat());
        assertFalse(focus.canRelease(80, new Vec3(0.145, 0, 0), true));
    }

    @Test
    void gateWarningCannotBeConfirmedByDuplicateInputsOrAfterItsWindow() {
        var cycle = new EightGateCycle();
        assertEquals(EightGateCycle.Confirm.WARN, cycle.confirm(120));
        assertEquals(EightGateCycle.Confirm.WAIT, cycle.confirm(120));
        assertEquals(EightGateCycle.Confirm.WAIT, cycle.confirm(125));
        assertEquals(EightGateCycle.Confirm.ACCEPT, cycle.confirm(126));
        var other = new EightGateCycle();
        assertEquals(EightGateCycle.Confirm.WARN, other.confirm(126));
        assertEquals(EightGateCycle.Confirm.WARN, cycle.confirm(160));
        cycle.clear();
        assertEquals(EightGateCycle.Confirm.WARN, cycle.confirm(161));
    }

    @Test
    void onlyEighthGatePersistsAndItKeepsTheOriginalDeadline() {
        var cycle = new EightGateCycle();
        CompoundTag tag = new CompoundTag();
        cycle.open(7, 1000);
        cycle.save(tag);
        assertTrue(tag.isEmpty());
        cycle.open(8, 1000);
        cycle.save(tag);
        var reloaded = new EightGateCycle();
        reloaded.load(tag);
        assertTrue(reloaded.hasDeathDeadline());
        assertEquals(237, reloaded.deathRemaining(1063));
        assertEquals(0, reloaded.deathRemaining(8000));
        reloaded.clear();
        assertFalse(reloaded.hasDeathDeadline());
    }

    @Test
    void gatePricesAndSilhouettesEscalateAndFinalSteamIsRed() {
        for (int i = 2; i <= EightGateCycle.LAST; i++) {
            var lower = EightGateCycle.profile(i - 1);
            var higher = EightGateCycle.profile(i);
            assertTrue(higher.chakra() > lower.chakra());
            assertTrue(higher.span() > lower.span());
            assertTrue(higher.height() > lower.height());
        }
        int blue = EightGateCycle.profile(7).tint();
        int red = EightGateCycle.profile(8).tint();
        assertTrue((blue & 255) > (blue >>> 16));
        assertTrue((red & 255) < (red >>> 16));
    }

    @Test
    void phasePacketUsesTheCorrectIntensityRangeAndKeepsTheWorldAnchor() {
        var world = new ResourceLocation("minecraft", "the_nether");
        var packet = new BodyRitePacket(world, 53, BodyRitePacket.Phase.SAGE_OVERFLOW, 86, new Vec3(21.7, 81, -128));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            BodyRitePacket.encode(packet, buffer);
            assertEquals(packet, BodyRitePacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
        assertThrows(IllegalArgumentException.class,
                () -> new BodyRitePacket(world, 53, BodyRitePacket.Phase.GATE_OPEN, 9, Vec3.ZERO));
    }
}
