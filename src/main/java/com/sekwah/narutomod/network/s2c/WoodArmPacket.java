package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.WoodArmRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record WoodArmPacket(ResourceLocation dimension, int cast, Stage stage, Vec3 base, Vec3 from, Vec3 tip) {
    public static void encode(WoodArmPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.cast);
        buffer.writeEnum(packet.stage);
        writePoint(buffer, packet.base);
        writePoint(buffer, packet.from);
        writePoint(buffer, packet.tip);
    }

    public static WoodArmPacket decode(FriendlyByteBuf buffer) {
        return new WoodArmPacket(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readEnum(Stage.class),
                readPoint(buffer), readPoint(buffer), readPoint(buffer));
    }

    public static void handle(WoodArmPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> WoodArmRenderer.receive(packet)));
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

    public enum Stage { BRACE, EXTEND, GRIP, BREAK, RETRACT }
}
