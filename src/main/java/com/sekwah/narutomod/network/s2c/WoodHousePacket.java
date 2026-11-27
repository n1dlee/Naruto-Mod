package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.WoodHouseRenderer;
import com.sekwah.narutomod.util.WoodHouseLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record WoodHousePacket(ResourceLocation dimension, BlockPos corner, Vec3i size, Stage stage) {
    public static void encode(WoodHousePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeBlockPos(packet.corner);
        buffer.writeByte(packet.size.getX());
        buffer.writeByte(packet.size.getY());
        buffer.writeByte(packet.size.getZ());
        buffer.writeEnum(packet.stage);
    }

    public static WoodHousePacket decode(FriendlyByteBuf buffer) {
        var dimension = buffer.readResourceLocation();
        var corner = buffer.readBlockPos();
        var size = new Vec3i(buffer.readUnsignedByte(), buffer.readUnsignedByte(), buffer.readUnsignedByte());
        if (!WoodHouseLayout.allowedSize(size)) {
            throw new IllegalArgumentException("Invalid Mokuton house volume " + size);
        }
        return new WoodHousePacket(dimension, corner, size, buffer.readEnum(Stage.class));
    }

    public static void handle(WoodHousePacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> WoodHouseRenderer.receive(packet)));
        context.setPacketHandled(true);
    }

    public enum Stage { SURVEY, REVEAL, CANCEL }
}
