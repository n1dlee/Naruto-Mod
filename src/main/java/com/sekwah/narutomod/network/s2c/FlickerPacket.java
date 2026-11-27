package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.FlickerRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record FlickerPacket(ResourceLocation dimension, int actor, Vec3 from, Vec3 to, float height, int seed) {
    public boolean gathering() {
        return this.from.equals(this.to);
    }

    public static void encode(FlickerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.actor);
        point(buffer, packet.from);
        point(buffer, packet.to);
        buffer.writeFloat(packet.height);
        buffer.writeInt(packet.seed);
    }

    public static FlickerPacket decode(FriendlyByteBuf buffer) {
        return new FlickerPacket(buffer.readResourceLocation(), buffer.readVarInt(), point(buffer), point(buffer),
                buffer.readFloat(), buffer.readInt());
    }

    private static void point(FriendlyByteBuf buffer, Vec3 point) {
        buffer.writeDouble(point.x);
        buffer.writeDouble(point.y);
        buffer.writeDouble(point.z);
    }

    private static Vec3 point(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(FlickerPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> FlickerRenderer.add(packet)));
        context.setPacketHandled(true);
    }
}
