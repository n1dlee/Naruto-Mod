package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.gui.ClientCooldownTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> client: a jutsu cooldown just started. Feeds the client-side cooldown HUD
 * (see ClientCooldownTracker) — the authoritative cooldown map is server-only state.
 */
public class ClientCooldownPacket {

    private final String abilityKey;
    private final int cooldownTicks;

    public ClientCooldownPacket(String abilityKey, int cooldownTicks) {
        this.abilityKey = abilityKey;
        this.cooldownTicks = cooldownTicks;
    }

    public static void encode(ClientCooldownPacket msg, FriendlyByteBuf buffer) {
        buffer.writeUtf(msg.abilityKey);
        buffer.writeVarInt(msg.cooldownTicks);
    }

    public static ClientCooldownPacket decode(FriendlyByteBuf buffer) {
        return new ClientCooldownPacket(buffer.readUtf(), buffer.readVarInt());
    }

    public static class Handler {
        public static void handle(ClientCooldownPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> ClientCooldownTracker.set(msg.abilityKey, msg.cooldownTicks));
            ctx.get().setPacketHandled(true);
        }
    }
}
