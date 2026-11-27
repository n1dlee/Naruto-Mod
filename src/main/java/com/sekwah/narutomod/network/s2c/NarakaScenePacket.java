package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.NarakaSceneRenderer;
import com.sekwah.narutomod.util.NarakaRestoration.Stage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record NarakaScenePacket(ResourceLocation dimension, int owner, Stage stage, Vec3 center, float yaw, int subject) {
    public static void encode(NarakaScenePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.owner);
        buffer.writeEnum(packet.stage);
        buffer.writeDouble(packet.center.x);
        buffer.writeDouble(packet.center.y);
        buffer.writeDouble(packet.center.z);
        buffer.writeFloat(packet.yaw);
        buffer.writeVarInt(packet.subject);
    }

    public static NarakaScenePacket decode(FriendlyByteBuf buffer) {
        return new NarakaScenePacket(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readEnum(Stage.class),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readFloat(), buffer.readVarInt());
    }

    public static void handle(NarakaScenePacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NarakaSceneRenderer.receive(packet)));
        context.setPacketHandled(true);
    }
}
