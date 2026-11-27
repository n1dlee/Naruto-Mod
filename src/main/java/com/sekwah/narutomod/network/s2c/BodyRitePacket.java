package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.BodyRiteRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BodyRitePacket(ResourceLocation dimension, int actor, Phase phase, int intensity, Vec3 at) {
    public enum Phase {
        SAGE_DRAW, SAGE_BALANCE, SAGE_OVERFLOW, SAGE_BREAK, SAGE_STONE, SAGE_ENTER, SAGE_EXIT,
        GATE_OPEN, GATE_WARNING, GATE_CLOSE, GATE_SPENT, GATE_DEATH
    }

    public BodyRitePacket {
        int max = phase.ordinal() < Phase.GATE_OPEN.ordinal() ? 100 : 8;
        if (intensity < 0 || intensity > max) {
            throw new IllegalArgumentException("Invalid body rite intensity: " + intensity);
        }
    }

    public static void encode(BodyRitePacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.dimension());
        buf.writeVarInt(packet.actor());
        buf.writeEnum(packet.phase());
        buf.writeByte(packet.intensity());
        buf.writeDouble(packet.at().x);
        buf.writeDouble(packet.at().y);
        buf.writeDouble(packet.at().z);
    }

    public static BodyRitePacket decode(FriendlyByteBuf buf) {
        return new BodyRitePacket(buf.readResourceLocation(), buf.readVarInt(), buf.readEnum(Phase.class),
                buf.readUnsignedByte(), new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    public static void handle(BodyRitePacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BodyRiteRenderer.receive(packet)));
        context.setPacketHandled(true);
    }
}
