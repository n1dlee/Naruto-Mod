package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.util.EyeTargeting;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Explosion Release: Detonating Clay - Deidara's sculpted charge.
 *
 * Thrown at whatever the caster is looking at, sticks for a beat, then goes off. The delay
 * is the whole character of the technique: it is art, it is meant to be watched, and it
 * gives the target one second to get clear.
 *
 * Terrain damage follows the same config switch the paper bomb already uses, so a player
 * who has turned off explosive griefing gets that answer here too.
 */
public class ClayBombAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 75f;
    private static final double RANGE = 28.0;
    private static final int FUSE_TICKS = 20;
    private static final float BLAST_RADIUS = 3.5f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 2321;
    }

    @Override
    public String element() {
        return "earth";
    }

    @Override
    public int elementLevelRequired() {
        return 8;
    }

    @Override
    public String secondaryElement() {
        return "fire";
    }

    @Override
    public int secondaryElementLevelRequired() {
        return 8;
    }

    @Override
    public int getCooldown() {
        return 12 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.SLIME_SQUISH;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 40);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        LivingEntity target = EyeTargeting.raycastLiving(player, RANGE);
        // Lands on the target if there is one, otherwise wherever the caster was aiming.
        Vec3 impact = target != null
                ? target.position().add(0, target.getBbHeight() * 0.5, 0)
                : player.getEyePosition().add(player.getLookAngle().scale(RANGE * 0.5));

        if (player.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel, impact, 30, 0.5, NarutoParticles.CLAY_GREY);
            serverLevel.playSound(null, impact.x, impact.y, impact.z, SoundEvents.SLIME_ATTACK,
                    SoundSource.PLAYERS, 1.2f, 0.7f);
        }
        ninjaData.scheduleDelayedTickEvent(p -> detonate(p, impact), FUSE_TICKS);
    }

    private void detonate(Player player, Vec3 at) {
        Level level = player.level();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0, 0, 0, 0.0);
        }
        level.explode(player, at.x, at.y, at.z, BLAST_RADIUS,
                NarutoConfig.paperbombBlockDamage
                        ? Level.ExplosionInteraction.TNT
                        : Level.ExplosionInteraction.NONE);
    }
}
