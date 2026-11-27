package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.KamuiTransitRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record KamuiTransitPacket(ResourceLocation dimension, int id, Cue cue, Vec3 anchor, float height, int waitTicks) {
    public enum Cue { GATHER, FOLD, OPEN, CANCEL }

    public static void encode(KamuiTransitPacket event, FriendlyByteBuf buf) {
        buf.writeResourceLocation(event.dimension);
        buf.writeVarInt(event.id);
        buf.writeEnum(event.cue);
        buf.writeDouble(event.anchor.x);
        buf.writeDouble(event.anchor.y);
        buf.writeDouble(event.anchor.z);
        buf.writeFloat(event.height);
        buf.writeVarInt(event.waitTicks);
    }

    public static KamuiTransitPacket decode(FriendlyByteBuf buf) {
        return new KamuiTransitPacket(buf.readResourceLocation(), buf.readVarInt(), buf.readEnum(Cue.class),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readFloat(), buf.readVarInt());
    }

    public static void handle(KamuiTransitPacket event, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> KamuiTransitRenderer.receive(event)));
        context.setPacketHandled(true);
    }
}
