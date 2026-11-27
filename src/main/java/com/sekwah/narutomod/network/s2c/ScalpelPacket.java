package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.ScalpelRenderer;
import com.sekwah.narutomod.util.ScalpelEdge.Phase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ScalpelPacket(ResourceLocation dimension, int actor, Phase phase, Vec3 palm, Vec3 point, Vec3 axis,
                            boolean right) {
    public static void encode(ScalpelPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.actor);
        buffer.writeEnum(packet.phase);
        writePoint(buffer, packet.palm);
        writePoint(buffer, packet.point);
        writePoint(buffer, packet.axis);
        buffer.writeBoolean(packet.right);
    }

    public static ScalpelPacket decode(FriendlyByteBuf buffer) {
        return new ScalpelPacket(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readEnum(Phase.class),
                readPoint(buffer), readPoint(buffer), readPoint(buffer), buffer.readBoolean());
    }

    public static void handle(ScalpelPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ScalpelRenderer.receive(packet)));
        context.setPacketHandled(true);
    }

    private static void writePoint(FriendlyByteBuf buffer, Vec3 p) {
        buffer.writeDouble(p.x);
        buffer.writeDouble(p.y);
        buffer.writeDouble(p.z);
    }

    private static Vec3 readPoint(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
