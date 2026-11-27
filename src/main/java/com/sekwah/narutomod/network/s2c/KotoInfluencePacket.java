package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.KotoInfluenceRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record KotoInfluencePacket(ResourceLocation dimension, int id, int subject, Cue cue, int ticks, Vec3 anchor) {
    public enum Cue { GLINT, VISION, END }

    public static void encode(KotoInfluencePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.id);
        buffer.writeVarInt(packet.subject);
        buffer.writeEnum(packet.cue);
        if (packet.cue == Cue.VISION) {
            buffer.writeVarInt(packet.ticks);
            buffer.writeDouble(packet.anchor.x);
            buffer.writeDouble(packet.anchor.y);
            buffer.writeDouble(packet.anchor.z);
        }
    }

    public static KotoInfluencePacket decode(FriendlyByteBuf buffer) {
        ResourceLocation dimension = buffer.readResourceLocation();
        int id = buffer.readVarInt();
        int actor = buffer.readVarInt();
        Cue cue = buffer.readEnum(Cue.class);
        if (cue == Cue.VISION) {
            return new KotoInfluencePacket(dimension, id, actor, cue, buffer.readVarInt(),
                    new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
        }
        return new KotoInfluencePacket(dimension, id, actor, cue, 0, Vec3.ZERO);
    }

    public static void handle(KotoInfluencePacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> KotoInfluenceRenderer.add(packet)));
        context.setPacketHandled(true);
    }
}
