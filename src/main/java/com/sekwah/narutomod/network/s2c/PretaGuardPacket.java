package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.PretaGuardRenderer;
import com.sekwah.narutomod.util.PretaReservoir.Stage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PretaGuardPacket(ResourceLocation dimension, int actor, Stage stage, Vec3 center, Vec3 contact, int tint) {
    public static void encode(PretaGuardPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.actor);
        buffer.writeEnum(packet.stage);
        point(buffer, packet.center);
        point(buffer, packet.contact);
        buffer.writeInt(packet.tint);
    }

    public static PretaGuardPacket decode(FriendlyByteBuf buffer) {
        return new PretaGuardPacket(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readEnum(Stage.class),
                point(buffer), point(buffer), buffer.readInt());
    }

    public static void handle(PretaGuardPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PretaGuardRenderer.receive(packet)));
        context.setPacketHandled(true);
    }

    private static void point(FriendlyByteBuf buffer, Vec3 point) {
        buffer.writeDouble(point.x);
        buffer.writeDouble(point.y);
        buffer.writeDouble(point.z);
    }

    private static Vec3 point(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
