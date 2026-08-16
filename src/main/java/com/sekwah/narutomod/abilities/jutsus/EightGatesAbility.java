package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * Eight Gates (Hachimon Tonkou) — combo 3131, INSTANT.
 * Each use opens the next gate (1→8).
 * Effects are applied in NinjaData.updateEightGates().
 * Cost: 15 chakra per gate. Cooldown: 30 seconds after all gates close.
 */
public class EightGatesAbility extends Ability {

    /** Exempt from the free-hands gate: this is a body state, not a hand seal, not a hand-cast technique. */
    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    private static final float CHAKRA_PER_GATE = 20f;
    private static final String[] GATE_NAMES = {
        "Gate of Opening", "Gate of Healing", "Gate of Life", "Gate of Pain",
        "Gate of Limit", "Gate of View", "Gate of Wonder", "Gate of Death"
    };

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 3131;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        int currentGates = ninjaData.getGatesOpen();

        // Sneak + the combo stands down, releasing every gate at once. Without this the only
        // way out of an open gate was to wait out the timer, so a fight that ended early left
        // you bleeding stamina with nothing to spend it on. The eighth is excluded on purpose.
        if (player.isShiftKeyDown() && currentGates > 0 && currentGates < 8) {
            ninjaData.setGatesOpen(0);
            ninjaData.setGatesTicks(0);
            player.displayClientMessage(Component.literal("Gates closed.")
                    .withStyle(ChatFormatting.GREEN), true);
            return false;
        }

        if (currentGates >= 8) {
            // No toggle, no take-backs. Once the eighth is open the only way out is the
            // timer, and the timer ends in a corpse.
            player.displayClientMessage(Component.literal("The Gate of Death cannot be closed.")
                    .withStyle(ChatFormatting.DARK_RED), true);
            return false;
        }

        if (ninjaData.getChakra() < CHAKRA_PER_GATE) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_PER_GATE, 10);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        int nextGate = ninjaData.getGatesOpen() + 1;
        ninjaData.setGatesOpen(nextGate);
        ninjaData.setGatesTicks(nextGate == 8 ? 15 * 20 : 30 * 20); // Reset timer

        String gateName = GATE_NAMES[nextGate - 1];
        ChatFormatting color = nextGate <= 4 ? ChatFormatting.GREEN :
                               nextGate <= 7 ? ChatFormatting.RED : ChatFormatting.DARK_RED;

        player.displayClientMessage(
                Component.literal(gateName + " - OPEN! (Gate " + nextGate + "/8)")
                        .withStyle(color), true);

        // Escalating gate-open sound — mirrors the GREEN->RED->BLACK particle escalation below
        var gateSound = nextGate <= 4 ? SoundEvents.BEACON_POWER_SELECT
                : nextGate <= 7 ? SoundEvents.ANVIL_LAND
                : SoundEvents.WARDEN_SONIC_BOOM;
        float gatePitch = 1.2f - nextGate * 0.08f;
        player.level().playSound(null, player, gateSound, SoundSource.PLAYERS, 1.0f, gatePitch);

        // Chakra pressure shockwave ring — color escalates green -> red -> black as gates open
        if (player.level() instanceof ServerLevel serverLevel) {
            ParticleOptions ringParticle = nextGate <= 4 ? NarutoParticles.GATE_GREEN
                    : nextGate <= 7 ? NarutoParticles.GATE_RED
                    : NarutoParticles.GATE_BLACK;
            double baseY = player.getY() + 0.1;
            for (int ring = 0; ring < 3; ring++) {
                double radius = 0.6 + ring * (0.5 + nextGate * 0.15);
                NarutoParticles.spawnRing(serverLevel, player.position().add(0, baseY - player.getY() + ring * 0.5, 0),
                        radius, 12 + nextGate * 2, ringParticle);
            }
            NarutoParticles.spawnBurst(serverLevel, player.position().add(0, player.getBbHeight() * 0.5, 0),
                    10 + nextGate * 3, 0.5, ringParticle);
        }
    }
}
