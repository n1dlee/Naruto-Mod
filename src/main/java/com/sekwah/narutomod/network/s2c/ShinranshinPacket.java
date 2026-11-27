package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.ShinranshinRenderer;
import com.sekwah.narutomod.util.Shinranshin.Stage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ShinranshinPacket(ResourceLocation dimension, int cast, int actor, int target, Stage stage, int ticks,
                                Vec3 origin, Vec3 contact) {
    public static void encode(ShinranshinPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.cast);
        buffer.writeVarInt(packet.actor);
        buffer.writeVarInt(packet.target);
        buffer.writeEnum(packet.stage);
        buffer.writeVarInt(packet.ticks);
        writePoint(buffer, packet.origin);
        writePoint(buffer, packet.contact);
    }

    public static ShinranshinPacket decode(FriendlyByteBuf buffer) {
        return new ShinranshinPacket(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readEnum(Stage.class), buffer.readVarInt(), readPoint(buffer), readPoint(buffer));
    }

    public static void handle(ShinranshinPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ShinranshinRenderer.receive(packet)));
        context.setPacketHandled(true);
    }

    private static void writePoint(FriendlyByteBuf buffer, Vec3 point) {
        buffer.writeDouble(point.x);
        buffer.writeDouble(point.y);
        buffer.writeDouble(point.z);
    }

    private static Vec3 readPoint(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
