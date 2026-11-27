package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.GunbaiGuardRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record GunbaiGuardPacket(ResourceLocation dimension, Vec3 contact, Vec3 normal) {
    public static void encode(GunbaiGuardPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeDouble(packet.contact.x);
        buffer.writeDouble(packet.contact.y);
        buffer.writeDouble(packet.contact.z);
        buffer.writeDouble(packet.normal.x);
        buffer.writeDouble(packet.normal.y);
        buffer.writeDouble(packet.normal.z);
    }

    public static GunbaiGuardPacket decode(FriendlyByteBuf buffer) {
        return new GunbaiGuardPacket(buffer.readResourceLocation(),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }

    public static void handle(GunbaiGuardPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GunbaiGuardRenderer.add(packet)));
        context.setPacketHandled(true);
    }
}
