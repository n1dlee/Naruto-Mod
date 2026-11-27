package com.sekwah.narutomod.network.c2s;

import com.sekwah.narutomod.events.SpiritProjectionEvents;
import com.sekwah.narutomod.util.SpiritProjectionPath.Input;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SpiritProjectionInputPacket(int session, Input input) {
    public static void encode(SpiritProjectionInputPacket command, FriendlyByteBuf buf) {
        buf.writeVarInt(command.session);
        buf.writeFloat(command.input.yaw());
        buf.writeByte(command.input.strafe());
        buf.writeByte(command.input.forward());
        buf.writeByte(command.input.vertical());
    }

    public static SpiritProjectionInputPacket decode(FriendlyByteBuf buf) {
        return new SpiritProjectionInputPacket(buf.readVarInt(), new Input(buf.readFloat(), buf.readByte(), buf.readByte(), buf.readByte()));
    }

    public static void handle(SpiritProjectionInputPacket command, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) {
                SpiritProjectionEvents.input(player, command.session(), command.input());
            }
        });
        context.setPacketHandled(true);
    }
}
