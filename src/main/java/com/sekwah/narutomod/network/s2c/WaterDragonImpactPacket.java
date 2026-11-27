package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.WaterDragonImpactRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record WaterDragonImpactPacket(ResourceLocation dimension, Vec3 point, Vec3 axis, Kind kind, int seed) {
    public enum Kind {
        BLOCK, ENTITY, DISSIPATE
    }

    public static void encode(WaterDragonImpactPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeDouble(packet.point.x);
        buffer.writeDouble(packet.point.y);
        buffer.writeDouble(packet.point.z);
        buffer.writeFloat((float) packet.axis.x);
        buffer.writeFloat((float) packet.axis.y);
        buffer.writeFloat((float) packet.axis.z);
        buffer.writeEnum(packet.kind);
        buffer.writeInt(packet.seed);
    }

    public static WaterDragonImpactPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation dimension = buffer.readResourceLocation();
        Vec3 point = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        Vec3 axis = new Vec3(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        return new WaterDragonImpactPacket(dimension, point, axis, buffer.readEnum(Kind.class), buffer.readInt());
    }

    public static void handle(WaterDragonImpactPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> WaterDragonImpactRenderer.add(packet)));
        context.setPacketHandled(true);
    }
}
