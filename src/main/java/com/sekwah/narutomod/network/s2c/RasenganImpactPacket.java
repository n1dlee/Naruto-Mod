package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.RasenganImpactRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RasenganImpactPacket(ResourceLocation dimension, Vec3 contact, Vec3 direction,
                                   float strength, int blockStateId, int seed) {
    public boolean hitsBlock() {
        return this.blockStateId >= 0;
    }

    public static void encode(RasenganImpactPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeDouble(packet.contact.x);
        buffer.writeDouble(packet.contact.y);
        buffer.writeDouble(packet.contact.z);
        buffer.writeFloat((float) packet.direction.x);
        buffer.writeFloat((float) packet.direction.y);
        buffer.writeFloat((float) packet.direction.z);
        buffer.writeFloat(packet.strength);
        buffer.writeVarInt(packet.blockStateId + 1);
        buffer.writeInt(packet.seed);
    }

    public static RasenganImpactPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation dimension = buffer.readResourceLocation();
        Vec3 contact = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        Vec3 direction = new Vec3(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        return new RasenganImpactPacket(dimension, contact, direction,
                buffer.readFloat(), buffer.readVarInt() - 1, buffer.readInt());
    }

    public static void handle(RasenganImpactPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> RasenganImpactRenderer.addImpact(packet)));
        context.setPacketHandled(true);
    }
}
