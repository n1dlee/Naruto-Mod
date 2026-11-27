package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.ChidoriImpactRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ChidoriImpactPacket(ResourceLocation dimension, Vec3 point, Vec3 direction, Kind kind, int seed) {
    public enum Kind {
        ENTITY, BLOCK, MISS
    }

    public static void encode(ChidoriImpactPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeDouble(packet.point.x);
        buffer.writeDouble(packet.point.y);
        buffer.writeDouble(packet.point.z);
        buffer.writeFloat((float) packet.direction.x);
        buffer.writeFloat((float) packet.direction.y);
        buffer.writeFloat((float) packet.direction.z);
        buffer.writeEnum(packet.kind);
        buffer.writeInt(packet.seed);
    }

    public static ChidoriImpactPacket decode(FriendlyByteBuf buffer) {
        var dimension = buffer.readResourceLocation();
        var point = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        var direction = new Vec3(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        return new ChidoriImpactPacket(dimension, point, direction, buffer.readEnum(Kind.class), buffer.readInt());
    }

    public static void handle(ChidoriImpactPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ChidoriImpactRenderer.addImpact(packet)));
        context.setPacketHandled(true);
    }
}
