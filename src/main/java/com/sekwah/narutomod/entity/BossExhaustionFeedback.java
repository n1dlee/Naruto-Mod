package com.sekwah.narutomod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;

final class BossExhaustionFeedback {
    private BossExhaustionFeedback() {
    }

    static void emit(LivingEntity actor) {
        if (actor.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, actor.getX(), actor.getY() + actor.getBbHeight() * .7,
                    actor.getZ(), 10, actor.getBbWidth() * .25, .18, actor.getBbWidth() * .25, .03);
            level.playSound(null, actor.blockPosition(), SoundEvents.PLAYER_BREATH, actor.getSoundSource(), 1.1f, .75f);
        }
    }
}
