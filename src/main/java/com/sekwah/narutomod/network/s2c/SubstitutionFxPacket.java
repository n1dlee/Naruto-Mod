package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.SubstitutionFxRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SubstitutionFxPacket(ResourceLocation dimension, Vec3 from, Vec3 to, int seed) {
    public static void encode(SubstitutionFxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        write(buffer, packet.from);
        write(buffer, packet.to);
        buffer.writeInt(packet.seed);
    }

    public static SubstitutionFxPacket decode(FriendlyByteBuf buffer) {
        return new SubstitutionFxPacket(buffer.readResourceLocation(), read(buffer), read(buffer), buffer.readInt());
    }

    private static void write(FriendlyByteBuf buffer, Vec3 point) {
        buffer.writeDouble(point.x);
        buffer.writeDouble(point.y);
        buffer.writeDouble(point.z);
    }

    private static Vec3 read(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(SubstitutionFxPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> SubstitutionFxRenderer.add(packet)));
        context.setPacketHandled(true);
    }
}
