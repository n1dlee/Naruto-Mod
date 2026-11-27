package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.WoodBurialWaveRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record WoodBurialWavePacket(ResourceLocation dimension, int seed, Mode mode, boolean heavy, List<Vec3> route) {
    public enum Mode { TRACE, BURST }

    public static void encode(WoodBurialWavePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeInt(packet.seed);
        buffer.writeEnum(packet.mode);
        buffer.writeBoolean(packet.heavy);
        buffer.writeByte(packet.route.size());
        for (Vec3 point : packet.route) {
            writePoint(buffer, point);
        }
    }

    public static WoodBurialWavePacket decode(FriendlyByteBuf buffer) {
        ResourceLocation dimension = buffer.readResourceLocation();
        int seed = buffer.readInt();
        Mode mode = buffer.readEnum(Mode.class);
        boolean heavy = buffer.readBoolean();
        int count = buffer.readUnsignedByte();
        if (count < 2 || count > 5) {
            throw new IllegalArgumentException("Invalid Mokuton route length " + count);
        }
        var samples = new ArrayList<Vec3>(count);
        for (int i = 0; i < count; i++) {
            samples.add(readPoint(buffer));
        }
        return new WoodBurialWavePacket(dimension, seed, mode, heavy, List.copyOf(samples));
    }

    public static void handle(WoodBurialWavePacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> WoodBurialWaveRenderer.receive(packet)));
        context.setPacketHandled(true);
    }

    private static void writePoint(FriendlyByteBuf buffer, Vec3 value) {
        buffer.writeDouble(value.x);
        buffer.writeDouble(value.y);
        buffer.writeDouble(value.z);
    }

    private static Vec3 readPoint(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
