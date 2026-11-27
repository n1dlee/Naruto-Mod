package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.LeapFxRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record LeapFxPacket(ResourceLocation dimension, int actor, Phase phase, Vec3 point,
                           Vec3 forward, int material, float strength) {
    public enum Phase {
        BRACE, TAKEOFF, LAND, WATER, CANCEL, BOOST
    }

    public static void encode(LeapFxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.actor);
        buffer.writeEnum(packet.phase);
        buffer.writeDouble(packet.point.x);
        buffer.writeDouble(packet.point.y);
        buffer.writeDouble(packet.point.z);
        buffer.writeFloat((float) packet.forward.x);
        buffer.writeFloat((float) packet.forward.z);
        buffer.writeVarInt(packet.material);
        buffer.writeFloat(packet.strength);
    }

    public static LeapFxPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation dimension = buffer.readResourceLocation();
        int actor = buffer.readVarInt();
        Phase phase = buffer.readEnum(Phase.class);
        Vec3 point = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        Vec3 forward = new Vec3(buffer.readFloat(), 0, buffer.readFloat());
        return new LeapFxPacket(dimension, actor, phase, point, forward, buffer.readVarInt(), buffer.readFloat());
    }

    public static void handle(LeapFxPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> LeapFxRenderer.accept(packet)));
        context.setPacketHandled(true);
    }
}
