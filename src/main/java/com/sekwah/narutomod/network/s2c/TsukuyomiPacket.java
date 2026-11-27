package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.TsukuyomiRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record TsukuyomiPacket(ResourceLocation dimension, int victim, int id, int ticks) {
    public static void encode(TsukuyomiPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.victim);
        buffer.writeVarInt(packet.id);
        buffer.writeVarInt(packet.ticks);
    }

    public static TsukuyomiPacket decode(FriendlyByteBuf buffer) {
        return new TsukuyomiPacket(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(TsukuyomiPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TsukuyomiRenderer.accept(packet)));
        context.setPacketHandled(true);
    }
}
