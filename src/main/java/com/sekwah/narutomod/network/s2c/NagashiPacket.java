package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.NagashiRenderer;
import com.sekwah.narutomod.util.NagashiWave;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record NagashiPacket(ResourceLocation dimension, int actor, Phase phase, Vec3 point,
                            float radius, List<Float> reaches, int seed) {
    public enum Phase {
        BRACE, FRONT, HIT
    }

    public static void encode(NagashiPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.actor);
        buffer.writeEnum(packet.phase);
        buffer.writeDouble(packet.point.x);
        buffer.writeDouble(packet.point.y);
        buffer.writeDouble(packet.point.z);
        buffer.writeFloat(packet.radius);
        if (packet.phase == Phase.FRONT) {
            packet.reaches.forEach(buffer::writeFloat);
        }
        buffer.writeInt(packet.seed);
    }

    public static NagashiPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation dimension = buffer.readResourceLocation();
        int actor = buffer.readVarInt();
        Phase phase = buffer.readEnum(Phase.class);
        Vec3 point = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        float radius = buffer.readFloat();
        List<Float> reaches = List.of();
        if (phase == Phase.FRONT) {
            List<Float> values = new ArrayList<>(NagashiWave.SPOKES);
            for (int i = 0; i < NagashiWave.SPOKES; i++) {
                values.add(buffer.readFloat());
            }
            reaches = List.copyOf(values);
        }
        return new NagashiPacket(dimension, actor, phase, point, radius, reaches, buffer.readInt());
    }

    public static void handle(NagashiPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> NagashiRenderer.accept(packet)));
        context.setPacketHandled(true);
    }
}
