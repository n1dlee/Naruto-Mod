package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.NinkenCueRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record NinkenCuePacket(ResourceLocation dimension, int seed, Stage stage, Vec3 feet, Vec3 axis) {
    public enum Stage { SEAL, ARRIVE, SEND_HOME, SCENT }

    public static void encode(NinkenCuePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeInt(packet.seed);
        buffer.writeEnum(packet.stage);
        point(buffer, packet.feet);
        point(buffer, packet.axis);
    }

    public static NinkenCuePacket decode(FriendlyByteBuf buffer) {
        return new NinkenCuePacket(buffer.readResourceLocation(), buffer.readInt(), buffer.readEnum(Stage.class), point(buffer), point(buffer));
    }

    public static void handle(NinkenCuePacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NinkenCueRenderer.receive(packet)));
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
