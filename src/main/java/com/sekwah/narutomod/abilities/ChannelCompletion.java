package com.sekwah.narutomod.abilities;

import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.ClientCooldownPacket;
import com.sekwah.narutomod.registries.NarutoRegistries;
import com.sekwah.narutomod.util.SharinganCopy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The one place a channelled jutsu is allowed to end.
 *
 * There were two exits and they disagreed about everything. A client-sent STOP ran the cast,
 * granted rank XP, offered the technique to watching Sharingan and registered a cooldown;
 * running out of chakra mid-channel ran the cast and registered nothing. Neither asked
 * whether the jutsu had actually done anything, and neither checked that the server had ever
 * ticked the channel at all.
 *
 * That produced three separate exploits:
 *
 *  - STOP immediately after START awarded rank XP for a jutsu that never ran. Chakra Charge
 *    has an empty performServer, so the whole cast was free XP at packet rate.
 *  - the same trick released a Fireball for nothing: the thirty-chakra base cost is charged
 *    from handleCost, which only runs on a server tick, while performServer spawns the
 *    projectile regardless.
 *  - letting chakra run out instead of releasing fired the technique with no cooldown. A
 *    Kirin charged to completion could strike without its forty-five second wait.
 *
 * The rule enforced here: a STOP packet is a statement of intent, not evidence that anything
 * happened. Only a channel the SERVER has ticked can produce a cast, and only a cast past the
 * ability's own commit threshold earns XP, a copy, or a cooldown.
 */
public final class ChannelCompletion {

    /** Why a channel ended. Both paths converge here so they cannot drift apart again. */
    public enum Reason {
        /** The player let go. */
        RELEASED,
        /** Chakra, stamina or the ability's own upkeep check gave out mid-channel. */
        EXHAUSTED
    }

    private ChannelCompletion() {
    }

    /**
     * Finishes a channel and applies exactly the consequences it earned.
     *
     * @param ticksChanneled how many ticks the SERVER ran the channel for. Zero means the
     *                       channel was started and stopped inside one tick, so nothing was
     *                       paid for and nothing is owed.
     * @return true if the technique actually cast
     */
    public static boolean finish(Player player, INinjaData ninjaData, Ability ability,
                                 int ticksChanneled, Reason reason) {
        if (ability == null) {
            return false;
        }
        // Never ticked server-side: no upkeep was charged, so no cast may be produced. This
        // single check is what closes the free-Fireball and free-XP holes, because both of
        // them depend on the release arriving before the server has had a chance to bill.
        if (ticksChanneled <= 0) {
            ninjaData.setCurrentlyChanneledAbility(player, null);
            return false;
        }

        ability.performServer(player, ninjaData, ticksChanneled);

        // The ability decides what counts as having gone far enough to matter. Below that
        // line a channel is an aborted attempt: it may still have had a visible effect, but
        // it does not pay out.
        boolean committed = ability.channelCommittedAt(ticksChanneled);
        if (committed) {
            ability.grantCastXp(ninjaData);
            consumeCopy(ninjaData, ability);
            registerCooldown(player, ninjaData, ability);
            if (player instanceof ServerPlayer serverPlayer) {
                NarutoRegistries.ABILITIES.getResourceKey(ability).ifPresent(key ->
                        SharinganCopy.onJutsuPerformed(serverPlayer, ability, key.location().getPath()));
            }
        }

        ninjaData.setCurrentlyChanneledAbility(player, null);
        return committed;
    }

    /**
     * A one-shot Sharingan copy is spent by USING it, however the cast was delivered.
     *
     * Only the instant path did this, so a copied Fireball released from a channel stayed in
     * the copy slot permanently and kept bypassing its fire-affinity requirement.
     */
    private static void consumeCopy(INinjaData ninjaData, Ability ability) {
        if (ability.isCopiedBySharingan(ninjaData)) {
            ninjaData.setCopiedJutsu("");
        }
    }

    /**
     * Registers the cooldown AND tells the client about it.
     *
     * The channel path used to register server-side only, so the HUD showed a technique as
     * ready while the server refused it - the player could not tell a bug from a miss.
     */
    private static void registerCooldown(Player player, INinjaData ninjaData, Ability ability) {
        if (!(ability instanceof Ability.Cooldown cooldownAbility)) {
            return;
        }
        String key = ability.getTranslationKey(ninjaData);
        cooldownAbility.registerCooldown(ninjaData, key);
        if (cooldownAbility.getCooldown() > 0 && player instanceof ServerPlayer serverPlayer) {
            PacketHandler.sendToPlayer(new ClientCooldownPacket(key, cooldownAbility.getCooldown()), serverPlayer);
        }
    }
}
