package com.sekwah.narutomod.events;

import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.ShadowCloneCloudPacket;
import com.sekwah.narutomod.network.s2c.ShadowCloneCloudPacket.Beat;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.ShadowCloneFormation.Profile;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class ShadowCloneEffects {
    private static final Map<ServerLevel, AudioWindow> DISPEL_AUDIO = new WeakHashMap<>();

    private ShadowCloneEffects() {
    }

    public static void prepare(ServerPlayer actor, Profile profile) {
        ServerLevel level = actor.serverLevel();
        Beat beat = profile == Profile.MASS ? Beat.MASS_SEAL : Beat.SEAL;
        emit(level, beat, actor.position(), List.of(actor.position()));
        play(level, actor.position(), NarutoSounds.CLONE_SEAL.get(), profile == Profile.MASS ? 0.72f : 0.49f);
    }

    public static void wave(ServerPlayer actor, Profile profile, List<Vec3> sites) {
        ServerLevel level = actor.serverLevel();
        Beat beat = profile == Profile.MASS ? Beat.MASS_FORM : Beat.FORM;
        Vec3 center = sites.stream().reduce(Vec3.ZERO, Vec3::add).scale(1.0 / sites.size());
        emit(level, beat, center, sites);
        play(level, center, NarutoSounds.CLONE_FORM.get(), profile == Profile.MASS ? 0.54f : 0.62f);
    }

    public static void dispel(ServerLevel level, ShadowCloneEntity clone) {
        Vec3 at = clone.position();
        emit(level, Beat.DISPEL, at, List.of(at));
        AudioWindow window = DISPEL_AUDIO.get(level);
        long period = level.getGameTime() / 4;
        if (window == null || window.period() != period) {
            window = new AudioWindow(period, new HashSet<>());
            DISPEL_AUDIO.put(level, window);
        }
        if (window.cells().add(SectionPos.asLong(clone.blockPosition()))) {
            play(level, at, NarutoSounds.CLONE_DISPEL.get(), 0.49f);
        }
    }

    private static void emit(ServerLevel level, Beat beat, Vec3 center, List<Vec3> sites) {
        PacketHandler.sendNear(new ShadowCloneCloudPacket(level.dimension().location(), beat, sites), level, center, 48);
    }

    private static void play(ServerLevel level, Vec3 at, SoundEvent sound, float volume) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, 1.0f);
    }

    private record AudioWindow(long period, Set<Long> cells) {
    }
}
