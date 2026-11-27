package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.KamuiPhaseRenderer;
import com.sekwah.narutomod.util.KamuiPhaseWindow.Stage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record KamuiPhasePacket(ResourceLocation dimension, int actor, Stage stage, int age, Vec3 contact, Vec3 normal) {
    public static void encode(KamuiPhasePacket event, FriendlyByteBuf buf) {
        buf.writeResourceLocation(event.dimension);
        buf.writeVarInt(event.actor);
        buf.writeEnum(event.stage);
        buf.writeVarInt(event.age);
        point(buf, event.contact);
        point(buf, event.normal);
    }

    public static KamuiPhasePacket decode(FriendlyByteBuf buf) {
        return new KamuiPhasePacket(buf.readResourceLocation(), buf.readVarInt(), buf.readEnum(Stage.class),
                buf.readVarInt(), point(buf), point(buf));
    }

    private static Vec3 point(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    private static void point(FriendlyByteBuf buf, Vec3 point) {
        buf.writeDouble(point.x);
        buf.writeDouble(point.y);
        buf.writeDouble(point.z);
    }

    public static void handle(KamuiPhasePacket event, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> KamuiPhaseRenderer.receive(event)));
        context.setPacketHandled(true);
    }
}
