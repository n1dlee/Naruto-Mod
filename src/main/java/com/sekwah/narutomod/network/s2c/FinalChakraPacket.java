package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.FinalChakraRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record FinalChakraPacket(ResourceLocation dimension, int subject, Phase phase, Shape shape, long started, float yaw) {
    public enum Phase { SWORD, SWORD_STOP, SHELL_CRACK, SHELL_BREAK, YASAKA_GATHER, YASAKA_RELEASE, YASAKA_HIT,
        KCM_ENTER, KCM_EXIT, BIJU_CHARGE_BREAK, BIJU_FIRE, BIJU_HIT, YASAKA_STOP }

    public FinalChakraPacket(ResourceLocation dimension, int subject, Phase phase, Shape shape) {
        this(dimension, subject, phase, shape, -1, 0);
    }

    public record Shape(Vec3 point, Vec3 axis, float size, int tint) {
    }

    public static void encode(FinalChakraPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.dimension);
        buf.writeVarInt(packet.subject);
        buf.writeEnum(packet.phase);
        write(buf, packet.shape.point());
        write(buf, packet.shape.axis());
        buf.writeFloat(packet.shape.size());
        buf.writeInt(packet.shape.tint());
        buf.writeLong(packet.started);
        buf.writeFloat(packet.yaw);
    }

    public static FinalChakraPacket decode(FriendlyByteBuf buf) {
        return new FinalChakraPacket(buf.readResourceLocation(), buf.readVarInt(), buf.readEnum(Phase.class),
                new Shape(read(buf), read(buf), buf.readFloat(), buf.readInt()), buf.readLong(), buf.readFloat());
    }

    private static void write(FriendlyByteBuf buf, Vec3 at) {
        buf.writeDouble(at.x);
        buf.writeDouble(at.y);
        buf.writeDouble(at.z);
    }

    private static Vec3 read(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(FinalChakraPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FinalChakraRenderer.receive(packet)));
        context.setPacketHandled(true);
    }
}
