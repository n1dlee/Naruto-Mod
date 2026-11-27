package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.ChidoriDashRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ChidoriDashPacket(ResourceLocation dimension, int actor, Phase phase, Vec3 start, Vec3 end, Vec3 heading) {
    public enum Phase {
        BRACE, TRAVEL, STRIKE, BLOCK, RECOVER
    }

    public static void encode(ChidoriDashPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.actor);
        buffer.writeEnum(packet.phase);
        writePoint(buffer, packet.start);
        writePoint(buffer, packet.end);
        buffer.writeFloat((float) packet.heading.x);
        buffer.writeFloat((float) packet.heading.z);
    }

    public static ChidoriDashPacket decode(FriendlyByteBuf buffer) {
        return new ChidoriDashPacket(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readEnum(Phase.class),
                readPoint(buffer), readPoint(buffer), new Vec3(buffer.readFloat(), 0, buffer.readFloat()));
    }

    private static void writePoint(FriendlyByteBuf buffer, Vec3 point) {
        buffer.writeDouble(point.x);
        buffer.writeDouble(point.y);
        buffer.writeDouble(point.z);
    }

    private static Vec3 readPoint(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(ChidoriDashPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ChidoriDashRenderer.accept(packet)));
        context.setPacketHandled(true);
    }
}
