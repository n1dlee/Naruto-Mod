package com.sekwah.narutomod.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * Who is on whose side.
 *
 * Every minion in this mod grew its own answer to that question, and each one only knew about
 * its own class: a summon skipped other summons, a puppet skipped other puppets, a clone
 * skipped nothing at all. So a player who called out Gamabunta, laid out five puppets and
 * made twenty shadow clones had an army that immediately attacked itself - the toad hunted
 * the puppets, the puppets hunted the clones, and none of it was wrong by its own rules.
 *
 * There is one question here and one place that answers it: two things are on the same side
 * when they share an owner, or when one of them IS the other's owner. Adding a new kind of
 * minion means teaching {@link #ownerOf} about it, and every existing filter keeps working.
 */
public final class Faction {

    private Faction() {
    }

    /**
     * The player an entity ultimately belongs to.
     *
     * A player owns themselves, which is what makes "does the summoner count as an ally of
     * their own summon" fall out of the same comparison instead of needing a special case.
     */
    public static Optional<UUID> ownerOf(Entity entity) {
        if (entity instanceof Player player) {
            return Optional.of(player.getUUID());
        }
        if (entity instanceof com.sekwah.narutomod.entity.SummonBeastEntity summon) {
            return summon.getOwnerUUID();
        }
        if (entity instanceof com.sekwah.narutomod.entity.PuppetEntity puppet) {
            return puppet.getOwnerUUID();
        }
        if (entity instanceof com.sekwah.narutomod.entity.ShadowCloneEntity clone) {
            return clone.getOwnerUUID();
        }
        if (entity instanceof com.sekwah.narutomod.entity.WoodGolemEntity golem) {
            return golem.getOwnerUUID();
        }
        return Optional.empty();
    }

    /**
     * True when these two must never fight.
     *
     * Two ownerless entities are NOT allies. Wild mobs share "no owner" and treating that as
     * a faction would make every zombie in the world friendly to every other one, which is a
     * much worse bug than the one this class exists to fix.
     */
    public static boolean sameSide(Entity a, Entity b) {
        if (a == null || b == null) {
            return false;
        }
        if (a == b) {
            return true;
        }
        Optional<UUID> ownerA = ownerOf(a);
        Optional<UUID> ownerB = ownerOf(b);
        return ownerA.isPresent() && ownerA.equals(ownerB);
    }

    /** Convenience for target filters: something worth attacking is alive and not an ally. */
    public static boolean isHostileTo(Entity self, LivingEntity candidate) {
        return candidate != null && candidate.isAlive() && !sameSide(self, candidate);
    }
}
