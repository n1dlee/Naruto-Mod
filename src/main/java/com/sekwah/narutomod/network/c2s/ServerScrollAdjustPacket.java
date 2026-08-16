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

    /**
     * Normalises the wire value to exactly -1 or +1.
     *
     * The field is only ever meant to carry the sign of one scroll notch, but it was read as
     * a raw float and passed straight into the transformation economy. A client sending NaN
     * poisoned transformPower, and from there the chakra and Kurama-bond arithmetic: every
     * {@code chakra < cost} comparison against a NaN silently becomes false, so drains stop
     * working and the corrupted value is then written to the player's save.
     *
     * Signum rather than a clamp on purpose - a clamp still lets 0.999 through, and there is
     * no legitimate scroll input that is not one whole notch in one direction.
     */
    public static ServerScrollAdjustPacket decode(FriendlyByteBuf inBuffer) {
        float raw = inBuffer.readFloat();
        return new ServerScrollAdjustPacket(Float.isFinite(raw) ? Math.signum(raw) : 0f);
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
