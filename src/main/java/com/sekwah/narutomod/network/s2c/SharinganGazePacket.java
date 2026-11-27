package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.SharinganGazeRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SharinganGazePacket(ResourceLocation dimension, int subject, Cue cue, int duration, int seed) {
    public enum Cue { FOCUS, LOCKED, MISSED, VICTIM }

    public boolean personal() {
        return this.cue == Cue.VICTIM;
    }

    public static void encode(SharinganGazePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.subject);
        buffer.writeEnum(packet.cue);
        buffer.writeInt(packet.seed);
        if (packet.personal()) {
            buffer.writeVarInt(packet.duration);
        }
    }

    public static SharinganGazePacket decode(FriendlyByteBuf buffer) {
        ResourceLocation dimension = buffer.readResourceLocation();
        int subject = buffer.readVarInt();
        Cue cue = buffer.readEnum(Cue.class);
        int seed = buffer.readInt();
        int duration = cue == Cue.VICTIM ? buffer.readVarInt() : 0;
        return new SharinganGazePacket(dimension, subject, cue, duration, seed);
    }

    public static void handle(SharinganGazePacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SharinganGazeRenderer.add(packet)));
        context.setPacketHandled(true);
    }
}
