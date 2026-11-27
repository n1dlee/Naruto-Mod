package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.CrowFlockRenderer;
import com.sekwah.narutomod.util.CrowFlock;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public record CrowFlockPacket(ResourceLocation dimension, int id, int viewer, Vec3 origin, float yaw, int age,
                              List<Integer> limits) {
    public CrowFlockPacket {
        limits = List.copyOf(limits);
        if (viewer == 0 && limits.size() != CrowFlock.LANES || viewer != 0 && !limits.isEmpty()) {
            throw new IllegalArgumentException("Invalid crow flock lanes");
        }
    }

    public boolean privateVision() {
        return this.viewer != 0;
    }

    public double limit(int lane) {
        return this.limits.get(lane) / 100.0;
    }

    public static void encode(CrowFlockPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.id);
        buffer.writeVarInt(packet.viewer);
        buffer.writeDouble(packet.origin.x);
        buffer.writeDouble(packet.origin.y);
        buffer.writeDouble(packet.origin.z);
        buffer.writeFloat(packet.yaw);
        buffer.writeByte(packet.age);
        for (int limit : packet.limits) {
            buffer.writeShort(limit);
        }
    }

    public static CrowFlockPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation dimension = buffer.readResourceLocation();
        int id = buffer.readVarInt();
        int viewer = buffer.readVarInt();
        Vec3 origin = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        float yaw = buffer.readFloat();
        int age = buffer.readUnsignedByte();
        List<Integer> limits = viewer == 0 ? IntStream.range(0, CrowFlock.LANES)
                .mapToObj(ignored -> buffer.readUnsignedShort()).toList() : List.of();
        return new CrowFlockPacket(dimension, id, viewer, origin, yaw, age, limits);
    }

    public static void handle(CrowFlockPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CrowFlockRenderer.add(packet)));
        context.setPacketHandled(true);
    }
}
