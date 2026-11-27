package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.KaitenRenderer;
import com.sekwah.narutomod.util.KaitenDome.Step;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record KaitenPacket(ResourceLocation dimension, int entity, Step step, Vec3 center, Vec3 contact) {
    public static void encode(KaitenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.entity);
        buffer.writeEnum(packet.step);
        writePoint(buffer, packet.center);
        writePoint(buffer, packet.contact);
    }

    public static KaitenPacket decode(FriendlyByteBuf buffer) {
        return new KaitenPacket(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readEnum(Step.class),
                readPoint(buffer), readPoint(buffer));
    }

    public static void handle(KaitenPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> KaitenRenderer.receive(packet)));
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
}
