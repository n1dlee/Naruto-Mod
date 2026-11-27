package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.KamuiStepRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record KamuiStepPacket(ResourceLocation dimension, int actor, Phase phase, Vec3 from, Vec3 to,
                              float yaw, float height, int seed, boolean leftEye) {
    public enum Phase { PREPARE, ARRIVED, ABORTED }

    public boolean hasExit() {
        return this.phase == Phase.ARRIVED;
    }

    public static void encode(KamuiStepPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.actor);
        buffer.writeEnum(packet.phase);
        writePoint(buffer, packet.from);
        if (packet.hasExit()) {
            writePoint(buffer, packet.to);
        }
        buffer.writeFloat(packet.yaw);
        buffer.writeFloat(packet.height);
        buffer.writeInt(packet.seed);
        buffer.writeBoolean(packet.leftEye);
    }

    public static KamuiStepPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation dimension = buffer.readResourceLocation();
        int actor = buffer.readVarInt();
        Phase phase = buffer.readEnum(Phase.class);
        Vec3 from = readPoint(buffer);
        Vec3 to = phase == Phase.ARRIVED ? readPoint(buffer) : from;
        return new KamuiStepPacket(dimension, actor, phase, from, to,
                buffer.readFloat(), buffer.readFloat(), buffer.readInt(), buffer.readBoolean());
    }

    private static void writePoint(FriendlyByteBuf buffer, Vec3 point) {
        buffer.writeDouble(point.x);
        buffer.writeDouble(point.y);
        buffer.writeDouble(point.z);
    }

    private static Vec3 readPoint(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(KamuiStepPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> KamuiStepRenderer.add(packet)));
        context.setPacketHandled(true);
    }
}
