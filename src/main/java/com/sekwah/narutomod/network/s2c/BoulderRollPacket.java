package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.BoulderRollRenderer;
import com.sekwah.narutomod.util.BoulderRoll.Stage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BoulderRollPacket(ResourceLocation dimension, int actor, Stage stage, Vec3 feet, Vec3 direction,
                                float radius, float rotation) {
    public static void encode(BoulderRollPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.actor);
        buffer.writeEnum(packet.stage);
        writePoint(buffer, packet.feet);
        writePoint(buffer, packet.direction);
        buffer.writeFloat(packet.radius);
        buffer.writeFloat(packet.rotation);
    }

    public static BoulderRollPacket decode(FriendlyByteBuf buffer) {
        return new BoulderRollPacket(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readEnum(Stage.class),
                readPoint(buffer), readPoint(buffer), buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(BoulderRollPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BoulderRollRenderer.receive(packet)));
        context.setPacketHandled(true);
    }

    private static void writePoint(FriendlyByteBuf buffer, Vec3 p) {
        buffer.writeDouble(p.x);
        buffer.writeDouble(p.y);
        buffer.writeDouble(p.z);
    }

    private static Vec3 readPoint(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
