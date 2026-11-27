package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.GatsugaRenderer;
import com.sekwah.narutomod.util.GatsugaMotion.Stage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record GatsugaPacket(ResourceLocation dimension, int cast, int body, Stage stage, boolean wolf,
                            Vec3 start, Vec3 end) {
    public static void encode(GatsugaPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.cast);
        buffer.writeVarInt(packet.body);
        buffer.writeEnum(packet.stage);
        buffer.writeBoolean(packet.wolf);
        point(buffer, packet.start);
        point(buffer, packet.end);
    }

    public static GatsugaPacket decode(FriendlyByteBuf buffer) {
        return new GatsugaPacket(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readEnum(Stage.class), buffer.readBoolean(), point(buffer), point(buffer));
    }

    public static void handle(GatsugaPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GatsugaRenderer.receive(packet)));
        context.setPacketHandled(true);
    }

    private static void point(FriendlyByteBuf buffer, Vec3 p) {
        buffer.writeDouble(p.x);
        buffer.writeDouble(p.y);
        buffer.writeDouble(p.z);
    }

    private static Vec3 point(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
