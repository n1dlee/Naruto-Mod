package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.ShinraWaveRenderer;
import com.sekwah.narutomod.util.ShinraWave.Mode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ShinraWavePacket(ResourceLocation dimension, int caster, Mode mode, Vec3 center) {
    public static void encode(ShinraWavePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.caster);
        buffer.writeEnum(packet.mode);
        buffer.writeDouble(packet.center.x);
        buffer.writeDouble(packet.center.y);
        buffer.writeDouble(packet.center.z);
    }

    public static ShinraWavePacket decode(FriendlyByteBuf buffer) {
        return new ShinraWavePacket(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readEnum(Mode.class),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }

    public static void handle(ShinraWavePacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ShinraWaveRenderer.receive(packet)));
        context.setPacketHandled(true);
    }
}
