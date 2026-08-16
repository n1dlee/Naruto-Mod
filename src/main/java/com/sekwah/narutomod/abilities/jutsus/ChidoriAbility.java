package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class ChidoriAbility extends Ability implements Ability.Cooldown {

    private static final float BASE_COST = 60.0F;
    /**
     * How long the lightning stays in the hand.
     *
     * Eight seconds made this a melee buff you switched on and then fought normally under.
     * Chidori is a single committed run at someone: you raise it, you go, and if you miss you
     * have spent it. Three seconds is long enough to close a realistic gap and short enough
     * that holding it is a decision rather than a state.
     */
    private static final int ACTIVE_TICKS = 3 * 20;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 22; // V V — activates Chidori buff mode
    }

    @Override
    public int getCooldown() {
        return 8 * 20;
    }
    // --- Phase 15: Nature Release ---
    @Override
    public String element() {
        return "lightning";
    }

    @Override
    public int elementLevelRequired() {
        return 6;
    }

    @Override
    public float elementXpReward() {
        return 25f;
    }


    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        // Phase 15: lightning-nature mastery gates this now (was Uchiha + Sharingan only)
        if (ninjaData.getChakra() < BASE_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(BASE_COST, 20);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        ninjaData.setChidoriTicks(ACTIVE_TICKS);
        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 pos = player.position().add(0.0D, player.getBbHeight() * 0.65D, 0.0D);
            serverLevel.sendParticles(NarutoParticles.CHIDORI_CYAN, pos.x, pos.y, pos.z, 16, 0.35D, 0.25D, 0.35D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 18, 0.45D, 0.35D, 0.45D, 0.08D);

            // Branching lightning: short jittered arcs radiating off the activation point
            for (int branch = 0; branch < 6; branch++) {
                double angle = player.getRandom().nextDouble() * Math.PI * 2;
                double dist = 0.3 + player.getRandom().nextDouble() * 0.6;
                double bx = pos.x + Math.cos(angle) * dist;
                double by = pos.y + (player.getRandom().nextDouble() - 0.5) * 0.6;
                double bz = pos.z + Math.sin(angle) * dist;
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, bx, by, bz, 3, 0.1, 0.1, 0.1, 0.02);
                serverLevel.sendParticles(NarutoParticles.CHIDORI_CYAN, bx, by, bz, 2, 0.08, 0.08, 0.08, 0.0);
            }
        }
    }

    @Override
    public SoundEvent castingSound() {
        return NarutoSounds.CHIDORI.get();
    }
}
