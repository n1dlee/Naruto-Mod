package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.BaikaRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BaikaPacket(ResourceLocation dimension, int actor, float scale, Stage stage, Vec3 feet) {
    public enum Stage { GROW, STABLE, SHRINK, DENIED, STEP }

    public static void encode(BaikaPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.actor);
        buffer.writeFloat(packet.scale);
        buffer.writeEnum(packet.stage);
        buffer.writeDouble(packet.feet.x);
        buffer.writeDouble(packet.feet.y);
        buffer.writeDouble(packet.feet.z);
    }

    public static BaikaPacket decode(FriendlyByteBuf buffer) {
        return new BaikaPacket(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readFloat(),
                buffer.readEnum(Stage.class), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }

    public static void handle(BaikaPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BaikaRenderer.receive(packet)));
        context.setPacketHandled(true);
    }
}
