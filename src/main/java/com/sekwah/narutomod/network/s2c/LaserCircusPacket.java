package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.LaserCircusRenderer;
import com.sekwah.narutomod.util.LaserCircusPath;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record LaserCircusPacket(ResourceLocation dimension, int caster, Stage stage, List<Vec3> points) {
    public static void encode(LaserCircusPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.caster);
        buffer.writeEnum(packet.stage);
        buffer.writeByte(packet.points.size());
        for (Vec3 point : packet.points) {
            buffer.writeDouble(point.x);
            buffer.writeDouble(point.y);
            buffer.writeDouble(point.z);
        }
    }

    public static LaserCircusPacket decode(FriendlyByteBuf buffer) {
        var dimension = buffer.readResourceLocation();
        int caster = buffer.readVarInt();
        Stage stage = buffer.readEnum(Stage.class);
        int count = buffer.readUnsignedByte();
        boolean single = stage == Stage.GATHER || stage == Stage.CANCEL;
        if (single ? count != 1 : count < 2 || count > LaserCircusPath.SAMPLES) {
            throw new IllegalArgumentException("Invalid Laser Circus point count " + count);
        }
        var points = new ArrayList<Vec3>(count);
        for (int i = 0; i < count; i++) {
            points.add(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
        }
        return new LaserCircusPacket(dimension, caster, stage, List.copyOf(points));
    }

    public static void handle(LaserCircusPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> LaserCircusRenderer.receive(packet)));
        context.setPacketHandled(true);
    }

    public enum Stage { GATHER, BEAM, BLOCK, HIT, CANCEL }
}
