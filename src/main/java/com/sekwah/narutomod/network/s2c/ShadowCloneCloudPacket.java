package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.ShadowCloneCloudRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record ShadowCloneCloudPacket(ResourceLocation dimension, Beat beat, List<Vec3> sites) {
    public enum Beat { SEAL, MASS_SEAL, FORM, MASS_FORM, DISPEL }

    public ShadowCloneCloudPacket {
        sites = List.copyOf(sites);
        if (sites.isEmpty() || sites.size() > 5) {
            throw new IllegalArgumentException("Clone cloud requires 1–5 points");
        }
    }

    public static void encode(ShadowCloneCloudPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.dimension());
        buf.writeEnum(packet.beat());
        buf.writeByte(packet.sites().size());
        for (Vec3 at : packet.sites()) {
            buf.writeDouble(at.x);
            buf.writeDouble(at.y);
            buf.writeDouble(at.z);
        }
    }

    public static ShadowCloneCloudPacket decode(FriendlyByteBuf buf) {
        var dimension = buf.readResourceLocation();
        Beat beat = buf.readEnum(Beat.class);
        int count = buf.readUnsignedByte();
        if (count < 1 || count > 5) {
            throw new IllegalArgumentException("Invalid clone cloud size: " + count);
        }
        List<Vec3> sites = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            sites.add(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
        }
        return new ShadowCloneCloudPacket(dimension, beat, sites);
    }

    public static void handle(ShadowCloneCloudPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ShadowCloneCloudRenderer.receive(packet)));
        context.setPacketHandled(true);
    }
}
