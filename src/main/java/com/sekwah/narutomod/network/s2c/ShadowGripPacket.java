package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.ShadowGripRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ShadowGripPacket(ResourceLocation dimension, int id, int target, Step step, Vec3 feet, float height) {
    public enum Step { LIFT, CLENCH, HOLD, DROP }

    public static void encode(ShadowGripPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.dimension);
        buf.writeVarInt(packet.id);
        buf.writeVarInt(packet.target);
        buf.writeEnum(packet.step);
        buf.writeDouble(packet.feet.x);
        buf.writeDouble(packet.feet.y);
        buf.writeDouble(packet.feet.z);
        buf.writeFloat(packet.height);
    }

    public static ShadowGripPacket decode(FriendlyByteBuf buf) {
        return new ShadowGripPacket(buf.readResourceLocation(), buf.readVarInt(), buf.readVarInt(), buf.readEnum(Step.class),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readFloat());
    }

    public static void handle(ShadowGripPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ShadowGripRenderer.receive(packet)));
        context.setPacketHandled(true);
    }
}
