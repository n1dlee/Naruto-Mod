package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.SummonSealRenderer;
import com.sekwah.narutomod.util.SummoningCircle.Stage;
import com.sekwah.narutomod.util.SummoningCircle.Style;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SummonSealPacket(ResourceLocation dimension, int owner, Style style, Stage stage, Vec3 center, float span) {
    public static void encode(SummonSealPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.owner);
        buffer.writeEnum(packet.style);
        buffer.writeEnum(packet.stage);
        buffer.writeDouble(packet.center.x);
        buffer.writeDouble(packet.center.y);
        buffer.writeDouble(packet.center.z);
        buffer.writeFloat(packet.span);
    }

    public static SummonSealPacket decode(FriendlyByteBuf buffer) {
        return new SummonSealPacket(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readEnum(Style.class),
                buffer.readEnum(Stage.class), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readFloat());
    }

    public static void handle(SummonSealPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SummonSealRenderer.receive(packet)));
        context.setPacketHandled(true);
    }
}
