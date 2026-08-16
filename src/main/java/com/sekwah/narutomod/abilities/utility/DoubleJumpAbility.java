package com.sekwah.narutomod.abilities.utility;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.gameevents.NarutoGameEvents;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class DoubleJumpAbility extends Ability {

    /** Exempt from the free-hands gate: this is a jump, not a hand-cast technique. */
    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    public static final float CHAKRA_COST = 2f;
    public static final float STAMINA_COST = 5f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public boolean logInChat() {
        return false;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public SoundEvent castingFailSound() {
        return null;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        return ninjaData.getDoubleJumpData().canDoubleJumpServer;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        ninjaData.useChakra(CHAKRA_COST, 30);
        ninjaData.useStamina(STAMINA_COST, 40);

        if(player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    player.getX(),
                    player.getY() + 0.1f,
                    player.getZ(),
                    35,
                    0, 0, 0, 0.6F);
        }
        ninjaData.getDoubleJumpData().canDoubleJumpServer = false;
        player.fallDistance = 0;
        float jumpBoost = 0.42f + ninjaData.getNinjaRank() * 0.08f;
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, Math.max(motion.y, 0.0D) + jumpBoost, motion.z);

        player.level().playSound(null,
                player, NarutoSounds.DOUBLE_JUMP.get(), SoundSource.PLAYERS, 1f, 1.0f);

        player.level().gameEvent(player, NarutoGameEvents.DOUBLE_JUMP.get(), player.position());
    }
}
