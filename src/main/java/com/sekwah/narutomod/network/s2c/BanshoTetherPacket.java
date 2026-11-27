package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.BanshoTetherRenderer;
import com.sekwah.narutomod.util.BanshoPull.Stage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BanshoTetherPacket(ResourceLocation dimension, int id, Stage stage, Vec3 hand, Vec3 target) {
    public static void encode(BanshoTetherPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.id);
        buffer.writeEnum(packet.stage);
        point(buffer, packet.hand);
        point(buffer, packet.target);
    }

    public static BanshoTetherPacket decode(FriendlyByteBuf buffer) {
        return new BanshoTetherPacket(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readEnum(Stage.class),
                point(buffer), point(buffer));
    }

    public static void handle(BanshoTetherPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BanshoTetherRenderer.receive(packet)));
        context.setPacketHandled(true);
    }

    private static void point(FriendlyByteBuf buffer, Vec3 value) {
        buffer.writeDouble(value.x);
        buffer.writeDouble(value.y);
        buffer.writeDouble(value.z);
    }

    private static Vec3 point(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
