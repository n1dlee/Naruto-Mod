package com.sekwah.narutomod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Client-only reader for the local player's live movement keys.
 *
 * Kept in its own class so the common-side ability code can reach it through DistExecutor
 * without ever naming a client class in a method a dedicated server might verify.
 */
public final class ClientInputAccess {

    private ClientInputAccess() {
    }

    /** @return {strafe, forward}, each roughly -1..1, or zeroes when there is no local player */
    public static float[] currentMoveInput() {
        LocalPlayer local = Minecraft.getInstance().player;
        if (local == null) {
            return new float[]{0f, 0f};
        }
        return new float[]{local.input.leftImpulse, local.input.forwardImpulse};
    }
}
