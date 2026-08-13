package com.sekwah.narutomod.network.c2s;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * The player picked a Susanoo tint on the progression screen.
 *
 * Validated server-side rather than trusted: only someone who has actually awakened a
 * Mangekyo has a Susanoo to paint, and a client that says otherwise is ignored.
 */
public class ServerSusanooColorPacket {

    /** Packed 0xRRGGBB, or -1 to fall back to the wielder's canon form colour. */
    private final int packedRgb;

    public ServerSusanooColorPacket(int packedRgb) {
        this.packedRgb = packedRgb;
    }

    public static void encode(ServerSusanooColorPacket msg, FriendlyByteBuf outBuffer) {
        outBuffer.writeInt(msg.packedRgb);
    }

    public static ServerSusanooColorPacket decode(FriendlyByteBuf inBuffer) {
        return new ServerSusanooColorPacket(inBuffer.readInt());
    }

    public static class Handler {
        public static void handle(ServerSusanooColorPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) {
                    return;
                }
                player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                    if (!ninjaData.isNinjaModeEnabled() || !ninjaData.isMangekyoAwakened()) {
                        return;
                    }
                    // -1 means "canon"; anything else is clamped into a real 24-bit colour so
                    // a malformed value cannot produce a negative tint in the renderer.
                    ninjaData.setSusanooColor(msg.packedRgb < 0 ? -1 : (msg.packedRgb & 0xFFFFFF));
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
