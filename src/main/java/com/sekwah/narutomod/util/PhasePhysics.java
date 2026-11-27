package com.sekwah.narutomod.util;

import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class PhasePhysics {
    private static final Map<Player, Lease> FLAGS = Collections.synchronizedMap(new WeakHashMap<>());

    private PhasePhysics() {
    }

    public static void reconcile(Player player, boolean phase, boolean grantFlight) {
        if (player.isSpectator()) {
            FLAGS.remove(player);
            return;
        }
        var abilities = player.getAbilities();
        if (!phase && !grantFlight) {
            Lease saved = FLAGS.remove(player);
            if (saved != null) {
                player.noPhysics = saved.noPhysics();
                if (!player.isCreative()) {
                    update(player, saved.mayFly(), saved.flying());
                }
            }
            return;
        }
        Lease saved = FLAGS.computeIfAbsent(player, ignored -> new Lease(player.noPhysics, abilities.mayfly, abilities.flying));
        player.noPhysics = phase || saved.noPhysics();
        if (phase) {
            update(player, true, true);
            player.setOnGround(false);
            player.fallDistance = 0;
        } else {
            update(player, true, abilities.flying);
        }
    }

    private static void update(Player player, boolean mayFly, boolean flying) {
        var abilities = player.getAbilities();
        if (abilities.mayfly != mayFly || abilities.flying != flying) {
            abilities.mayfly = mayFly;
            abilities.flying = flying;
            if (!player.level().isClientSide) {
                player.onUpdateAbilities();
            }
        }
    }

    private record Lease(boolean noPhysics, boolean mayFly, boolean flying) {
    }
}
