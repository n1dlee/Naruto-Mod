package com.sekwah.narutomod.network.c2s;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerWallWalkDetachPacket {

    public static void encode(ServerWallWalkDetachPacket msg, FriendlyByteBuf outBuffer) {
    }

    public static ServerWallWalkDetachPacket decode(FriendlyByteBuf inBuffer) {
        return new ServerWallWalkDetachPacket();
    }

    public static class Handler {
        public static void handle(ServerWallWalkDetachPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) {
                    return;
                }
                player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                    if (ninjaData.isWallWalkAttached()) {
                        // Restore gravity before detaching
                        player.setNoGravity(false);
                        ninjaData.setWallWalkDetachTicks(14);
                        // Give a small upward boost when jumping off wall
                        player.resetFallDistance();
                        var vel = player.getDeltaMovement();
                        player.setDeltaMovement(vel.x, 0.35, vel.z);
                    }
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
