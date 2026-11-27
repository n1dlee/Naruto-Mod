package com.sekwah.narutomod.events;

import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.BodyRitePacket;
import com.sekwah.narutomod.network.s2c.BodyRitePacket.Phase;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public final class BodyRiteEffects {
    private BodyRiteEffects() {
    }

    public static long clock(Player player) {
        return player instanceof ServerPlayer actor ? actor.server.overworld().getGameTime() : player.level().getGameTime();
    }

    public static void emit(Player player, Phase phase, int strength, boolean audible) {
        if (!(player instanceof ServerPlayer actor)) {
            return;
        }
        var level = actor.serverLevel();
        var at = actor.position();
        var packet = new BodyRitePacket(level.dimension().location(), actor.getId(), phase, strength, at);
        if (actor.isInvisible()) {
            PacketHandler.sendToPlayer(packet, actor);
            return;
        }
        PacketHandler.sendNear(packet, level, at, 48);
        if (audible) {
            SoundEvent sound = sound(phase);
            if (sound != null) {
                float pitch = phase == Phase.GATE_OPEN ? 1.28f - strength * 0.075f : 1.0f;
                level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, 0.77f, pitch);
            }
        }
    }

    private static SoundEvent sound(Phase phase) {
        return switch (phase) {
            case SAGE_DRAW, SAGE_BALANCE -> NarutoSounds.RITE_GATHER.get();
            case SAGE_ENTER -> NarutoSounds.RITE_BALANCE.get();
            case SAGE_STONE -> NarutoSounds.RITE_STONE.get();
            case SAGE_BREAK, SAGE_EXIT, GATE_CLOSE, GATE_SPENT -> NarutoSounds.RITE_RELEASE.get();
            case SAGE_OVERFLOW, GATE_WARNING -> NarutoSounds.RITE_HEART.get();
            case GATE_OPEN -> NarutoSounds.RITE_GATE.get();
            case GATE_DEATH -> NarutoSounds.RITE_END.get();
        };
    }
}
