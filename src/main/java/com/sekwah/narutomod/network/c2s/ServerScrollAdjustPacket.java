package com.sekwah.narutomod.network.c2s;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Mouse-wheel size/power adjustment while a tiered transformation (Susanoo / Kurama Cloak)
 * or a held Rasengan is active. direction is +1 (scrolled up) or -1 (scrolled down).
 */
public class ServerScrollAdjustPacket {

    private final float direction;

    public ServerScrollAdjustPacket(float direction) {
        this.direction = direction;
    }

    public static void encode(ServerScrollAdjustPacket msg, FriendlyByteBuf outBuffer) {
        outBuffer.writeFloat(msg.direction);
    }

    public static ServerScrollAdjustPacket decode(FriendlyByteBuf inBuffer) {
        return new ServerScrollAdjustPacket(inBuffer.readFloat());
    }

    public static class Handler {
        public static void handle(ServerScrollAdjustPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) {
                    return;
                }
                player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                    if (ninjaData.isSusanooActive() || ninjaData.isKuramaCloakActive()) {
                        ninjaData.adjustTransformPower(msg.direction * 0.1f);
                    }
                    if (ninjaData.isRasenganHeld()) {
                        ninjaData.adjustRasenganCharge((int) (msg.direction * 4));
                    }
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
