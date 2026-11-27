package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.TrigramPalmsRenderer;
import com.sekwah.narutomod.util.TrigramSequence.Cue;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record TrigramPalmsPacket(ResourceLocation dimension, int cast, int actor, int target, Cue cue, int phase,
                                 Vec3 anchor, Vec3 contact) {
    public static void encode(TrigramPalmsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.cast);
        buffer.writeVarInt(packet.actor);
        buffer.writeVarInt(packet.target);
        buffer.writeEnum(packet.cue);
        buffer.writeByte(packet.phase);
        writePoint(buffer, packet.anchor);
        writePoint(buffer, packet.contact);
    }

    public static TrigramPalmsPacket decode(FriendlyByteBuf buffer) {
        return new TrigramPalmsPacket(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readEnum(Cue.class), buffer.readByte(), readPoint(buffer), readPoint(buffer));
    }

    public static void handle(TrigramPalmsPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TrigramPalmsRenderer.receive(packet)));
        context.setPacketHandled(true);
    }

    private static void writePoint(FriendlyByteBuf buffer, Vec3 value) {
        buffer.writeDouble(value.x);
        buffer.writeDouble(value.y);
        buffer.writeDouble(value.z);
    }

    private static Vec3 readPoint(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
