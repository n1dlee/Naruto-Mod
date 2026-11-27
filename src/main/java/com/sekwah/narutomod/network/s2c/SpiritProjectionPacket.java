package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.SpiritProjectionRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SpiritProjectionPacket(ResourceLocation dimension, int actor, int session, State state, Vec3 body, Vec3 eye,
                                     float yaw) {
    public enum State { GATHER, DRIFT, RELEASE }

    public static void encode(SpiritProjectionPacket event, FriendlyByteBuf buf) {
        buf.writeResourceLocation(event.dimension);
        buf.writeVarInt(event.actor);
        buf.writeVarInt(event.session);
        buf.writeEnum(event.state);
        point(buf, event.body);
        point(buf, event.eye);
        buf.writeFloat(event.yaw);
    }

    public static SpiritProjectionPacket decode(FriendlyByteBuf buf) {
        return new SpiritProjectionPacket(buf.readResourceLocation(), buf.readVarInt(), buf.readVarInt(), buf.readEnum(State.class),
                point(buf), point(buf), buf.readFloat());
    }

    private static Vec3 point(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    private static void point(FriendlyByteBuf buf, Vec3 point) {
        buf.writeDouble(point.x);
        buf.writeDouble(point.y);
        buf.writeDouble(point.z);
    }

    public static void handle(SpiritProjectionPacket event, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SpiritProjectionRenderer.receive(event)));
        context.setPacketHandled(true);
    }
}
