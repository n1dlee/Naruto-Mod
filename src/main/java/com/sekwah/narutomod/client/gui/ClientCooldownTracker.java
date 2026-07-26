package com.sekwah.narutomod.client.gui;

import net.minecraft.client.Minecraft;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client-side mirror of jutsu cooldowns for the HUD. The authoritative map lives
 * server-side in NinjaData and isn't capability-synced, so the server pushes a small
 * ClientCooldownPacket whenever a cooldown is registered and this tracker converts it
 * to a game-time expiry the HUD can count down from.
 */
public class ClientCooldownTracker {

    /** ability translation key -> game time (ticks) when the cooldown expires */
    private static final Map<String, Long> EXPIRY = new LinkedHashMap<>();

    public static void set(String abilityKey, int cooldownTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        EXPIRY.put(abilityKey, mc.level.getGameTime() + cooldownTicks);
    }

    /** Live view of active cooldowns as remaining ticks; expired entries are pruned. */
    public static Map<String, Integer> activeCooldowns() {
        Minecraft mc = Minecraft.getInstance();
        Map<String, Integer> remaining = new LinkedHashMap<>();
        if (mc.level == null) {
            EXPIRY.clear();
            return remaining;
        }
        long now = mc.level.getGameTime();
        EXPIRY.entrySet().removeIf(entry -> entry.getValue() <= now);
        EXPIRY.forEach((key, expiry) -> remaining.put(key, (int) (expiry - now)));
        return remaining;
    }
}
