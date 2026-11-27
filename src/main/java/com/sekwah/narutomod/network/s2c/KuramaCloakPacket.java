package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.KuramaClawRenderer;
import com.sekwah.narutomod.util.KuramaCloakMotion.Blow;
import com.sekwah.narutomod.util.KuramaCloakMotion.Strike;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record KuramaCloakPacket(ResourceLocation dimension, int actor, Phase phase, Blow blow) {
    public enum Phase { WINDUP, CONTACT, CANCEL }

    public static void encode(KuramaCloakPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.dimension);
        buf.writeVarInt(packet.actor);
        buf.writeEnum(packet.phase);
        buf.writeEnum(packet.blow.kind());
        buf.writeDouble(packet.blow.feet().x);
        buf.writeDouble(packet.blow.feet().y);
        buf.writeDouble(packet.blow.feet().z);
        buf.writeFloat(packet.blow.yaw());
        buf.writeBoolean(packet.blow.giant());
    }

    public static KuramaCloakPacket decode(FriendlyByteBuf buf) {
        var dimension = buf.readResourceLocation();
        int actor = buf.readVarInt();
        Phase phase = buf.readEnum(Phase.class);
        Strike kind = buf.readEnum(Strike.class);
        Vec3 feet = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new KuramaCloakPacket(dimension, actor, phase, new Blow(kind, feet, buf.readFloat(), buf.readBoolean()));
    }

    public static void handle(KuramaCloakPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> KuramaClawRenderer.receive(packet)));
        context.setPacketHandled(true);
    }
}
