package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.KikaichuRenderer;
import com.sekwah.narutomod.util.KikaichuFlight.Stage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record KikaichuPacket(ResourceLocation dimension, int cast, int owner, Stage stage, Vec3 from, Vec3 center,
                             boolean cargo) {
    public static void encode(KikaichuPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.cast);
        buffer.writeVarInt(packet.owner);
        buffer.writeEnum(packet.stage);
        point(buffer, packet.from);
        point(buffer, packet.center);
        buffer.writeBoolean(packet.cargo);
    }

    public static KikaichuPacket decode(FriendlyByteBuf buffer) {
        return new KikaichuPacket(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readVarInt(), buffer.readEnum(Stage.class),
                point(buffer), point(buffer), buffer.readBoolean());
    }

    public static void handle(KikaichuPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> KikaichuRenderer.receive(packet)));
        context.setPacketHandled(true);
    }

    private static void point(FriendlyByteBuf buffer, Vec3 value) {
        buffer.writeDouble(value.x);
        buffer.writeDouble(value.y);
        buffer.writeDouble(value.z);
    }

    private static Vec3 point(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
