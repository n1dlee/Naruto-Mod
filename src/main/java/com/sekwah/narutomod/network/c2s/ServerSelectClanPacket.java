package com.sekwah.narutomod.network.c2s;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Sent when a player selects their clan for the first time.
 */
public class ServerSelectClanPacket {

    /** Derived from the one clan list, so the screen and the server cannot disagree. */
    private static final Set<String> VALID_CLANS = com.sekwah.narutomod.clan.NinjaClan.VALID_IDS;

    private final String clanId;

    public ServerSelectClanPacket(String clanId) {
        this.clanId = clanId;
    }

    public static void encode(ServerSelectClanPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.clanId);
    }

    public static ServerSelectClanPacket decode(FriendlyByteBuf buf) {
        return new ServerSelectClanPacket(buf.readUtf(32));
    }

    public static class Handler {
        public static void handle(ServerSelectClanPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;
                player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                    // Only allow selection if no clan chosen yet
                    if (ninjaData.getClanId().isEmpty() && VALID_CLANS.contains(msg.clanId)) {
                        ninjaData.setClanId(msg.clanId);
                    }
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
